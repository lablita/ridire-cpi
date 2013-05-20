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

import it.drwolf.ridire.util.data.CorpusDataReport;
import it.drwolf.ridire.util.data.UserDataReport;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("resourcesReporterData")
@Scope(ScopeType.APPLICATION)
public class ResourcesReporterData {
	private Map<String, Integer> allTotal = new HashMap<String, Integer>();
	private Map<String, Integer> functionalTotal = new HashMap<String, Integer>();
	private Map<String, Integer> semanticTotal = new HashMap<String, Integer>();
	private Map<String, Integer> allTotalValid = new HashMap<String, Integer>();
	private Map<String, Integer> functionalTotalValid = new HashMap<String, Integer>();
	private Map<String, Integer> semanticTotalValid = new HashMap<String, Integer>();
	private Map<String, List<CorpusDataReport>> all = new HashMap<String, List<CorpusDataReport>>();
	private Map<String, List<CorpusDataReport>> functional = new HashMap<String, List<CorpusDataReport>>();
	private Map<String, List<CorpusDataReport>> semantic = new HashMap<String, List<CorpusDataReport>>();
	private Map<String, List<UserDataReport>> mappedJobs = new HashMap<String, List<UserDataReport>>();
	private Map<String, List<UserDataReport>> totalJobs = new HashMap<String, List<UserDataReport>>();
	private Map<String, List<UserDataReport>> notMappedJobs = new HashMap<String, List<UserDataReport>>();
	private Map<String, List<UserDataReport>> notStartedJobs = new HashMap<String, List<UserDataReport>>();
	private Map<String, List<UserDataReport>> wordsNumber = new HashMap<String, List<UserDataReport>>();

	public void clearAll() {
		this.all.clear();
		this.allTotal.clear();
		this.allTotalValid.clear();
		this.functional.clear();
		this.functionalTotal.clear();
		this.functionalTotalValid.clear();
		this.mappedJobs.clear();
		this.notMappedJobs.clear();
		this.notStartedJobs.clear();
		this.semantic.clear();
		this.semanticTotal.clear();
		this.totalJobs.clear();
		this.wordsNumber.clear();
	}

	public Map<String, List<CorpusDataReport>> getAll() {
		return this.all;
	}

	public Map<String, Integer> getAllTotal() {
		return this.allTotal;
	}

	public Map<String, Integer> getAllTotalValid() {
		return this.allTotalValid;
	}

	public Map<String, List<CorpusDataReport>> getFunctional() {
		return this.functional;
	}

	public Map<String, Integer> getFunctionalTotal() {
		return this.functionalTotal;
	}

	public Map<String, Integer> getFunctionalTotalValid() {
		return this.functionalTotalValid;
	}

	public Map<String, List<UserDataReport>> getMappedJobs() {
		return this.mappedJobs;
	}

	public Map<String, List<UserDataReport>> getNotMappedJobs() {
		return this.notMappedJobs;
	}

	public Map<String, List<UserDataReport>> getNotStartedJobs() {
		return this.notStartedJobs;
	}

	public Map<String, List<CorpusDataReport>> getSemantic() {
		return this.semantic;
	}

	public Map<String, Integer> getSemanticTotal() {
		return this.semanticTotal;
	}

	public Map<String, Integer> getSemanticTotalValid() {
		return this.semanticTotalValid;
	}

	public Map<String, List<UserDataReport>> getTotalJobs() {
		return this.totalJobs;
	}

	public Map<String, List<UserDataReport>> getWordsNumber() {
		return this.wordsNumber;
	}

	public void setAll(Map<String, List<CorpusDataReport>> all) {
		this.all = all;
	}

	public void setAllTotal(Map<String, Integer> allTotal) {
		this.allTotal = allTotal;
	}

	public void setAllTotalValid(Map<String, Integer> allTotalValid) {
		this.allTotalValid = allTotalValid;
	}

	public void setFunctional(Map<String, List<CorpusDataReport>> functional) {
		this.functional = functional;
	}

	public void setFunctionalTotal(Map<String, Integer> functionalTotal) {
		this.functionalTotal = functionalTotal;
	}

	public void setFunctionalTotalValid(
			Map<String, Integer> functionalTotalValid) {
		this.functionalTotalValid = functionalTotalValid;
	}

	public void setMappedJobs(Map<String, List<UserDataReport>> mappedJobs) {
		this.mappedJobs = mappedJobs;
	}

	public void setNotMappedJobs(Map<String, List<UserDataReport>> notMappedJobs) {
		this.notMappedJobs = notMappedJobs;
	}

	public void setNotStartedJobs(
			Map<String, List<UserDataReport>> notStartedJobs) {
		this.notStartedJobs = notStartedJobs;
	}

	public void setSemantic(Map<String, List<CorpusDataReport>> semantic) {
		this.semantic = semantic;
	}

	public void setSemanticTotal(Map<String, Integer> semanticTotal) {
		this.semanticTotal = semanticTotal;
	}

	public void setSemanticTotalValid(Map<String, Integer> semanticTotalValid) {
		this.semanticTotalValid = semanticTotalValid;
	}

	public void setTotalJobs(Map<String, List<UserDataReport>> totalJobs) {
		this.totalJobs = totalJobs;
	}

	public void setWordsNumber(Map<String, List<UserDataReport>> wordsNumber) {
		this.wordsNumber = wordsNumber;
	}
}
