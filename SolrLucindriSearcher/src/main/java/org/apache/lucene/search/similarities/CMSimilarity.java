package org.apache.lucene.search.similarities;

import org.apache.lucene.search.similarities.BasicStats;
import org.apache.lucene.search.similarities.LMDirichletSimilarity;

public class CMSimilarity extends LMDirichletSimilarity {

	@Override
	protected double score(BasicStats stats, double freq, double docLen) {
		return 1.0;
	}

}
