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
package it.drwolf.ridire.session;

import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.Role;
import it.drwolf.ridire.entity.ScheduledJobHandle;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.session.ssl.EasySSLProtocolSocketFactory;
import it.drwolf.ridire.util.exceptions.CrawlingFileException;
import it.drwolf.ridire.util.exceptions.HeritrixException;
import it.drwolf.ridire.util.exceptions.HeritrixNotStartingException;
import it.drwolf.ridire.util.validators.RegExpValidator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ejb.Remove;
import javax.faces.validator.ValidatorException;
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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.archive.crawler.framework.CrawlStatus;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.xml.sax.SAXException;

@Scope(ScopeType.CONVERSATION)
@Name("crawlerManager")
public class CrawlerManager {
	public static final String FILE_SEPARATOR = System
			.getProperty("file.separator");
	public static final Pattern pDownloadedBytes = Pattern.compile(
			"total crawled bytes: (\\d+)", Pattern.MULTILINE);
	public static final Pattern pFinishedURICount = Pattern.compile(
			"URI successes: (\\d+)", Pattern.MULTILINE);
	public static final Pattern pURLs = Pattern
			.compile("(\\d+) downloaded \\+ (\\d+) queued = (\\d+) total");
	@In
	public EntityManager entityManager;
	@In(create = true)
	private FacesMessages facesMessages;
	@In(create = true)
	private Map<String, String> messages;

	public static final String RUNNING = "running";
	public static final String STOPPED = "stopped";
	private static final int CONN_ATTEMPTS = 5;

	public static final Pattern childJobPattern = Pattern
			.compile(".*__(\\d+).*");
	private HttpClient httpClient = null;
	private String engineUri;
	private String jobsDir;

	public static final Pattern pJob = Pattern.compile(
			"<span class='job'>.*?<a href='job/.+?'>(.+?)</a>",
			Pattern.MULTILINE | Pattern.DOTALL);

	public static final Pattern pStatus = Pattern.compile(
			"<h2>Job is (.+?)(:.*?)?</h2>", Pattern.MULTILINE);

	private Pattern progressStatisticsDatePattern = Pattern.compile(
			"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z", Pattern.MULTILINE);

	private SimpleDateFormat progressStatisticsDateFormat = new SimpleDateFormat(
			"yyyy-MM-dd'T'HH:mm:ss'Z'");

	public long calculateQuota(User currentUser) {
		Long quota = currentUser.getQuota();
		if (quota != null && quota > 0L) {
			Long alreadyCrawled = (Long) this.entityManager
					.createQuery(
							"select sum(cr.length) from CrawledResource cr join cr.job as j where j.crawlerUser.id=:userid")
					.setParameter("userid", currentUser.getId())
					.getSingleResult();
			if (alreadyCrawled != null) {
				return quota - alreadyCrawled;
			}
		}
		return quota;
	}

	@SuppressWarnings("unchecked")
	private boolean checkNewJob(String jobName, String followedURLPattern,
			String writtenResourceURLPattern, String mts) {
		boolean result = true;
		// check if jobName already exists
		List<Job> jobs = this.entityManager
				.createQuery("from Job job where job.name=:name")
				.setParameter("name", jobName).getResultList();
		if (jobs != null && jobs.size() > 0) {
			this.facesMessages.addFromResourceBundle(Severity.ERROR,
					this.messages.get("jobNameExisting"));
			result = false;
		}
		// check for invalid regular expression
		RegExpValidator regExprValidator = new RegExpValidator();
		if (followedURLPattern != null
				&& followedURLPattern.trim().length() > 0) {
			try {
				regExprValidator.validate(null, null, followedURLPattern);
			} catch (ValidatorException e) {
				this.facesMessages.addFromResourceBundle(
						Severity.ERROR,
						this.messages.get("regExprInvalidPattern") + ": "
								+ e.getMessage());
				result = false;
			}
		}
		if (writtenResourceURLPattern != null
				&& writtenResourceURLPattern.trim().length() > 0) {
			try {
				regExprValidator
						.validate(null, null, writtenResourceURLPattern);
			} catch (ValidatorException e) {
				this.facesMessages.addFromResourceBundle(
						Severity.ERROR,
						this.messages.get("regExprInvalidPattern") + ": "
								+ e.getMessage());
				result = false;
			}
		}
		if (mts != null && mts.trim().length() > 0) {
			try {
				regExprValidator.validate(null, null, mts);
			} catch (ValidatorException e) {
				this.facesMessages.addFromResourceBundle(
						Severity.ERROR,
						this.messages.get("regExprInvalidPattern") + ": "
								+ e.getMessage());
				result = false;
			}
		}
		return result;
	}

