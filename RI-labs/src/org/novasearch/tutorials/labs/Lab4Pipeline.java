package org.novasearch.tutorials.labs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class Lab4Pipeline extends Paper1Pipeline {

	public static void main(String[] args) {
		Lab4Pipeline pipeline = new Lab4Pipeline();
		pipeline.run();
		debugPrintln("terminated");
	}

	// @Override
	protected File searchIndex1(Analyzer analyzer, Similarity similarity, String indexPath, String queriesPath,
			String searchResultsPath, int numHits) {

		IndexReader reader = null;
		IndexSearcher searcher = null;
		try {
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));

			QueryParser parser = new QueryParser("Body", analyzer);
			while (true) {
				System.out.println("Enter query: ");

				String queryString = in.readLine();

				if (queryString == null || queryString.length() == -1) {
					break;
				}

				queryString = queryString.trim();
				if (queryString.length() == 0) {
					break;
				}

				String expandedQuery = QueryExpansion.expandByPseudoRelevanceFeedback(searcher, queryString, analyzer,
						similarity);

				System.out.println("Original Query: >> " + queryString + " <<");
				System.out.println("Expanded Query: >> " + expandedQuery + " <<");

				if (queryString.equals("")) {
					break;
				}
			}
			reader.close();
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		}

		return null;
	}

	@Override
	protected File searchIndex(Analyzer analyzer, Similarity similarity, String indexPath, String queriesPath,
			String searchResultsPath, int numHits) {

		IndexReader reader = null;
		BufferedWriter writer = null;
		BufferedReader in = null;

		try {
			// This reader reads the index file
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			// This writer writes the search results to a file
			File resultsFile = new File(searchResultsPath);
			writer = new BufferedWriter(new FileWriter(resultsFile));
			writer.write("QueryID\tQ0\tDocID\tRank\tScore\tRunID\n");

			// This reader parses from queries-file
			in = new BufferedReader(new FileReader(queriesPath));

			QueryParser parser = new QueryParser("Body", analyzer);
			while (true) {
				// System.out.println("Enter query: ");

				String line = in.readLine();

				if (line == null || line.length() == -1) {
					break;
				}

				line = line.trim();
				if (line.length() == 0) {
					break;
				}

				// parse query id
				Integer qid = Integer.parseInt(line.substring(0, line.indexOf(":")));

				String expandedQuery = QueryExpansion.expandByPseudoRelevanceFeedback(searcher, line, analyzer,
						similarity);
				debugPrintln("Your query   : " + line);
				debugPrintln("expandedQuery: " + expandedQuery);

				Query query;
				try {
					// query = parser.parse(line);
					query = parser.parse(expandedQuery);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					debugPrintln("Error parsing query string.");
					continue;
				}

				TopDocs results = searcher.search(query, numHits);
				ScoreDoc[] hits = results.scoreDocs;

				for (int j = 0; j < hits.length; j++) {
					Document doc = searcher.doc(hits[j].doc);
					Integer Id = doc.getField("AnswerId").numericValue().intValue();
					writer.write(qid + "\tQ0\t" + Id + "\t" + (j + 1) + "\t" + hits[j].score + "\trun1\n");

				}

				if (line.equals("")) {
					break;
				}
			}
			return resultsFile;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
				if (writer != null)
					writer.close();
				if (in != null)
					in.close();

			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return null;

	}

	public class QueryParserAnalyzer extends Analyzer {
		private List<String> stopWords = Arrays.asList("a", "i", "the", "r", "with", "have", "and", "using", "are",
				"has", "also", "you", "as", "be", "do", "for", "if", "in", "is", "it", "use", "that", "of", "on", "or",
				"to", "this", "can");
		private CharArraySet stopSet = new CharArraySet(stopWords, true);
		/** Default maximum allowed token length */
		private int maxTokenLength = 25;

		@Override
		protected TokenStreamComponents createComponents(String arg0) {
			final StandardTokenizer src = new StandardTokenizer();
			TokenStream tok = null;
			tok = new StandardFilter(src); // text into non punctuated text
			tok = new StopFilter(tok, stopSet); // removes stop words

			return new TokenStreamComponents(src, tok) {
				@Override
				protected void setReader(final Reader reader) {
					src.setMaxTokenLength(QueryParserAnalyzer.this.maxTokenLength);
				}
			};
		}

		@Override
		protected TokenStream normalize(String fieldName, TokenStream in) {
			TokenStream result = new StandardFilter(in);
			result = new LowerCaseFilter(result);
			return result;
		}
	}
}
