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
package it.drwolf.ridire.index.sketch;

import java.util.ArrayList;
import java.util.List;

public class SketchTable {
	private String sketchName;
	private Integer globalFrequency;
	private Integer position = 1;
	private List<SketchResultRow> rows = new ArrayList<SketchResultRow>();

	public SketchTable(String name) {
		this.sketchName = name;
	}

	public Integer getGlobalFrequency() {
		return this.globalFrequency;
	}

	public Integer getPosition() {
		return this.position;
	}

	public List<SketchResultRow> getRows() {
		return this.rows;
	}

	public String getSketchName() {
		return this.sketchName;
	}

	public void setGlobalFrequency(Integer globalFrequency) {
		this.globalFrequency = globalFrequency;
	}

	public void setPosition(Integer position) {
		this.position = position;
	}

	public void setRows(List<SketchResultRow> rows) {
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
