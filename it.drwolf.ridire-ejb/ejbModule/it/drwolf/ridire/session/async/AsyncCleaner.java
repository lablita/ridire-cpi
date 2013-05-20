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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.session.JobCleaner;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

@Name("asyncCleaner")
@Scope(ScopeType.APPLICATION)
public class AsyncCleaner {

	private static final int MAXRESULTS = 10;
	@In
	private FlagBearer flagBearer;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private Long totResources = 0L;
	private float progress = 0.0f;

	@Asynchronous
	public void cleanJob(Integer jobId) {
		this.flagBearer.addJobToCleaningProcess(jobId);
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String perlUser = this.entityManager.find(Parameter.class,
					Parameter.PERL_CLEANER_USER.getKey()).getValue();
			String perlPw = this.entityManager.find(Parameter.class,
					Parameter.PERL_CLEANER_PW.getKey()).getValue();
			String cleanerPath = this.entityManager.find(Parameter.class,
					Parameter.PERL_CLEANER_PATH.getKey()).getValue();
			String treeTaggerBin = this.entityManager.find(
					CommandParameter.class,
					CommandParameter.TREETAGGER_EXECUTABLE_KEY)
					.getCommandValue();
			Job job = this.entityManager.find(Job.class, jobId);
			int resDone = 0;
			if (job != null) {
				String cleaningScript = job.getCleaningScript();
				JobCleaner cleaner = new JobCleaner(perlUser, perlPw,
						cleanerPath, cleaningScript, treeTaggerBin);
				this.totResources = (Long) this.entityManager
						.createQuery(
								"select count(cr.id) from CrawledResource cr where cr.deleted is false and cr.wordsNumber>0 and cr.job=:j")
						.setParameter("j", job).getSingleResult();
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
				for (int k = 0; k < this.totResources; k += AsyncCleaner.MAXRESULTS) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					this.entityManager.joinTransaction();
					List<CrawledResource> crawledResources = this.entityManager
							.createQuery(
									"from CrawledResource cr where cr.deleted is false and cr.job=:j and cr.wordsNumber>0")
							.setParameter("j", job).setFirstResult(k)
							.setMaxResults(AsyncCleaner.MAXRESULTS)
							.getResultList();
					List<String> arcFiles = new ArrayList<String>();
					List<String> digests = new ArrayList<String>();
					for (CrawledResource cr : crawledResources) {
						arcFiles.add(cr.getArcFile());
						digests.add(cr.getDigest());
					}
					cleaner.clean(arcFiles, digests);
					resDone += AsyncCleaner.MAXRESULTS;
					this.setProgress(resDone / (this.totResources * 1.0f));
					this.entityManager.flush();
					this.entityManager.clear();
					this.userTx.commit();
				}
			} else {
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
			}
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.flagBearer.removeJobFromCleaningProcess(jobId);
	}

	public Integer getPercentageInt() {
		return Math.round(this.getProgress() * 100);
	}

	public float getProgress() {
		return this.progress;
	}

	public void setProgress(float progress) {
		this.progress = progress;
	}

}
