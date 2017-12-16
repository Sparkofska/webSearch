package org.novasearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.Query;
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

		System.out.println("Parsing tweets...");
		List<Status> tweets = parseJSONTweets(Parameter.PATH_TWEETS);
		System.out.println(tweets.size() + " Tweets were parsed.");

		IndexCreator index = new IndexCreator(Parameter.PATH_INDEX, analyzer, Parameter.getSimilarity());
		index.index(tweets);
		index.close();

		List<Profile> profiles = parseProfiles(Parameter.PATH_PROFILES);
		System.out.println(profiles.size() + " Profiles were parsed.");

		SearchEngine searcher = new SearchEngine(Parameter.PATH_INDEX, Parameter.getSimilarity());

		
		HashMap<String, HashMap<Profile, List<ScoredDocument>>> digests = new HashMap<String, HashMap<Profile,List<ScoredDocument>>>();
		HashMap<Profile, List<ScoredDocument>> a = new HashMap<Profile, List<ScoredDocument>>();
		for(Profile profile : profiles)
		{
			Query query = searcher.toQuery(profile, analyzer);
			List<ScoredDocument> hits = searcher.search(query, Parameter.N_SEARCH_RESULTS);
			System.out.println(hits.size() + " hits for " + profile.getTitle());
			
			MinHashClusterer clus = new MinHashClusterer();
			hits = clus.removeDublicates(hits, 0);
			System.out.println(hits.size() + " hits left after removing duplicates");
			
			a.put(profile, hits);
		}
		digests.put("YYYYMMDD", a); // TODO put the correct date
		
		makeDigestOutput("output.txt", digests);
	}

	// YYYYMMDD topic_id Q0 tweet_id rank score runtag
	private void makeDigestOutput(String path, HashMap<String, HashMap<Profile, List<ScoredDocument>>> digests)
			throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter(path));
		for (String date : digests.keySet())
		{
			HashMap<Profile, List<ScoredDocument>> a = digests.get(date);
			for (Profile profile : a.keySet())
			{
				List<ScoredDocument> docs = a.get(profile);
				int rank = 1;
				for (ScoredDocument doc : docs)
				{
					StringBuilder line = new StringBuilder();

					line.append(date);
					line.append("\t");
					line.append(profile.getTopid());
					line.append("\tQ0\t");
					line.append(doc.getDocument().get("id")); 
					line.append("\t");
					line.append(rank++);
					line.append("\t");
					line.append(doc.getScore());
					line.append("\t");
					line.append("runtag"); //TODO get runtag

					writer.write(line.toString());
					writer.newLine();
				}
			}
		}
		writer.close();
	}

	private void writeTweets(List<Status> tweets) throws IOException
	{
		BufferedWriter writer = new BufferedWriter(new FileWriter("res/tweets/textonly.txt"));
		for (Status t : tweets)
		{
			StringBuilder line = new StringBuilder(t.getId() + " : " + t.getText());
			for (HashtagEntity h : t.getHashtagEntities())
				line.append(" #" + h.getText());

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
				// if(++c >= MAX_TWEETS)
				// break;
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
