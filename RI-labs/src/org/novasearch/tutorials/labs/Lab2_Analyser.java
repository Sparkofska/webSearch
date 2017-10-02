/**
 * 
 */
package org.novasearch.tutorials.labs;

import java.util.List;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.snowball.SnowballFilter;
import org.apache.lucene.analysis.standard.StandardFilter;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;

/**
 * @author jmag
 *
 */
public class Lab2_Analyser extends Analyzer {

	/**
	 * An unmodifiable set containing some common English words that are not
	 * usually useful for searching.
	 */
	static List<String> stopWords = Arrays.asList("a", "an", "and", "are", "as", "at", "be", "but", "by", "for", "if",
			"in", "into", "is", "it", "no", "not", "of", "on", "or", "such", "that", "the", "their", "then", "there",
			"these", "they", "this", "to", "was", "will", "with");
	static CharArraySet stopSet = new CharArraySet(stopWords, false);

	/** Default maximum allowed token length */
	private int maxTokenLength = 25;

	/**
	 * Builds an analyzer with the default stop words ({@link #STOP_WORDS_SET}).
	 */
	public Lab2_Analyser() {

	}

	@Override
	protected TokenStreamComponents createComponents(final String fieldName) {

		
		// THE FIELD IS IGNORED 
		// ___BUT___ 
		// you can provide different TokenStremComponents according to the fieldName
		
		final StandardTokenizer src = new StandardTokenizer();
		
		TokenStream tok = null;
		tok = new StandardFilter(src);					// text into non punctuated text
//		tok = new LowerCaseFilter(tok);					// changes all texto into lowercase
//		tok = new StopFilter(tok, stopSet);				// removes stop words

//		tok = new ShingleFilter(tok, 2, 3);				// creates word-grams with neighboring works
//		tok = new CommonGramsFilter(tok, stopwords);	// creates word-grams with stopwords

// 		tok = new NGramTokenFilter(tok,2,5);			// creates unbounded n-grams
//		tok = new EdgeNGramTokenFilter(tok,2,5);		// creates word-bounded n-grams

//		tok = new SnowballFilter(tok, "English");		// stems workds according to the specified language
		
		return new TokenStreamComponents(src, tok) {
			@Override
			protected void setReader(final Reader reader) {
				src.setMaxTokenLength(Lab2_Analyser.this.maxTokenLength);
				super.setReader(reader);
			}
		};
	}

	@Override
	protected TokenStream normalize(String fieldName, TokenStream in) {
		TokenStream result = new StandardFilter(in);
		result = new LowerCaseFilter(result);
		return result;
	}
	
	// ===============================================
	// Test the different filters
	public static void main(String[] args) throws IOException {

		final String text = "This is a demonstration, of the TokenStream Lucene-API,";

		Lab2_Analyser analyzer = new Lab2_Analyser();
		TokenStream stream = analyzer.tokenStream("field", new StringReader(text));

		// get the CharTermAttribute from the TokenStream
		CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);

		try {
			stream.reset();

			// print all tokens until stream is exhausted
			while (stream.incrementToken()) {
				System.out.println(termAtt.toString());
			}

			stream.end();
		} finally {
			stream.close();
		}
	}
}
