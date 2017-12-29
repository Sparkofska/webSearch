package org.novasearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class Parameter
{

	public static final String PATH_TWEETS = "res/tweets/rts2016-qrels-tweets2016.jsonl";
	public static final String PATH_PROFILES = "res/profiles/TREC2016-RTS-topics.json";
	public static final String PATH_INDEX = "index/";
	public static final int N_SEARCH_RESULTS = 50; // TODO tune

	public static Analyzer getAnalyzer()
	{
		return new StandardAnalyzer();
		// TODO implement actual Analyzer
		/*
		 * return new Analyzer() {
		 * 
		 * @Override protected TokenStreamComponents createComponents(String
		 * arg0) { // TODO Auto-generated method stub return null; }
		 * 
		 * };
		 */
	}

	public static Similarity getSimilarity()
	{
		// TODO tune that parameter
		return new ClassicSimilarity();
	}

	public static String getIndexPath(String date)
	{
		return PATH_INDEX + date + "/";
	}

	public static Analyzer getTopWordsAnalyzer()
	{
		// TODO build a nice analyzer
		return new StandardAnalyzer();
	}

	public static Clusterer getClusterer()
	{
		// return null;
		return new MinHashClusterer();
	}

	public static boolean isQueryExpansionEnabled()
	{
		return true;
	}
}
