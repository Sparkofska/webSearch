package org.novasearch.tutorials.labs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
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

	@Override
	protected File searchIndex(Analyzer analyzer, Similarity similarity, String indexPath, String queriesPath,
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

                Map<String, Integer> expansionTerms = getExpansionTerms(searcher, queryString, 100, analyzer, similarity);

                for (Map.Entry<String, Integer> term: expansionTerms.entrySet()) {
                    // This is the minimum frequency
                    if (term.getValue() >= 20)
                        System.out.println( term.getKey() + " -> " + term.getValue() + " times");
                }

                // Implement the query expansion by selecting terms from the expansionTerms

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

    public Map<String, Integer>  getExpansionTerms(IndexSearcher searcher, String queryString, int numExpDocs, Analyzer analyzer, Similarity similarity) {

        Map<String, Integer> topTerms = new HashMap<String, Integer>();

        try {
            QueryParser parser = new QueryParser("Body", analyzer);
            Query query;
            try {
                query = parser.parse(queryString);
            } catch (org.apache.lucene.queryparser.classic.ParseException e) {
                System.out.println("Error parsing query string.");
                return null;
            }

            TopDocs results = searcher.search(query, numExpDocs);
            ScoreDoc[] hits = results.scoreDocs;

            int numTotalHits = results.totalHits;
            System.out.println(numTotalHits + " total matching documents");

            for (int j = 0; j < hits.length; j++) {
                Document doc = searcher.doc(hits[j].doc);
                String answer = doc.get("Body");
                Integer AnswerId = doc.getField("AnswerId").numericValue().intValue();

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
                } finally {
                    stream.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return topTerms;
    }

	
	//@Override
	protected File searchIndex1(Analyzer analyzer, Similarity similarity, String indexPath, String queriesPath,
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

				debugPrintln("Your query: " + line);

				// parse query id
				Integer qid = Integer.parseInt(line.substring(0, line.indexOf(":")));

				Query query;
				try {
					query = parser.parse(line);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					debugPrintln("Error parsing query string.");
					continue;
				}

				TopDocs results = searcher.search(query, numHits);
				ScoreDoc[] hits = results.scoreDocs;

				// TODO get top words from results
				

				// TODO filter top words (a lot of them are garbage)
				// TODO - by adding the unwanted ones to a stopword-List
				// TODO - by getting the k top words from the p most relevant
				// documents AND the top words from the least relevant
				// documents. Remove the intersection (words that occur in every
				// document) from the top words

				// TODO add the expansion (filtered top words) to the query
				// (with a weight: query + expansion^alpha , alpha < 1)
				// - to do the weight: either create a according query string, or use the API (BooleanQueries...) and add several terms giving a weight

				// TODO search again with improved query

				// TODO return search results

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
	
}
