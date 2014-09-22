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

import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.util.data.CorpusName;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("flagBearer")
@Scope(ScopeType.APPLICATION)
public class FlagBearer {

	private boolean jobMapperRunning = false;
	private boolean resourcesUpdaterRunning = false;
	private String ownerName;
	private String ownerSurname;
	private String emailAddress;
	private String hostname;
	private String jobName;
	private boolean mappingSuspended = true;
	private List<Integer> jobUnderCleaning = new ArrayList<Integer>();
	private boolean mergingStopped = true;
	private boolean perlemmaFLStopped = true;

	private boolean collocationExtractionGoing = false;
	// @In(create = true)
	// private ContextsIndexManager contextsIndexManager;

	@In
	private EntityManager entityManager;

	private List<CorpusName> corpora;

	public void addJobToCleaningProcess(Integer jobId) {
		this.jobUnderCleaning.add(jobId);
	}

	public List<CorpusName> getCorpora() {
		return this.corpora;
	}

	// public List<CorpusName> getCorporaList() {
	// List<CorpusName> corporaNames = new ArrayList<CorpusName>();
	// if (this.isIndexingEnabled()) {
	// try {
	// this.contextsIndexManager.reOpenIndexReaderR();
	// TermEnum termEnum = this.contextsIndexManager
	// .getIndexSearcherR().getIndexReader()
	// .terms(new Term("corpus", ""));
	// if (termEnum != null) {
	// while (termEnum.term() != null
	// && "corpus".equals(termEnum.term().field())) {
	// String cn = termEnum.term().text();
	// TermQuery tq = new TermQuery(new Term("corpus", cn));
	// TopDocs td = this.contextsIndexManager
	// .getIndexSearcherR().search(tq, 1);
	// if (td.totalHits > 0) {
	// CorpusName corpusName = new CorpusName();
	// corpusName.setCorpusName(cn);
	// corporaNames.add(corpusName);
	// }
	// if (!termEnum.next()) {
	// break;
	// }
	// }
	// }
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// }
	// this.setCorpora(corporaNames);
	// return corporaNames;
	// }

	public String getEmailAddress() {
		return this.emailAddress;
	}

	public String getHostname() {
		return this.hostname;
	}

	public String getJobName() {
		return this.jobName;
	}

	public String getOwnerName() {
		return this.ownerName;
	}

	public String getOwnerSurname() {
		return this.ownerSurname;
	}

	public boolean isCollocationExtractionGoing() {
		return this.collocationExtractionGoing;
	}

	public boolean isIndexingEnabled() {
		return this.entityManager.find(Parameter.class,
				Parameter.INDEXING_ENABLED.getKey()).getValue().equals("true") ? true
				: false;
	}

	public boolean isJobMapperRunning() {
		return this.jobMapperRunning;
	}

	public boolean isJobUnderCleaning(Integer jobId) {
		return this.jobUnderCleaning.contains(jobId);
	}

	public boolean isMappingSuspended() {
		return this.mappingSuspended;
	}

	public boolean isMergingStopped() {
		return this.mergingStopped;
	}

	public boolean isPerlemmaFLStopped() {
		return this.perlemmaFLStopped;
	}

	public boolean isResourcesUpdaterRunning() {
		return this.resourcesUpdaterRunning;
	}

	public void removeJobFromCleaningProcess(Integer jobId) {
		this.jobUnderCleaning.remove(jobId);
	}

	public void setCollocationExtractionGoing(boolean collocationExtractionGoing) {
		this.collocationExtractionGoing = collocationExtractionGoing;
	}

	public void setCorpora(List<CorpusName> corpora) {
		this.corpora = corpora;
	}

	public void setEmailAddress(String emailAddress) {
		this.emailAddress = emailAddress;
	}

	public void setHostname(String hostname) {
		this.hostname = hostname;
	}

	public void setJobMapperRunning(boolean jobMapperRunning) {
		this.jobMapperRunning = jobMapperRunning;
	}

	public void setJobName(String jobName) {
		this.jobName = jobName;
	}

	public void setMergingStopped(boolean stopped) {
		this.mergingStopped = stopped;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}

	public void setOwnerSurname(String ownerSurname) {
		this.ownerSurname = ownerSurname;
	}

	public void setPerlemmaFLStopped(boolean perlemmaFLStopped) {
		this.perlemmaFLStopped = perlemmaFLStopped;
	}

	public void setResourcesUpdaterRunning(boolean resourcesUpdaterRunning) {
		this.resourcesUpdaterRunning = resourcesUpdaterRunning;
	}

	public void toggleMappingSuspended() {
		this.mappingSuspended = !this.mappingSuspended;
	}

}
