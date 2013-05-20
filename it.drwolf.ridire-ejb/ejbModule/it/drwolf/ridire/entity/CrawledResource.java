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
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

@Entity
public class CrawledResource implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8000386648735053276L;

	private Integer id;

	private String url;

	private Long offset;
	private String contentType;
	private Long length;
	private String ip;
	private boolean deleted;
	private FunctionalMetadatum functionalMetadatum;
	private SemanticMetadatum semanticMetadatum;
	private Date archiveDate;
	private Date lastModified;
	private String digest;
	private String arcFile;
	private String cleaner;
	private boolean checked;
	private boolean noMoreAvailable = false;
	private Integer wordsNumber = 0;
	private String extractedTextHash;
	private Job job;
	private List<CrawledResource> children = new ArrayList<CrawledResource>();
	private CrawledResource father;
	private Integer processed = Parameter.NOT_PROCESSED;
	private String language;
	public static Integer NOT_CHOOSEN_FOR_VALIDATION = 0;
	public static Integer CHOOSEN_FOR_VALIDATION = 1;
	public static Integer VALIDATED = 2;
	private Integer validation = CrawledResource.NOT_CHOOSEN_FOR_VALIDATION;
	public static Integer TO_BE_VALIDATED_RESOURCE = 0;
	public static Integer VALID_RESOURCE = 1;
	public static Integer NOT_VALID_RESOURCE = 2;
	private Integer validationStatus = CrawledResource.TO_BE_VALIDATED_RESOURCE;

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof CrawledResource)) {
			return false;
		}
		CrawledResource other = (CrawledResource) obj;
		if (this.getUrl() == null) {
			if (other.getUrl() != null) {
				return false;
			}
		} else if (!this.getUrl().equals(other.getUrl())) {
			return false;
		}
		return true;
	}

	// private ProcessingPhase currentProcessingPhase;

	public String getArcFile() {
		return this.arcFile;
	}

	public Date getArchiveDate() {
		return this.archiveDate;
	}

	@Transient
	public List<CrawledResource> getChildren() {
		return this.children;
	}

	public String getCleaner() {
		return this.cleaner;
	}

	public String getContentType() {
		return this.contentType;
	}

	public String getDigest() {
		return this.digest;
	}

	// @ManyToOne(fetch = FetchType.LAZY)
	// public ProcessingPhase getCurrentProcessingPhase() {
	// return this.currentProcessingPhase;
	// }

	public String getExtractedTextHash() {
		return this.extractedTextHash;
	}

	@Transient
	public CrawledResource getFather() {
		return this.father;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public FunctionalMetadatum getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	public String getIp() {
		return this.ip;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public Job getJob() {
		return this.job;
	}

	public String getLanguage() {
		return this.language;
	}

	public Date getLastModified() {
		return this.lastModified;
	}

	public Long getLength() {
		return this.length;
	}

	@Transient
	public CrawledResource[] getNodes() {
		return this.children.toArray(new CrawledResource[0]);
	}

	/**
	 * It's used to easily retrieve the record inside the ARC file
	 * 
	 * @return The offset inside the ARC file
	 */
	public Long getOffset() {
		return this.offset;
	}

	public Integer getProcessed() {
		return this.processed;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public SemanticMetadatum getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	@Lob
	@Index(name = "url_idx")
	@Column(columnDefinition = "TEXT")
	public String getUrl() {
		return this.url;
	}

	public Integer getValidation() {
		return this.validation;
	}

	public Integer getValidationStatus() {
		return this.validationStatus;
	}

	public Integer getWordsNumber() {
		return this.wordsNumber;
	}

	// @Transient
	// public boolean isFinishedProcessing() {
	// if (this.getCurrentProcessingPhase() != null
	// && this.getCurrentProcessingPhase().getSubsequentPhase() == null) {
	// return true;
	// }
	// return false;
	// }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.url == null ? 0 : this.url.hashCode());
		return result;
	}

	@Transient
	public boolean isChecked() {
		return this.checked;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	@Transient
	public boolean isFinishedProcessing() {
		if (this.getProcessed() != null
				&& this.getProcessed().equals(Parameter.FINISHED)) {
			return true;
		}
		return false;
	}

	@Transient
	public boolean isIndexed() {
		return this.getProcessed().equals(Parameter.INDEXED);
	}

	// public void setCurrentProcessingPhase(ProcessingPhase
	// currentProcessingPhase) {
	// this.currentProcessingPhase = currentProcessingPhase;
	// }

	public boolean isNoMoreAvailable() {
		return this.noMoreAvailable;
	}

	public void setArcFile(String arcFile) {
		this.arcFile = arcFile;
	}

	public void setArchiveDate(Date archiveDate) {
		this.archiveDate = archiveDate;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	public void setChildren(List<CrawledResource> children) {
		this.children = children;
	}

	public void setCleaner(String cleaner) {
		if (cleaner != null) {
			cleaner = cleaner.trim();
		}
		this.cleaner = cleaner;

	}

	@Index(name = "contenttype_idx")
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public void setExtractedTextHash(String extractedTextHash) {
		this.extractedTextHash = extractedTextHash;
	}

	public void setFather(CrawledResource father) {
		this.father = father;
	}

	public void setFunctionalMetadatum(FunctionalMetadatum functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setIp(String ip) {
		this.ip = ip;
	}

	public void setJob(Job job) {
		this.job = job;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setLastModified(Date lastModified) {
		this.lastModified = lastModified;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setNoMoreAvailable(boolean noMoreAvailable) {
		this.noMoreAvailable = noMoreAvailable;
	}

	public void setOffset(Long offset) {
		this.offset = offset;
	}

	public void setProcessed(Integer step) {
		this.processed = step;

	}

	public void setSemanticMetadatum(SemanticMetadatum semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public void setValidation(Integer validation) {
		this.validation = validation;
	}

	public void setValidationStatus(Integer validationStatus) {
		this.validationStatus = validationStatus;
	}

	public void setWordsNumber(Integer wordsNumber) {
		this.wordsNumber = wordsNumber;
	}

	@Override
	public String toString() {
		return "CrawledResource [url=" + this.url + "]";
	}

}
