/*
 * ===============================================================================================
 * Copyright (c) 2019 Carnegie Mellon University and University of Massachusetts. All Rights
 * Reserved.
 *
 * Use of the Lemur Toolkit for Language Modeling and Information Retrieval is subject to the terms
 * of the software license set forth in the LICENSE file included with this software, and also
 * available at http://www.lemurproject.org/license.html
 *
 * ================================================================================================
 */
package org.lemurproject.lucindri.searcher;

import java.io.IOException;

import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.search.LeafSimScorer;
import org.apache.lucene.search.Weight;

public class IndriTermScorer extends IndriScorer {

	private final PostingsEnum postingsEnum;
	private final DocIdSetIterator iterator;
	private final LeafSimScorer docScorer;

	protected IndriTermScorer(Weight weight, PostingsEnum postingsEnum, LeafSimScorer docScorer, float boost) {
		super(weight, boost);
		this.docScorer = docScorer;
		iterator = this.postingsEnum = postingsEnum;
	}

	@Override
	public float smoothingScore(int docId) throws IOException {
		return docScorer.score(docId, 0);
	}

	@Override
	public int docID() {
		return postingsEnum.docID();
	}

	@Override
	public float score() throws IOException {
		assert docID() != DocIdSetIterator.NO_MORE_DOCS;
		return docScorer.score(postingsEnum.docID(), postingsEnum.freq());
	}

	@Override
	public DocIdSetIterator iterator() {
		return iterator;
	}

	@Override
	public float getMaxScore(int upTo) throws IOException {
		return 0;
	}

	final int freq() throws IOException {
		return postingsEnum.freq();
	}

}
