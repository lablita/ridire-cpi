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
package it.drwolf.ridire.util.data;

public class CorpusDataReport {
	private String domFunzionale;
	private String domSemantico;
	private Integer words;
	private Integer wordsValid;

	public CorpusDataReport(String domFunzionale, String domSemantico,
			Integer words) {
		super();
		this.domFunzionale = domFunzionale;
		this.domSemantico = domSemantico;
		this.words = words;
	}

	public String getDomFunzionale() {
		return this.domFunzionale;
	}

	public String getDomSemantico() {
		return this.domSemantico;
	}

	public Integer getWords() {
		return this.words;
	}

	public Integer getWordsValid() {
		return this.wordsValid;
	}

	public void setDomFunzionale(String domFunzionale) {
		this.domFunzionale = domFunzionale;
	}

	public void setDomSemantico(String domSemantico) {
		this.domSemantico = domSemantico;
	}

	public void setWords(Integer words) {
		this.words = words;
	}

	public void setWordsValid(Integer wordsValid) {
		this.wordsValid = wordsValid;
	}
}
