package org.novasearch;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

public class Pipeline {

	public static void main(String[] args) {
		debugPrintln("Hello World!");
		Pipeline pipeline = new Pipeline();
		pipeline.runCatched();
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
	private void run() throws IOException, TwitterException {

		// proof that lucene dependencies are working
		Analyzer analyzer = Parameter.getAnalyzer();

		List<Status> tweets = parseJSONTweets("res/tweets/rts2016-qrels-tweets2016.jsonl");
		System.out.println(tweets.size() + " Tweets were parsed.");

		Collection<Profile> profiles = parseProfiles("res/profiles/TREC2016-RTS-topics.json");
		System.out.println(profiles.size() + " Profiles were parsed.");
	}

	private void runCatched() {
		try {
			run();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TwitterException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private List<Status> parseJSONTweets(final String jsonFilePath) throws IOException, TwitterException {
		List<Status> tweets = new ArrayList<Status>();
		BufferedReader reader = new BufferedReader(new FileReader(jsonFilePath));
		String line = null;
		while ((line = reader.readLine()) != null) {

			Status tweet = TwitterObjectFactory.createStatus(line);
			tweets.add(tweet);
		}
		reader.close();

		return tweets;
	}

	private Collection<Profile> parseProfiles(String jsonFilePath) throws FileNotFoundException, IOException {
		JSONParser parser = new JSONParser();

		try {

			Collection<Profile> profiles = new ArrayList<Profile>();
			Object parsed = parser.parse(new FileReader(jsonFilePath));

			JSONArray jsonProfiles = (JSONArray) parsed;
			for (Object i : jsonProfiles) {
				JSONObject jsonProfile = (JSONObject) i;
				Profile profile = new Profile();
				profile.setTopid((String) jsonProfile.get("topid"));
				profile.setTitle((String) jsonProfile.get("title"));
				profile.setDescription((String) jsonProfile.get("description"));
				profile.setNarrative((String) jsonProfile.get("narrative"));
				profiles.add(profile);
			}

			return profiles;
		} catch (ParseException e) {
			e.printStackTrace();
		}

		return null;
	}

	protected static void debugPrintln(String msg) {
		System.out.println(msg);
	}
}
