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

import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.index.ContextsIndexManager;
import it.drwolf.ridire.util.SelectableJob;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class JobToCorpusAdder2 extends IndexingCommand {
	private static final int STEP = 10;
	private ContextsIndexManager contextsIndexManager;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private List<SelectableJob> indexedJobs;
	private List<String> corpusNames;
	private float percentage = 0.0f;
	private String jobName;
	private Long totResources = 0L;
	private Long indexedResources = 0L;

	public JobToCorpusAdder2(List<SelectableJob> indexedJobs,
			List<String> corpusNames) {
		this.indexedJobs = indexedJobs;
		this.corpusNames = corpusNames;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		this.contextsIndexManager = (ContextsIndexManager) Component
				.getInstance("contextsIndexManager");
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			for (SelectableJob selectableJob : this.indexedJobs) {
				if (selectableJob.isSelectedForCorpusCreation()) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					this.entityManager.joinTransaction();
					this.totResources += (Long) this.entityManager
							.createQuery(
									"select count(cr.id) from CrawledResource cr where cr.job.id=:job and cr.deleted is false and cr.wordsNumber>0")
							.setParameter("job", selectableJob.getId())
							.getSingleResult();
					this.entityManager.flush();
					this.entityManager.clear();
					this.userTx.commit();
				}
			}
			for (SelectableJob selectableJob : this.indexedJobs) {
				if (selectableJob.isSelectedForCorpusCreation()) {
					List<String> newNames = new ArrayList<String>();
					newNames.addAll(selectableJob.getCorporaName());
					for (String nn : this.corpusNames) {
						if (!newNames.contains(nn)) {
							newNames.add(nn);
						}
					}
					this.contextsIndexManager
							.deleteResourcesOfJob(selectableJob.getId());
					this.indexJob(selectableJob.getId(), newNames);
				}
			}
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
		return indexingResult;
	}

	public String getDescription() {
		return "Indexing job: " + this.jobName;
	}

	public float getPercentage() {
		if (this.totResources < 1) {
			return 0.0f;
		}
		return this.indexedResources / (this.totResources * 1.0f);
	}

	private void indexJob(Integer jobId, List<String> newNames)
			throws SystemException, NotSupportedException, SecurityException,
			IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		Job j = this.entityManager.find(Job.class, jobId);
		this.jobName = j.getName();
		if (j != null && j.isMappedResources()) {
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			long crawledResourcesSize = (Long) this.entityManager
					.createQuery(
							"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.deleted is false and cr.wordsNumber>0")
					.setParameter("job", j).getSingleResult();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			System.out.println("Job: " + j.getName()
					+ " - Resources to be indexed: " + crawledResourcesSize);
			for (int x = 0; x < crawledResourcesSize; x += JobToCorpusAdder2.STEP) {
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				this.entityManager.joinTransaction();
				List<Integer> crawledResourcesId = this.entityManager
						.createQuery(
								"select cr.id from CrawledResource cr where cr.job=:job and cr.deleted is false and cr.wordsNumber>0 order by cr.id")
						.setParameter("job", j).setFirstResult(x)
						.setMaxResults(JobToCorpusAdder2.STEP).getResultList();
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
				for (int i = 0; i < crawledResourcesId.size(); i++) {
					this.contextsIndexManager
							.indexSingleCrawledResourceNoDBWriting(
									crawledResourcesId.get(i),
									newNames.toArray(new String[newNames.size()]),
									false, null);
				}
				this.indexedResources += crawledResourcesId.size();
				System.out.println("Job: " + j.getName()
						+ " - Indexed resources: " + this.indexedResources
						+ " on total " + this.totResources);
			}
			this.contextsIndexManager.closeIndexWriter();
		}
	}
}
