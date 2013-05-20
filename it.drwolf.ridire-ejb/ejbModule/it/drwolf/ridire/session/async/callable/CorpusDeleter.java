/*******************************************************************************
 * Copyright 2013 University of Florence
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package it.drwolf.ridire.session.async.callable;

import it.drwolf.ridire.index.ContextsIndexManager;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;

public class CorpusDeleter extends IndexingCommand {
	private String corpusName;
	private ContextsIndexManager contextsIndexManager;
	private float percentage = 0.0f;

	public CorpusDeleter(String corpusName) {
		super();
		this.corpusName = corpusName;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		try {
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			TopDocs termDocs = null;
			int updated = 0;
			int totToBeModified = 0;
			this.contextsIndexManager.reOpenIndexReaderW();
			termDocs = this.contextsIndexManager.getIndexSearcherW().search(
					new TermQuery(new Term("corpus", this.corpusName)),
					Integer.MAX_VALUE);
			totToBeModified = termDocs.totalHits;
			for (int i = 0; i < termDocs.totalHits; i += 30) {
				ScoreDoc[] tmp = (ScoreDoc[]) ArrayUtils.subarray(
						termDocs.scoreDocs, i, i + 30);
				for (ScoreDoc scoreDoc : tmp) {
					this.contextsIndexManager.reOpenIndexReaderW();
					Document d = this.contextsIndexManager.getIndexSearcherW()
							.doc(scoreDoc.doc);
					List<String> corporaNames = new ArrayList<String>();
					for (String cn : d.getValues("corpus")) {
						if (!cn.equals(this.corpusName)) {
							corporaNames.add(cn);
						}
					}
					d.removeFields("corpus");
					if (corporaNames.size() > 0) {
						for (String cn : corporaNames) {
							d.add(new Field("corpus", cn, Field.Store.YES,
									Field.Index.NOT_ANALYZED));
						}
					}
					try {
						this.contextsIndexManager.closeIndexWriter();
						this.contextsIndexManager.getIndexSearcherW()
								.getIndexReader().deleteDocument(scoreDoc.doc);
						this.contextsIndexManager.getIndexSearcherW()
								.getIndexReader().close();
						this.contextsIndexManager
								.openIndexWriterWithoutCreating();
						this.contextsIndexManager.getIndexWriter()
								.updateDocument(new Term("corpus"), d);
						this.contextsIndexManager.closeIndexWriter();
					} catch (RuntimeException e) {
						e.printStackTrace();
						throw e;
					}
					++updated;
					this.percentage = updated / (totToBeModified * 1.0f);
				}
			}
			this.contextsIndexManager.openIndexWriterWithoutCreating();
			this.contextsIndexManager.getIndexWriter().expungeDeletes();
			this.contextsIndexManager.closeIndexWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}
		Lifecycle.endCall();
		this.setTerminated(true);
		return indexingResult;
	}

	public String getDescription() {
		return "Deleting corpus: " + this.corpusName;
	}

	public float getPercentage() {
		return this.percentage;
	}

}
