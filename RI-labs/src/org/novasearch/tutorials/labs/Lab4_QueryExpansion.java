package org.novasearch.tutorials.labs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class Lab4_QueryExpansion extends Lab1_Baseline {

    IndexSearcher searcher = null;

    public void indexSearchQE(Analyzer analyzer, Similarity similarity) {

        IndexReader reader = null;
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

                Map<String, Integer> expansionTerms = getExpansionTerms(queryString, 100, analyzer, similarity);

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
    }


    public Map<String, Integer>  getExpansionTerms(String queryString, int numExpDocs, Analyzer analyzer, Similarity similarity) {

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

    public static void main(String[] args) {

        Lab4_QueryExpansion baseline = new Lab4_QueryExpansion();

        Analyzer analyzer = new StandardAnalyzer();
        Similarity similarity = new ClassicSimilarity();

        // Search the index
        baseline.indexSearchQE(analyzer, similarity);
    }

}
