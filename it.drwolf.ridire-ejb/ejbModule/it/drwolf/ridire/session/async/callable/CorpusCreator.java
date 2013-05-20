/*******************************************************************************
 * Copyright 2013 Università degli Studi di Firenze
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

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.index.ContextsIndexManager;
import it.drwolf.ridire.util.SelectableJob;

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class CorpusCreator extends IndexingCommand {
	private static final int MAXRESULTS = 100;
	private List<SelectableJob> indexedJobs;
	private List<String> corpusNames;
	private ContextsIndexManager contextsIndexManager;
	private EntityManager entityManager;
	private UserTransaction userTx;

	private long totResources = 0L;

	private long indexedResource = 0L;

	private SelectableJob currentJob;

	public CorpusCreator(List<SelectableJob> indexedJobs,
			List<String> corpusNames) {
		super();
		this.indexedJobs = indexedJobs;
		this.corpusNames = corpusNames;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		try {
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.contextsIndexManager.closeIndexWriter();
			this.contextsIndexManager.getIndexSearcherW().getIndexReader()
					.close();
			this.entityManager = (EntityManager) Component
					.getInstance("entityManager");
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			int jobsToAdd = 0;
			for (int i = 0; i < this.indexedJobs.size(); i++) {
				if (this.indexedJobs.get(i).isSelectedForCorpusCreation()) {
					this.setCurrentJob(this.indexedJobs.get(i));
					if (!this.indexedJobs.get(i).isExternal()) {
						if (!this.userTx.isActive()) {
							this.userTx.begin();
						}
						this.entityManager.joinTransaction();
						this.totResources += (Long) this.entityManager
								.createQuery(
										"select count(cr.id) from CrawledResource cr where cr.deleted is false and cr.job.id=:jId and cr.wordsNumber>0")
								.setParameter("jId",
										this.indexedJobs.get(i).getId())
								.getSingleResult();
						this.entityManager.flush();
						this.entityManager.clear();
						this.userTx.commit();
						++jobsToAdd;
					} else {
						this.totResources += this.contextsIndexManager
								.getResourcesNumberOfJob(this.indexedJobs
										.get(i).getName());
					}
				}
			}
			System.out.println("Adding job to corpus; total resources "
					+ this.totResources);
			int addedJobs = 0;
			for (int i = 0; i < this.indexedJobs.size(); i++) {
				SelectableJob selectableJob = this.indexedJobs.get(i);
				if (selectableJob.isSelectedForCorpusCreation()) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					System.out.println("Adding job " + addedJobs + " of "
							+ jobsToAdd);
					this.entityManager.joinTransaction();
					Job j = this.entityManager.find(Job.class,
							selectableJob.getId());
					this.setCurrentJob(selectableJob);
					this.entityManager.flush();
					this.entityManager.clear();
					this.userTx.commit();
					for (int k = 0; k < this.totResources; k += CorpusCreator.MAXRESULTS) {
						if (!this.userTx.isActive()) {
							this.userTx.begin();
						}
						this.entityManager.joinTransaction();
						List<CrawledResource> crawledResources = this.entityManager
								.createQuery(
										"from CrawledResource cr where cr.deleted is false and cr.job=:j and cr.wordsNumber>0")
								.setParameter("j", j).setFirstResult(k)
								.setMaxResults(CorpusCreator.MAXRESULTS)
								.getResultList();
						for (CrawledResource cr : crawledResources) {
							this.contextsIndexManager.addResourceToCorpus(cr,
									this.corpusNames, false);
							++this.indexedResource;
						}
						this.entityManager.flush();
						this.entityManager.clear();
						this.userTx.commit();
						System.out.println("Adding job; resource " + k + " of "
								+ this.totResources);
					}
					this.contextsIndexManager.closeIndexWriter();
					++addedJobs;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
				}
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
		Lifecycle.endCall();
		this.setTerminated(true);
		return indexingResult;
	}

	public List<String> getCorpusName() {
		return this.corpusNames;
	}

	public SelectableJob getCurrentJob() {
		return this.currentJob;
	}

	public String getDescription() {
		return "Creating corpora: " + StringUtils.join(this.corpusNames, ", ");
	}

	public float getPercentage() {
		if (this.totResources == 0L) {
			return 0.0f;
		}
		return this.indexedResource / (this.totResources * 1.0f);
	}

	private void setCurrentJob(SelectableJob currentJob) {
		this.currentJob = currentJob;
	}

}
