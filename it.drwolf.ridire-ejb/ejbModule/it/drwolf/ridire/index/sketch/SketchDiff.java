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

import java.util.ArrayList;
import java.util.List;

public class SketchDiff {
	private String sketchName;
	private Integer globalFrequency1;
	private Integer globalFrequency2;
	private Integer position = 1;
	private List<SketchDiffRow> rows = new ArrayList<SketchDiffRow>();

	public SketchDiff(String name) {
		this.sketchName = name;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SketchDiff)) {
			return false;
		}
		SketchDiff other = (SketchDiff) obj;
		if (this.sketchName == null) {
			if (other.sketchName != null) {
				return false;
			}
		} else if (!this.sketchName.equals(other.sketchName)) {
			return false;
		}
		return true;
	}

	public Integer getGlobalFrequency1() {
		return this.globalFrequency1;
	}

	public Integer getGlobalFrequency2() {
		return this.globalFrequency2;
	}

	public Integer getPosition() {
		return this.position;
	}

	public List<SketchDiffRow> getRows() {
		return this.rows;
	}

	public String getSketchName() {
		return this.sketchName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.sketchName == null ? 0 : this.sketchName.hashCode());
		return result;
	}

	public void setGlobalFrequency1(Integer globalFrequency1) {
		this.globalFrequency1 = globalFrequency1;
	}

	public void setGlobalFrequency2(Integer globalFrequency2) {
		this.globalFrequency2 = globalFrequency2;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public void setRows(List<SketchDiffRow> rows) {
		this.rows = rows;
	}

	public void setSketchName(String sketchName) {
		this.sketchName = sketchName;
	}

	@Override
	public String toString() {
		return this.getSketchName() + " " + this.getPosition();
	}
}
