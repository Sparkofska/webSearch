package org.novasearch;

import org.apache.lucene.analysis.Analyzer;

public class Parameter {

	public static Analyzer getAnalyzer()
	{
		return new Analyzer(){

			@Override
			protected TokenStreamComponents createComponents(String arg0) {
				// TODO Auto-generated method stub
				return null;
			}
			
		};
	}
}
