package org.lemurproject.lucindri.searcher.parser;

import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;

public class IndriQParserPlugin extends QParserPlugin {

	@Override
	public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		QParser indriParser = new IndriQParser(qstr, localParams, params, req);
		return indriParser;
	}

}
