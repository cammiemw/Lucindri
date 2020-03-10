package org.lemurproject.lucindri.solr;

import org.apache.lucene.search.similarities.Similarity;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.schema.SimilarityFactory;
import org.lemurproject.lucindri.searcher.similarities.IndriDirichletSimilarity;

public class IndriDirichletSimilarityFactory extends SimilarityFactory {

	private Float mu;

	@Override
	public void init(SolrParams params) {
		super.init(params);
		mu = params.getFloat("mu");
	}

	@Override
	public Similarity getSimilarity() {
		Similarity sim = (mu != null) ? new IndriDirichletSimilarity(mu) : new IndriDirichletSimilarity();
		return sim;
	}

}
