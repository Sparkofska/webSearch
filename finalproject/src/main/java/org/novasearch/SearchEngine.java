package org.novasearch;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Paths;
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
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class SearchEngine
{
	private static final int NUMBER_OF_DOCS_FIRST_SEARCH = 100;
	private static final int N_NUMBER_OF_RELEVANT_DOCS = 21;
	private static final int K_NUMBER_OF_DOCS_FOR_TOP_TERMS = 20;
	private static final int TOP_TERM_MINIMUM_FREQUENCY = 0;
	private static final int MAX_EXPANDED_QUERY_LENGTH = 30;
	private static final Double WEIGHT_OF_QUERY_EXPANSION = 0.5;

	private IndexSearcher searcher;

	public IndexSearcher getSearcher()
	{
		return searcher;
	}

	public SearchEngine(String indexPath, Similarity similarity) throws IOException
	{
		IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));

		this.searcher = new IndexSearcher(reader);
		searcher.setSimilarity(similarity);
	}

	public Query toQuery(Profile profile, Analyzer analyzer)
	{
		// TODO tune this stuff
		StringBuilder queryBuilder = new StringBuilder();
		queryBuilder.append("text:");
		queryBuilder.append("\"");
		queryBuilder.append(profile.getTitle());
		queryBuilder.append("\"");

		queryBuilder.append(" OR ");

		queryBuilder.append("hashtags:");
		queryBuilder.append("\"");
		queryBuilder.append(profile.getTitle());
		queryBuilder.append("\"");

		String query = queryBuilder.toString();
		// System.out.println(query);

		// <default field> is the field that QueryParser will search if you
		// don't prefix it with a field.
		QueryParser queryParser = new QueryParser("<default field>", analyzer);

		try
		{
			return queryParser.parse(query);
		}
		catch (ParseException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Query toQuery(String s, Analyzer analyzer) throws ParseException
	{
		QueryParser queryParser = new QueryParser("text", analyzer);
		return queryParser.parse("\"" + s + "\"");
	}

	public Query expandQueryByPseudoRelevanceFeedback(String origQueryString, Query origQuery) throws IOException
	{
		List<ScoredDocument> origScores = search(origQuery, NUMBER_OF_DOCS_FIRST_SEARCH);

		int N = Math.min(origScores.size(), N_NUMBER_OF_RELEVANT_DOCS);
		int k = Math.min(N, K_NUMBER_OF_DOCS_FOR_TOP_TERMS);

		List<ScoredDocument> topKDocs = new ArrayList<ScoredDocument>(k);
		List<ScoredDocument> leastKDocs = new ArrayList<ScoredDocument>(k);
		for (int i = 0; i < k; i++)
		{
			// fill the top k docs
			topKDocs.add(origScores.get(i));

			// fill the least k docs
			leastKDocs.add(origScores.get(N - k + i));
		}

		Analyzer topWordAnalyzer = Parameter.getAnalyzer();
		Map<String, Integer> allTerms = getTopWords(topWordAnalyzer, origScores);
		Map<String, Integer> topWordsFromTopKDocs = getTopWords(topWordAnalyzer, topKDocs);
		Map<String, Integer> topWordsFromLeastKDocs = getTopWords(topWordAnalyzer, leastKDocs);

		Map<String, Integer> topTerms = removeStopwordsByIntersection(allTerms, topWordsFromTopKDocs,
				topWordsFromLeastKDocs);

		return expandQuery(origQuery, topTerms, topWordAnalyzer);
	}

	private Query expandQuery(Query origQuery, Map<String, Integer> topTerms, Analyzer analyzer)
	{

		ArrayList<Map.Entry<String, Integer>> expansionTermsSorted = new ArrayList<Map.Entry<String, Integer>>(
				topTerms.entrySet());
		Collections.sort(expansionTermsSorted, new TermFrequencyComparator());
		int nTermsToAdd = Math.min(MAX_EXPANDED_QUERY_LENGTH, expansionTermsSorted.size());
		if (nTermsToAdd <= 0)
			return origQuery;

		StringBuilder sb = new StringBuilder();
		sb.append("(");

		for (int i = 0; i < nTermsToAdd; i++)
		{
			sb.append(expansionTermsSorted.get(i).getKey() + " ");
		}

		sb.append(")^");
		sb.append(WEIGHT_OF_QUERY_EXPANSION);

		Query expansion;
		try
		{
			expansion = new QueryParser("text", analyzer).parse(sb.toString());
		}
		catch (ParseException e)
		{
			e.printStackTrace();
			return null;
		}

		BooleanQuery.Builder querybuilder = new BooleanQuery.Builder();
		querybuilder.add(new BooleanClause(origQuery, Occur.MUST));
		querybuilder.add(new BooleanClause(expansion, Occur.SHOULD));
		return querybuilder.build();
	}

	private static Map<String, Integer> getTopWords(Analyzer analyzer, List<ScoredDocument> docs)
	{

		Map<String, Integer> topTerms = new HashMap<String, Integer>();

		for (ScoredDocument doc : docs)
		{
			// TODO put the correct "Body"
			String answer = doc.getDocument().get("text");
			TokenStream stream = analyzer.tokenStream("field", new StringReader(answer));
			// get the CharTermAttribute from the TokenStream
			CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

			try
			{
				stream.reset();

				// print all tokens until stream is exhausted
				while (stream.incrementToken())
				{
					String term = termAtt.toString();
					Integer termCount = topTerms.get(term);
					if (termCount == null)
						topTerms.put(term, 1);
					else
						topTerms.put(term, ++termCount);
				}

				stream.end();
			}
			catch (IOException e)
			{
				e.printStackTrace();
			}
			finally
			{
				try
				{
					stream.close();
				}
				catch (IOException e)
				{
					e.printStackTrace();
				}
			}
		}

		return topTerms;
	}

	private static Map<String, Integer> removeStopwordsByIntersection(Map<String, Integer> allTerms,
			Map<String, Integer> topWordsFromTopKDocs, Map<String, Integer> topWordsFromLeastKDocs)
	{

		Map<String, Integer> topTerms = new HashMap<String, Integer>();
		// only for debugOutput
		Map<String, Integer> discardedTerms = new HashMap<String, Integer>();

		for (Entry<String, Integer> term : topWordsFromTopKDocs.entrySet())
		{
			if (topWordsFromLeastKDocs.get(term.getKey()) != null)
			{
				double ratio = (double) term.getValue() / (double) topWordsFromLeastKDocs.get(term.getKey());
				if (ratio < 1.5)
				{
					discardedTerms.put(term.getKey(), term.getValue());
					continue;
				}
			}
			topTerms.put(term.getKey(), term.getValue());
		}

		// debugPrintTopWords(discardedTerms, "DiscardedTerms", 0);

		return topTerms;
	}

	public List<ScoredDocument> search(Query query, int nResults) throws IOException
	{
		// run the search
		TopDocs results = this.searcher.search(query, nResults);
		ScoreDoc[] hits = results.scoreDocs;

		// retrieve the documents
		List<ScoredDocument> sdocs = new ArrayList<ScoredDocument>();
		for (ScoreDoc hit : hits)
			sdocs.add(new ScoredDocument(hit.score, searcher.doc(hit.doc)));

		return sdocs;
	}

	private static void debugPrintTopWords(Map<String, Integer> topTerms, String header)
	{
		if (true)
		{

			List<Map.Entry<String, Integer>> entries = new ArrayList<Map.Entry<String, Integer>>(topTerms.entrySet());
			Collections.sort(entries, new TermFrequencyComparator());

			System.out.println("\n\n --- \n " + header + " \n");
			for (Map.Entry<String, Integer> term : entries)
			{
				// This is the minimum frequency
				if (term.getValue() >= TOP_TERM_MINIMUM_FREQUENCY)
					System.out.println(term.getKey() + " -> " + term.getValue() + " times");
			}
		}
	}

	private static class TermFrequencyComparator implements Comparator<Map.Entry<String, Integer>>
	{

		@Override
		public int compare(Entry<String, Integer> a, Entry<String, Integer> b)
		{
			return Integer.compare(b.getValue(), a.getValue());
		}

	}
}