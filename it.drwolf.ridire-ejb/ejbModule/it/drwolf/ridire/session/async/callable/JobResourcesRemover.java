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

import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.ContextsIndexManager;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class JobResourcesRemover extends IndexingCommand {
	private Integer jobId;
	private ContextsIndexManager contextsIndexManager;
	private float percentage = 0.0f;
	private UserTransaction userTx;
	private EntityManager entityManager;
	private boolean external = false;
	private String jobName;

	public JobResourcesRemover(Integer jobId, boolean external, String jobName) {
		super();
		this.jobId = jobId;
		this.jobName = jobName;
		this.external = external;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			this.userTx.begin();
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			if (!this.external) {
				this.contextsIndexManager.deleteResourcesOfJob(this.jobId);
				this.entityManager = (EntityManager) Component
						.getInstance("entityManager");
				this.entityManager.joinTransaction();
				this.entityManager
						.createQuery(
								"update CrawledResource cr set cr.processed=:status where cr.job.id=:jobId")
						.setParameter("status", Parameter.FINISHED)
						.setParameter("jobId", this.jobId).executeUpdate();
				this.entityManager
						.createQuery(
								"update Job j set j.indexed=:indexed where j.id=:jobId")
						.setParameter("indexed", false)
						.setParameter("jobId", this.jobId).executeUpdate();
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
			} else {
				this.contextsIndexManager.deleteResourcesOfJob(this.jobName);
			}
			this.percentage = 1.0f;
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
		return "Removing resources of job: " + this.jobId;
	}

	public float getPercentage() {
		return this.percentage;
	}

}
