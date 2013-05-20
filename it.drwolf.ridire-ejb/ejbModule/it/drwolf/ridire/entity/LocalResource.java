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
package it.drwolf.ridire.entity;

import static javax.persistence.GenerationType.AUTO;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

@Entity
public class LocalResource implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5123167208139365474L;

	private Integer id;
	private String contentType;
	private Long length;
	private boolean deleted;
	private FunctionalMetadatum functionalMetadatum;
	private SemanticMetadatum semanticMetadatum;
	private Date archiveDate;
	private String digest;
	private boolean checked;
	private String origFileName;
	private String uniqueFileName;
	private User crawlerUser;

	public Date getArchiveDate() {
		return this.archiveDate;
	}

	public String getContentType() {
		return this.contentType;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public User getCrawlerUser() {
		return this.crawlerUser;
	}

	public String getDigest() {
		return this.digest;
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

	public Long getLength() {
		return this.length;
	}

	public String getOrigFileName() {
		return this.origFileName;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public SemanticMetadatum getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public String getUniqueFileName() {
		return this.uniqueFileName;
	}

	@Transient
	public boolean isChecked() {
		return this.checked;
	}

	public boolean isDeleted() {
		return this.deleted;
	}

	public void setArchiveDate(Date archiveDate) {
		this.archiveDate = archiveDate;
	}

	public void setChecked(boolean checked) {
		this.checked = checked;
	}

	@Index(name = "contenttype_idx")
	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public void setCrawlerUser(User crawlerUser) {
		this.crawlerUser = crawlerUser;
	}

	public void setDeleted(boolean deleted) {
		this.deleted = deleted;
	}

	public void setDigest(String digest) {
		this.digest = digest;
	}

	public void setFunctionalMetadatum(FunctionalMetadatum functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setLength(Long length) {
		this.length = length;
	}

	public void setOrigFileName(String origFileName) {
		this.origFileName = origFileName;
	}

	public void setSemanticMetadatum(SemanticMetadatum semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setUniqueFileName(String uniqueFileName) {
		this.uniqueFileName = uniqueFileName;
	}

}
