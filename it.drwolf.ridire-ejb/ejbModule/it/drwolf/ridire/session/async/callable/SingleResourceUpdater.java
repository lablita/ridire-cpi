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

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.ContextsIndexManager;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.transaction.UserTransaction;

public class SingleResourceUpdater extends IndexingCommand {
	private Integer crId;
	private boolean commit;
	private ContextsIndexManager contextsIndexManager;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private float percentage = 0.0f;

	public SingleResourceUpdater(Integer crId, boolean commit) {
		super();
		this.crId = crId;
		this.commit = commit;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		try {
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			this.userTx.begin();
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.entityManager = (EntityManager) Component
					.getInstance("entityManager");
			this.entityManager.joinTransaction();
			this.contextsIndexManager.updateSingleCrawledResource(this.crId,
					this.commit);
			CrawledResource cr = this.entityManager.find(CrawledResource.class,
					this.crId);
			if (cr != null) {
				cr.setProcessed(Parameter.INDEXED);
				this.entityManager.merge(cr);
			}
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
		return "Updating single resource: " + this.crId;
	}

	public float getPercentage() {
		return this.percentage;
	}
}
