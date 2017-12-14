package org.novasearch;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import twitter4j.HashtagEntity;
import twitter4j.Status;
import twitter4j.User;

public class IndexCreator
{

	private IndexWriter writer;

	public IndexCreator(final String indexPath, final Analyzer analyzer, final Similarity similarity) throws IOException
	{
		Directory dir = FSDirectory.open(Paths.get(indexPath));

		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		if (DirectoryReader.indexExists(dir))
			config.setOpenMode(OpenMode.CREATE_OR_APPEND);
		else
			config.setOpenMode(OpenMode.CREATE);
		config.setSimilarity(similarity);

		this.writer = new IndexWriter(dir, config);
	}

	public void index(Collection<Status> tweets) throws IOException
	{
		for (Status tweet : tweets)
		{
			// transform tweet into lucene-doc
			Document doc = toIndexableDocument(tweet);

			// Add the document to the index
			this.writer.updateDocument(new Term("id", Long.toString(tweet.getId())), doc);
		}

		writer.flush();
		writer.commit();
	}

	private Document toIndexableDocument(Status tweet)
	{
		Document doc = new Document();

		// id
		// id_str
		// text
		// timestamp_ms
		// user.id
		// user.name
		// user.screen_name
		// entities.hashtags

		long id = tweet.getId();
		String text = tweet.getText();
		Date date = tweet.getCreatedAt();
		User user = tweet.getUser();
		long userId = user.getId();
		String userName = user.getName();
		String userScreenName = user.getScreenName();
		HashtagEntity[] hashtags = tweet.getHashtagEntities();
		StringBuilder hashtagsString = new StringBuilder(" ");
		for (HashtagEntity hashtag : hashtags)
			hashtagsString.append(hashtag.getText() + " ");

		doc.add(new LongPoint("id", id));
		doc.add(new StoredField("id", Long.toString(id)));
		doc.add(new TextField("text", text, Field.Store.NO));
		doc.add(new LongPoint("date", date.getTime()));
		doc.add(new LongPoint("user_id", userId));
		doc.add(new StringField("user_name", userName, Field.Store.NO));
		doc.add(new StringField("user_screen_name", userScreenName, Field.Store.NO));
		doc.add(new TextField("hashtags", hashtagsString.toString(), Field.Store.NO));

		return doc;
	}

	public void close() throws IOException
	{
		this.writer.close();
	}

	// @formatter:off
	// This is an exmaple tweet
//	{
//		  "created_at": "Sat Aug 06 12:39:04 +0000 2016",
//		  "id": 7.619044232538e+17,
//		  "id_str": "761904423253803008",
//		  "text": "Some of the most promising advances in cancer involve these treatments. Here are answers to some basic questions about this  rapidly",
//		  "source": "<a href=\"http:\/\/www.forum-auto.com\" rel=\"nofollow\">Twitter App2347<\/        a>",
//		  "truncated": false,
//		  "in_reply_to_status_id": null,
//		  "in_reply_to_status_id_str": null,
//		  "in_reply_to_user_id": null,
//		  "in_reply_to_user_id_str": null,
//		  "in_reply_to_screen_name": null,
//		  "user": {
//		    "id": 3078843052,
//		    "id_str": "3078843052",
//		    "name": "Alexandra Mcghee",
//		    "screen_name": "nelwynhallman",
//		    "location": null,
//		    "url": null,
//		    "description": null,
//		    "protected": false,
//		    "verified": false,
//		    "followers_count": 197,
//		    "friends_count": 195,
//		    "listed_count": 15,
//		    "favourites_count": 0,
//		    "statuses_count": 4406,
//		    "created_at": "Sun Mar 08 05:40:28 +0000 2015",
//		    "utc_offset": -25200,
//		    "time_zone": "Pacific Time (US & Canada)",
//		    "geo_enabled": false,
//		    "lang": "en",
//		    "contributors_enabled": false,
//		    "is_translator": false,
//		    "profile_background_color": "C0DEED",
//		    "profile_background_image_url": "http:\/\/abs.twimg.com\/images\/themes\/theme1\/bg.png",
//		    "profile_background_image_url_https": "https:\/\/abs.twimg.com\/images\/themes\/theme1\/bg.png",
//		    "profile_background_tile": false,
//		    "profile_link_color": "0084B4",
//		    "profile_sidebar_border_color": "C0DEED",
//		    "profile_sidebar_fill_color": "DDEEF6",
//		    "profile_text_color": "333333",
//		    "profile_use_background_image": true,
//		    "profile_image_url": "http:\/\/abs.twimg.com\/sticky\/default_profile_images\/default_profile_5_normal.png",
//		    "profile_image_url_https": "https:\/\/abs.twimg.com\/sticky\/default_profile_images\/default_profile_5_normal.png",
//		    "default_profile": true,
//		    "default_profile_image": true,
//		    "following": null,
//		    "follow_request_sent": null,
//		    "notifications": null
//		  },
//		  "geo": null,
//		  "coordinates": null,
//		  "place": null,
//		  "contributors": null,
//		  "is_quote_status": false,
//		  "retweet_count": 0,
//		  "favorite_count": 0,
//		  "entities": {
//		    "hashtags": [
//		      
//		    ],
//		    "urls": [
//		      
//		    ],
//		    "user_mentions": [
//		      
//		    ],
//		    "symbols": [
//		      
//		    ]
//		  },
//		  "favorited": false,
//		  "retweeted": false,
//		  "filter_level": "low",
//		  "lang": "en",
//		  "timestamp_ms": "1470487144660"
//		}
	// @formatter:on
}
