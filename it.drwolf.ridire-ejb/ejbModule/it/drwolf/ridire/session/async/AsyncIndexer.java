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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.session.async.callable.CorpusCreator;
import it.drwolf.ridire.session.async.callable.CorpusDeleter;
import it.drwolf.ridire.session.async.callable.JobIndexer;
import it.drwolf.ridire.session.async.callable.JobResourcesRemover;
import it.drwolf.ridire.session.async.callable.JobToCorpusAdder;
import it.drwolf.ridire.session.async.callable.JobToCorpusAdder2;
import it.drwolf.ridire.session.async.callable.PerlemmaFLRemover;
import it.drwolf.ridire.session.async.callable.PerlemmaIndexer;
import it.drwolf.ridire.session.async.callable.SingleResourceIndexer;
import it.drwolf.ridire.session.async.callable.SingleResourceRemover;
import it.drwolf.ridire.session.async.callable.SingleResourceUpdater;
import it.drwolf.ridire.util.SelectableJob;

import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;

@Name("asyncIndexer")
@Scope(ScopeType.APPLICATION)
public class AsyncIndexer {

	@In(create = true)
	private IndexerService indexerService;

	@Asynchronous
	public void addJobToCorpora(List<SelectableJob> indexedJobs,
			List<String> corpusNames) {
		JobToCorpusAdder corpusAdder = new JobToCorpusAdder(indexedJobs,
				corpusNames);
		JobToCorpusAdder2 corpusAdder2 = new JobToCorpusAdder2(indexedJobs,
				corpusNames);
		this.indexerService.submitCallable(corpusAdder2);
	}

	@Asynchronous
	public void createCorpus(List<SelectableJob> indexedJobs,
			List<String> corpusNames) {
		CorpusCreator corpusCreator = new CorpusCreator(indexedJobs,
				corpusNames);
		this.indexerService.submitCallable(corpusCreator);
	}

	@Asynchronous
	public void deleteCorpus(String corpusName) {
		CorpusDeleter corpusDeleter = new CorpusDeleter(corpusName);
		this.indexerService.submitCallable(corpusDeleter);
	}

	@Asynchronous
	public void deleteJobResources(Integer jobId, boolean external,
			String jobName) {
		JobResourcesRemover jobResourcesRemover = new JobResourcesRemover(
				jobId, external, jobName);
		this.indexerService.submitCallable(jobResourcesRemover);
	}

	@Asynchronous
	public void deleteSingleCrawledResource(Integer crId) {
		SingleResourceRemover singleResourceRemover = new SingleResourceRemover(
				crId);
		this.indexerService.submitCallable(singleResourceRemover);
	}

	@Asynchronous
	public void indexJob(Integer jobId, String jobName) {
		JobIndexer jobIndexer = new JobIndexer(jobId, jobName);
		this.indexerService.submitCallable(jobIndexer);
	}

	@Asynchronous
	public void indexPerlemmaForAllDocs(String corpusName) {
		PerlemmaIndexer perlemmaIndexer = new PerlemmaIndexer();
		this.indexerService.submitCallable(perlemmaIndexer);
	}

	@Asynchronous
	public void indexSingleCrawledResource(Integer crId, String corpusName,
			boolean commit) {
		SingleResourceIndexer singleResourceIndexer = new SingleResourceIndexer(
				crId, corpusName, commit);
		this.indexerService.submitCallable(singleResourceIndexer);
	}

	@Asynchronous
	public void removePerlemmaFL() {
		PerlemmaFLRemover perlemmaFLRemover = new PerlemmaFLRemover();
		this.indexerService.submitCallable(perlemmaFLRemover);
	}

	@Asynchronous
	public void updateSingleCrawledResource(Integer crId, boolean commit) {
		SingleResourceUpdater singleResourceUpdater = new SingleResourceUpdater(
				crId, commit);
		this.indexerService.submitCallable(singleResourceUpdater);
	}
}
