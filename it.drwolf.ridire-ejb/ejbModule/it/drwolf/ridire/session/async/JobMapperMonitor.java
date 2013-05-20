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

//import it.drwolf.ridire.cleaners.ReadabilityCleaner;
import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.session.async.factories.PoolThreadFactory;
import it.drwolf.ridire.session.ssl.EasySSLProtocolSocketFactory;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthChallengeProcessor;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.archive.crawler.framework.CrawlStatus;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.async.Expiration;
import org.jboss.seam.annotations.async.FinalExpiration;
import org.jboss.seam.annotations.async.IntervalCron;
import org.jboss.seam.async.QuartzTriggerHandle;
import org.jboss.seam.transaction.UserTransaction;

@Name("jobMapperMonitor")
@Scope(ScopeType.APPLICATION)
public class JobMapperMonitor {

	private static final int DELAY = 10000;

	private static final int THREADS_PRIORITY = 5;

	private ThreadPoolExecutor threadPool = null;

	public static final String FILE_SEPARATOR = System
			.getProperty("file.separator");

	@In(required = true, create = true)
	private FlagBearer flagBearer;

	// @In(create = true)
	// private NCleaner ncleaner;
	// @In(create = true)
	// private AlchemyCleaner alchemyCleaner;
	// @In(create = true)
	// private ReadabilityCleaner readabilityCleaner;
	@In
	private EntityManager eventEntityManager;

	private UserTransaction mainUserTx;

	public static final String RESOURCESDIR = "resources/";

	private HttpClient httpClient = null;
	private PoolThreadFactory highPoolThreadFactory;

	private Map<Integer, Mapper> mappers = new HashMap<Integer, Mapper>();
	public static String JOBSDIR;

	private void create() {
		int transactionTimeoutSeconds = 240;
		try {
			((javax.transaction.UserTransaction) org.jboss.seam.transaction.Transaction
					.instance())
					.setTransactionTimeout(transactionTimeoutSeconds);
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Protocol.registerProtocol("https", new Protocol("https",
				new EasySSLProtocolSocketFactory(), 8443));
		this.httpClient = new HttpClient();
		this.httpClient.getParams().setAuthenticationPreemptive(true);
		this.mainUserTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		Credentials defaultcreds = null;
		int jobsToBeProcessed = 4;
		try {
			this.mainUserTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			this.mainUserTx.begin();
			this.eventEntityManager.joinTransaction();
			defaultcreds = new UsernamePasswordCredentials("admin",
					this.eventEntityManager.find(CommandParameter.class,
							CommandParameter.HERITRIX_ADMINPW_KEY)
							.getCommandValue());
			jobsToBeProcessed = Integer.parseInt(this.eventEntityManager.find(
					Parameter.class, Parameter.JOBS_TO_BE_PROCESSED.getKey())
					.getValue());
			JobMapperMonitor.JOBSDIR = this.eventEntityManager.find(
					Parameter.class, Parameter.JOBS_DIR.getKey()).getValue();
			this.flagBearer.setHostname(this.eventEntityManager.find(
					Parameter.class, Parameter.HOSTNAME.getKey()).getValue());
			this.eventEntityManager.flush();
			this.mainUserTx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.httpClient.getState().setCredentials(
				new AuthScope(AuthScope.ANY_SCHEME, AuthScope.ANY_PORT,
						AuthScope.ANY_REALM), defaultcreds);
		Logger httpClientlogger = Logger.getLogger(this.httpClient.getClass());
		httpClientlogger.setLevel(Level.ERROR);
		Logger authChallengeProcessorLogger = Logger
				.getLogger(AuthChallengeProcessor.class);
		authChallengeProcessorLogger.setLevel(Level.ERROR);
		Logger httpMethodBaseLogger = Logger.getLogger(HttpMethodBase.class);
		httpMethodBaseLogger.setLevel(Level.ERROR);
		this.highPoolThreadFactory = new PoolThreadFactory(
				JobMapperMonitor.THREADS_PRIORITY);
		this.threadPool = new ThreadPoolExecutor(jobsToBeProcessed,
				jobsToBeProcessed, 100, TimeUnit.SECONDS,
				new LinkedBlockingQueue<Runnable>(), this.highPoolThreadFactory);
	}

	@SuppressWarnings("unchecked")
	private void lookForNotMappedJob() {
		JobSizeComparator jobSizeComparator = new JobSizeComparator();
		this.mappers.clear();
		try {
			this.mainUserTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.mainUserTx.setTransactionTimeout(10 * 10 * 60); // set timeout
																	// to
																	// 10 mins
			do {
				this.mainUserTx.begin();
				this.eventEntityManager.joinTransaction();
				List<Job> jobs = this.eventEntityManager
						.createQuery(
								"from Job j where j.mappedResources=:mp and (j.jobStage=:aborted or j.jobStage=:finished)")
						.setParameter("aborted", CrawlStatus.ABORTED.toString())
						.setParameter("finished",
								CrawlStatus.FINISHED.toString())
						.setParameter("mp", false).getResultList();
				this.eventEntityManager.flush();
				this.mainUserTx.commit();
				BlockingQueue<Runnable> q = this.threadPool.getQueue();
				Collections.sort(jobs, jobSizeComparator);
				for (Job job : jobs) {
					// look if job is awaiting in the queue
					boolean skipThis = false;
					Iterator<Runnable> itOnQ = q.iterator();
					while (itOnQ.hasNext()) {
						Mapper m = (Mapper) itOnQ.next();
						if (job.getId() != null
								&& job.getId().equals(m.getJobId())) {
							skipThis = true;
							break;
						}
					}
					if (skipThis) {
						continue;
					}
					// look if job is running or completed
					if (!this.mappers.containsKey(job.getId())) {
						Mapper mapper = new Mapper(job, this.flagBearer);
						this.threadPool.execute(mapper);
						this.mappers.put(job.getId(), mapper);
					}
				}
				try {
					Thread.sleep(JobMapperMonitor.DELAY);
				} catch (InterruptedException e) {
					continue;
				}
			} while (this.threadPool.getActiveCount() > 0);
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (this.mainUserTx != null && this.mainUserTx.isActive()) {
					this.mainUserTx.rollback();
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
	}

	@Asynchronous
	public QuartzTriggerHandle lookForNotMappedJob(
			@Expiration Date expirationDate, @IntervalCron String cronData,
			@FinalExpiration Date endDate) {
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE job mapper monitor");
		if (!this.flagBearer.isJobMapperRunning()
				&& !this.flagBearer.isMappingSuspended()) {
			this.flagBearer.setJobMapperRunning(true);
			System.out.println("Job mapper running");
			this.create();
			this.lookForNotMappedJob();
			this.flagBearer.setJobMapperRunning(false);
			System.out.println("Job mapper done");
		}
		return handle;
	}
}
