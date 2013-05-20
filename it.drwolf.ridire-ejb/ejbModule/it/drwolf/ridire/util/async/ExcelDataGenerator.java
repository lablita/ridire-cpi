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
package it.drwolf.ridire.util.async;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name(value = "excelDataGenerator")
@Scope(ScopeType.SESSION)
public class ExcelDataGenerator {
	private boolean fileReady = false;
	private ByteArrayOutputStream baos;
	private boolean inProgress = false;

	private String toBeVisualized;

	private String forma;

	private String lemma;

	private String pos;
	private String easypos;

	private String phrase;

	private Integer contextLength = 5;
	private List<String> functionalMetadatum;
	private List<String> semanticMetadatum;
	private Integer contextGroupingLength = 1;
	private List<String> corporaNames;
	private String sortBy;
	private String sortOrder;
	private int progress = 0;
	private Integer quantity = 100;
	private Integer threshold = 0;
	private String frequencyBy;

	public ByteArrayOutputStream getBaos() {
		return this.baos;
	}

	public Integer getContextGroupingLength() {
		return this.contextGroupingLength;
	}

	public Integer getContextLength() {
		return this.contextLength;
	}

	public List<String> getCorporaNames() {
		return this.corporaNames;
	}

	public String getEasypos() {
		return this.easypos;
	}

	public String getForma() {
		return this.forma;
	}

	public String getFrequencyBy() {
		return this.frequencyBy;
	}

	public List<String> getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public String getLemma() {
		return this.lemma;
	}

	public String getPhrase() {
		return this.phrase;
	}

	public String getPos() {
		return this.pos;
	}

	public int getProgress() {
		return this.progress;
	}

	public Integer getQuantity() {
		return this.quantity;
	}

	public List<String> getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public String getSortBy() {
		return this.sortBy;
	}

	public String getSortOrder() {
		return this.sortOrder;
	}

	public Integer getThreshold() {
		return this.threshold;
	}

	public String getToBeVisualized() {
		return this.toBeVisualized;
	}

	public boolean isFileReady() {
		return this.fileReady;
	}

	public boolean isInProgress() {
		return this.inProgress;
	}

	public void setBaos(ByteArrayOutputStream baos) {
		this.baos = baos;
	}

	public void setContextGroupingLength(Integer contextGroupingLength) {
		this.contextGroupingLength = contextGroupingLength;
	}

	public void setContextLength(Integer contextLength) {
		this.contextLength = contextLength;
	}

	public void setCorporaNames(List<String> corporaNames) {
		this.corporaNames = corporaNames;
	}

	public void setEasypos(String easypos) {
		this.easypos = easypos;
	}

	public void setFileReady(boolean fileReady) {
		this.fileReady = fileReady;
	}

	public void setForma(String forma) {
		this.forma = forma;
	}

	public void setFrequencyBy(String frequencyBy) {
		this.frequencyBy = frequencyBy;
	}

	public void setFunctionalMetadatum(List<String> functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setInProgress(boolean inProgress) {
		this.inProgress = inProgress;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public void setProgress(int i) {
		this.progress = i;

	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public void setSemanticMetadatum(List<String> semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;
	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;
	}

	public void setThreshold(Integer threshold) {
		this.threshold = threshold;
	}

	public void setToBeVisualized(String toBeVisualized) {
		this.toBeVisualized = toBeVisualized;
	}
}
