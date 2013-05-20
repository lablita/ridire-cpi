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

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.ScheduledJobHandle;
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.session.async.JobMapperMonitor;
import it.drwolf.ridire.session.async.ScheduledJobExecutor;
import it.drwolf.ridire.util.MD5DigestCreator;
import it.drwolf.ridire.util.data.PoSLine;
import it.drwolf.ridire.util.exceptions.CrawlingFileException;
import it.drwolf.ridire.util.exceptions.FileHandlingException;
import it.drwolf.ridire.util.exceptions.HeritrixException;
import it.drwolf.ridire.util.exceptions.JobHandlingException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.math.BigDecimal;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.archive.crawler.framework.CrawlStatus;
import org.hibernate.Session;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Synchronized;
import org.jboss.seam.annotations.Transactional;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.QuartzTriggerHandle;
import org.jboss.seam.contexts.Contexts;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.security.Identity;
import org.jboss.seam.transaction.UserTransaction;
import org.quartz.SchedulerException;

@Scope(ScopeType.CONVERSATION)
@Name("jobManager")
@Synchronized
public class JobManager {
	final private static DecimalFormat DECIMAL_FORMAT = new DecimalFormat(
			"0.00");
	final private static int MAX_FILE_SIZE = 100 * 1024 * 1024;
	// private static final int PAGE_SIZE = 10;

	@In(create = true)
	private CrawlerManager crawlerManager;

	@In(create = true)
	private Map<String, String> messages;

	@In
	private Identity identity;
	@In(create = true)
	private ScheduledJobExecutor scheduledJobExecutor;

	@In
	private EntityManager entityManager;
	@In(required = false)
	private Profile profile;

	private Job job;

	private String filterNameValue;

	List<Job> jobs;
	private Integer functionalMetadatum;

	private Integer semanticMetadatum;
	private Integer functionalMetadatumValue;
	private Integer semanticMetadatumValue;
	private String pollInterval = "30000"; // default 30 sec.
	private String filterUserValue;

	private String filterURLValue;
	private Integer filterStatusValue;
	private Integer filterResStatusValue;
	private String filterMimeTypeValue;
	public Integer firstResult;
	// parameter used to delete resources with a lower words number
	private Integer wordsLimitDeletionNumber = 40;

	private int pageSize = 10;

	private final Map<String, Integer> allStatusMap = new LinkedHashMap<String, Integer>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5866892546719358941L;

