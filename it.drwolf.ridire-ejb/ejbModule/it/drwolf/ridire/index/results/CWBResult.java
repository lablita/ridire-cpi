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
package it.drwolf.ridire.index.results;

import java.util.ArrayList;
import java.util.List;

public class CWBResult {

	private String searchedText;
	private String leftContext;
	private String rightContext;
	private String url;
	private boolean grouped = false;
	private boolean groupExpanded = false;
	private Integer groupSize = 0;
	private String semantic;
	private String functional;
	private Integer startPosition = 0;
	private Integer endPosition = 0;
	private Integer leftContextSize = 10;
	private Integer rightContextSize = 10;
	private List<String> groupQuery = new ArrayList<String>();
	private List<CWBResult> members = new ArrayList<CWBResult>();

	public CWBResult(String leftContext, String searchedText,
			String rightContext, String url, String semantic, String functional) {
		this.setSearchedText(searchedText);
		this.setLeftContext(leftContext);
		this.setRightContext(rightContext);
		this.setUrl(url);
		this.setSemantic(semantic);
		this.setFunctional(functional);
	}

	public String getDomain() {
		return (this.getFunctional() + " " + this.getSemantic()).trim();
	}

	public Integer getEndPosition() {
		return this.endPosition;
	}

	public String getFunctional() {
		return this.functional;
	}

	public List<String> getGroupQuery() {
		return this.groupQuery;
	}

	public Integer getGroupSize() {
		return this.groupSize;
	}

	public String getLeftContext() {
		return this.leftContext;
	}

	public List<CWBResult> getMembers() {
		return this.members;
	}

	public String getRightContext() {
		return this.rightContext;
	}

	public String getSearchedText() {
		return this.searchedText;
	}

	public String getSemantic() {
		return this.semantic;
	}

	public Integer getStartPosition() {
		return this.startPosition;
	}

	public String getUrl() {
		return this.url;
	}

	public boolean isGrouped() {
		return this.grouped;
	}

	public boolean isGroupExpanded() {
		return this.groupExpanded;
	}

	public void setEndPosition(Integer endPosition) {
		this.endPosition = endPosition;
	}

	public void setFunctional(String functional) {
		this.functional = functional;
	}

	public void setGrouped(boolean grouped) {
		this.grouped = grouped;
	}

	public void setGroupExpanded(boolean groupExpanded) {
		this.groupExpanded = groupExpanded;
	}

	public void setGroupQuery(List<String> groupQuery) {
		this.groupQuery = groupQuery;
	}

	public void setGroupSize(Integer groupSize) {
		this.groupSize = groupSize;
	}

	public void setLeftContext(String leftContext) {
		this.leftContext = leftContext;
	}

	public void setMembers(List<CWBResult> members) {
		this.members = members;
	}

	public void setRightContext(String rightContext) {
		this.rightContext = rightContext;
	}

	public void setSearchedText(String searchedText) {
		this.searchedText = searchedText;
	}

	public void setSemantic(String semantic) {
		this.semantic = semantic;
	}

	public void setStartPosition(Integer startPosition) {
		this.startPosition = startPosition;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getLeftContextSize() {
		return leftContextSize;
	}

	public void setLeftContextSize(Integer leftContextSize) {
		this.leftContextSize = leftContextSize;
	}

	public Integer getRightContextSize() {
		return rightContextSize;
	}

	public void setRightContextSize(Integer rightContextSize) {
		this.rightContextSize = rightContextSize;
	}
}
