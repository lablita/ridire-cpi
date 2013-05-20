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
package it.drwolf.ridire.session;

import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.session.async.AsyncCleaner;
import it.drwolf.ridire.session.async.FlagBearer;
import it.drwolf.ridire.session.async.WordCounter;
import it.drwolf.ridire.util.SelectableJob;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.Renderer;

@Name("resourcesAdmin")
@Scope(ScopeType.CONVERSATION)
public class ResourcesAdmin {

	@In
	private EntityManager entityManager;

	private String corpusName;
	private String corpusName4PerLemmaFL;

	private String testEmail;
	// @In(create = true)
	// private AsyncIndexer asyncIndexer;

	@In(create = true)
	private WordCounter wordCounter;

	// @In(create = true)
	// private ContextsIndexManager contextsIndexManager;

	@In(create = true)
	private FlagBearer flagBearer;

	@In(create = true)
	private AsyncCleaner asyncCleaner;

	@In(create = true)
	private Renderer renderer;

	public void cleanJob(Job job) {
		this.asyncCleaner.cleanJob(job.getId());
	}

	public void countWords(SelectableJob j) {
		this.wordCounter.countWords(j.getId());
	}

	public void countWordsForAllJobs() {
		for (Job j : (List<Job>) this.entityManager.createQuery("from Job")
				.getResultList()) {
			this.wordCounter.countWords(j.getId());
		}
	}

	public String getCorpusName() {
		return this.corpusName;
	}

	public String getCorpusName4PerLemmaFL() {
		return this.corpusName4PerLemmaFL;
	}

	public Long getJobWordsCount(Job j) {
		return (Long) this.entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.deleted=false and cr.noMoreAvailable=false")
				.setParameter("job", j).getSingleResult();
	}

	public String getTestEmail() {
		return this.testEmail;
	}

	public void setCorpusName(String corpusName) {
		this.corpusName = corpusName;
	}

	public void setTestEmail(String testEmail) {
		this.testEmail = testEmail;
	}

	public void stopPerlemmaFL() {
		this.flagBearer.setPerlemmaFLStopped(true);
	}

	public String testEmail() {
		this.flagBearer.setHostname(this.entityManager.find(Parameter.class,
				Parameter.HOSTNAME.getKey()).getValue());
		this.flagBearer.setJobName("testJob");
		this.flagBearer.setOwnerName("Nome");
		this.flagBearer.setOwnerSurname("Cognome");
		this.flagBearer.setEmailAddress(this.getTestEmail());
		this.renderer.render("/mail/mappedJob.xhtml");
		return "OK";
	}
}