		{
			this.put("", -1);
			this.put("RUNNING", 1);
			this.put("FINISHED", 0);
			this.put("CREATED", 2);
		}
	};
	private final Map<String, Integer> allResStatusMap = new LinkedHashMap<String, Integer>() {

		/**
	 * 
	 */
		private static final long serialVersionUID = -2832735400156804117L;

		{
			this.put("", -1);
			this.put("Da mappare", 2);
			this.put("Mappatura in corso", 3);
			this.put("Mappate", 0);
			this.put("Non presenti", 1);
		}
	};
	private List<CrawledResource> results;

	private Long totalResults;

	private String cleanedText;
	private List<PoSLine> posText = new ArrayList<PoSLine>();
	private static final String RESOURCESDIR = "resources/";
	// private static final String NO_POS_AVAILABLE =
	// "Non esiste il PoS tagging per la risorsa.";
	private String sortField;

	private String sortOrder;

	private List<String> languagesToBeDeleted = new ArrayList<String>();

	private Map<Integer, Integer> analyzedResourcesPercentage = new HashMap<Integer, Integer>();

	private boolean filterAnalyzed;
	private boolean filterValidated;
	private Long totalSelectedResourcesNumber;
	private UserTransaction userTx;

	private boolean validationToBeSaved = false;

	// @In(create = true)
	// private AsyncIndexer asyncIndexer;
	private String perlPw;
	private String cleanerPath;
	private String perlUser;
	private String perlCleanerTemplate;
	private String testAfter;
	private String testBefore;
	private String testOutput;

	public String applyValidationData() {
		for (CrawledResource cr : this.results) {
			this.entityManager.merge(cr);
		}
		if (this.getValidatedResourcesNumber() == 0) {
			this.job.setValidationStatus(Job.TO_BE_VALIDATED);
		} else if (this.getValidatedResourcesNumber() < this
				.getTotalSelectedResourcesNumber()) {
			this.job.setValidationStatus(Job.VALIDATION_IN_PROGRESS);
		} else {
			this.job.setValidationStatus(this.calculateValidationResult());
		}
		this.entityManager.merge(this.job);
		this.setValidationToBeSaved(false);
		return "OK";
	}

	public void assignMetadata() {
		for (CrawledResource cr : this.job.getCrawledResources()) {
			if (cr.isChecked()) {
				FunctionalMetadatum fm = this.entityManager.find(
						FunctionalMetadatum.class, this.functionalMetadatum);
				cr.setFunctionalMetadatum(fm);
				SemanticMetadatum sm = this.entityManager.find(
						SemanticMetadatum.class, this.semanticMetadatum);
				cr.setSemanticMetadatum(sm);
				cr.setChecked(false);
				if (cr.getProcessed().equals(Parameter.INDEXED)) {
					cr.setProcessed(Parameter.INDEXING_PHASE);
					this.entityManager.merge(cr);
					// this.asyncIndexer.updateSingleCrawledResource(cr.getId(),
					// true);
				}
			}
		}
	}

	private String calculateCronString() {
		String cronString = "";
		Calendar startDate = Calendar.getInstance();
		if (this.job.getFirstDate() == null) {
			return cronString;
		}
		startDate.setTime(this.job.getFirstDate());
		if (this.job.getPeriodFrequency().equals("yearly")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + " "
					+ startDate.get(Calendar.HOUR_OF_DAY) + " "
					+ startDate.get(Calendar.DAY_OF_MONTH) + " "
					+ startDate.get(Calendar.MONTH) + 1 + " ?";
		} else if (this.job.getPeriodFrequency().equals("monthly")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + " "
					+ startDate.get(Calendar.HOUR_OF_DAY) + " "
					+ startDate.get(Calendar.DAY_OF_MONTH) + " * ?";
		} else if (this.job.getPeriodFrequency().equals("weekly")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + " "
					+ startDate.get(Calendar.HOUR_OF_DAY) + " ? * "
					+ startDate.get(Calendar.DAY_OF_WEEK);
		} else if (this.job.getPeriodFrequency().equals("daily")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + " "
					+ startDate.get(Calendar.HOUR_OF_DAY) + " * * ?";
		} else if (this.job.getPeriodFrequency().equals("dummy5")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + "/5 " + "* * * ?";
		} else if (this.job.getPeriodFrequency().equals("dummy2")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + "/2 " + "* * * ?";
		} else if (this.job.getPeriodFrequency().equals("dummy1")) {
			cronString = startDate.get(Calendar.SECOND) + " "
					+ startDate.get(Calendar.MINUTE) + "/1 " + "* * * ?";
		}
		return cronString;
	}

	private Integer calculateValidationResult() {
		Long notValidResults = (Long) this.entityManager
				.createQuery(
						"select count(cr) from CrawledResource cr where cr.job=:j and cr.validationStatus="
								+ CrawledResource.NOT_VALID_RESOURCE
								+ " and cr.validation="
								+ CrawledResource.VALIDATED)
				.setParameter("j", this.getJob()).getSingleResult();
		if (notValidResults
				/ this.getTotalSelectedResourcesNumber().floatValue() * 100 >= this
				.getJob().getValidationThreshold()) {
			return Job.VALIDATED_NOK;
		} else {
			return Job.VALIDATED_OK;
		}
	}

	public String canCreateJob() {
		long quota = this.crawlerManager.calculateQuota(this.getCurrentUser());
		if (quota > 0L) {
			return "OK";
		}
		return "NOK";
	}

	public void checkAll() {
		for (CrawledResource cr : this.job.getCrawledResources()) {
			cr.setChecked(true);
		}
	}

	public void checkNothing() {
		for (CrawledResource cr : this.job.getCrawledResources()) {
			cr.setChecked(false);
		}
	}

	public void deleteAllInPage() {
		for (CrawledResource cr : this.results) {
			cr.setDeleted(true);
			this.entityManager.merge(cr);
		}
	}

	public String deleteJob() throws HeritrixException {
		String ret = this.crawlerManager.deleteJob(this.job.getName(),
				this.job.getJobStage(), this.getCurrentUser());
		// this.asyncIndexer.deleteJobResources(this.job.getId(), false, null);
		return ret;
	}

	public void deleteResourcesByLanguage() {
		if (this.getLanguagesToBeDeleted() != null
				&& this.getLanguagesToBeDeleted().size() > 0) {
			List<Integer> crIds = this.entityManager
					.createQuery(
							"select cr.id from CrawledResource cr where cr.job=:job and cr.language in (:languages) and cr.processed=:status ")
					.setParameter("job", this.job)
					.setParameter("status", Parameter.FINISHED)
					.setParameter("languages", this.getLanguagesToBeDeleted())
					.getResultList();
			String queryString = "update CrawledResource cr set cr.deleted=true, cr.processed=:status where cr.job=:job and cr.language in (:languages)";
			int deletedResource = this.entityManager.createQuery(queryString)
					.setParameter("job", this.job)
					.setParameter("languages", this.getLanguagesToBeDeleted())
					.setParameter("status", Parameter.FINISHED).executeUpdate();
			// for (Integer crId : crIds) {
			// this.asyncIndexer.deleteSingleCrawledResource(crId.intValue());
			// }
			Conversation.instance().endBeforeRedirect();
		}
	}

	public void deleteResourcesWithFewWords() {
		if (this.getWordsLimitDeletionNumber() != null
				&& this.getWordsLimitDeletionNumber() > 0) {
			List<Integer> crIds = this.entityManager
					.createQuery(
							"select cr.id from CrawledResource cr where cr.job=:job and cr.wordsNumber < :wn and cr.processed=:status ")
					.setParameter("job", this.job)
					.setParameter("status", Parameter.FINISHED)
					.setParameter("wn", this.getWordsLimitDeletionNumber())
					.getResultList();
			String queryString = "update CrawledResource cr set cr.processed=:status, cr.deleted=true where cr.job=:job and cr.wordsNumber < :wn";
			int deletedResource = this.entityManager.createQuery(queryString)
					.setParameter("job", this.job)
					.setParameter("wn", this.getWordsLimitDeletionNumber())
					.setParameter("status", Parameter.FINISHED).executeUpdate();
			// for (Integer crId : crIds) {
			// this.asyncIndexer.deleteSingleCrawledResource(crId.intValue());
			// }
			Conversation.instance().endBeforeRedirect();
		}
	}

	public boolean filterFunctionalMetadatum(Object current) {
		if (this.functionalMetadatumValue == null
				|| this.functionalMetadatumValue < 0) {
			return true;
		}
		CrawledResource cr = (CrawledResource) current;
		if (cr.getFunctionalMetadatum() != null
				&& cr.getFunctionalMetadatum().getId()
						.equals(this.functionalMetadatumValue)) {
			return true;
		}
		return false;
	}

	public boolean filterMimeTypes(Object current) {
		if (this.filterMimeTypeValue == null
				|| this.filterMimeTypeValue.length() == 0) {
			return true;
		}
		CrawledResource cr = (CrawledResource) current;
		if (cr.getContentType() != null
				&& cr.getContentType().toLowerCase()
						.contains(this.filterMimeTypeValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean filterNames(Object current) {
		if (this.filterNameValue == null || this.filterNameValue.length() == 0) {
			return true;
		}
		Job j = (Job) current;
		if (j.getName().toLowerCase()
				.contains(this.filterNameValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean filterResStatus(Object current) {
		if (this.filterResStatusValue == null || this.filterResStatusValue < 0) {
			return true;
		}
		Job j = (Job) current;
		if (j.getJobStage() == null && this.filterResStatusValue == 1) {
			return true;
		}
		if (j.getJobStage() != null && j.isMappedResources()
				&& this.filterResStatusValue == 0) {
			return true;
		}
		if (j.getJobStage() != null
				&& !j.isMappedResources()
				&& (j.getCrawledResources() == null || j.getCrawledResources()
						.size() == 0) && this.filterResStatusValue == 2) {
			return true;
		}
		if (j.getJobStage() != null && !j.isMappedResources()
				&& j.getCrawledResources() != null
				&& j.getCrawledResources().size() != 0
				&& this.filterResStatusValue == 3) {
			return true;
		}
		return false;
	}

	public boolean filterSemanticMetadatum(Object current) {
		if (this.semanticMetadatumValue == null
				|| this.semanticMetadatumValue < 0) {
			return true;
		}
		CrawledResource cr = (CrawledResource) current;
		if (cr.getSemanticMetadatum() != null
				&& cr.getSemanticMetadatum().getId()
						.equals(this.semanticMetadatumValue)) {
			return true;
		}
		return false;
	}

	public boolean filterStatus(Object current) {
		if (this.filterStatusValue == null || this.filterStatusValue < 0) {
			return true;
		}
		Job j = (Job) current;
		Integer statusId = this.allStatusMap.get(j.getJobStage());
		if (statusId == null || statusId.equals(this.filterStatusValue)) {
			return true;
		}
		return false;
	}

	public boolean filterUsers(Object current) {
		if (this.filterUserValue == null || this.filterUserValue.length() == 0) {
			return true;
		}
		Job j = (Job) current;
		if (j.getCrawlerUser() != null
				&& j.getCrawlerUser().getUsername() != null
				&& j.getCrawlerUser().getUsername()
						.contains(this.filterUserValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public List<FunctionalMetadatum> getAllFunctionalMetadata() {
		return this.entityManager.createQuery(
				"from FunctionalMetadatum m order by m.description")
				.getResultList();
	}

	public Map<String, Integer> getAllResStatusMap() {
		return this.allResStatusMap;
	}

	@SuppressWarnings("unchecked")
	public List<SemanticMetadatum> getAllSemanticMetadata() {
		return this.entityManager.createQuery(
				"from SemanticMetadatum m order by m.description")
				.getResultList();
	}

	public Map<String, Integer> getAllStatusMap() {
		return this.allStatusMap;
	}

	public float getAnalyzedResourcesPercentage(Job j) {
		int res = 0;
		if (!this.analyzedResourcesPercentage.containsKey(j.getId())) {
			BigDecimal percentage = null;
			if (this.isSemanticCrawlerUser(j.getCrawlerUser())) {
				percentage = (BigDecimal) this.entityManager

						.createNativeQuery(
								"SELECT count(cr.id)/(SELECT count(cr2.id) FROM CrawledResource cr2 where cr2.job_id=:job1) FROM CrawledResource cr where cr.job_id=:job2 and (cr.semanticMetadatum_id is not null or cr.deleted=true) ")
						.setParameter("job1", j.getId())
						.setParameter("job2", j.getId()).getSingleResult();
			} else {
				percentage = (BigDecimal) this.entityManager
						.createNativeQuery(
								"SELECT count(cr.id)/(SELECT count(cr2.id) FROM CrawledResource cr2 where cr2.job_id=:job1) FROM CrawledResource cr where cr.job_id=:job2 and (cr.functionalMetadatum_id is not null or cr.deleted=true) ")
						.setParameter("job1", j.getId())
						.setParameter("job2", j.getId()).getSingleResult();
			}
			if (percentage == null) {
				percentage = new BigDecimal(0);
			}
			this.analyzedResourcesPercentage.put(j.getId(),
					Math.round(percentage.floatValue() * 10000));
		}
		res = this.analyzedResourcesPercentage.get(j.getId());
		return res;
	}

	public String getCleanedText() {
		return this.cleanedText;
	}

	public String getCompletedResources() {
		Long finished = (Long) this.entityManager
				.createQuery(
						"select count(*) from CrawledResource cr where cr.job=:job and cr.processed=:finished")
				.setParameter("job", this.job)
				.setParameter("finished", Parameter.FINISHED).getSingleResult();
		if (finished == 0L) {
			return "0";
		}
		return Long.toString(finished);
	}

	@SuppressWarnings("unchecked")
	@Factory("crawledResourcesRoots")
	public CrawledResource[] getCrawledResourcesRoots() {
		List<CrawledResource> roots = new ArrayList<CrawledResource>();
		String queryString = "select cr from CrawledResource cr where cr.job=:job order by cr.url";
		List<CrawledResource> orderedResources = this.entityManager
				.createQuery(queryString).setParameter("job", this.job)
				.getResultList();
		CrawledResource lastVisitedResource = null;
		for (CrawledResource cr : orderedResources) {
			if (lastVisitedResource == null) {
				roots.add(cr);
			} else {
				CrawledResource curFather = lastVisitedResource;
				while (curFather != null) {
					if (cr.getUrl().contains(curFather.getUrl())) {
						curFather.getChildren().add(cr);
						cr.setFather(curFather);
						break;
					}
					curFather = curFather.getFather();
				}
				if (curFather == null) {
					roots.add(cr);
				}
			}
			lastVisitedResource = cr;
		}
		CrawledResource[] array = roots.toArray(new CrawledResource[0]);
		return array;
	}

	public List<SelectItem> getCurrentLanguages() {
		List<SelectItem> resultList = new ArrayList<SelectItem>();
		String queryString = "select distinct(cr.language) from CrawledResource cr where cr.job=:job";
		Query query = this.entityManager.createQuery(queryString).setParameter(
				"job", this.job);
		List<String> curLanguages = query.getResultList();
		if (curLanguages == null || curLanguages.size() < 1
				|| curLanguages.size() == 1 && curLanguages.get(0) == null) {
			SelectItem si = new SelectItem("");
			resultList.add(si);
		}
		for (String lang : curLanguages) {
			if (lang != null) {
				resultList.add(new SelectItem(lang));
			}
		}
		return resultList;
	}

	@SuppressWarnings("unchecked")
	private User getCurrentUser() {
		List<User> users = this.entityManager
				.createQuery("from User u where u.username=:username")
				.setParameter("username",
						this.identity.getCredentials().getUsername())
				.getResultList();
		if (users.size() == 1) {
			return users.get(0);
		}
		return null;
	}

	public Integer getDetachedJobId() {
		if (this.job != null) {
			return this.job.getId();
		}
		return null;
	}

	public Long getDiscoveredURICount() throws HeritrixException,
			CrawlingFileException {
		return this.crawlerManager.getDiscoveredURICount(this.job,
				this.getCurrentUser());
	}

	public String getFilterMimeTypeValue() {
		return this.filterMimeTypeValue;
	}

	public String getFilterNameValue() {
		return this.filterNameValue;
	}

	public Integer getFilterResStatusValue() {
		return this.filterResStatusValue;
	}

	public Integer getFilterStatusValue() {
		return this.filterStatusValue;
	}

	public String getFilterURLValue() {
		return this.filterURLValue;
	}

	public String getFilterUserValue() {
		return this.filterUserValue;
	}

	public Long getFinishedURICount() throws HeritrixException,
			CrawlingFileException {
		return this.crawlerManager.getFinishedURICount(this.job,
				this.getCurrentUser());
	}

	public Integer getFirstResult() {
		if (this.firstResult == null) {
			return 0;
		}
		return this.firstResult;
	}

	public Integer getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public Integer getFunctionalMetadatumValue() {
		return this.functionalMetadatumValue;
	}

	public Job getJob() {
		return this.job;
	}

	@SuppressWarnings("unchecked")
	@Factory("jobCrawledResources")
	public List<CrawledResource> getJobCrawledResources() {
		String queryString = "select cr from CrawledResource cr left join cr.semanticMetadatum left join cr.functionalMetadatum where cr.job=:job";
		String queryCountString = "select count(*) from CrawledResource cr where cr.job=:job";
		if (this.getFilterURLValue() != null
				&& this.getFilterURLValue().length() > 0) {
			queryString += " and cr.url like :url";
			queryCountString += " and cr.url like :url";
		}
		if (this.getFilterMimeTypeValue() != null
				&& this.getFilterMimeTypeValue().length() > 0) {
			queryString += " and cr.contentType like :mimeType";
			queryCountString += " and cr.contentType like :mimeType";
		}
		if (this.getSemanticMetadatumValue() != null
				&& this.getSemanticMetadatumValue() != -1) {
			queryString += " and cr.semanticMetadatum.id=:semanticMetadatumId";
			queryCountString += " and cr.semanticMetadatum.id=:semanticMetadatumId";
		}
		if (this.getFunctionalMetadatumValue() != null
				&& this.getFunctionalMetadatumValue() != -1) {
			queryString += " and cr.functionalMetadatum.id=:functionalMetadatumId";
			queryCountString += " and cr.functionalMetadatum.id=:functionalMetadatumId";
		}
		if (this.isFilterAnalyzed()) {
			if (this.isSemanticCrawlerUser(this.job.getCrawlerUser())) {
				queryString += " and (cr.semanticMetadatum is null and cr.deleted=false) ";
				queryCountString += " and (cr.semanticMetadatum is null and cr.deleted=false) ";
			} else {
				queryString += " and (cr.functionalMetadatum is null and cr.deleted=false) ";
				queryCountString += " and (cr.functionalMetadatum is null and cr.deleted=false) ";
			}
		}
		if (this.sortField != null && this.sortField.length() > 0) {
			queryString += " order by cr." + this.sortField;
			if (this.sortOrder != null) {
				if (this.sortOrder.equals("down")) {
					queryString += " desc ";
				}
			}
		}
		Query query = this.entityManager.createQuery(queryString).setParameter(
				"job", this.job);
		Query queryCount = this.entityManager.createQuery(queryCountString)
				.setParameter("job", this.job);
		if (this.getFilterURLValue() != null
				&& this.getFilterURLValue().length() > 0) {
			query.setParameter("url", "%" + this.getFilterURLValue() + "%");
			queryCount
					.setParameter("url", "%" + this.getFilterURLValue() + "%");
		}
		if (this.getFilterMimeTypeValue() != null
				&& this.getFilterMimeTypeValue().length() > 0) {
			query.setParameter("mimeType", "%" + this.getFilterMimeTypeValue()
					+ "%");
			queryCount.setParameter("mimeType",
					"%" + this.getFilterMimeTypeValue() + "%");
		}
		if (this.getSemanticMetadatumValue() != null
				&& this.getSemanticMetadatumValue() != -1) {
			query.setParameter("semanticMetadatumId",
					this.semanticMetadatumValue);
			queryCount.setParameter("semanticMetadatumId",
					this.semanticMetadatumValue);
		}
		if (this.getFunctionalMetadatumValue() != null
				&& this.getFunctionalMetadatumValue() != -1) {
			query.setParameter("functionalMetadatumId",
					this.functionalMetadatumValue);
			queryCount.setParameter("functionalMetadatumId",
					this.functionalMetadatumValue);
		}
		if (this.firstResult == null) {
			this.firstResult = 0;
		}
		// System.out.println("*****" + this.pageSize);
		this.results = query.setFirstResult(this.firstResult)
				.setMaxResults(this.pageSize).getResultList();
		if (this.results != null) {
			this.setTotalResults((Long) queryCount.getSingleResult());
			if (this.getFirstResult() > this.getTotalResults()) {
				this.setFirstResult(0);

			}
		}
		return this.results;
	}

	public Integer getJobId() {
		return this.job.getId();
	}

	public String getJobSeeds() throws HeritrixException {
		List<String> jobSeeds = new ArrayList<String>();
		if (this.profile == null || this.profile.getProfileName() == null) {
			jobSeeds = this.crawlerManager.getJobSeeds(null,
					this.job.getName(), this.getCurrentUser());
		} else {
			jobSeeds = this.crawlerManager.getJobSeeds(
					this.profile.getProfileName(), this.job.getName(),
					this.getCurrentUser());
		}
		StringBuffer b = new StringBuffer();
		for (String j : jobSeeds) {
			b.append(j.trim() + "\n");
		}
		return b.toString().trim();
	}

	@Factory("jobValidationData")
	public Map<String, Number> getJobValidationData() {
		return this.getJobValidationDataForJob(this.job, this.entityManager);
	}

	private Map<String, Number> getJobValidationDataForJob(Job job,
			EntityManager entityManager) {
		Map<String, Number> res = new HashMap<String, Number>();
		// risorse
		Long resTot = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job")
				.setParameter("job", job).getSingleResult();
		res.put("res-tot", resTot);
		Long res0_100 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101")
				.setParameter("job", job).getSingleResult();
		res.put("res0-100", res0_100);
		Long res0_50 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 51")
				.setParameter("job", job).getSingleResult();
		res.put("res0-50", res0_50);
		Long res51_100 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101 and cr.wordsNumber > 50 ")
				.setParameter("job", job).getSingleResult();
		res.put("res51-100", res51_100);
		Long res101_1000 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>100")
				.setParameter("job", job).getSingleResult();
		res.put("res101-1000", res101_1000);
		Long res101_500 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 501 and cr.wordsNumber>100")
				.setParameter("job", job).getSingleResult();
		res.put("res101-500", res101_500);
		Long res501_1000 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>500")
				.setParameter("job", job).getSingleResult();
		res.put("res501-1000", res501_1000);
		Long res1001_5000 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 5001 and cr.wordsNumber>1000")
				.setParameter("job", job).getSingleResult();
		res.put("res1001-5000", res1001_5000);
		Long res5001_10000 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 10001 and cr.wordsNumber>5000")
				.setParameter("job", job).getSingleResult();
		res.put("res5001-10000", res5001_10000);
		Long resGT10000 = (Long) entityManager
				.createQuery(
						"select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber > 10001")
				.setParameter("job", job).getSingleResult();
		res.put("res10000+", resGT10000);
		if (resTot > 0L) {
			res.put("res0-100p", res0_100 * 100.0 / resTot);
			res.put("res0-50p", res0_50 * 100.0 / resTot);
			res.put("res51-100p", res51_100 * 100.0 / resTot);
			res.put("res101-1000p", res101_1000 * 100.0 / resTot);
			res.put("res101-500p", res101_500 * 100.0 / resTot);
			res.put("res501-1000p", res501_1000 * 100.0 / resTot);
			res.put("res1001-5000p", res1001_5000 * 100.0 / resTot);
			res.put("res5001-10000p", res5001_10000 * 100.0 / resTot);
			res.put("res10000+p", resGT10000 * 100.0 / resTot);
		}
		String q = "select count(cr.id) from CrawledResource cr where cr.job=:job  and cr.deleted=false ";
		Long ndResTot = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res-tot", ndResTot);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 100  and cr.deleted=false ";
		Long ndRes0_100 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res0-100", ndRes0_100);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 51  and cr.deleted=false ";
		Long ndRes0_50 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res0-50", ndRes0_50);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101 and cr.wordsNumber > 50  and cr.deleted=false ";
		Long ndRes51_100 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res51-100", ndRes51_100);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>101  and cr.deleted=false ";
		Long ndRes101_1000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res101-1000", ndRes101_1000);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 501 and cr.wordsNumber>100 and cr.deleted=false ";
		Long ndRes101_500 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res101-500", ndRes101_500);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>500 and cr.deleted=false ";
		Long ndRes501_1000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res501-1000", ndRes501_1000);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 5001 and cr.wordsNumber>1000 and cr.deleted=false ";
		Long ndRes1001_5000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res1001-5000", ndRes1001_5000);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 10001 and cr.wordsNumber>5000 and cr.deleted=false ";
		Long ndRes5001_10000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res5001-10000", ndRes5001_10000);
		q = "select count(cr.id) from CrawledResource cr where cr.job=:job and cr.wordsNumber > 10001 and cr.deleted=false ";
		Long ndResGT10000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		res.put("nd-res10000+", ndResGT10000);
		if (ndResTot > 0L) {
			res.put("nd-res0-100p", ndRes0_100 * 100.0 / ndResTot);
			res.put("nd-res0-50p", ndRes0_50 * 100.0 / ndResTot);
			res.put("nd-res51-100p", ndRes51_100 * 100.0 / ndResTot);
			res.put("nd-res101-1000p", ndRes101_1000 * 100.0 / ndResTot);
			res.put("nd-res101-500p", ndRes101_500 * 100.0 / ndResTot);
			res.put("nd-res501-1000p", ndRes501_1000 * 100.0 / ndResTot);
			res.put("nd-res1001-5000p", ndRes1001_5000 * 100.0 / ndResTot);
			res.put("nd-res5001-10000p", ndRes5001_10000 * 100.0 / ndResTot);
			res.put("nd-res10000+p", ndResGT10000 * 100.0 / ndResTot);
		}

		// parole
		Long wordsTot = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job")
				.setParameter("job", job).getSingleResult();
		if (wordsTot == null) {
			wordsTot = 0L;
		}
		res.put("words-tot", wordsTot);
		Long words0_100 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101")
				.setParameter("job", job).getSingleResult();
		if (words0_100 == null) {
			words0_100 = 0L;
		}
		res.put("words0-100", words0_100);
		Long words0_50 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 51")
				.setParameter("job", job).getSingleResult();
		if (words0_50 == null) {
			words0_50 = 0L;
		}
		res.put("words0-50", words0_50);
		Long words51_100 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101 and cr.wordsNumber > 50 ")
				.setParameter("job", job).getSingleResult();
		if (words51_100 == null) {
			words51_100 = 0L;
		}
		res.put("words51-100", words51_100);
		Long words101_1000 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>101")
				.setParameter("job", job).getSingleResult();
		if (words101_1000 == null) {
			words101_1000 = 0L;
		}
		res.put("words101-1000", words101_1000);
		Long words101_500 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 501 and cr.wordsNumber>100")
				.setParameter("job", job).getSingleResult();
		if (words101_500 == null) {
			words101_500 = 0L;
		}
		res.put("words101-500", words101_500);
		Long words501_1000 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>500")
				.setParameter("job", job).getSingleResult();
		if (words501_1000 == null) {
			words501_1000 = 0L;
		}
		res.put("words501-1000", words501_1000);
		Long words1001_5000 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 5001 and cr.wordsNumber>1000")
				.setParameter("job", job).getSingleResult();
		if (words1001_5000 == null) {
			words1001_5000 = 0L;
		}
		res.put("words1001-5000", words1001_5000);
		Long words5001_10000 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 10001 and cr.wordsNumber>5000")
				.setParameter("job", job).getSingleResult();
		if (words5001_10000 == null) {
			words5001_10000 = 0L;
		}
		res.put("words5001-10000", words5001_10000);
		Long wordsGT10000 = (Long) entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber > 10001")
				.setParameter("job", job).getSingleResult();
		if (wordsGT10000 == null) {
			wordsGT10000 = 0L;
		}
		res.put("words10000+", wordsGT10000);
		if (wordsTot > 0L) {
			res.put("words0-100p", words0_100 * 100.0 / wordsTot);
			res.put("words0-50p", words0_50 * 100.0 / wordsTot);
			res.put("words51-100p", words51_100 * 100.0 / wordsTot);
			res.put("words101-1000p", words101_1000 * 100.0 / wordsTot);
			res.put("words101-1000p", words101_1000 * 100.0 / wordsTot);
			res.put("words101-500p", words101_500 * 100.0 / wordsTot);
			res.put("words501-1000p", words501_1000 * 100.0 / wordsTot);
			res.put("words1001-5000p", words1001_5000 * 100.0 / wordsTot);
			res.put("words5001-10000p", words5001_10000 * 100.0 / wordsTot);
			res.put("words10000+p", wordsGT10000 * 100.0 / wordsTot);
		}

		// // parole analizzate

		// parole non cancellate
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job  and cr.deleted=false ";
		Long ndWordsTot = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWordsTot == null) {
			ndWordsTot = 0L;
		}
		res.put("nd-words-tot", ndWordsTot);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101 and cr.deleted=false ";
		Long ndWords0_100 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords0_100 == null) {
			ndWords0_100 = 0L;
		}
		res.put("nd-words0-100", ndWords0_100);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 51 and cr.deleted=false ";
		Long ndWords0_50 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords0_50 == null) {
			ndWords0_50 = 0L;
		}
		res.put("nd-words0-50", ndWords0_50);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 101 and cr.wordsNumber > 50 and cr.deleted=false ";
		Long ndWords51_100 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords51_100 == null) {
			ndWords51_100 = 0L;
		}
		res.put("nd-words51-100", ndWords51_100);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>100 and cr.deleted=false ";
		Long ndWords101_1000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords101_1000 == null) {
			ndWords101_1000 = 0L;
		}
		res.put("nd-words101-1000", ndWords101_1000);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 501 and cr.wordsNumber>100 and cr.deleted=false ";
		Long ndWords101_500 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords101_500 == null) {
			ndWords101_500 = 0L;
		}
		res.put("nd-words101-500", ndWords101_500);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 1001 and cr.wordsNumber>500 and cr.deleted=false ";
		Long ndWords501_1000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords501_1000 == null) {
			ndWords501_1000 = 0L;
		}
		res.put("nd-words501-1000", ndWords501_1000);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 5001 and cr.wordsNumber>1000 and cr.deleted=false ";
		Long ndWords1001_5000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords1001_5000 == null) {
			ndWords1001_5000 = 0L;
		}
		res.put("nd-words1001-5000", ndWords1001_5000);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber < 10001 and cr.wordsNumber>5000 and cr.deleted=false ";
		Long ndWords5001_10000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWords5001_10000 == null) {
			ndWords5001_10000 = 0L;
		}
		res.put("nd-words5001-10000", ndWords5001_10000);
		q = "select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.wordsNumber > 10001 and cr.deleted=false ";
		Long ndWordsGT10000 = (Long) entityManager.createQuery(q)
				.setParameter("job", job).getSingleResult();
		if (ndWordsGT10000 == null) {
			ndWordsGT10000 = 0L;
		}
		res.put("nd-words10000+", ndWordsGT10000);
		if (ndWordsTot > 0L) {
			res.put("nd-words0-100p", ndWords0_100 * 100.0 / ndWordsTot);
			res.put("nd-words0-50p", ndWords0_50 * 100.0 / ndWordsTot);
			res.put("nd-words51-100p", ndWords51_100 * 100.0 / ndWordsTot);
			res.put("nd-words101-1000p", ndWords101_1000 * 100.0 / ndWordsTot);
			res.put("nd-words101-1000p", ndWords101_1000 * 100.0 / ndWordsTot);
			res.put("nd-words101-500p", ndWords101_500 * 100.0 / ndWordsTot);
			res.put("nd-words501-1000p", ndWords501_1000 * 100.0 / ndWordsTot);
			res.put("nd-words1001-5000p", ndWords1001_5000 * 100.0 / ndWordsTot);
			res.put("nd-words5001-10000p", ndWords5001_10000 * 100.0
					/ ndWordsTot);
			res.put("nd-words10000+p", ndWordsGT10000 * 100.0 / ndWordsTot);
		}
		return res;
	}

	@Factory("jobValidationResources")
	public List<CrawledResource> getJobValidationResources() {
		String queryString = "select cr from CrawledResource cr left join cr.semanticMetadatum left join cr.functionalMetadatum where cr.validation <> :v and cr.job=:job";
		String queryCountString = "select count(*) from CrawledResource cr where cr.validation <> :v and cr.job=:job";
		String queryTotalCountString = queryCountString;
		if (this.getFilterURLValue() != null
				&& this.getFilterURLValue().length() > 0) {
			queryString += " and cr.url like :url";
			queryCountString += " and cr.url like :url";
		}
		if (this.getFilterMimeTypeValue() != null
				&& this.getFilterMimeTypeValue().length() > 0) {
			queryString += " and cr.contentType like :mimeType";
			queryCountString += " and cr.contentType like :mimeType";
		}
		if (this.isFilterValidated()) {
			queryString += " and cr.validation <> " + CrawledResource.VALIDATED
					+ " ";
			queryCountString += " and cr.validation <> "
					+ CrawledResource.VALIDATED + " ";
		}
		if (this.sortField != null && this.sortField.length() > 0) {
			queryString += " order by cr." + this.sortField;
			if (this.sortOrder != null) {
				if (this.sortOrder.equals("down")) {
					queryString += " desc ";
				}
			}
		}
		Query query = this.entityManager.createQuery(queryString)
				.setParameter("job", this.job)
				.setParameter("v", CrawledResource.NOT_CHOOSEN_FOR_VALIDATION);
		Query queryCount = this.entityManager.createQuery(queryCountString)
				.setParameter("job", this.job)
				.setParameter("v", CrawledResource.NOT_CHOOSEN_FOR_VALIDATION);
		Query queryTotalCount = this.entityManager
				.createQuery(queryTotalCountString)
				.setParameter("job", this.job)
				.setParameter("v", CrawledResource.NOT_CHOOSEN_FOR_VALIDATION);
		if (this.getFilterURLValue() != null
				&& this.getFilterURLValue().length() > 0) {
			query.setParameter("url", "%" + this.getFilterURLValue() + "%");
			queryCount
					.setParameter("url", "%" + this.getFilterURLValue() + "%");
		}
		if (this.getFilterMimeTypeValue() != null
				&& this.getFilterMimeTypeValue().length() > 0) {
			query.setParameter("mimeType", "%" + this.getFilterMimeTypeValue()
					+ "%");
			queryCount.setParameter("mimeType",
					"%" + this.getFilterMimeTypeValue() + "%");
		}
		if (this.firstResult == null) {
			this.firstResult = 0;
		}
		// System.out.println("*****" + this.pageSize);
		this.results = query.setFirstResult(this.firstResult)
				.setMaxResults(this.pageSize).getResultList();
		this.setTotalSelectedResourcesNumber((Long) queryTotalCount
				.getSingleResult());
		if (this.results != null) {
			this.setTotalResults((Long) queryCount.getSingleResult());
			if (this.getFirstResult() > this.getTotalResults()) {
				this.setFirstResult(0);

			}
		}
		this.entityManager.clear();
		return this.results;
	}

	@Factory("jobWordsCount")
	public Long getJobWordsCount() {
		return (Long) this.entityManager
				.createQuery(
						"select sum(cr.wordsNumber) from CrawledResource cr where cr.job=:job and cr.deleted=false and cr.noMoreAvailable=false")
				.setParameter("job", this.job).getSingleResult();
	}

	public List<String> getLanguagesToBeDeleted() {
		return this.languagesToBeDeleted;
	}

	public long getLastFirstResult() {
		return (this.getTotalResults() - 1) / this.pageSize * this.pageSize;
	}

	public Date getLastUpdate() {
		return (Date) this.entityManager
				.createQuery(
						"select max(cr.lastModified) from CrawledResource cr where cr.job=:job")
				.setParameter("job", this.job).getSingleResult();
	}

	public String getMimeTypeList() {
		return StringUtils.join(this.job.getChosenMimeTypes(), ", ");
	}

	public Integer getNextFirstResult() {

		return (this.getFirstResult() == null ? 0 : this.getFirstResult())
				+ this.pageSize;
	}

	public int getPageSize() {
		// System.out.println(this.pageSize);
		return this.pageSize;
	}

	public String getPercentCompleted() {
		Double percent = this.getPercentDouble();
		return JobManager.DECIMAL_FORMAT.format(percent);
	}

	public String getPercentCompletedInt() {
		return "" + Math.round(this.getPercentDouble());
	}

	private Double getPercentDouble() {
		String completed = this.getCompletedResources();
		String total = this.getTotalResources();
		if (completed == null || total == null) {
			return 0.0;
		}
		Long totalLong = Long.parseLong(total);
		if (totalLong < 1L) {
			return 0.0;
		}
		Long completedLong = Long.parseLong(completed);
		Double percent = completedLong * 1.0 / totalLong * 100;
		return percent;
	}

	public String getPollInterval() {
		return this.pollInterval;
	}

	public List<PoSLine> getPosText() {
		return this.posText;
	}

	public Integer getPreviousFirstResult() {
		if (this.pageSize > (this.getFirstResult() == null ? 0 : this
				.getFirstResult())) {
			return 0;
		} else {
			return this.getFirstResult() - this.pageSize;
		}
	}

	public Long getQueuedURICount() throws HeritrixException,
			CrawlingFileException {
		return this.crawlerManager.getQueuedURICount(this.job,
				this.getCurrentUser());
	}

	public Long getRawBytes() throws HeritrixException, CrawlingFileException {
		return this.crawlerManager.getRawBytes(this.job.getName(),
				this.getCurrentUser());
	}

	public Long getResourcesBytes() {
		return (Long) this.entityManager
				.createQuery(
						"select sum(cr.length) from CrawledResource cr where cr.job=:job and cr.deleted=false")
				.setParameter("job", this.job).getSingleResult();
	}

	public Integer getResultCount() {
		if (this.results != null) {
			return this.results.size();
		}
		return 0;
	}

	public Integer getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public Integer getSemanticMetadatumValue() {
		return this.semanticMetadatumValue;
	}

	public String getSortField() {
		return this.sortField;
	}

	public String getSortOrder() {
		return this.sortOrder;
	}

	public String getTestAfter() {
		return this.testAfter;
	}

	public String getTestBefore() {
		return this.testBefore;
	}

	public String getTestOutput() {
		return this.testOutput;
	}

	public String getTotalResources() {
		Long all = (Long) this.entityManager
				.createQuery(
						"select count(*) from CrawledResource cr where cr.job=:job")
				.setParameter("job", this.job).getSingleResult();
		if (all == 0L) {
			return "0";
		}
		return Long.toString(all);
	}

	public Long getTotalResults() {
		// System.out.println(this.totalResults);
		return this.totalResults;
	}

	public Long getTotalSelectedResourcesNumber() {
		return this.totalSelectedResourcesNumber;
	}

	public long getValidatedResourcesNumber() {
		return (Long) this.entityManager
				.createQuery(
						"select count (cr) from CrawledResource cr where cr.job=:j and cr.validation = "
								+ CrawledResource.VALIDATED)
				.setParameter("j", this.getJob()).getSingleResult();
	}

	public Integer getWordsLimitDeletionNumber() {
		return this.wordsLimitDeletionNumber;
	}

	public String indexSingleCrawledResource(CrawledResource crawledResource) {
		crawledResource.setProcessed(Parameter.INDEXING_PHASE);
		this.entityManager.merge(crawledResource);
		this.entityManager.flush();
		// this.asyncIndexer.indexSingleCrawledResource(crawledResource.getId(),
		// null, true);
		return "OK";
	}

	@Factory("jobs")
	public List<Job> initJobs() throws HeritrixException {
		return this.crawlerManager.getJobs(this.getCurrentUser());
	}

	@Create
	public void initParams() {
		this.perlUser = this.entityManager.find(Parameter.class,
				Parameter.PERL_CLEANER_USER.getKey()).getValue();
		this.perlPw = this.entityManager.find(Parameter.class,
				Parameter.PERL_CLEANER_PW.getKey()).getValue();
		this.cleanerPath = this.entityManager.find(Parameter.class,
				Parameter.PERL_CLEANER_PATH.getKey()).getValue();
		this.perlCleanerTemplate = this.entityManager.find(Parameter.class,
				Parameter.PERL_CLEANER_TEMPLATE.getKey()).getValue();
	}

	public boolean isFilterAnalyzed() {
		return this.filterAnalyzed;
	}

	public boolean isFilterValidated() {
		return this.filterValidated;
	}

	public boolean isJobCompleted() throws HeritrixException {
		return this.job.getJobStage().equals(CrawlStatus.ABORTED.toString())
				|| this.job.getJobStage().equals(
						CrawlStatus.FINISHED.toString());
	}

	public boolean isJobPaused() throws HeritrixException {
		String jobStage = this.job.getJobStage();
		if (jobStage == null) {
			return false;
		}
		return jobStage.equals(CrawlStatus.PAUSED.toString());
	}

	public boolean isJobRunning() throws HeritrixException {
		return this.job.getJobStage().equals(CrawlStatus.RUNNING.toString());
	}

	public boolean isNextExists() {
		return this.results != null
				&& this.results.size() >= this.pageSize
				&& this.getTotalResults() > this.getFirstResult()
						+ this.pageSize;
	}

	public boolean isPollEnabled() {
		if (this.getPollInterval() == null
				|| this.getPollInterval().equals("0")) {
			return false;
		}
		return true;
	}

	public boolean isPreviousExists() {
		if (this.results != null) {
			return this.getFirstResult() != null && this.getFirstResult() != 0;
		}
		return false;
	}

	private boolean isSemanticCrawlerUser(User crawlerUser) {
		String body = crawlerUser.getBody();
		if (body != null) {
			if (body.contains("LABLITA") || body.contains("UNITO")
					|| body.contains("UNIFI") || body.contains("UNISI")) {
				return false;
			}
		}
		return true;
	}

	public boolean isValidationToBeSaved() {
		return this.validationToBeSaved;
	}

	public boolean noCrawledResources(Integer jobId) {
		return this.entityManager
				.createQuery("from CrawledResource cr where cr.job.id=:jobId")
				.setParameter("jobId", jobId).setMaxResults(1).getResultList()
				.size() != 1;
	}

	public void pauseJob() throws HeritrixException {
		this.crawlerManager.pauseJob(this.job.getName(), this.getCurrentUser());
	}

	private void persistHandle(QuartzTriggerHandle handle) throws IOException,
			SchedulerException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos;
		oos = new ObjectOutputStream(baos);
		oos.writeObject(handle);
		oos.close();
		ScheduledJobHandle sjh = new ScheduledJobHandle();
		sjh.setJobFullName(handle.getTrigger().getFullJobName());
		sjh.setSerializedHandle(baos.toByteArray());
		sjh.setJob(this.job);
		this.entityManager.persist(sjh);
		this.job.setScheduledJobHandle(sjh);
	}

	public void refresh() throws HeritrixException {
		this.crawlerManager.updateJobsList(this.getCurrentUser());
	}

	private void removePreviousScheduling() throws IOException,
			ClassNotFoundException, SchedulerException {
		ScheduledJobHandle sjh = this.job.getScheduledJobHandle();
		if (sjh == null) {
			return;
		}
		byte[] serializedHandle = sjh.getSerializedHandle();
		ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(
				serializedHandle));
		QuartzTriggerHandle qth = (QuartzTriggerHandle) ois.readObject();
		ois.close();
		qth.cancel();
		sjh.setJob(null);
		this.job.setScheduledJobHandle(null);
		this.entityManager.remove(sjh);
	}

	public String reprocessResources() {
		this.job.setMappedResources(false);
		List<CrawledResource> crawledResources = new ArrayList<CrawledResource>(
				this.job.getCrawledResources());
		for (CrawledResource cr : crawledResources) {
			this.job.getCrawledResources().remove(cr);
			this.entityManager.remove(cr);
			// this.asyncIndexer.deleteSingleCrawledResource(cr.getId());
		}
		List<CrawledResource> noMoreAvailableResources = new ArrayList<CrawledResource>(
				this.job.getNoMoreAvailableResources());
		for (CrawledResource cr : noMoreAvailableResources) {
			this.job.getCrawledResources().remove(cr);
			this.entityManager.remove(cr);
		}
		this.job.setValidationNotes(null);
		this.job.setValidationStatus(Job.TO_BE_VALIDATED);
		this.entityManager.merge(this.job);
		this.entityManager.flush();
		return "OK";
	}

	public String resetFactory() {
		Contexts.getConversationContext().set("jobCrawledResources", null);
		return "RESET";
	}

	public String resetValidationData() {
		this.entityManager
				.createQuery(
						"update CrawledResource cr set cr.validation="
								+ CrawledResource.NOT_CHOOSEN_FOR_VALIDATION
								+ ", cr.validationStatus="
								+ CrawledResource.TO_BE_VALIDATED_RESOURCE
								+ " where cr.job=:j")
				.setParameter("j", this.getJob()).executeUpdate();
		this.getJob().setValidationStatus(Job.TO_BE_VALIDATED);
		this.getJob().setValidationNotes(null);
		this.getJob().setValidationThreshold(10);
		this.entityManager.merge(this.getJob());
		this.entityManager.flush();
		this.entityManager.clear();
		this.resetValidationFactory();
		return "OK";
	}

	public String resetValidationFactory() {
		Contexts.getConversationContext().set("jobValidationResources", null);
		return "RESET";
	}

	public List<CrawledResource> resourcesSample() {
		return this.job.getCrawledResources().subList(0,
				Math.min(this.job.getCrawledResources().size(), 30));
	}

	public void resumeJob() throws HeritrixException {
		this.crawlerManager
				.resumeJob(this.job.getName(), this.getCurrentUser());
	}

	public void retrieveCleanedText(CrawledResource cr) {
		File resourceDir = new File(FilenameUtils.getFullPath(cr.getArcFile()
				.replaceAll("__\\d+", "")) + JobManager.RESOURCESDIR);
		File cleanedTextFile = new File(resourceDir, cr.getDigest() + ".txt");
		if (cleanedTextFile.exists() && cleanedTextFile.canRead()) {
			try {
				this.setCleanedText(FileUtils.readFileToString(cleanedTextFile));
			} catch (IOException e) {
				e.printStackTrace();
			}
		} else {
			this.setCleanedText("");
		}
	}

	public void retrievePoSText(CrawledResource cr) {
		File resourceDir = new File(FilenameUtils.getFullPath(cr.getArcFile()
				.replaceAll("__\\d+", "")) + JobManager.RESOURCESDIR);
		File posTextFile = new File(resourceDir, cr.getDigest() + ".txt.pos");
		List<PoSLine> posLines = new ArrayList<PoSLine>();
		try {
			List<String> lines = FileUtils.readLines(posTextFile);
			StrTokenizer tokenizer = StrTokenizer.getTSVInstance();
			for (String l : lines) {
				tokenizer.reset(l);
				String[] tokens = tokenizer.getTokenArray();
				if (tokens.length == 3) {
					PoSLine poSLine = new PoSLine();
					poSLine.setForm(tokens[0].trim());
					poSLine.setPosTag(tokens[1].trim());
					poSLine.setLemma(tokens[2].trim());
					posLines.add(poSLine);
				}
			}

		} catch (IOException e) {

		}
		this.setPosText(posLines);
	}

	public void saveJob() {
		if (this.job != null) {
			this.entityManager.persist(this.job);
		}
	}

	public void savePeriodicity() throws HeritrixException,
			FileHandlingException, JobHandlingException {
		this.entityManager.merge(this.job);
		User user = this.getCurrentUser();
		try {
			this.removePreviousScheduling();
			if (this.job.getPeriodicity().equals("periodic")) {
				String cronString = this.calculateCronString();
				QuartzTriggerHandle handle = this.scheduledJobExecutor
						.schedulePeriodicJob(new Date(), cronString,
								this.job.getEndDate(), this.job, user);
				this.persistHandle(handle);
				this.job.setCronData(cronString);
			} else if (this.job.getPeriodicity().equals("once")) {
				QuartzTriggerHandle handle = this.scheduledJobExecutor
						.scheduleRunOnceJob(this.job.getFirstDate(), this.job,
								user);
				this.persistHandle(handle);
				this.job.setCronData(this.job.getFirstDate().toString());
			} else {
				this.job.setCronData("never");
			}
		} catch (IOException e) {
			throw new FileHandlingException(
					this.messages.get("FileRetrievingError"));

		} catch (ClassNotFoundException e) {
			throw new JobHandlingException(
					this.messages.get("JobRetrievingError"));
		} catch (SchedulerException e) {
			throw new JobHandlingException(
					this.messages.get("JobRetrievingError"));
		}
	}

	public String saveValidationThreshold() {
		if (this.getJob().getValidationStatus().equals(Job.VALIDATED_NOK)
				|| this.getJob().getValidationStatus().equals(Job.VALIDATED_OK)) {
			this.getJob().setValidationStatus(this.calculateValidationResult());
		}
		this.entityManager.merge(this.getJob());
		this.resetValidationFactory();
		return "OK";
	}

	private void selectResourceToBeValidated(int amount, int startIdx,
			int endIdx, EntityManager entityManager2, Job j)
			throws NoSuchAlgorithmException, IOException {
		if (amount < 1) {
			return;
		}
		List<Integer> ids = new ArrayList<Integer>();
		Map<String, Integer> hashes = new HashMap<String, Integer>();
		int cursize = hashes.size();
		while (hashes.size() < amount) {
			for (CrawledResource cr : (List<CrawledResource>) entityManager2
					.createQuery(
							"from CrawledResource cr where cr.job=:j and cr.wordsNumber<:eidx and cr.wordsNumber>=:sidx and cr.deleted=false order by rand()")
					.setParameter("j", j).setParameter("sidx", startIdx)
					.setParameter("eidx", endIdx).setMaxResults(amount)
					.getResultList()) {
				if (cr.getExtractedTextHash() == null) {
					File f = new File(
							FilenameUtils.getFullPath(cr.getArcFile())
									+ JobMapperMonitor.RESOURCESDIR
									+ cr.getDigest() + ".txt");
					if (f.exists() && f.canRead()) {
						cr.setExtractedTextHash(MD5DigestCreator
								.getMD5Digest(f));
						entityManager2.merge(cr);
					} else {
						continue;
					}
				}
				hashes.put(cr.getExtractedTextHash(), cr.getId());
			}
			if (cursize == hashes.size()) {
				break;
			} else {
				cursize = hashes.size();
			}
		}
		ids.addAll(hashes.values());
		if (ids.size() > 0) {
			entityManager2
					.createQuery(
							"update CrawledResource cr set cr.validation=:v where cr.id in (:ids)")
					.setParameter("ids", ids)
					.setParameter("v", CrawledResource.CHOOSEN_FOR_VALIDATION)
					.executeUpdate();
		}
	}

	@Asynchronous
	@Transactional
	public String selectValidationResources(Integer jobId) {
		int transactionTimeoutSeconds = 240;
		try {
			((javax.transaction.UserTransaction) org.jboss.seam.transaction.Transaction
					.instance())
					.setTransactionTimeout(transactionTimeoutSeconds);
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// set timeout to
			// 10 mins
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			Job j = this.entityManager.find(Job.class, jobId);
			j.setValidationStatus(Job.VALIDATION_WAIT);
			this.entityManager.persist(j);
			this.entityManager.flush();
			this.userTx.commit();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			Number resourcesWordsNumberAverage = (Number) this.entityManager
					.createQuery(
							"select avg(cr.wordsNumber) from CrawledResource cr where cr.job=:j")
					.setParameter("j", j).getSingleResult();
			double percent = 0.0;
			if (resourcesWordsNumberAverage.doubleValue() < 100.0) {
				percent = 0.01;
			} else if (resourcesWordsNumberAverage.doubleValue() > 99100.0) {
				percent = 1;
			} else {
				percent = resourcesWordsNumberAverage.doubleValue() / 1000 + 0.9;
			}
			percent = percent / 100;
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			Map<String, Number> jobValidationData = this
					.getJobValidationDataForJob(j, this.entityManager);
			this.entityManager.flush();
			this.userTx.commit();
			int n0_100 = new Double(Math.ceil(jobValidationData.get(
					"nd-res0-100").doubleValue()
					* percent)).intValue();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.selectResourceToBeValidated(n0_100, 0, 100,
					this.entityManager, j);
			this.entityManager.flush();
			this.userTx.commit();
			int n101_1000 = new Double(Math.ceil(jobValidationData.get(
					"nd-res101-1000").doubleValue()
					* percent)).intValue();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.selectResourceToBeValidated(n101_1000, 101, 1000,
					this.entityManager, j);
			this.entityManager.flush();
			this.userTx.commit();
			int n1001_5000 = new Double(Math.ceil(jobValidationData.get(
					"nd-res1001-5000").doubleValue()
					* percent)).intValue();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.selectResourceToBeValidated(n1001_5000, 1001, 5000,
					this.entityManager, j);
			this.entityManager.flush();
			this.userTx.commit();
			int n5001_10000 = new Double(Math.ceil(jobValidationData.get(
					"nd-res5001-10000").doubleValue()
					* percent)).intValue();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.selectResourceToBeValidated(n5001_10000, 5001, 10000,
					this.entityManager, j);
			this.entityManager.flush();
			this.userTx.commit();
			int n10000 = new Double(Math.ceil(jobValidationData.get(
					"nd-res10000+").doubleValue()
					* percent)).intValue();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.selectResourceToBeValidated(n10000, 10001, Integer.MAX_VALUE,
					this.entityManager, j);
			this.entityManager.flush();
			this.userTx.commit();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			j.setValidationStatus(Job.VALIDATION_IN_PROGRESS);
			this.entityManager.joinTransaction();
			this.entityManager.merge(j);
			this.entityManager.flush();
			this.userTx.commit();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
		} catch (Exception e) {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
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
			e.printStackTrace();
		} finally {

		}
		return "OK";
	}

	private void setCleanedText(String cleanedText) {
		this.cleanedText = cleanedText;

	}

	public void setDetachedJobId(Integer jobId) {
		if (jobId != null) {
			this.job = this.entityManager.find(Job.class, jobId);
			Session sess = (Session) this.entityManager.getDelegate();
			sess.evict(this.job);
		} else {
			this.job = null;
		}
	}

	public void setFilterAnalyzed(boolean filterAnalyzed) {
		this.filterAnalyzed = filterAnalyzed;
	}

	public void setFilterMimeTypeValue(String filterMimeTypeValue) {
		this.filterMimeTypeValue = filterMimeTypeValue;
	}

	public void setFilterNameValue(String filterNameValue) {
		this.filterNameValue = filterNameValue;
	}

	public void setFilterResStatusValue(Integer filterResStatusValue) {
		this.filterResStatusValue = filterResStatusValue;
	}

	public void setFilterStatusValue(Integer filterStatusValue) {
		this.filterStatusValue = filterStatusValue;
	}

	public void setFilterURLValue(String filterURLValue) {
		this.filterURLValue = filterURLValue;
	}

	public void setFilterUserValue(String filterUserValue) {
		this.filterUserValue = filterUserValue;
	}

	public void setFilterValidated(boolean filterValidated) {
		this.filterValidated = filterValidated;
	}

	public void setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
	}

	public void setFunctionalMetadatum(Integer functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setFunctionalMetadatumValue(Integer functionalMetadatumValue) {
		this.functionalMetadatumValue = functionalMetadatumValue;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setJobId(Integer jobId) {
		if (jobId != null) {
			this.job = this.entityManager.find(Job.class, jobId);
		} else {
			this.job = null;
		}
		if (this.job.getCleaningScript() == null
				|| this.job.getCleaningScript().trim().length() < 1) {
			try {
				this.job.setCleaningScript(FileUtils.readFileToString(new File(
						this.perlCleanerTemplate)));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void setLanguagesToBeDeleted(List<String> languagesToBeDeleted) {
		this.languagesToBeDeleted = languagesToBeDeleted;
	}

	public void setPageSize(int resPerPage) {
		this.pageSize = resPerPage;
	}

	public void setPollInterval(String pollInterval) {
		this.pollInterval = pollInterval;
	}

	public void setPosText(List<PoSLine> posText) {
		this.posText = posText;
	}

	public void setSemanticMetadatum(Integer semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSemanticMetadatumValue(Integer semanticMetadatumValue) {
		this.semanticMetadatumValue = semanticMetadatumValue;
	}

	public void setSortField(String sortField) {
		this.sortField = sortField;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setTotalResults(Long singleResult) {
		this.totalResults = singleResult;

	}

	public void setTotalSelectedResourcesNumber(Long singleResult) {
		this.totalSelectedResourcesNumber = singleResult;

	}

	public void setValidation(CrawledResource crawledResource,
			Integer validationStatus) {
		if (crawledResource != null
				&& validationStatus >= CrawledResource.TO_BE_VALIDATED_RESOURCE
				&& validationStatus <= CrawledResource.NOT_VALID_RESOURCE) {
			crawledResource.setValidationStatus(validationStatus);
			if (!validationStatus
					.equals(CrawledResource.TO_BE_VALIDATED_RESOURCE)) {
				crawledResource.setValidation(CrawledResource.VALIDATED);
			} else {
				crawledResource
						.setValidation(CrawledResource.CHOOSEN_FOR_VALIDATION);
			}
			this.setValidationToBeSaved(true);
			// this.entityManager.merge(crawledResource);
		}
		// if (this.getValidatedResourcesNumber() == 0) {
		// this.job.setValidationStatus(Job.TO_BE_VALIDATED);
		// } else if (this.getValidatedResourcesNumber() < this
		// .getTotalSelectedResourcesNumber()) {
		// this.job.setValidationStatus(Job.VALIDATION_IN_PROGRESS);
		// } else {
		// this.job.setValidationStatus(this.calculateValidationResult());
		// }
		// this.entityManager.merge(this.job);
	}

	public void setValidationToBeSaved(boolean validationToBeSaved) {
		this.validationToBeSaved = validationToBeSaved;
	}

	public void setWordsLimitDeletionNumber(Integer wordsLimitDeletionNumber) {
		this.wordsLimitDeletionNumber = wordsLimitDeletionNumber;
	}

	public void startJob() throws HeritrixException {
		this.crawlerManager.startJob(this.job.getName(), this.getCurrentUser());
	}

	public void stopJob() throws HeritrixException {
		this.crawlerManager.stopJob(this.job.getName(), this.getCurrentUser());
	}

	public void testScript(CrawledResource cr) {
		JobCleaner jobCleaner = new JobCleaner(this.perlUser, this.perlPw,
				this.cleanerPath, cr.getJob().getCleaningScript(), null);
		jobCleaner.testScript(cr, cr.getJob().getCleaningScript());
		this.testBefore = jobCleaner.getTestBefore();
		this.testAfter = jobCleaner.getTestAfter();
		this.testOutput = jobCleaner.getTestOutput();
	}

	public void toggleDeleted(CrawledResource crawledResource) {
		if (crawledResource != null) {
			crawledResource.setDeleted(!crawledResource.isDeleted());
			if (crawledResource.isDeleted() && crawledResource.isIndexed()) {
				crawledResource.setProcessed(Parameter.FINISHED);
				// this.asyncIndexer.deleteSingleCrawledResource(crawledResource
				// .getId());
			} else if (!crawledResource.isDeleted()
					&& !crawledResource.isIndexed()) {
				crawledResource.setProcessed(Parameter.INDEXING_PHASE);
				// this.asyncIndexer.indexSingleCrawledResource(
				// crawledResource.getId(), null, true);
			}
			this.entityManager.merge(crawledResource);
		}
	}

}
