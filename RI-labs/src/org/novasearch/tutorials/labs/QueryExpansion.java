package org.novasearch.tutorials.labs;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;

public class QueryExpansion {

	private static final int NUMBER_OF_DOCS_FIRST_SEARCH = 1000;
	private static final int N_NUMBER_OF_RELEVANT_DOCS = 51;
	private static final int K_NUMBER_OF_DOCS_FOR_TOP_TERMS = 50;
	private static final int TOP_TERM_MINIMUM_FREQUENCY = 1;
	private static final int MAX_EXPANDED_QUERY_LENGTH = 30;
	private static final Double WEIGHT_OF_QUERY_EXPANSION = 0.5;

	public static String expandCorpusBased(IndexSearcher searcher, final String queryString, Analyzer analyzer,
			Similarity similarity) {
		// TODO We should actually implement this approach acoording to Lab4
		return queryString;
	}

	public static String expandByPseudoRelevanceFeedback(IndexSearcher searcher, final String queryString,
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
			results = searcher.search(query, NUMBER_OF_DOCS_FIRST_SEARCH);
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

		return expandQuery(queryString, topTerms);
	}

	private static Map<String, Integer> getTopWords(Document[] docs, Analyzer analyzer) {

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

	private static Map<String, Integer> removeStopwordsByIntersection(Map<String, Integer> allTerms,
			Map<String, Integer> topWordsFromTopKDocs, Map<String, Integer> topWordsFromLeastKDocs) {

		Map<String, Integer> topTerms = new HashMap<String, Integer>();
		Map<String, Integer> discardedTerms = new HashMap<String, Integer>(); // only
																				// for
																				// debugOutput

		for (Entry<String, Integer> term : topWordsFromTopKDocs.entrySet()) {
			if (topWordsFromLeastKDocs.get(term.getKey()) != null) {
				double ratio = (double) term.getValue() / (double) topWordsFromLeastKDocs.get(term.getKey());
				if (ratio < 1.5) {
					discardedTerms.put(term.getKey(), term.getValue());
					continue;
				}
			}
			topTerms.put(term.getKey(), term.getValue());
		}

		debugPrintTopWords(discardedTerms, "DiscardedTerms", 0);

		return topTerms;
	}

	private static String expandQuery(String origQuery, Map<String, Integer> expansionTerms) {

		StringBuilder expandedQuery = new StringBuilder();
//		expandedQuery.append("(");
		expandedQuery.append(origQuery);
//		expandedQuery.append(")^");
//		expandedQuery.append(1.0);
		expandedQuery.append(" (");

		// Sort the expansionTerms by frequency of terms
		ArrayList<Map.Entry<String, Integer>> expansionTermsSorted = new ArrayList<Map.Entry<String, Integer>>(
				expansionTerms.entrySet());
		Collections.sort(expansionTermsSorted, new TermFrequencyComparator());

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

	private static void debugPrintTopWords(Map<String, Integer> topTerms, String header, int minFreq) {
		if (false) {

			List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(topTerms.entrySet());
			Collections.sort(entries, new TermFrequencyComparator());

			System.out.println("\n\n --- \n " + header + " \n");
			for (Map.Entry<String, Integer> term : entries) {
				// This is the minimum frequency
				if (term.getValue() >= TOP_TERM_MINIMUM_FREQUENCY)
					System.out.println(term.getKey() + " -> " + term.getValue() + " times");
			}
		}
	}

	private static class TermFrequencyComparator implements Comparator<Map.Entry<String, Integer>> {

		@Override
		public int compare(Entry<String, Integer> a, Entry<String, Integer> b) {
			return Integer.compare(b.getValue(), a.getValue());
		}

	}
}
