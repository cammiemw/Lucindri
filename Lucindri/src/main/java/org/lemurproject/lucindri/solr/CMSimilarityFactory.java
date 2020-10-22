package org.lemurproject.lucindri.solr;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;
import org.lemurproject.lucindri.searcher.similarities.CMSimilarity;

public class CMSimilarityFactory extends SimilarityFactory {

	@Override
	public void init(SolrParams params) {
		super.init(params);
	}

	@Override
	public Similarity getSimilarity() {
		CMSimilarity sim = new CMSimilarity();
		return sim;
	}

}
