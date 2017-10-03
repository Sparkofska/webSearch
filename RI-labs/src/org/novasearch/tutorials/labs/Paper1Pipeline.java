package org.novasearch.tutorials.labs;

import java.io.File;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.search.similarities.Similarity;

public class Paper1Pipeline {

	private static final String INDEX_PATH = ""; // TODO
	private static final String DOCUMENTS_PATH = ""; // TODO
	private static final String QUERIES_PATH = ""; // TODO
	private static final String SEARCH_RESULTS_PATH = ""; // TODO
	private static final String GROUND_TRUTH_PATH = ""; // TODO

	public static void main(String[] args) {
		Paper1Pipeline pipeline = new Paper1Pipeline();
		pipeline.run();
		debugPrintln("terminated");
	}

	private static void debugPrintln(String msg) {
		System.out.println(msg);
	}

	public void run() {

		// get custom Analyzers
		Analyzer analyzer = getAnalyzer();
		Similarity similarity = getSimilarity();

		// Create the Index from given Documents
		IndexWriter idx = createOrOpenIndex(INDEX_PATH);
		indexDocuments(idx, DOCUMENTS_PATH);
		closeIndex(idx);

		// perform the search with given queries
		File my_results = searchIndex(analyzer, similarity, QUERIES_PATH, SEARCH_RESULTS_PATH);

		// evaluate the results of the search
		doEvaluation(my_results, SEARCH_RESULTS_PATH, GROUND_TRUTH_PATH);
	}

	private Analyzer getAnalyzer() {
		// TODO Auto-generated method stub
		return null;
	}

	private Similarity getSimilarity() {
		// TODO Auto-generated method stub
		return null;
	}

	private IndexWriter createOrOpenIndex(String indexPath) {
		// TODO Auto-generated method stub
		return null;
	}

	private void indexDocuments(IndexWriter idx, String docPath) {
		// TODO Auto-generated method stub

	}

	private void closeIndex(IndexWriter idx) {
		// TODO Auto-generated method stub

	}

	private File searchIndex(Analyzer analyzer, Similarity similarity, String queriesPath, String searchResultsPath) {
		// TODO Auto-generated method stub
		return null;
	}

	private void doEvaluation(File my_results, String searchResultsPath, String groundTruthPath) {
		// TODO run trec_eval
		// TODO run plot script
	}
}
