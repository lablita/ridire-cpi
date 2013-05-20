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
package it.drwolf.ridire.index.pattern;

public class PatternData {
	private String text;
	private String textType = "FORMA";
	private int distance = 0;
	private boolean exactDistance = false;

	public int getDistance() {
		return this.distance;
	}

	public String getText() {
		return this.text;
	}

	public String getTextType() {
		return this.textType;
	}

	public boolean hasWildcards() {
		if (this.getText() != null && this.getText().trim().length() > 0) {
			if (this.getText().contains("*") || this.getText().contains("?")) {
				return true;
			}
		}
		return false;
	}

	public void setDistance(int distance) {
		this.distance = distance;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setTextType(String textType) {
		this.textType = textType;
	}

	public void setExactDistance(boolean exactDistance) {
		this.exactDistance = exactDistance;
	}

	public boolean isExactDistance() {
		return exactDistance;
	}
}
