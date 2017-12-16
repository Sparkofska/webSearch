package org.novasearch;

import java.util.List;

public interface Clusterer
{
	public List<ScoredDocument> removeDublicates(List<ScoredDocument> set, int minRemainingElements);
}
