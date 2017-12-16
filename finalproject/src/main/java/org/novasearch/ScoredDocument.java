package org.novasearch;

import org.apache.lucene.document.Document;

/**
 * 
 * A Document from the index with its ranked score
 */
class ScoredDocument
{
	private float score;
	private Document document;

	public ScoredDocument()
	{
		this(-1.f, null);
	}

	public ScoredDocument(float score, Document document)
	{
		super();
		this.score = score;
		this.document = document;
	}

	public float getScore()
	{
		return score;
	}

	public void setScore(float score)
	{
		this.score = score;
	}

	public Document getDocument()
	{
		return document;
	}

	public void setDocument(Document document)
	{
		this.document = document;
	}

}
