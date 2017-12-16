package org.novasearch;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

public class MinHashClusterer
{

	private static final int NUMBER_OF_HASH_FUNCTIONS = 50; // TODO Tune this parameter!!
	private static final float SIMILARITY_THRESHOLD = 0.7f; // TODO tune this
	private HashMap<String, Set<String>> shingleCache;

	private HashMap<String, Set<String>> getShingleChache()
	{
		if (this.shingleCache == null)
			this.shingleCache = new HashMap<String, Set<String>>();
		return this.shingleCache;
	}

	public List<ScoredDocument> removeDublicates(List<ScoredDocument> set, int minRemainingElements) throws IOException
	{
		if (set.size() < minRemainingElements)
			throw new IllegalArgumentException();

		List<ScoredDocument> cleaned = new ArrayList<ScoredDocument>();

		int numHash = NUMBER_OF_HASH_FUNCTIONS;
		MinHash<String> algorithm = new MinHash<String>(numHash);

		for (ScoredDocument element : set)
		{
			boolean nuggetNotAppearedBefore = true;
			for (ScoredDocument existing : cleaned)
			{
				Set<String> set1 = createShingles(element.getDocument().get("text"));
				Set<String> set2 = createShingles(existing.getDocument().get("text"));
				double similarity = algorithm.similarity(set1, set2);
				// System.out.println("similarity = " + similarity);
				nuggetNotAppearedBefore = similarity < SIMILARITY_THRESHOLD;
			}
			if (nuggetNotAppearedBefore)
				cleaned.add(element);
		}

		return cleaned;
	}

	public Set<String> createShingles(String text) throws IOException
	{
		// using the cache, the shingles will only be created once and stored
		// for the next call
		HashMap<String, Set<String>> cache = getShingleChache();
		Set<String> cached = cache.get(text);
		if (cached != null)
			return cached;

		Set<String> shingles = new HashSet<String>();

		Analyzer analyzer = getAnalyzer();
		TokenStream stream = analyzer.tokenStream(null, new StringReader(text));

		stream.reset();
		while (stream.incrementToken())
		{
			shingles.add(stream.getAttribute(CharTermAttribute.class).toString());
		}

		cache.put(text, shingles);

		return shingles;
	}

	private static Analyzer getAnalyzer()
	{
		return new Analyzer()
		{
			final List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for",
					"if", "in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their",
					"then", "there", "these", "they", "this", "to", "was", "will", "with");
			final CharArraySet stopSet = new CharArraySet(stopWords, false);

			@Override
			protected TokenStreamComponents createComponents(String fieldName)
			{
				StandardTokenizer src = new StandardTokenizer();
				TokenStream tok = null;
				tok = new StandardFilter(src);

				tok = new LowerCaseFilter(tok);
				tok = new StopFilter(tok, stopSet);
				tok = new ShingleFilter(tok, 4, 4);
				tok = new RemoveSingleWordsFilter(tok);

				return new TokenStreamComponents(src, tok);
			}

			class RemoveSingleWordsFilter extends TokenFilter
			{
				private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

				private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
				private final PositionIncrementAttribute posIncAttribute = addAttribute(
						PositionIncrementAttribute.class);

				private final CharArraySet previous = new CharArraySet(8, false);

				protected RemoveSingleWordsFilter(TokenStream input)
				{
					super(input);
				}

				@Override
				public boolean incrementToken() throws IOException
				{
					while (input.incrementToken())
					{
						final char term[] = termAttribute.buffer();
						final int length = termAttribute.length();
						final int posIncrement = posIncAttribute.getPositionIncrement();

						if (posIncrement > 0)
						{
							previous.clear();
						}

						boolean predicate = termAtt.toString().contains(" ");

						// clone the term, and add to the set of seen terms.
						char saved[] = new char[length];
						System.arraycopy(term, 0, saved, 0, length);
						previous.add(saved);

						if (predicate)
						{
							return true;
						}
					}
					return false;
				}
			}
		};
	}
}
