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

//import it.drwolf.ridire.cleaners.ReadabilityCleaner;
import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.session.CrawlerManager;
import it.drwolf.ridire.session.ssl.EasySSLProtocolSocketFactory;
import it.drwolf.ridire.util.exceptions.HeritrixException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthChallengeProcessor;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.archive.crawler.framework.CrawlStatus;
import org.dom4j.DocumentException;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.async.Expiration;
import org.jboss.seam.annotations.async.FinalExpiration;
import org.jboss.seam.annotations.async.IntervalCron;
import org.jboss.seam.async.QuartzTriggerHandle;
import org.xml.sax.SAXException;

@Name("jobDBDataUpdater")
@Scope(ScopeType.APPLICATION)
public class JobDBDataUpdater {

	private HttpClient httpClient;

	@In
	private EntityManager entityManager;

	private String engineUri;

	private String jobsDir;

	private Pattern progressStatisticsDatePattern = Pattern.compile(
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", Pattern.MULTILINE);

	private SimpleDateFormat progressStatisticsDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

	private void create() {
		Protocol.registerProtocol("https", new Protocol("https",
				new EasySSLProtocolSocketFactory(), 8443));
		this.httpClient = new HttpClient();
		this.httpClient.getParams().setAuthenticationPreemptive(true);
		Credentials defaultcreds = new UsernamePasswordCredentials("admin",
				this.entityManager.find(CommandParameter.class,
						CommandParameter.HERITRIX_ADMINPW_KEY)
						.getCommandValue());
		this.httpClient.getState().setCredentials(
				new AuthScope(AuthScope.ANY_SCHEME, AuthScope.ANY_PORT,
						AuthScope.ANY_REALM), defaultcreds);
		this.engineUri = this.entityManager.find(Parameter.class,
				Parameter.ENGINE_URI.getKey()).getValue();
		Logger httpClientlogger = Logger.getLogger(this.httpClient.getClass());
		httpClientlogger.setLevel(Level.ERROR);
		Logger authChallengeProcessorLogger = Logger
				.getLogger(AuthChallengeProcessor.class);
		authChallengeProcessorLogger.setLevel(Level.ERROR);
		Logger httpMethodBaseLogger = Logger.getLogger(HttpMethodBase.class);
		httpMethodBaseLogger.setLevel(Level.ERROR);
		this.jobsDir = this.entityManager.find(Parameter.class,
				Parameter.JOBS_DIR.getKey()).getValue();
	}

	public String getCrawlerEngineStatus() {
		HttpMethod method = null;
		int status = -1;
		try {
			method = new GetMethod(this.engineUri);
			// TODO check status
			status = this.httpClient.executeMethod(method);
			method.releaseConnection();
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		if (status == 200) {
			return CrawlerManager.RUNNING;
		}
		return CrawlerManager.STOPPED;
	}

	public String getCrawlStatus(String jobName) throws HeritrixException {
		Job j = this.getPersistedJob(jobName);
		if (j != null) {
			try {
				return this.getJobStatus(jobName);
			} catch (HttpException e) {
				e.printStackTrace();
				throw new HeritrixException();
			} catch (IOException e) {
				e.printStackTrace();
				throw new HeritrixException();
			} catch (DocumentException e) {
				e.printStackTrace();
				throw new HeritrixException();
			}
		}
		return null;
	}

	private String[] getJobsArray() throws HttpException, IOException,
			SAXException, XPathExpressionException {
		if (this.getCrawlerEngineStatus().equals(CrawlerManager.STOPPED)) {
			return new String[] {};
		}
		HttpMethod method = null;
		List<String> ret = new ArrayList<String>();
		try {
			method = new PostMethod(this.engineUri);
			// method.setFollowRedirects(true);
			((PostMethod) method).addParameter(new NameValuePair("action",
					"rescan"));
			// TODO check status code
			int status = this.httpClient.executeMethod(method);
			method.releaseConnection();
			method = new GetMethod(this.engineUri);
			status = this.httpClient.executeMethod(method);
			String body = method.getResponseBodyAsString();
			Matcher m = CrawlerManager.pJob.matcher(body);
			int start = 0;
			while (m.find(start)) {
				ret.add(m.group(1));
				start = m.end();
			}
			method.releaseConnection();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return ret.toArray(new String[ret.size()]);
	}

	private String getJobStatus(String encodedJobName) throws HttpException,
			IOException, DocumentException {
		// back compatibility Heritrix 2
		if (encodedJobName.startsWith("completed-")) {
			return CrawlStatus.FINISHED.toString();
		}
		File jobDir = new File(this.jobsDir + CrawlerManager.FILE_SEPARATOR
				+ encodedJobName);
		String[] files = jobDir.list();
		if (files == null || files.length < 2) {
			return CrawlStatus.CREATED.toString();
		}
		String ret = CrawlStatus.CREATED.toString();
		RandomAccessFile progressStatistics = null;
		Calendar now = new GregorianCalendar();
		Date comparingDate = DateUtils.addDays(now.getTime(), -3);
		try {
			progressStatistics = new RandomAccessFile(
					this.jobsDir + CrawlerManager.FILE_SEPARATOR
							+ encodedJobName + CrawlerManager.FILE_SEPARATOR
							+ "logs" + CrawlerManager.FILE_SEPARATOR
							+ "progress-statistics.log", "r");
			if (progressStatistics != null) {
				progressStatistics.seek(Math.max(0,
						progressStatistics.length() - 3000));
				String line = progressStatistics.readLine();
				StringBuffer buffer = new StringBuffer();
				while (line != null) {
					buffer.append(line + "\n");
					line = progressStatistics.readLine();
				}
				String progressStatisticsContent = buffer.toString();
				Matcher m = this.progressStatisticsDatePattern
						.matcher(progressStatisticsContent);
				int start = 0;
				String lastDateString = "";
				while (m.find(start)) {
					start = m.end();
					lastDateString = m.group();
				}
				Date lastDate = this.progressStatisticsDateFormat
						.parse(lastDateString);
				if (!progressStatisticsContent
						.contains("CRAWL ENDED - Finished")
						&& lastDate.after(comparingDate)) {
					ret = CrawlStatus.RUNNING.toString();
				} else {
					ret = CrawlStatus.FINISHED.toString();
				}

			}
		} catch (FileNotFoundException e) {
			// TODO: handle exception
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (progressStatistics != null) {
				progressStatistics.close();
			}
		}
		// File crawlReport = new File(this.jobsDir + FILE_SEPARATOR
		// + encodedJobName + FILE_SEPARATOR + "reports" + FILE_SEPARATOR
		// + "crawl-report.txt");
		// if (crawlReport != null && crawlReport.canRead()) {
		// String crawlReportContent = FileUtils.readFileToString(crawlReport);
		// if (crawlReportContent.contains("crawl status: Finished")) {
		// ret = CrawlStatus.FINISHED.toString();
		// }
		// }
		return ret;
	}

	public Job getPersistedJob(String jobName) {
		// back compatibilty with Heritrix 2
		if (jobName != null
				&& (jobName.startsWith("completed-")
						|| jobName.startsWith("ready-") || jobName
							.startsWith("active-"))) {
			jobName = jobName.substring(jobName.indexOf("-") + 1);
		}
		List<Job> persistedJobs = this.entityManager
				.createQuery("from Job j where j.name=:name")
				.setParameter("name", jobName).getResultList();
		Job j = null;
		if (persistedJobs.size() == 1) {
			j = persistedJobs.get(0);
		}
		return j;
	}

	private List<Job> update(String status) throws HeritrixException,
			XPathExpressionException, SAXException {
		List<Job> jobs = new ArrayList<Job>();
		try {
			for (String encodedJobName : this.getJobsArray()) {
				if (!encodedJobName.trim().startsWith("profil")) {
					Matcher m = CrawlerManager.childJobPattern
							.matcher(encodedJobName);
					String parentJobName = encodedJobName;
					Job parentJob = null;
					Job persistedJob = null;
					boolean childJob = false;
					if (m.find()) {
						childJob = true;
						parentJobName = encodedJobName.substring(0,
								encodedJobName.indexOf("__"));
						parentJob = this.getPersistedJob(parentJobName);
						if (parentJob == null
								|| parentJob.getChildJobName() == null
								|| !parentJob.getChildJobName().equals(
										encodedJobName)) {
							// this is not the last child job
							continue;
						}
					}
					if (childJob) {
						// the last child job sets the status of the parent
						persistedJob = parentJob;
						if (this.getJobStatus(encodedJobName).equals(
								CrawlStatus.FINISHED.toString())) {
							persistedJob.setJobStage(CrawlStatus.FINISHED
									.toString());
						} else {
							persistedJob.setJobStage(this
									.getCrawlStatus(parentJobName));
						}
					} else {
						persistedJob = this.getPersistedJob(encodedJobName);
						if (persistedJob == null) {
							System.out.println(encodedJobName + " not found.");
							continue;
						}
						if (persistedJob.getChildJobName() == null
								|| persistedJob.getChildJobName().trim()
										.length() < 1) {
							if (this.getJobStatus(encodedJobName).equals(
									CrawlStatus.FINISHED.toString())) {
								persistedJob.setJobStage(CrawlStatus.FINISHED
										.toString());
							} else {
								persistedJob.setJobStage(this
										.getCrawlStatus(parentJobName));
							}

						}
					}
					if (childJob || persistedJob.getChildJobName() == null) {
						persistedJob.setJobStage(this
								.getJobStatus(encodedJobName));
						// back compatibility
						if (encodedJobName.startsWith("completed-")) {
							persistedJob.setJobStage(CrawlStatus.FINISHED
									.toString());
						}
						if (encodedJobName.startsWith("ready-")) {
							persistedJob.setJobStage(CrawlStatus.CREATED
									.toString());
						}
						// System.out.println(encodedJobName);
						this.entityManager.merge(persistedJob);
					}
					// show jobs belonging to the currentuser or to assigned
					// users. If admin or guest show all jobs
					if (status == null
							|| status.equalsIgnoreCase(persistedJob
									.getJobStage())) {
						jobs.add(persistedJob);
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		}
		return jobs;
	}

	@Asynchronous
	public QuartzTriggerHandle updater(@Expiration Date expirationDate,
			@IntervalCron String cronData, @FinalExpiration Date endDate) {
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE job data updater");
		try {
			this.create();
			this.update(null);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeritrixException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return handle;
	}
}
