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
package it.drwolf.ridire.index.cwb.scripts;

import org.apache.commons.math3.util.ArithmeticUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("cwbCorpusBuilder")
@Scope(ScopeType.CONVERSATION)
public class CWBCorpusBuilder {

	@In(create = true)
	private VRTFilesBuilderData vrtFilesBuilderData;

	@In(create = true)
	private VRTFilesBuilder vrtFilesBuilder;

	private String origDir;

	private String destDir;

	private String jobsList;
	private Integer n;
	private Integer lfNumber = 0;
	private boolean inverse = false;

	public void buildCorpus() {
		this.vrtFilesBuilderData.setDestDir(this.getDestDir());
		this.vrtFilesBuilder.buildFiles(this.vrtFilesBuilderData);
	}

	public void buildCorpusFromFiles() {
		this.vrtFilesBuilderData.setDestDir(this.getDestDir());
		this.vrtFilesBuilderData.setOrigDir(this.getOrigDir());
		this.vrtFilesBuilderData.setInverse(this.inverse);
		this.vrtFilesBuilder.buildFilesFromFiles(this.vrtFilesBuilderData);
	}

	public void buildCorpusFromJobNames() {
		this.vrtFilesBuilderData.setDestDir(this.getDestDir());
		this.vrtFilesBuilderData.setJobsList(this.getJobsList());
		this.vrtFilesBuilder.buildFilesFromJobNames(this.vrtFilesBuilderData);
	}

	public void calculateN() {
		int ret = 0;
		if (this.getN() < 1) {
			return;
		}
		for (int k = 1; k <= this.getN(); k++) {
			ret += ArithmeticUtils.binomialCoefficient(this.getN(), k);
		}
		this.setLfNumber(ret);
	}

	public String getDestDir() {
		return this.destDir;
	}

	public String getJobsList() {
		return this.jobsList;
	}

	public Integer getLfNumber() {
		return this.lfNumber;
	}

	public Integer getN() {
		return this.n;
	}

	public String getOrigDir() {
		return this.origDir;
	}

	public boolean isInverse() {
		return this.inverse;
	}

	public void retagFiles() {
		this.vrtFilesBuilderData.setDestDir(this.getDestDir());
		this.vrtFilesBuilderData.setOrigDir(this.getOrigDir());
		this.vrtFilesBuilder.retagFiles(this.vrtFilesBuilderData);
	}

	public void reverseVRTFiles() {
		this.vrtFilesBuilderData.setDestDir(this.getDestDir());
		this.vrtFilesBuilderData.setOrigDir(this.getOrigDir());
		this.vrtFilesBuilder.reverseVRTFiles(this.vrtFilesBuilderData);
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public void setJobsList(String jobsList) {
		this.jobsList = jobsList;
	}

	public void setLfNumber(Integer lfNumber) {
		this.lfNumber = lfNumber;
	}

	public void setN(Integer n) {
		this.n = n;
	}

	public void setOrigDir(String origDir) {
		this.origDir = origDir;
	}
}
