package org.novasearch.tutorials.labs;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Lab1_Baseline {

	String indexPath = "./index";
	String docPath = "./eval/Answers.csv";
	String resultsPath = "./eval/myresults.txt";
	String queriesPath = "./eval/queries.offline.txt";

	boolean create = true;

	private IndexWriter idx;

	public static void main(String[] args) {
		Analyzer analyzer = new Lab2_Analyser();
		Similarity similarity = new ClassicSimilarity();

		Lab1_Baseline baseline = new Lab1_Baseline();

		// Create a new index
		baseline.openIndex(analyzer, similarity);
		baseline.indexDocuments();
		baseline.close();

		// Search the index
		baseline.indexSearch(analyzer, similarity);

		baseline.doEvaluation();

		System.out.println("terminated.");
	}

	public void doEvaluation() {
		String basePath = "~/M/Uni/10Semester/webSearch/";
		String trecEvalExecutablePath = basePath + "trec_eval/trec_eval";
		String qrelsPath = basePath + "repository/RI-labs/eval/qrels.offline.txt";
		String resultsPath = basePath + "repository/RI-labs/eval/myresults.txt";
		runTrecEval(trecEvalExecutablePath, qrelsPath, resultsPath);
	}

	public static void runTrecEval(String trecEvalExecutablePath, String qrelsPath, String resultsPath) {
		Runtime rt = Runtime.getRuntime();
		try {
			String[] command = new String[] { "/bin/bash", "-c",
					trecEvalExecutablePath + " " + qrelsPath + " " + resultsPath };
			Process pr = rt.exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

			String line = null;

			while ((line = stdInput.readLine()) != null) {
				System.out.println(line);
			}
			int exitVal = pr.waitFor();
			if (exitVal != 0) {
				System.out.println("trec_eval exits with Error code " + exitVal);
				while ((line = stdError.readLine()) != null) {
					System.out.println(line);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void openIndex(Analyzer analyzer, Similarity similarity) {
		try {
			// ====================================================
			// Configure the index to be created/opened
			//
			// IndexWriterConfig has many options to be set if needed.
			//
			// Example: for better indexing performance, if you
			// are indexing many documents, increase the RAM
			// buffer. But if you do this, increase the max heap
			// size to the JVM (eg add -Xmx512m or -Xmx1g):
			//
			// iwc.setRAMBufferSizeMB(256.0);
			IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
			iwc.setSimilarity(similarity);
			if (create) {
				// Create a new index, removing any
				// previously indexed documents:
				iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
			} else {
				// Add new documents to an existing index:
				iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
			}

			// ====================================================
			// Open/create the index in the specified location
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			idx = new IndexWriter(dir, iwc);

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public void indexDocuments() {
		if (idx == null)
			return;

		// ====================================================
		// Parse the Answers data
		try (BufferedReader br = new BufferedReader(new FileReader(docPath))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine(); // The first line is dummy
			line = br.readLine();

			// ====================================================
			// Read documents
			System.out.println("adding documents...");
			int errorCounter = 0;
			while (line != null) {
				int i = line.length();

				// Search for the end of document delimiter
				if (i != 0)
					sb.append(line);
				sb.append(System.lineSeparator());
				if (((i >= 2) && (line.charAt(i - 1) == '"') && (line.charAt(i - 2) != '"'))
						|| ((i == 1) && (line.charAt(i - 1) == '"'))) {
					// Index the document
					if (!indexDoc(sb.toString()))
						errorCounter++;

					// Start a new document
					sb = new StringBuilder();
				}
				line = br.readLine();
			}
			System.out.println(errorCounter + " Errors while indexing documents");
			System.out.println("documents added.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean indexDoc(String rawDocument) {

		Document doc = new Document();

		// ====================================================
		// Each document is organized as:
		// Id,OwnerUserId,CreationDate,ParentId,Score,Body
		Integer AnswerId = 0;
		int errorAddCounter = 0;
		int errorParseCounter = 0;
		try {

			// Extract field Id
			Integer start = 0;
			Integer end = rawDocument.indexOf(',');
			String aux = rawDocument.substring(start, end);
			AnswerId = Integer.decode(aux);

			// Index _and_ store the AnswerId field
			doc.add(new IntPoint("AnswerId", AnswerId));
			doc.add(new StoredField("AnswerId", AnswerId));

			// Extract field OwnerUserId
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer OwnerUserId = Integer.decode(aux);
			doc.add(new IntPoint("OwnerUserId", OwnerUserId));

			// Extract field CreationDate
			try {
				start = end + 1;
				end = rawDocument.indexOf(',', start);
				aux = rawDocument.substring(start, end);
				Date creationDate;
				creationDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(aux);
				doc.add(new LongPoint("CreationDate", creationDate.getTime()));
			} catch (ParseException e1) {
				System.out.println("Error parsing date for document " + AnswerId);
			}

			// Extract field ParentId
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer ParentId = Integer.decode(aux);
			doc.add(new IntPoint("ParentId", ParentId));

			// Extract field Score
			start = end + 1;
			end = rawDocument.indexOf(',', start);
			aux = rawDocument.substring(start, end);
			Integer Score = Integer.decode(aux);
			doc.add(new IntPoint("Score", Score));

			// Extract field Body
			String body = rawDocument.substring(end + 1);
			doc.add(new TextField("Body", body, Field.Store.YES));

			// ====================================================
			// Add the document to the index
			if (idx.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
				// System.out.println("adding " + AnswerId);
				idx.addDocument(doc);
			} else {
				idx.updateDocument(new Term("AnswerId", AnswerId.toString()), doc);
			}
		} catch (IOException e) {
			// System.out.println("Error adding document " + AnswerId);
			errorAddCounter++;
		} catch (Exception e) {
			// System.out.println("Error parsing document " + AnswerId);
			errorParseCounter++;
		}
		return errorAddCounter + errorParseCounter == 0;
	}

	// ====================================================
	// Comment and refactor this method yourself
	public void indexSearch(Analyzer analyzer, Similarity similarity) {

		IndexReader reader = null;
		BufferedReader in = null;
		try {
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			BufferedWriter writer = new BufferedWriter(new FileWriter(resultsPath));
			writer.write("QueryID\tQ0\tDocID\tRank\tScore\tRunID\n");

			// in = new BufferedReader(new InputStreamReader(System.in,
			// StandardCharsets.UTF_8));
			// This reader parses from the commandline
			// in = new BufferedReader(new InputStreamReader(System.in,
			// StandardCharsets.UTF_8));

			// This reader parses from queries-file
			in = new BufferedReader(new FileReader(queriesPath));

			QueryParser parser = new QueryParser("Body", analyzer);
			while (true) {
				// System.out.println("Enter query: ");

				String line = in.readLine();

				if (line == null || line.length() == -1) {
					break;
				}

				line = line.trim();
				if (line.length() == 0) {
					break;
				}

				System.out.println("Your query: " + line);
				
				//parse query id
				Integer qid = Integer.parseInt(line.substring(0, line.indexOf(":")));

				Query query;
				try {
					query = parser.parse(line);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					System.out.println("Error parsing query string.");
					continue;
				}

				TopDocs results = searcher.search(query, 3);
				ScoreDoc[] hits = results.scoreDocs;

				int numTotalHits = results.totalHits;
				System.out.println(numTotalHits + " total matching documents");

				for (int j = 0; j < hits.length; j++) {
					Document doc = searcher.doc(hits[j].doc);
					Integer Id = doc.getField("AnswerId").numericValue().intValue();
					if (j > 0) {
						writer.write("\n");
					}
					writer.write(qid + "\tQ0\t" + Id + "\t" + (j + 1) + "\t" + hits[j].score + "\trun1");

				}

				if (line.equals("")) {
					break;
				}
			}
			reader.close();
			writer.close();
		} catch (IOException e) {
			try {
				reader.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			e.printStackTrace();
		} finally {
			if (in != null)
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public void close() {
		try {
			idx.close();
		} catch (IOException e) {
			System.out.println("Error closing the index.");
		}
	}

}
