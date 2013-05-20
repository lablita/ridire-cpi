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

import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.ContextsIndexManager;
import it.drwolf.ridire.session.async.FlagBearer;

import java.io.File;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class PerlemmaIndexer extends IndexingCommand {

	private static final int STEP = 100;
	private ContextsIndexManager contextsIndexManager;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private FlagBearer flagBearer;

	public IndexingResult call() throws Exception {
		Lifecycle.beginCall();
		this.flagBearer = (FlagBearer) Component.getInstance("flagBearer");
		this.flagBearer.setPerlemmaFLStopped(false);
		IndexingResult indexingResult = new IndexingResult();
		try {
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.entityManager = (EntityManager) Component
					.getInstance("entityManager");
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String corpusName = this.entityManager.find(Parameter.class,
					Parameter.RIDIRETESTCorpus.getKey()).getValue();
			// local jobs
			List<Integer> jobIds = this.entityManager.createQuery(
					"select j.id from Job j where j.indexed=true")
					.getResultList();
			// new ArrayList<Integer>();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			int jobCount = 1;
			for (Integer jobId : jobIds) {
				BooleanQuery bq = new BooleanQuery();
				bq.add(new TermQuery(new Term("jobId", jobId + "")), Occur.MUST);
				bq.add(new TermQuery(new Term("corpus", corpusName)),
						Occur.MUST);
				TopDocs topDocs = this.contextsIndexManager.getIndexSearcherR()
						.search(bq, 1);
				if (topDocs == null || topDocs.totalHits < 1) {
					continue;
				}
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				this.entityManager.joinTransaction();
				long crawledResourcesSize = (Long) this.entityManager
						.createQuery(
								"select count(cr.id) from CrawledResource cr where cr.job.id=:jobId and cr.deleted is false and cr.processed=:indexed and cr.wordsNumber>0")
						.setParameter("jobId", jobId)
						.setParameter("indexed", Parameter.INDEXED)
						.getSingleResult();
				System.out.println("Perlemma FL Local Job: " + jobId
						+ " - Resources to be indexed: " + crawledResourcesSize
						+ " (" + jobCount + "/" + jobIds.size() + ")");
				++jobCount;
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
				for (int x = 0; x < crawledResourcesSize; x += PerlemmaIndexer.STEP) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					this.entityManager.joinTransaction();
					List<Integer> crawledResourcesIds = this.entityManager
							.createQuery(
									"select cr.id from CrawledResource cr where cr.job.id=:jobId and cr.deleted is false and cr.processed=:indexed and cr.wordsNumber>0 order by cr.id")
							.setParameter("jobId", jobId)
							.setParameter("indexed", Parameter.INDEXED)
							.setFirstResult(x)
							.setMaxResults(PerlemmaIndexer.STEP)
							.getResultList();
					this.entityManager.flush();
					this.entityManager.clear();
					this.userTx.commit();
					for (int i = 0; i < crawledResourcesIds.size(); i++) {
						this.contextsIndexManager
								.indexSingleCrawledResource4PerlemmaFL(
										crawledResourcesIds.get(i), corpusName,
										null, null);
					}
					System.out.println("Perlemma FL Local Job: " + jobId
							+ " - Indexed resources: "
							+ (x + crawledResourcesIds.size()) + " on total "
							+ crawledResourcesSize);
					if (this.isInterrupted()) {
						this.setTerminated(true);
						this.flagBearer.setPerlemmaFLStopped(false);
						return indexingResult;
					}
				}
			}
			// external jobs
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String www1localstore = this.entityManager.find(Parameter.class,
					Parameter.WWW1_LOCAL_STORE.getKey()).getValue();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			File[] externalJobs = new File(www1localstore).listFiles();
			jobCount = 0;
			for (File externalJob : externalJobs) {
				File[] externalJobFiles = externalJob.listFiles();
				if (externalJobFiles == null || externalJobFiles.length < 1) {
					continue;
				}
				System.out.println("Perlemma FL External Job: "
						+ externalJob.getName()
						+ " - Resources to be indexed: "
						+ externalJobFiles.length + " (" + jobCount + "/"
						+ externalJobs.length + ")");
				++jobCount;
				int resCount = 0;
				for (File externalJobFile : externalJobFiles) {
					this.contextsIndexManager
							.indexSingleCrawledResource4PerlemmaFL(null,
									corpusName,
									externalJobFile.getAbsolutePath(),
									externalJob.getName());
					++resCount;
					if (resCount % 10 == 0) {
						System.out.println("Perlemma FL External Job: "
								+ externalJob.getName()
								+ " - Indexed resources: " + resCount
								+ " on total " + externalJobFiles.length);
						if (this.isInterrupted()) {
							this.setTerminated(true);
							this.flagBearer.setPerlemmaFLStopped(false);
							return indexingResult;
						}
					}
				}
			}
			this.contextsIndexManager.closeIndexWriter();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
				}
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		Lifecycle.endCall();
		this.setTerminated(true);
		this.flagBearer.setPerlemmaFLStopped(false);
		return indexingResult;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

	private boolean isInterrupted() {
		return this.flagBearer.isPerlemmaFLStopped();
	}

}
