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
package it.drwolf.ridire.index.results;

public class ItemWithContext {
	private String searchedText;
	private LeftContext leftContext;
	private String rightContext;
	private String path;
	private int rightContextSize;
	private int leftContextSize;
	private long offset;
	private boolean rightContextEnd = false;
	private boolean leftContextEnd = false;

	public ItemWithContext() {
		// TODO Auto-generated constructor stub
	}

	public ItemWithContext(String searchedText, String leftContext,
			String rightContext) {
		super();
		this.searchedText = searchedText;
		this.leftContext = new LeftContext(leftContext);
		this.rightContext = rightContext;
	}

	public LeftContext getLeftContext() {
		return this.leftContext;
	}

	public int getLeftContextSize() {
		return this.leftContextSize;
	}

	public long getOffset() {
		return this.offset;
	}

	public String getPath() {
		return this.path;
	}

	public String getRightContext() {
		return this.rightContext;
	}

	public int getRightContextSize() {
		return this.rightContextSize;
	}

	public String getSearchedText() {
		return this.searchedText;
	}

	public boolean isLeftContextEnd() {
		return this.leftContextEnd;
	}

	public boolean isRightContextEnd() {
		return this.rightContextEnd;
	}

	public void setLeftContext(LeftContext leftContext) {
		this.leftContext = leftContext;
	}

	public void setLeftContextEnd(boolean leftContextEnd) {
		this.leftContextEnd = leftContextEnd;
	}

	public void setLeftContextSize(int leftContextSize) {
		this.leftContextSize = leftContextSize;
	}

	public void setOffset(long offset) {
		this.offset = offset;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public void setRightContext(String rightContext) {
		this.rightContext = rightContext;
	}

	public void setRightContextEnd(boolean rightContextEnd) {
		this.rightContextEnd = rightContextEnd;
	}

	public void setRightContextSize(int rightContextSize) {
		this.rightContextSize = rightContextSize;
	}

	public void setSearchedText(String searchedText) {
		this.searchedText = searchedText;
	}
}