	@Create
	public void create() {
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

	/*
	 * (non-Javadoc)
	 * 
	 * java.lang.String)
	 */
	@Restrict("#{s:hasRole('Crawler User')}")
	public String createJob(Profile profile, User currentUser, boolean fromJob)
			throws HeritrixException {
		String profileName = profile.getProfileName();
		String jobName = profile.getJobName();
		String jobSeeds = profile.getJobSeeds();
		List<String> chosenMimeTypes = profile.getChosenMimeTypes();
		String goodURLs = profile.getGoodURLs();
		String writtenURLs = profile.getWrittenURLs();
		String writtenResourceURLpattern = profile
				.getWrittenResourceURLPattern();
		String followedURLPattern = profile.getFollowedURLPattern();
		boolean followedURLPatternDenied = profile.isFollowedURLPatternDenied();
		boolean writtenResourceURLPatternDenied = profile
				.isWrittenResourceURLPatternDenied();
		String ret = null;
		try {
			ret = this.createJob(profileName, jobName, jobSeeds, currentUser,
					fromJob, followedURLPattern, followedURLPatternDenied,
					writtenResourceURLpattern, writtenResourceURLPatternDenied,
					chosenMimeTypes, goodURLs, writtenURLs);
		} catch (HttpException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ret;
	}

	@Restrict("#{s:hasRole('Crawler User')}")
	private String createJob(String profileName, String jobName,
			String jobSeeds, User currentUser, boolean fromJob,
			String followedURLPattern, boolean followedURLPatternDenied,
			String writtenResourceURLPattern,
			boolean writtenResourceURLPatternDenied,
			List<String> chosenMimeTypes, String goodURLs, String writtenURLs)
			throws HeritrixException, DocumentException, HttpException,
			IOException, XPathExpressionException, SAXException {
		String mts = null;
		String fatherJobName = null;
		String escapedWrittenResourceURLPattern = writtenResourceURLPattern;
		String escapedFollowedURLPattern = followedURLPattern;
		if (!fromJob) {
			if (chosenMimeTypes != null && chosenMimeTypes.size() > 0) {
				mts = "^.*(";
				List<String> allMT = new ArrayList<String>();
				for (String cmt : chosenMimeTypes) {
					List<String> mimetypes = Profile.SUPPORTED_MIME_TYPES
							.get(cmt);
					allMT.addAll(mimetypes);
				}
				mts += StringUtils.join(allMT, "|");
				mts += ").*$";

			}
			if (!this.checkNewJob(jobName, followedURLPattern,
					writtenResourceURLPattern, mts)) {
				return "ko";

			}
		}
		String jobsDir = this.entityManager.find(Parameter.class,
				Parameter.JOBS_DIR.getKey()).getValue();
		SAXReader saxReader = new SAXReader();
		Document document = null;
		if (!fromJob) {
			document = saxReader.read(new File(jobsDir
					+ CrawlerManager.FILE_SEPARATOR + profileName
					+ CrawlerManager.FILE_SEPARATOR
					+ "profile-crawler-beans.cxml"));
		} else {
			fatherJobName = jobName.substring(0, jobName.indexOf("__"));
			document = saxReader.read(new File(jobsDir
					+ CrawlerManager.FILE_SEPARATOR + fatherJobName
					+ CrawlerManager.FILE_SEPARATOR + "crawler-beans.cxml"));
		}
		// set quota
		long quota = this.calculateQuota(currentUser);
		if (quota > 0) {
			Element quotaProp = (Element) document
					.selectSingleNode("//*[name()='property' and @name='maxFileSizeBytes']");
			quotaProp.addAttribute("value", "" + quota);
		}
		if (!fromJob) {
			boolean followedURLPatternEnabled = false;
			if (followedURLPattern != null
					&& followedURLPattern.trim().length() > 0) {
				followedURLPatternEnabled = true;
			} else if (goodURLs != null && goodURLs.trim().length() > 0) {
				String[] urls = goodURLs.split("\r\n");
				followedURLPattern = "^.*";
				String options = StringUtils.join(urls, '|');
				if (options != null && options.trim().length() > 0) {
					followedURLPattern += "(" + options + ").*";
				}
				followedURLPattern += "$";
				followedURLPatternEnabled = true;
				// workaround for '&' in xml + regex and to deal with escaped &
				// in links
				escapedFollowedURLPattern = followedURLPattern.replaceAll("&",
						"&(amp;)?");
			}
			if (followedURLPatternEnabled) {
				Element urlMatchEnabled = (Element) document
						.selectSingleNode("//*[name()='bean' and @class='org.archive.modules.deciderules.MatchesRegexDecideRule']/*[name()='property' and @name='enabled']");
				Element urlMatchNotEnabled = (Element) document
						.selectSingleNode("//*[name()='bean' and @class='org.archive.modules.deciderules.NotMatchesRegexDecideRule']/*[name()='property' and @name='enabled']");
				Pattern.compile(followedURLPattern); // already
				// checked
				// for
				// patternSyntaxExceptions
				if (followedURLPatternDenied) {
					urlMatchEnabled.addAttribute("value", "true");
					urlMatchNotEnabled.addAttribute("value", "false");
					Element urlMatchRegex = (Element) document
							.selectSingleNode("//*[name()='bean' and @class='org.archive.modules.deciderules.MatchesRegexDecideRule']/*[name()='property' and @name='regex']");
					urlMatchRegex.addAttribute("value",
							escapedFollowedURLPattern);
				} else {
					urlMatchEnabled.addAttribute("value", "false");
					urlMatchNotEnabled.addAttribute("value", "true");
					Element urlNotMatchRegex = (Element) document
							.selectSingleNode("//*[name()='bean' and @class='org.archive.modules.deciderules.NotMatchesRegexDecideRule']/*[name()='property' and @name='regex']");
					urlNotMatchRegex.addAttribute("value",
							escapedFollowedURLPattern);
				}
			}
			boolean writtenResourceURLPatternEnabled = false;
			if (writtenResourceURLPattern != null
					&& writtenResourceURLPattern.trim().length() > 0) {
				writtenResourceURLPatternEnabled = true;
			} else if (writtenURLs != null && writtenURLs.trim().length() > 0) {
				String[] urls = writtenURLs.split("\r\n");
				writtenResourceURLPattern = "^.*";
				String options = StringUtils.join(urls, '|');
				if (options != null && options.trim().length() > 0) {
					writtenResourceURLPattern += "(" + options + ").*";
				}
				writtenResourceURLPattern += "$";
				writtenResourceURLPatternEnabled = true;
				// workaround for '&' in xml + regex and to deal with escaped &
				// in links
				escapedWrittenResourceURLPattern = writtenResourceURLPattern
						.replaceAll("&", "&(amp;)?");
			}
			if (writtenResourceURLPatternEnabled) {
				Element writtenMatchEnabled = (Element) document
						.selectSingleNode("//*[name()='bean' and @id='arcWriter']/*[name()='property' and @name='matchesFilePatternEnabled']");
				Element writtenMatchNotEnabled = (Element) document
						.selectSingleNode("//*[name()='bean' and @id='arcWriter']/*[name()='property' and @name='notMatchesFilePatternEnabled']");
				Pattern.compile(writtenResourceURLPattern); // already
				// checked
				// for
				// patternSyntaxExceptions
				if (writtenResourceURLPatternDenied) {
					writtenMatchEnabled.addAttribute("value", "true");
					writtenMatchNotEnabled.addAttribute("value", "false");
					Element writtenMatchRegex = (Element) document
							.selectSingleNode("//*[name()='bean' and @id='arcWriter']/*[name()='property' and @name='matchesFilePattern']");
					writtenMatchRegex.addAttribute("value",
							escapedWrittenResourceURLPattern);
				} else {
					writtenMatchEnabled.addAttribute("value", "false");
					writtenMatchNotEnabled.addAttribute("value", "true");
					Element writtenNotMatchRegex = (Element) document
							.selectSingleNode("//*[name()='bean' and @id='arcWriter']/*[name()='property' and @name='notMatchesFilePattern']");
					writtenNotMatchRegex.addAttribute("value",
							escapedWrittenResourceURLPattern);
				}
			}
			if (mts != null && mts.trim().length() > 0) {
				Pattern.compile(mts); // already checked for
				// patternSyntaxExceptions
				Element contentTypeMatch = (Element) document
						.selectSingleNode("//*[name()='bean' and @id='arcWriter']/*[name()='property' and @name='contentTypeNotMatchesPattern']");
				contentTypeMatch.addAttribute("value", mts.trim());
			}
			// update seeds
			if (jobSeeds != null) {
				Element seeds = (Element) document
						.selectSingleNode("//*[name()='bean' and @id='longerOverrides']/*[name()='property']/*[name()='props']/*[name()='prop']");
				seeds.setText(jobSeeds);
			}
			// jobName
			Element simpleOverrides = (Element) document
					.selectSingleNode("//*[name()='bean' and @id='simpleOverrides']/*[name()='property']/*[name()='value']");
			String simpleOverridesText = simpleOverrides.getText();
			StringTokenizer stringTokenizer = new StringTokenizer(
					simpleOverridesText, "\n");
			StringBuffer buf = new StringBuffer();
			while (stringTokenizer.hasMoreTokens()) {
				String tok = stringTokenizer.nextToken().trim();
				if (tok.contains("metadata.jobName")) {
					tok = "metadata.jobName=" + jobName;
				}
				buf.append(tok + "\n");
			}
			simpleOverrides.setText(buf.toString());
		}
		String cxml = document.asXML();
		HttpMethod method = null;
		try {
			if (!fromJob) {
				method = new PostMethod(this.engineUri + "job/"
						+ URLEncoder.encode(profileName, "UTF-8"));
			} else {
				method = new PostMethod(this.engineUri + "job/"
						+ URLEncoder.encode(fatherJobName, "UTF-8"));
			}
			((PostMethod) method).addParameter(new NameValuePair("copyTo",
					jobName));
			// TODO check status code
			int status = this.httpClient.executeMethod(method);
			method.releaseConnection();
			String jobConfFile = this.entityManager.find(Parameter.class,
					Parameter.JOBS_DIR.getKey()).getValue()
					+ "/" + jobName + "/crawler-beans.cxml";
			FileUtils.writeStringToFile(new File(jobConfFile), cxml);
			Job j = new Job(jobName, CrawlStatus.CREATED.toString());
			this.entityManager.persist(j);
			this.entityManager.refresh(j);
			j.setCrawlerUser(currentUser);
			j.setWrittenResourceURLPattern(writtenResourceURLPattern);
			j.setFollowedURLPattern(followedURLPattern);
			j.setGoodURLs(goodURLs);
			j.setGoodURLsDenied(followedURLPatternDenied);
			j.setWrittenURLs(writtenURLs);
			j.setWrittenURLsDenied(writtenResourceURLPatternDenied);
			j.setChosenMimeTypes(chosenMimeTypes);
			j.setSeeds(jobSeeds);
			currentUser.getJobs().add(j);
			this.entityManager.merge(j);
			this.entityManager.merge(currentUser);
			this.updateJobsList(currentUser);
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return "ok";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#deleteJob(java.lang.String)
	 */
	@Restrict("#{s:hasRole('Crawler User')}")
	public String deleteJob(String jobName, String jobStatus, User currentUser)
			throws HeritrixException {
		this.updateJobsList(currentUser);
		Job j = this.getPersistedJob(jobName);
		String childJobName = j.getChildJobName();
		int lastChildIndex = 0;
		if (childJobName != null && childJobName.length() > 0) {
			lastChildIndex = Integer.parseInt(childJobName
					.substring(childJobName.indexOf("__") + 2));
		}
		HttpMethod method = null;
		String jobsDir = this.entityManager.find(Parameter.class,
				Parameter.JOBS_DIR.getKey()).getValue();
		try {
			if (lastChildIndex > 0) {
				for (int i = lastChildIndex; i > 0; i--) {
					method = new PostMethod(this.engineUri + "job/"
							+ URLEncoder.encode(jobName, "UTF-8") + "__" + i);
					((PostMethod) method).setParameter("action", "teardown");
					this.httpClient.executeMethod(method);
					method.releaseConnection();
					FileUtils.deleteDirectory(new File(jobsDir
							+ CrawlerManager.FILE_SEPARATOR + jobName + "__"
							+ i));
					Job childJob = this.getPersistedJob(jobName + "__" + i);
					this.entityManager.remove(childJob);
				}
			}

			method = new PostMethod(this.engineUri + "job/"
					+ URLEncoder.encode(jobName, "UTF-8"));
			((PostMethod) method).setParameter("action", "teardown");
			this.httpClient.executeMethod(method);
			method.releaseConnection();
			FileUtils.deleteDirectory(new File(jobsDir
					+ CrawlerManager.FILE_SEPARATOR + jobName));

			if (j != null) {
				this.entityManager
						.createQuery(
								"delete from CrawledResource cr where cr.job.id=:id")
						.setParameter("id", j.getId()).executeUpdate();
				ScheduledJobHandle sjh = j.getScheduledJobHandle();
				if (sjh != null) {
					sjh.setJob(null);
					this.entityManager.remove(sjh);
				}
				this.entityManager.remove(j);
			}
		} catch (HttpException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return "ok";
	}

	@Remove
	@Destroy
	public void destroy() {

	}

	private List<Job> doGetJobs(String status, User currentUser)
			throws HeritrixException, XPathExpressionException, SAXException {
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
					User crawlerUser = persistedJob.getCrawlerUser();
					if (!childJob
							&& crawlerUser != null
							&& (crawlerUser.equals(currentUser)
									|| currentUser.getAssignedUsers().contains(
											crawlerUser)
									|| currentUser.hasRole(Role.ADMIN) || currentUser
										.hasRole(Role.GUEST))) {
						if (status == null
								|| status.equalsIgnoreCase(persistedJob
										.getJobStage())) {
							jobs.add(persistedJob);
						}
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

	public long getDiscoveredURICount(Job job, User currentUser)
			throws HeritrixException {
		try {
			return this.getURICount(job, "discoveredUriCount", currentUser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (HeritrixException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (SAXException e) {
			e.printStackTrace();
			throw new HeritrixException();
		}
	}

	public long getFinishedURICount(Job job, User currentUser)
			throws HeritrixException {
		try {
			return this.getURICount(job, "finishedUriCount", currentUser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (HeritrixException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (SAXException e) {
			e.printStackTrace();
			throw new HeritrixException();
		}
	}

	private String getHeritrixBinPath() {
		return this.entityManager.find(CommandParameter.class,
				CommandParameter.HERITRIX_DIR_KEY).getCommandValue()
				+ CrawlerManager.FILE_SEPARATOR
				+ this.entityManager.find(CommandParameter.class,
						CommandParameter.HERITRIX_BINDIR_KEY).getCommandValue();
	}

	public List<Job> getJobs(User currentUser) {
		if (currentUser.hasRole(Role.GUEST) || currentUser.hasRole(Role.ADMIN)
				|| currentUser.hasRole(Role.INDEXER)) {
			return this.entityManager.createQuery(
					"from Job j order by j.id desc").getResultList();
		}
		Set<User> assignedUsers = currentUser.getAssignedUsers();
		if (assignedUsers != null && assignedUsers.size() > 0) {
			return this.entityManager
					.createQuery(
							"from Job j where j.crawlerUser=:u or j.crawlerUser in ( :assignedUsers )  order by j.id desc")
					.setParameter("u", currentUser)
					.setParameter("assignedUsers", assignedUsers)
					.getResultList();
		}
		return this.entityManager
				.createQuery(
						"from Job j where j.crawlerUser=:u  order by j.id desc")
				.setParameter("u", currentUser).getResultList();
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

	@Restrict("#{identity.loggedIn}")
	public List<String> getJobSeeds(String profileName, String jobName,
			User currentUser) throws HeritrixException {
		List<String> seeds = new ArrayList<String>();
		HttpMethod method = null;
		String cxml = "crawler-beans.cxml";
		try {
			this.updateJobsList(currentUser);
			String filename = null;
			Job j = this.getPersistedJob(jobName);
			if (j != null) {
				if (j.getChildJobName() != null) {
					filename = j.getChildJobName();
				} else {
					filename = j.getName();
				}
			} else {
				filename = profileName;
				cxml = "profile-crawler-beans.cxml";
			}
			if (j != null) {
				String[] s = j.getSeeds().split("\n");
				for (int i = 0; i < s.length; i++) {
					seeds.add(s[i].trim());
				}
			} else {
				method = new GetMethod(this.engineUri + "job/"
						+ URLEncoder.encode(filename, "UTF-8") + "/jobdir/"
						+ cxml);
				int status = this.httpClient.executeMethod(method);
				SAXReader saxReader = new SAXReader();
				Document d = saxReader.read(method.getResponseBodyAsStream());
				method.releaseConnection();
				Element seedsElement = (Element) d
						.selectSingleNode("//*[name()='bean' and @id='longerOverrides']/*[name()='property']/*[name()='props']/*[name()='prop']");
				if (seedsElement != null) {
					String seedsText = seedsElement.getText();
					StringTokenizer stringTokenizer = new StringTokenizer(
							seedsText, "\n");
					while (stringTokenizer.hasMoreTokens()) {
						String t = stringTokenizer.nextToken().trim();
						if (!t.startsWith("#")) {
							seeds.add(t);
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return seeds;
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

	private String getJobStatus_old(String encodedJobName)
			throws HttpException, IOException, DocumentException {
		Pattern finishedPattern = Pattern.compile("<h1>Job <i>"
				+ encodedJobName + "</i> \\(0 launches");
		HttpMethod method = null;
		String ret = "";
		try {
			method = new GetMethod(this.engineUri + "job/"
					+ URLEncoder.encode(encodedJobName, "UTF-8"));
			// TODO check status
			int status = this.httpClient.executeMethod(method);
			String body = method.getResponseBodyAsString();
			Matcher mFinished = finishedPattern.matcher(body);
			boolean finished = false;
			if (mFinished.find()) {
				finished = true;
			}
			Matcher m = CrawlerManager.pStatus.matcher(body);
			if (m.find()) {
				ret = m.group(1).trim();
				if (ret.equals("Active")) {
					ret = m.group(2).substring(1).trim();
					if (ret.equalsIgnoreCase("Pausing")) {
						ret = CrawlStatus.PAUSED.toString();
					}
				}
			}
			if (ret.equalsIgnoreCase("Unbuilt") && finished) {
				ret = CrawlStatus.FINISHED.toString();
			}
			method.releaseConnection();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return ret;
	}

	private String getNewJobName(Job job) throws IOException,
			DocumentException, NumberFormatException, HeritrixException,
			XPathExpressionException, SAXException {
		// new job name policy:
		// original name + '__' + integer
		String[] jobsArray = this.getJobsArray();
		Arrays.sort(jobsArray);
		int inc = 0;
		// search among already completed jobs
		for (String encodedJobName : jobsArray) {
			if (this.getJobStatus(encodedJobName).equalsIgnoreCase(
					CrawlStatus.FINISHED.toString())) {
				if (encodedJobName.contains(job.getName())) {
					Matcher m = CrawlerManager.childJobPattern
							.matcher(encodedJobName);
					if (m.find()) {
						String increment = m.group(1);
						int curInc = Integer.parseInt(increment);
						inc = curInc > inc ? curInc : inc;
					}
				}
			}
		}
		++inc;
		return job.getName() + "__" + inc;
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

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#getProfilesStrings()
	 */
	@Restrict("#{s:hasRole('Crawler User')}")
	public Map<String, String> getProfilesStrings() throws HeritrixException,
			XPathExpressionException, SAXException {
		Map<String, String> allProfiles = new LinkedHashMap<String, String>();
		allProfiles.put("Scegli il profilo...", "");
		Map<String, String> sortedProfiles = new TreeMap<String, String>();
		try {
			for (String job : this.getJobsArray()) {
				if (job.trim().startsWith("profilo")) {
					sortedProfiles.put(job, job);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		}
		allProfiles.putAll(sortedProfiles);
		return allProfiles;
	}

	public long getQueuedURICount(Job job, User currentUser)
			throws HeritrixException {
		try {
			return this.getURICount(job, "queuedUriCount", currentUser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (HeritrixException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (SAXException e) {
			e.printStackTrace();
			throw new HeritrixException();
		}
	}

	public Long getRawBytes(String jobName, User user)
			throws HeritrixException, CrawlingFileException {
		// this.updateJobsList(user);
		Job j = this.getPersistedJob(jobName);
		if (j == null) {
			return 0L;
		}
		if (j.getChildJobName() != null && j.getChildJobName().length() > 0) {
			jobName = j.getChildJobName();
		}
		HttpMethod method = null;
		try {
			method = new GetMethod(this.engineUri + "job/"
					+ URLEncoder.encode(jobName, "UTF-8")
					+ "/jobdir/reports/crawl-report.txt");
			// TODO check status
			int status = this.httpClient.executeMethod(method);
			String report = method.getResponseBodyAsString();
			method.releaseConnection();
			long parseLong = 0L;
			Matcher m = CrawlerManager.pDownloadedBytes.matcher(report);
			if (m.find()) {
				String bytes = m.group(1);
				parseLong = Long.parseLong(bytes);
			}
			if (parseLong == 0L) {
				try {
					parseLong = this.getRawBytesFromFileSystem(jobName);
				} catch (Exception exception) {
					parseLong = 0;
				}
			}
			return parseLong;
		} catch (HttpException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	private long getRawBytesFromFileSystem(String jobName)
			throws NullPointerException {
		String dir = this.entityManager.find(Parameter.class,
				Parameter.JOBS_DIR.getKey()).getValue();
		return FileUtils.sizeOfDirectory(new File(dir
				+ CrawlerManager.FILE_SEPARATOR + jobName
				+ CrawlerManager.FILE_SEPARATOR + "arcs"
				+ CrawlerManager.FILE_SEPARATOR));
	}

	private long getURICount(Job job, String whichCount, User currentUser)
			throws IOException, HeritrixException, DocumentException,
			XPathExpressionException, SAXException {
		// this.updateJobsList(currentUser);
		Pattern pURICount = Pattern
				.compile(
						"\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}Z\\s+(\\d+)\\s+(\\d+)\\s+(\\d+)",
						Pattern.MULTILINE);
		String jobName = job.getName();
		Job j = this.getPersistedJob(jobName);
		if (j == null) {
			return 0L;
		}
		if (job.getChildJobName() != null && job.getChildJobName().length() > 0) {
			jobName = job.getChildJobName();
		}
		String dir = this.entityManager.find(Parameter.class,
				Parameter.JOBS_DIR.getKey()).getValue();
		long uriCountFromCrawlReport = 0L;
		long queuedURICount = 0L;
		long discoveredURICount = 0L;
		HttpMethod method = null;
		String jobStatus = this.getJobStatus(jobName);
		// jobName = jobName.replaceAll(" ", "\\\\ ");
		try {
			while (true) {
				if (jobStatus.equals(CrawlStatus.RUNNING.toString())) {
					RandomAccessFile progressStatistics = null;
					try {
						progressStatistics = new RandomAccessFile(this.jobsDir
								+ CrawlerManager.FILE_SEPARATOR + jobName
								+ CrawlerManager.FILE_SEPARATOR + "logs"
								+ CrawlerManager.FILE_SEPARATOR
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
							String progressStatisticsContent = buffer
									.toString();
							Matcher m = pURICount
									.matcher(progressStatisticsContent);
							int start = 0;
							long queuedURICountTemp = 0L;
							long discoveredURICountTemp = 0L;
							long uriCountFromCrawlReportTemp = 0L;
							while (m.find(start)) {
								start = m.end();
								queuedURICountTemp = Long.parseLong(m.group(2));
								discoveredURICountTemp = Long.parseLong(m
										.group(1));
								uriCountFromCrawlReportTemp = Long.parseLong(m
										.group(3));
							}
							queuedURICount += queuedURICountTemp;
							discoveredURICount = discoveredURICountTemp;
							uriCountFromCrawlReport = uriCountFromCrawlReportTemp;
						}
					} catch (FileNotFoundException e) {
						// TODO: handle exception
					} finally {
						if (progressStatistics != null) {
							progressStatistics.close();
						}
					}
					break;
				} else if (whichCount.equalsIgnoreCase("finishedURICount")) {
					File reportFile = new File(dir
							+ CrawlerManager.FILE_SEPARATOR + jobName
							+ CrawlerManager.FILE_SEPARATOR + "reports"
							+ CrawlerManager.FILE_SEPARATOR
							+ "crawl-report.txt");
					if (reportFile.exists() && reportFile.canRead()) {
						String content = FileUtils.readFileToString(reportFile);
						Matcher m = CrawlerManager.pFinishedURICount
								.matcher(content);
						if (m.find()) {
							String bytes = m.group(1);
							uriCountFromCrawlReport += Long.parseLong(bytes);
						}
					}
					Matcher m = CrawlerManager.childJobPattern.matcher(jobName);
					if (m.matches()) {
						Integer count = Integer.parseInt(m.group(1));
						if (count > 1) {
							count--;
							jobName = jobName.substring(0,
									jobName.indexOf("__"))
									+ "__" + count;
						} else if (count == 1) {
							jobName = jobName.substring(0,
									jobName.indexOf("__"));
						} else {
							break;
						}
					} else {
						break;
					}
				} else {
					return 0L;
				}
			}
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		if (whichCount.equals("discoveredUriCount")) {
			return discoveredURICount;
		}
		if (whichCount.equals("queuedUriCount")) {
			return queuedURICount;
		}
		return uriCountFromCrawlReport;
	}

	@Restrict("#{s:hasRole('Crawler User')}")
	public void pauseJob(String jobName, User currentUser)
			throws HeritrixException {
		this.updateJobsList(currentUser);
		HttpMethod method = null;
		try {
			Job j = this.getPersistedJob(jobName);
			if (this.getJobStatus(j.getName()).equals(
					CrawlStatus.RUNNING.toString())) {
				method = new PostMethod(this.engineUri + "job/"
						+ URLEncoder.encode(jobName, "UTF-8"));
				((PostMethod) method).addParameter(new NameValuePair("action",
						"pause"));
				// TODO check status
				int status = this.httpClient.executeMethod(method);
				method.releaseConnection();
				j.setJobStage(CrawlStatus.PAUSED.toString());
				this.entityManager.merge(j);
				this.updateJobsList(currentUser);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	public void reRunJob(Job job, User currentUser) throws HeritrixException,
			NumberFormatException {
		// if crawlStatus eq null, then this is the first time this job is run
		// do a normal job start then
		this.updateJobsList(currentUser);
		job = this.entityManager.find(Job.class, job.getId());
		if (job == null) {
			return;
		}
		if (job.getJobStage() == null || job.getJobStage().trim().length() < 1
				|| job.getJobStage().equals(CrawlStatus.CREATED.toString())) {
			this.startJob(job.getName(), currentUser);
		} else {
			// do rerun only if parent job or other child job is FINISHED
			if (job.getPeriodicity().equals("periodic")
					&& !job.getJobStage().equals(
							CrawlStatus.FINISHED.toString())) {
				return;
			}
			try {
				String newJobName = this.getNewJobName(job);
				List<String> jobSeeds = this.getJobSeeds(null, job.getName(),
						currentUser);
				StringBuffer b = new StringBuffer();
				for (String j : jobSeeds) {
					b.append(j.trim() + "\n");
				}
				this.createJob(job.getName(), newJobName, b.toString().trim(),
						job.getCrawlerUser(), true, null, false, null, false,
						null, null, null);
				job.setChildJobName(newJobName);
				job.setMappedResources(false);
				this.entityManager.merge(job);
				this.startJob(newJobName, job.getCrawlerUser());
			} catch (IOException e) {
				e.printStackTrace();
				throw new HeritrixException();
			} catch (DocumentException e) {
				e.printStackTrace();
				throw new HeritrixException();
			} catch (XPathExpressionException e) {
				e.printStackTrace();
				throw new HeritrixException();
			} catch (SAXException e) {
				e.printStackTrace();
				throw new HeritrixException();
			}
			this.updateJobsList(job.getCrawlerUser());
			// System.out.println("Rerunning job: " + job.getName());
		}
	}

	@Restrict("#{s:hasRole('Crawler User')}")
	public void resumeJob(String jobName, User currentUser)
			throws HeritrixException {
		this.updateJobsList(currentUser);
		HttpMethod method = null;
		try {
			Job j = this.getPersistedJob(jobName);
			if (this.getJobStatus(j.getName()).equals(
					CrawlStatus.PAUSED.toString())) {
				method = new PostMethod(this.engineUri + "job/"
						+ URLEncoder.encode(jobName, "UTF-8"));
				((PostMethod) method).addParameter(new NameValuePair("action",
						"unpause"));
				// TODO check status
				int status = this.httpClient.executeMethod(method);
				method.releaseConnection();
				j.setJobStage(CrawlStatus.RUNNING.toString());
				this.entityManager.merge(j);
				this.updateJobsList(currentUser);
			}
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (DocumentException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#startCrawlerEngine()
	 */
	@Restrict("#{s:hasRole('Admin')}")
	public String startCrawlerEngine() throws HeritrixNotStartingException {
		String launchCommand = this.getHeritrixBinPath()
				+ CrawlerManager.FILE_SEPARATOR
				+ this.entityManager.find(CommandParameter.class,
						CommandParameter.HERITRIX_LAUNCH_KEY).getCommandValue();
		// + " -a "
		// + this.entityManager.find(CrawlerCommand.class,
		// CrawlerCommand.HERITRIX_ADMINPW_KEY).getCommandValue();
		ProcessBuilder pb = new ProcessBuilder();
		pb.directory(new File(this.getHeritrixBinPath()));
		pb.command(
				launchCommand,
				"-a",
				"admin:"
						+ this.entityManager.find(CommandParameter.class,
								CommandParameter.HERITRIX_ADMINPW_KEY)
								.getCommandValue(), "-p", this.entityManager
						.find(Parameter.class, Parameter.ENGINE_PORT.getKey())
						.getValue());
		try {
			pb.start();
			// try CONN_ATTEMPTS times to get connection to heritrix
			for (int i = 0; i < CrawlerManager.CONN_ATTEMPTS; i++) {
				Thread.sleep(10000);
				if (this.getCrawlerEngineStatus()
						.equals(CrawlerManager.RUNNING)) {
					break;
				}
			}
		} catch (IOException e) {
			throw new HeritrixNotStartingException();
		} catch (InterruptedException e) {
			throw new HeritrixNotStartingException();
		}
		return "OK";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#startJob(java.lang.String)
	 */
	@Restrict("#{s:hasRole('Crawler User')}")
	public void startJob(String jobName, User currentUser)
			throws HeritrixException {
		this.updateJobsList(currentUser);
		HttpMethod method = null;
		try {
			Matcher m = CrawlerManager.childJobPattern.matcher(jobName);
			method = new PostMethod(this.engineUri + "job/"
					+ URLEncoder.encode(jobName, "UTF-8"));
			((PostMethod) method).addParameter(new NameValuePair("action",
					"build"));
			// TODO check status
			int status = this.httpClient.executeMethod(method);
			Thread.sleep(3000);
			method = new PostMethod(this.engineUri + "job/"
					+ URLEncoder.encode(jobName, "UTF-8"));
			method.releaseConnection();
			((PostMethod) method).addParameter(new NameValuePair("action",
					"launch"));
			// TODO check status
			status = this.httpClient.executeMethod(method);
			method.releaseConnection();
			this.updateJobsList(currentUser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (InterruptedException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#stopCrawlerEngine()
	 */
	@Restrict("#{s:hasRole('Admin')}")
	public String stopCrawlerEngine() throws HeritrixException {
		HttpMethod method = null;
		try {
			String job = this.getJobsArray()[0];
			method = new PostMethod(this.engineUri + "job/" + job + "/script");
			// method.setFollowRedirects(true);
			((PostMethod) method).addParameter(new NameValuePair("engine",
					"groovy"));
			((PostMethod) method).addParameter(new NameValuePair("script",
					"System.exit(0);"));
			int status = this.httpClient.executeMethod(method);
			method.releaseConnection();
		} catch (HttpException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (XPathExpressionException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} catch (SAXException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
		return "OK";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#stopJob(java.lang.String)
	 */
	@Restrict("#{s:hasRole('Crawler User')}")
	public void stopJob(String jobName, User currentUser)
			throws HeritrixException {
		this.updateJobsList(currentUser);
		HttpMethod method = null;
		try {
			method = new PostMethod(this.engineUri + "job/"
					+ URLEncoder.encode(jobName, "UTF-8"));
			((PostMethod) method).addParameter(new NameValuePair("action",
					"terminate"));
			// TODO check status
			int status = this.httpClient.executeMethod(method);
			method.releaseConnection();
			this.updateJobsList(currentUser);
		} catch (IOException e) {
			e.printStackTrace();
			throw new HeritrixException();
		} finally {
			if (method != null) {
				method.releaseConnection();
			}
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see it.drwolf.ridire.session.ICrawlerManager#updateJobsList()
	 */
	public void updateJobsList(User currentUser) throws HeritrixException {
		try {
			this.doGetJobs(null, currentUser);
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
