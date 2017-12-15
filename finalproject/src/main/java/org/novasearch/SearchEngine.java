package org.novasearch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.FSDirectory;

public class SearchEngine
{
	private static final int N_SEARCH_RESULTS = 10; // TODO tune
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
		return queryParser.parse("\""+s+"\"");
	}

	public Collection<ScoreDoc> search(Query query) throws IOException
	{
		// TODO return proper value
		TopDocs results = this.searcher.search(query, N_SEARCH_RESULTS);
		ScoreDoc[] hits = results.scoreDocs;
		
		System.out.println(results.totalHits);
		System.out.println(hits.length);
		
		// This is how to get the docs
		Collection<Document> docs = new ArrayList<Document>();
		for (ScoreDoc hit : hits)
		{
			Document doc = searcher.doc(hit.doc);
			docs.add(doc);
		}
		return Arrays.asList(hits);
	}
}
