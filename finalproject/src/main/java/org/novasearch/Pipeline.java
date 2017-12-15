package org.novasearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class Pipeline
{

	 private static final int MAX_TWEETS = 30;

	public static void main(String[] args) throws org.apache.lucene.queryparser.classic.ParseException
	{
		debugPrintln("Hello World!");
		Pipeline pipeline = new Pipeline();
		try
		{
			pipeline.run();
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		debugPrintln("terminated");
	}

	// @formatter:off
	//
	// TODO this is stuff which needs to be done
	//
	// [ ] 1. parse tweets (using Twitter4J)
	// [ ] 2. parse interest profiles
	// [ ] 3. split interest profiles into train and test
	// [ ] 4. build index incrementally (for each day, no future included)
	// [ ] 5. output daily digest (per day, 10 most relevant tweets for each
	// interest profile)
	// [ ] - LMD retrieval model
	// [ ] - BM25 retrieval model
	// [ ] - pseudo relevance feedback
	//
	// IMPROVE BY
	//
	// [ ] - k-Means
	// [ ] - MinHash
	//
	// [ ] - Rank fusion: combine multiple fields
	//
	//
	// @formatter:on
	private void run() throws IOException, org.apache.lucene.queryparser.classic.ParseException
	{

		// proof that lucene dependencies are working
		Analyzer analyzer = Parameter.getAnalyzer();

		List<Status> tweets = parseJSONTweets(Parameter.PATH_TWEETS);
		System.out.println(tweets.size() + " Tweets were parsed.");

		IndexCreator index = new IndexCreator(Parameter.PATH_INDEX, analyzer, Parameter.getSimilarity());
		index.index(tweets);
		index.close();

		List<Profile> profiles = parseProfiles(Parameter.PATH_PROFILES);
		System.out.println(profiles.size() + " Profiles were parsed.");

		SearchEngine searcher = new SearchEngine(Parameter.PATH_INDEX, Parameter.getSimilarity());

		Profile profile = profiles.get(0);
		Query q = searcher.toQuery(profile, analyzer);
		System.out.println("Query: " + q);
		Collection<ScoreDoc> hits = searcher.search(q);
		IndexSearcher indexSearcher = searcher.getSearcher();
		for(ScoreDoc h:hits)
		{
			Document doc = indexSearcher.doc(h.doc);
			System.out.println(doc.get("text"));
			System.out.println("\n---\n");
		}
	}

	private void writeTweets(List<Status> tweets) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter("res/tweets/textonly.txt"));
		for(Status t:tweets)
		{
			StringBuilder line = new StringBuilder(t.getId() + " : " + t.getText());
			for(HashtagEntity h:t.getHashtagEntities())
				line.append(" #"+h.getText());
			
			writer.write(line.toString());
			writer.newLine();
		}
		writer.close();
	}

	private List<Status> parseJSONTweets(final String jsonFilePath) throws IOException
	{
		BufferedReader reader = new BufferedReader(new FileReader(jsonFilePath));
		try
		{
			List<Status> tweets = new ArrayList<Status>();
			String line = null;
			int c = 0;
			while ((line = reader.readLine()) != null)
			{
				Status tweet = TwitterObjectFactory.createStatus(line);
				tweets.add(tweet);
//				 if(++c >= MAX_TWEETS)
//				 break;
			}
			return tweets;
		}
		catch (TwitterException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally
		{
			reader.close();
		}
		return null;
	}

	private List<Profile> parseProfiles(String jsonFilePath) throws FileNotFoundException, IOException
	{
		JSONParser parser = new JSONParser();

		try
		{
			List<Profile> profiles = new ArrayList<Profile>();
			Object parsed = parser.parse(new FileReader(jsonFilePath));

			JSONArray jsonProfiles = (JSONArray) parsed;
			for (Object i : jsonProfiles)
			{
				JSONObject jsonProfile = (JSONObject) i;
				Profile profile = new Profile();
				profile.setTopid((String) jsonProfile.get("topid"));
				profile.setTitle((String) jsonProfile.get("title"));
				profile.setDescription((String) jsonProfile.get("description"));
				profile.setNarrative((String) jsonProfile.get("narrative"));
				profiles.add(profile);
			}

			return profiles;
		}
		catch (ParseException e)
		{
			// TODO handle Exception properly
			e.printStackTrace();
		}

		return null;
	}

	protected static void debugPrintln(String msg)
	{
		System.out.println(msg);
	}
}
