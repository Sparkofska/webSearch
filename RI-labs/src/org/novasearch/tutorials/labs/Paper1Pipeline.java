package org.novasearch.tutorials.labs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.ClassicSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class Paper1Pipeline {

	protected static final IndexWriterConfig.OpenMode INDEX_OPEN_MODE = OpenMode.CREATE; // false:
																						// append
																						// to
	// existing index
	private static final String INDEX_PATH = "./index";
	private static final String DOCUMENTS_PATH = "./eval/Answers.csv";
	private static final String QUERIES_PATH = "./eval/queries.offline.txt";
	private static final String SEARCH_RESULTS_PATH = "./eval/myresults.txt";
	private static final String GROUND_TRUTH_PATH = "./eval/qrels.offline.txt";
	
	private static final int NUMBER_OF_HITS_PER_QUERY = 60;

	public static void main(String[] args) {
		Paper1Pipeline pipeline = new Paper1Pipeline();
		pipeline.run();
		debugPrintln("terminated");
	}

	protected static void debugPrintln(String msg) {
		System.out.println(msg);
	}

	public void run() {

		// get custom Analyzers
		Analyzer analyzer = getAnalyzer();
		Similarity similarity = getSimilarity();

		// Create the Index from given Documents
		IndexWriter idx = createOrOpenIndex(analyzer, similarity, INDEX_PATH, INDEX_OPEN_MODE);
		indexDocuments(idx, DOCUMENTS_PATH);
		closeIndex(idx);

		// perform the search with given queries
		File my_results = searchIndex(analyzer, similarity, INDEX_PATH, QUERIES_PATH, SEARCH_RESULTS_PATH, NUMBER_OF_HITS_PER_QUERY);

		// evaluate the results of the search
		doEvaluation(my_results, GROUND_TRUTH_PATH);
	}

	protected Analyzer getAnalyzer() {
		// TODO return List of Analyzers
		return new Lab2_Analyser();
	}

	protected Similarity getSimilarity() {
		return new ClassicSimilarity();
	}

	/**
	 * 
	 * @param analyzer
	 * @param similarity
	 * @param indexPath
	 * @param indexOpenMode
	 * @return The indexWriter to access the index. <code>null</code> if there
	 *         are problems opening the IndexWriter.
	 */
	protected IndexWriter createOrOpenIndex(Analyzer analyzer, Similarity similarity, String indexPath,
			OpenMode indexOpenMode) {
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
		iwc.setOpenMode(indexOpenMode);

		// Open/create the index in the specified location
		IndexWriter idx = null;
		try {
			Directory dir = FSDirectory.open(Paths.get(indexPath));
			idx = new IndexWriter(dir, iwc);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return idx;
	}

	protected void indexDocuments(IndexWriter idx, String docPath) {
		if (idx == null)
			throw new NullPointerException("IndexWriter must not be NULL.");

		// Parse the Answers data
		try (BufferedReader br = new BufferedReader(new FileReader(docPath))) {
			StringBuilder sb = new StringBuilder();
			String line = br.readLine(); // The first line is dummy
			line = br.readLine();

			// Read documents
			debugPrintln("adding documents...");
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
					if (!indexDoc(idx, sb.toString()))
						errorCounter++;

					// Start a new document
					sb = new StringBuilder();
				}
				line = br.readLine();
			}
			debugPrintln(errorCounter + " Errors while indexing documents");
			debugPrintln("documents added.");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public boolean indexDoc(IndexWriter idx, String rawDocument) {

		Document doc = new Document();

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

			// Add the document to the index
			if (idx.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
				// debugPrintln("adding " + AnswerId);
				idx.addDocument(doc);
			} else {
				idx.updateDocument(new Term("AnswerId", AnswerId.toString()), doc);
			}
		} catch (IOException e) {
			// debugPrintln("Error adding document " + AnswerId);
			errorAddCounter++;
		} catch (Exception e) {
			// debugPrintln("Error parsing document " + AnswerId);
			errorParseCounter++;
		}
		return errorAddCounter + errorParseCounter == 0;
	}

	protected void closeIndex(IndexWriter idx) {
		try {
			idx.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	protected File searchIndex(Analyzer analyzer, Similarity similarity, String indexPath, String queriesPath,
			String searchResultsPath, int numHits) {
		IndexReader reader = null;
		BufferedWriter writer = null;
		BufferedReader in = null;

		try {
			// This reader reads the index file
			reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
			IndexSearcher searcher = new IndexSearcher(reader);
			searcher.setSimilarity(similarity);

			// This writer writes the search results to a file
			File resultsFile = new File(searchResultsPath);
			writer = new BufferedWriter(new FileWriter(resultsFile));
			writer.write("QueryID\tQ0\tDocID\tRank\tScore\tRunID\n");

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

				debugPrintln("Your query: " + line);

				// parse query id
				Integer qid = Integer.parseInt(line.substring(0, line.indexOf(":")));

				Query query;
				try {
					query = parser.parse(line);
				} catch (org.apache.lucene.queryparser.classic.ParseException e) {
					debugPrintln("Error parsing query string.");
					continue;
				}

				TopDocs results = searcher.search(query, numHits);
				ScoreDoc[] hits = results.scoreDocs;

				int numTotalHits = results.totalHits;
				debugPrintln(numTotalHits + " total matching documents");

				for (int j = 0; j < hits.length; j++) {
					Document doc = searcher.doc(hits[j].doc);
					Integer Id = doc.getField("AnswerId").numericValue().intValue();
					writer.write(qid + "\tQ0\t" + Id + "\t" + (j + 1) + "\t" + hits[j].score + "\trun1\n");

				}

				if (line.equals("")) {
					break;
				}
			}
			return resultsFile;
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (reader != null)
					reader.close();
				if (writer != null)
					writer.close();
				if (in != null)
					in.close();

			} catch (Exception e2) {
				e2.printStackTrace();
			}
		}
		return null;
	}

	protected void doEvaluation(File my_results, String groundTruthPath) {
		if (my_results == null)
			throw new NullPointerException("Results file is NULL. Probably an Error occured during searchIndex().");
		// TODO run trec_eval
		runTrecEvalOnJonasMachine();
		// TODO run plot script
	}

	private void runTrecEvalOnJonasMachine() {
		String basePath = "/home/jonas/M/Uni/10Semester/webSearch/";
		String trecEvalExecutablePath = basePath + "trec_eval/trec_eval";
		String qrelsPath = basePath + "repository/RI-labs/eval/qrels.offline.txt";
		String resultsPath = basePath + "repository/RI-labs/eval/myresults.txt";
		String outputPath = basePath + "repository/RI-labs/eval/trec_eval_output.txt";

		String[] command = new String[] { "/bin/bash", "-c",
				trecEvalExecutablePath + " " + qrelsPath + " " + resultsPath };
		executeCommand(command, outputPath);
	}

	private boolean executeCommand(String[] command, String outputPath) {
		FileWriter out = null;
		try {
			Runtime rt = Runtime.getRuntime();
			Process pr = rt.exec(command);
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(pr.getInputStream()));
			BufferedReader stdError = new BufferedReader(new InputStreamReader(pr.getErrorStream()));

			boolean writeOutputToFile = outputPath != null;
			if (writeOutputToFile) {
				File outputFile = new File(outputPath);
				if (outputFile.exists())
					outputFile.delete();
				outputFile.createNewFile();
				out = new FileWriter(outputFile);
			}

			String line = null;
			while ((line = stdInput.readLine()) != null) {
				debugPrintln(line);
				if (writeOutputToFile) {
					out.write(line + System.lineSeparator());
				}
			}
			int exitVal = pr.waitFor();
			StringBuilder sb = new StringBuilder();
			if (exitVal != 0) {
				sb.append("The command ");
				for (String s : command)
					sb.append(s + " ");
				sb.append("exits with Error code " + exitVal + " leaving the following error message:");
				debugPrintln(sb.toString());
				while ((line = stdError.readLine()) != null) {
					debugPrintln(line);
				}
				return false;
			}
			if (writeOutputToFile) {
				debugPrintln("Output written to: " + outputPath);
			}
			return true;
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			if (out != null)
				try {
					out.flush();
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}
}
