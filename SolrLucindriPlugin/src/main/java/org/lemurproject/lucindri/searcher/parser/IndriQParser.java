package org.lemurproject.lucindri.searcher.parser;

import java.io.IOException;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.SyntaxError;

public class IndriQParser extends QParser {

	public IndriQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req) {
		super(qstr, localParams, params, req);
	}

	@Override
	public Query parse() throws SyntaxError {
		// String fieldName = params.getParams("qf")[0];
		Query query = null;

		IndriQueryParser parser;
		try {
			parser = new IndriQueryParser("fulltext_lucindri");
			query = parser.parseQuery(qstr);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return query;
	}

}
