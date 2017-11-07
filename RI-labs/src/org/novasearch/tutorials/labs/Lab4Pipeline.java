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

	private static final int K_NUMBER_OF_DOCS_FOR_TOP_TERMS = 30;
	private static final int N_NUMBER_OF_RELEVANT_DOCS = 900;
	private static final int TOP_TERM_MINIMUM_FREQUENCY = 6;
	private static final int MAX_EXPANDED_QUERY_LENGTH = 20;
	private static final String WEIGHT_OF_QUERY_EXPANSION = "0.5";

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

				Map<String, Integer> expansionTerms = getExpansionTerms(searcher, queryString,
						N_NUMBER_OF_RELEVANT_DOCS, analyzer, similarity);

				// Do the query expansion by selecting terms from the
				// expansionTerms
				String expandedQuery = expandQuery(queryString, expansionTerms);

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

	private String expandQuery(String origQuery, Map<String, Integer> expansionTerms) {

		StringBuilder expandedQuery = new StringBuilder();
		expandedQuery.append(origQuery);
		expandedQuery.append(" (");

		// Sort the expansionTerms by frequency of terms
		ArrayList<Map.Entry<String, Integer>> expansionTermsSorted = new ArrayList<Map.Entry<String, Integer>>(
				expansionTerms.entrySet());
		Collections.sort(expansionTermsSorted, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
				return Integer.compare(b.getValue(), a.getValue());
			}
		});

		// use this for filling up the query to a certain length
		// int nTermsToAdd = Math.min(MAX_EXPANDED_QUERY_LENGTH -
		// origQuery.split(" ").length, expansionTermsSorted.size());
		
		// use this to add a certain number of terms
		int nTermsToAdd = Math.min(MAX_EXPANDED_QUERY_LENGTH, expansionTermsSorted.size());
		
		for (int i = 0; i < nTermsToAdd; i++) {
			expandedQuery.append(expansionTermsSorted.get(i).getKey() + " ");
		}

		expandedQuery.append(")^");
		expandedQuery.append(WEIGHT_OF_QUERY_EXPANSION);

		return expandedQuery.toString();
	}

	public Map<String, Integer> getExpansionTerms(IndexSearcher searcher, String queryString, int numExpDocs,
			Analyzer analyzer, Similarity similarity) {

		QueryParser parser = new QueryParser("Body", analyzer);
		Query query;
		try {
			query = parser.parse(queryString);
		} catch (org.apache.lucene.queryparser.classic.ParseException e) {
			System.out.println("Error parsing query string.");
			return null;
		}

		TopDocs results = null;
		try {
			results = searcher.search(query, numExpDocs);
		} catch (IOException e1) {
			e1.printStackTrace();
			return null;
		}
		ScoreDoc[] hits = results.scoreDocs;

		int numTotalHits = results.totalHits;
		System.out.println(numTotalHits + " total matching documents; using the top " + K_NUMBER_OF_DOCS_FOR_TOP_TERMS
				+ " for queryExpansion.");

		int N = Math.min(numTotalHits, N_NUMBER_OF_RELEVANT_DOCS);
		int k = Math.min(N, K_NUMBER_OF_DOCS_FOR_TOP_TERMS);

		Document[] allDocs = new Document[N];
		Document[] topKDocs = new Document[k];
		Document[] leastKDocs = new Document[k];
		try {
			for (int i = 0; i < N; i++) {
				// fill all docs
				allDocs[i] = searcher.doc(hits[i].doc);
			}
			for (int i = 0; i < k; i++) {
				// fill the top k docs
				topKDocs[i] = searcher.doc(hits[i].doc);

				// fill the least k docs
				leastKDocs[i] = searcher.doc(hits[N - k + i].doc);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, Integer> topWordsFromTopKDocs = getTopWords(topKDocs, analyzer);
		Map<String, Integer> topWordsFromLeastKDocs = getTopWords(leastKDocs, analyzer);
		Map<String, Integer> allTerms = getTopWords(allDocs, analyzer);

		Map<String, Integer> topTerms = removeStopwordsByIntersection(allTerms, topWordsFromTopKDocs,
				topWordsFromLeastKDocs);

		debugPrintTopWords(topWordsFromTopKDocs, "topWordsFromTopKDocs", TOP_TERM_MINIMUM_FREQUENCY);
		debugPrintTopWords(topWordsFromLeastKDocs, "topWordsFromLeastKDocs", TOP_TERM_MINIMUM_FREQUENCY);
		debugPrintTopWords(topTerms, "ExpansionTerms", TOP_TERM_MINIMUM_FREQUENCY);

		return topTerms;
	}

	private Map<String, Integer> removeStopwordsByIntersection(Map<String, Integer> allTerms,
			Map<String, Integer> topWordsFromTopKDocs, Map<String, Integer> topWordsFromLeastKDocs) {

		Map<String, Integer> topTerms = new HashMap<String, Integer>();
		Map<String, Integer> discardedTerms = new HashMap<String, Integer>(); // only
																				// for
																				// debugOutput
		
		for(Entry<String, Integer> term : topWordsFromTopKDocs.entrySet())
		{
			if(topWordsFromLeastKDocs.get(term.getKey()) != null)
			{
				double ratio = (double) term.getValue() / (double) topWordsFromLeastKDocs.get(term.getKey());
				if(ratio < 1.5)
				{
					discardedTerms.put(term.getKey(), term.getValue());
					continue;
				}				
			}
			topTerms.put(term.getKey(), term.getValue());
		}
		
		
		debugPrintTopWords(discardedTerms, "DiscardedTerms", 0);

		return topTerms;
	}

	public Map<String, Integer> getTopWords(Document[] docs, Analyzer analyzer) {

		Map<String, Integer> topTerms = new HashMap<String, Integer>();

		for (Document doc : docs) {
			String answer = doc.get("Body");
			TokenStream stream = analyzer.tokenStream("field", new StringReader(answer));
			// get the CharTermAttribute from the TokenStream
			CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

			try {
				stream.reset();

				// print all tokens until stream is exhausted
				while (stream.incrementToken()) {
					String term = termAtt.toString();
					Integer termCount = topTerms.get(term);
					if (termCount == null)
						topTerms.put(term, 1);
					else
						topTerms.put(term, ++termCount);
				}

				stream.end();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
				try {
					stream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return topTerms;
	}

	private void debugPrintTopWords(Map<String, Integer> topTerms, String header, int minFreq) {
		if (false) {
			List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(topTerms.entrySet());
			Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
				public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
					return Integer.compare(b.getValue(), a.getValue());
				}
			});
			System.out.println("\n\n --- \n " + header + " \n");
			for (Map.Entry<String, Integer> term : entries) {
				// This is the minimum frequency
				if (term.getValue() >= TOP_TERM_MINIMUM_FREQUENCY)
					System.out.println(term.getKey() + " -> " + term.getValue() + " times");
			}
		}
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

				String expandedQuery = expandQuery(line,
						getExpansionTerms(searcher, line, N_NUMBER_OF_RELEVANT_DOCS, analyzer, similarity));
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
