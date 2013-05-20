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
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.ContextsIndexManager;

import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class JobIndexer extends IndexingCommand {
	private static final int STEP = 10;
	private ContextsIndexManager contextsIndexManager;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private Integer jobId;
	private String jobName = "";
	private float percentage = 0.0f;

	public JobIndexer(Integer jobId, String jobName) {
		super();
		this.jobId = jobId;
		this.jobName = jobName;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		try {
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.entityManager = (EntityManager) Component
					.getInstance("entityManager");
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			if (this.jobId == null) {
				return indexingResult;
			}
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			Job j = this.entityManager.find(Job.class, this.jobId);
			if (j != null && j.isMappedResources() && !j.isIndexed()) {
				long crawledResourcesSize = (Long) this.entityManager
						.createQuery(
								"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.deleted is false and cr.processed<>:indexed and cr.wordsNumber>0")
						.setParameter("job", j)
						.setParameter("indexed", Parameter.INDEXED)
						.getSingleResult();
				System.out
						.println("Job: " + j.getName()
								+ " - Resources to be indexed: "
								+ crawledResourcesSize);
				for (int x = 0; x < crawledResourcesSize; x += JobIndexer.STEP) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					this.entityManager.joinTransaction();
					List<CrawledResource> crawledResources = this.entityManager
							.createQuery(
									"from CrawledResource cr where cr.job=:job and cr.deleted is false and cr.processed<>:indexed and cr.wordsNumber>0 order by cr.id")
							.setParameter("job", j)
							.setParameter("indexed", Parameter.INDEXED)
							.setFirstResult(x).setMaxResults(JobIndexer.STEP)
							.getResultList();
					this.entityManager.flush();
					this.entityManager.clear();
					this.userTx.commit();
					for (int i = 0; i < crawledResources.size(); i++) {
						if (!this.userTx.isActive()) {
							this.userTx.begin();
						}
						this.entityManager.joinTransaction();
						CrawledResource cr = crawledResources.get(i);
						cr.setProcessed(Parameter.INDEXING_PHASE);
						this.entityManager.merge(cr);
						this.entityManager.flush();
						if (this.contextsIndexManager
								.indexSingleCrawledResource(cr.getId(), null,
										false, null)) {
							cr.setProcessed(Parameter.INDEXED);
						} else {
							cr.setProcessed(Parameter.FINISHED);
						}
						this.entityManager.merge(cr);
						this.entityManager.flush();
						this.entityManager.clear();
						this.userTx.commit();
						this.percentage = i + JobIndexer.STEP * x * 1.0f
								/ crawledResourcesSize;
					}
					System.out.println("Job: " + j.getName()
							+ " - Indexed resources: "
							+ (x / JobIndexer.STEP + 1) * JobIndexer.STEP);
				}
				this.contextsIndexManager.closeIndexWriter();
			}
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			j = this.entityManager.merge(j);
			j.setIndexed(true);
			this.percentage = 1.0f;
			this.entityManager.persist(j);
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
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
		return this.percentage;
	}

}
