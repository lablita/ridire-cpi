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
package it.drwolf.ridire.util;

import java.util.ArrayList;
import java.util.List;

public class SelectableJob {
	private Integer id;
	private String name;
	private Integer wordsNumber;
	private boolean external = false;
	private boolean selectedForCorpusCreation;
	private List<String> corporaName = new ArrayList<String>();

	public Integer getId() {
		return this.id;
	}

	public String getName() {
		return this.name;
	}

	public Integer getWordsNumber() {
		return this.wordsNumber;
	}

	public boolean isExternal() {
		return this.external;
	}

	public boolean isSelectedForCorpusCreation() {
		return this.selectedForCorpusCreation;
	}

	public void setExternal(boolean external) {
		this.external = external;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSelectedForCorpusCreation(boolean selectedForCorpusCreation) {
		this.selectedForCorpusCreation = selectedForCorpusCreation;
	}

	public void setWordsNumber(Integer wordsNumber) {
		this.wordsNumber = wordsNumber;
	}

	public List<String> getCorporaName() {
		return corporaName;
	}

	public void setCorporaName(List<String> corporaName) {
		this.corporaName = corporaName;
	}
}
