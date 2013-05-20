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
package it.drwolf.ridire.entity;

import static javax.persistence.GenerationType.AUTO;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;

import org.archive.crawler.framework.CrawlStatus;
import org.hibernate.annotations.CollectionOfElements;

@Entity
public class Job implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6049392106441802338L;

	private String jobStage = "";

	private String name;

	private Integer id;
	private String childJobName = null;
	private User crawlerUser;
	private String validationNotes;
	private String cronData = "";
	private Date firstDate;
	private Date endDate;
	private String periodicity = "";
	private String periodFrequency = "";
	private String writtenResourceURLPattern = "";
	private String writtenURLs = "";
	private String goodURLs = "";
	private boolean writtenURLsDenied;
	private boolean goodURLsDenied;
	private String followedURLPattern = "";
	private boolean mappedResources = false;
	private Integer wordsNumber;
	private boolean indexed = false;
	private ScheduledJobHandle scheduledJobHandle;
	public static Integer TO_BE_VALIDATED = 0;
	public static Integer VALIDATION_IN_PROGRESS = 1;
	public static Integer VALIDATED_OK = 2;
	public static Integer VALIDATED_NOK = 3;
	public static Integer VALIDATION_WAIT = 4;
	private Integer validationStatus = Job.TO_BE_VALIDATED;
	private Integer validationThreshold = 10;
	private List<CrawledResource> crawledResources = new ArrayList<CrawledResource>(
			0);
	private List<CrawledResource> noMoreAvailableResources = new ArrayList<CrawledResource>(
			0);
	private List<String> chosenMimeTypes = new ArrayList<String>();
	private String seeds = "";
	private String cleaningScript;

	private boolean selectedForCorpusCreation = false;

	public Job() {
	}

	public Job(String name, String jobStage) {
		this.name = name;
		this.jobStage = jobStage;
	}

	public String getChildJobName() {
		return this.childJobName;
	}

	@CollectionOfElements
	public List<String> getChosenMimeTypes() {
		return this.chosenMimeTypes;
	}

	@Lob
	public String getCleaningScript() {
		return this.cleaningScript;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "job")
	public List<CrawledResource> getCrawledResources() {
		return this.crawledResources;
	}

	@ManyToOne(fetch = FetchType.EAGER)
	public User getCrawlerUser() {
		return this.crawlerUser;
	}

	public String getCronData() {
		return this.cronData;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getEndDate() {
		return this.endDate;
	}

	@Temporal(TemporalType.TIMESTAMP)
	public Date getFirstDate() {
		return this.firstDate;
	};

	@Lob
	@Column(columnDefinition = "TEXT")
	public String getFollowedURLPattern() {
		return this.followedURLPattern;
	}

	@Lob
	@Column(columnDefinition = "TEXT")
	public String getGoodURLs() {
		return this.goodURLs;
	}

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	public String getJobStage() {
		if (this.jobStage.startsWith("Unbuilt")) {
			return CrawlStatus.CREATED.toString();
		}
		if (this.jobStage.startsWith("Finished")) {
			return CrawlStatus.FINISHED.toString();
		}
		if (this.jobStage.startsWith("Active")) {
			return CrawlStatus.RUNNING.toString();
		}
		return this.jobStage;
	}

	public String getName() {
		return this.name;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "job")
	public List<CrawledResource> getNoMoreAvailableResources() {
		return this.noMoreAvailableResources;
	}

	public String getPeriodFrequency() {
		return this.periodFrequency;
	}

	public String getPeriodicity() {
		return this.periodicity;
	}

	@OneToOne(fetch = FetchType.LAZY, mappedBy = "job")
	public ScheduledJobHandle getScheduledJobHandle() {
		return this.scheduledJobHandle;
	}

	@Lob
	@Column(columnDefinition = "TINYTEXT")
	public String getSeeds() {
		return this.seeds;
	}

	@Lob
	public String getValidationNotes() {
		return this.validationNotes;
	}

	public Integer getValidationStatus() {
		return this.validationStatus;
	}

	public Integer getValidationThreshold() {
		return this.validationThreshold;
	}

	public Integer getWordsNumber() {
		return this.wordsNumber;
	}

	@Lob
	@Column(columnDefinition = "TEXT")
	public String getWrittenResourceURLPattern() {
		return this.writtenResourceURLPattern;
	}

	@Lob
	@Column(columnDefinition = "TEXT")
	public String getWrittenURLs() {
		return this.writtenURLs;
	}

	public boolean isGoodURLsDenied() {
		return this.goodURLsDenied;
	}

	public boolean isIndexed() {
		return this.indexed;
	}

	public boolean isMappedResources() {
		return this.mappedResources;
	}

	@Transient
	public boolean isSelectedForCorpusCreation() {
		return this.selectedForCorpusCreation;
	}

	public boolean isWrittenURLsDenied() {
		return this.writtenURLsDenied;
	}

	public void setChildJobName(String childJobName) {
		this.childJobName = childJobName;
	}

	public void setChosenMimeTypes(List<String> chosenMimeTypes) {
		this.chosenMimeTypes = chosenMimeTypes;

	}

	public void setCleaningScript(String cleaningScript) {
		this.cleaningScript = cleaningScript;
	}

	public void setCrawledResources(List<CrawledResource> crawledResources) {
		this.crawledResources = crawledResources;
	}

	public void setCrawlerUser(User crawlerUser) {
		this.crawlerUser = crawlerUser;
	}

	public void setCronData(String cronData) {
		this.cronData = cronData;
	}

	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}

	public void setFirstDate(Date firstDate) {
		this.firstDate = firstDate;
	}

	public void setFollowedURLPattern(String followedURLPattern) {
		this.followedURLPattern = followedURLPattern;
	}

	public void setGoodURLs(String goodURLs) {
		this.goodURLs = goodURLs;
	}

	public void setGoodURLsDenied(boolean goodURLsDenied) {
		this.goodURLsDenied = goodURLsDenied;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setIndexed(boolean indexed) {
		this.indexed = indexed;
	}

	public void setJobStage(String jobStage) {
		this.jobStage = jobStage;
	}

	public void setMappedResources(boolean mappedResources) {
		this.mappedResources = mappedResources;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNoMoreAvailableResources(
			List<CrawledResource> noMoreAvailableResources) {
		this.noMoreAvailableResources = noMoreAvailableResources;
	}

	public void setPeriodFrequency(String periodFrequency) {
		this.periodFrequency = periodFrequency;
	}

	public void setPeriodicity(String periodicity) {
		this.periodicity = periodicity;
	}

	public void setScheduledJobHandle(ScheduledJobHandle scheduledJobHandle) {
		this.scheduledJobHandle = scheduledJobHandle;
	}

	public void setSeeds(String jobSeeds) {
		this.seeds = jobSeeds;

	}

	public void setSelectedForCorpusCreation(boolean selectedForCorpusCreation) {
		this.selectedForCorpusCreation = selectedForCorpusCreation;
	}

	public void setValidationNotes(String validationNotes) {
		this.validationNotes = validationNotes;
	}

	public void setValidationStatus(Integer validationStatus) {
		this.validationStatus = validationStatus;
	}

	public void setValidationThreshold(Integer validationThreshold) {
		this.validationThreshold = validationThreshold;
	}

	public void setWordsNumber(Integer wordsNumber) {
		this.wordsNumber = wordsNumber;
	}

	public void setWrittenResourceURLPattern(String pattern) {
		this.writtenResourceURLPattern = pattern;
	}

	public void setWrittenURLs(String writtenURLs) {
		this.writtenURLs = writtenURLs;
	}

	public void setWrittenURLsDenied(boolean writtenURLsDenied) {
		this.writtenURLsDenied = writtenURLsDenied;
	}

	@Override
	public String toString() {
		return this.getName();
	}
}
