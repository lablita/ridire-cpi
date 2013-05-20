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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

@Entity
public class ProcessingPhase implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 2822752394069182716L;
	private String id;
	private Set<CrawledResource> crawledResources = new HashSet<CrawledResource>(
			0);
	private ProcessingPhase precedingPhase;
	private ProcessingPhase subsequentPhase;
	public final static String TEXT_EXTRACTION = "text extraction";
	public final static String DOCUMENT_FORMAT_CONVERSION = "document format conversion";
	public final static String POS_TAGGING = "PoS tagging";
	public final static String INDEXING = "indexing";
	public static final String[] DEFAULT = new String[] {
			DOCUMENT_FORMAT_CONVERSION, TEXT_EXTRACTION, POS_TAGGING, INDEXING };

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "currentProcessingPhase")
	public Set<CrawledResource> getCrawledResources() {
		return this.crawledResources;
	}

	@Id
	public String getId() {
		return this.id;
	}

	@OneToOne(fetch = FetchType.EAGER)
	public ProcessingPhase getPrecedingPhase() {
		return this.precedingPhase;
	}

	@OneToOne(fetch = FetchType.EAGER)
	public ProcessingPhase getSubsequentPhase() {
		return this.subsequentPhase;
	}

	public void setCrawledResources(Set<CrawledResource> crawledResources) {
		this.crawledResources = crawledResources;
	}

	public void setId(String id) {
		this.id = id;
	}

	public void setPrecedingPhase(ProcessingPhase precedingPhase) {
		this.precedingPhase = precedingPhase;
	}

	public void setSubsequentPhase(ProcessingPhase subsequentPhase) {
		this.subsequentPhase = subsequentPhase;
	}

}
