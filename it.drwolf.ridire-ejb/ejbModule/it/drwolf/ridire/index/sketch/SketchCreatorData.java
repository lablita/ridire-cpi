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
package it.drwolf.ridire.index.sketch;

import org.apache.lucene.index.IndexWriter;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("sketchCreatorData")
@Scope(ScopeType.APPLICATION)
public class SketchCreatorData {
	private String workingDir;
	private Integer processNumber;
	private IndexWriter indexWriter;

	public IndexWriter getIndexWriter() {
		return this.indexWriter;
	}

	public Integer getProcessNumber() {
		return this.processNumber;
	}

	public String getWorkingDir() {
		return this.workingDir;
	}

	public void setIndexWriter(IndexWriter indexWriter) {
		this.indexWriter = indexWriter;
	}

	public void setProcessNumber(Integer processNumber) {
		this.processNumber = processNumber;
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}
}
