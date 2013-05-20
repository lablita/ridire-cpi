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
package it.drwolf.ridire.index.cwb.scripts;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("vrtFilesBuilderData")
@Scope(ScopeType.SESSION)
public class VRTFilesBuilderData {
	private String origDir;
	private String destDir;
	private String jobsList;
	private boolean inverse;

	public String getDestDir() {
		return this.destDir;
	}

	public String getJobsList() {
		return this.jobsList;
	}

	public String getOrigDir() {
		return this.origDir;
	}

	public boolean isInverse() {
		return this.inverse;
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

	public void setOrigDir(String origDir) {
		this.origDir = origDir;
	}
}
