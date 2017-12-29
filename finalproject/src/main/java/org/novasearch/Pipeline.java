package org.novasearch;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		Analyzer analyzer = Parameter.getAnalyzer();

		// parse tweets from json and separate into lists for each day
		System.out.println("Parsing tweets...");
		List<Status> tweets = parseJSONTweets(Parameter.PATH_TWEETS);
		System.out.println(tweets.size() + " Tweets were parsed.");
		Map<String, Collection<Status>> tweetsPerDay = separateByDate(tweets);

		// create a separate search index for each day
		/*
		 * System.out.println("Creating indexes..."); for (String date :
		 * tweetsPerDay.keySet()) { Collection<Status> tweetsThisDay =
		 * tweetsPerDay.get(date); IndexCreator index = new
		 * IndexCreator(Parameter.getIndexPath(date), analyzer,
		 * Parameter.getSimilarity()); index.index(tweetsThisDay);
		 * index.close(); }
		 */

		// parse interest profiles from file
		List<Profile> profiles = parseProfiles(Parameter.PATH_PROFILES);
		System.out.println(profiles.size() + " Profiles were parsed.");

		HashMap<String, HashMap<Profile, List<ScoredDocument>>> digests = new HashMap<String, HashMap<Profile, List<ScoredDocument>>>();
		for (String date : tweetsPerDay.keySet())
		{
			SearchEngine searcher = new SearchEngine(Parameter.getIndexPath(date), Parameter.getSimilarity());

			HashMap<Profile, List<ScoredDocument>> a = new HashMap<Profile, List<ScoredDocument>>();
			for (Profile profile : profiles)
			{
				Query query = searcher.toQuery(profile, analyzer);
				if (Parameter.isQueryExpansionEnabled())
					query = searcher.expandQueryByPseudoRelevanceFeedback("", query);
				List<ScoredDocument> hits = searcher.search(query, Parameter.N_SEARCH_RESULTS);
				System.out.println(hits.size() + " hits for " + profile.getTitle());

				Clusterer clusterer = Parameter.getClusterer();
				if (clusterer != null)
				{
					hits = clusterer.removeDublicates(hits, 0);
					System.out.println(hits.size() + " hits left after removing duplicates");
				}

				a.put(profile, hits);
			}
			digests.put(date, a);
//			break; // TODO remove this to everything for each day
		}

		makeDigestOutput("output.txt", digests);
	}

	// YYYYMMDD topic_id Q0 tweet_id rank score runtag
	private void makeDigestOutput(String path, HashMap<String, HashMap<Profile, List<ScoredDocument>>> digests)
			throws IOException
	{
		File outFile = new File(path);
		System.out.println("Writing digests to " + outFile.getAbsolutePath());
		BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
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
					line.append("runtag"); // TODO get runtag

					writer.write(line.toString());
					writer.newLine();
				}
			}
		}
		writer.close();
		System.out.println("Digests written.");
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
			while ((line = reader.readLine()) != null)
			{
				Status tweet = TwitterObjectFactory.createStatus(line);
				tweets.add(tweet);
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

	private Map<String, Collection<Status>> separateByDate(Collection<Status> tweets)
	{
		// separate tweets b single days
		Map<String, Collection<Status>> separated = new HashMap<String, Collection<Status>>();
		for (Status tweet : tweets)
		{
			Date date = tweet.getCreatedAt();
			String s = new SimpleDateFormat("yyyyMMdd").format(date);

			Collection<Status> thisday = separated.get(s);
			if (thisday == null)
			{
				thisday = new ArrayList<Status>();
				separated.put(s, thisday);
			}
			thisday.add(tweet);
		}

		// cumulate tweets by "that day and all previous days"
		for (String thisdate : separated.keySet())
			for (String thatdate : separated.keySet())
				if (isBefore(thisdate, thatdate))
					separated.get(thatdate).addAll(separated.get(thisdate));

		return separated;
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

	private static boolean isBefore(String date1, String date2)
	{
		return date1.compareTo(date2) < 0;
	}

	protected static void debugPrintln(String msg)
	{
		System.out.println(msg);
	}
}
