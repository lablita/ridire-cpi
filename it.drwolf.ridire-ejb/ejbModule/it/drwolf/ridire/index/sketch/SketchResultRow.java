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

public class SketchResultRow implements Comparable<SketchResultRow> {
	private String item;

	private Integer frequency;

	private Double score;

	public int compareTo(SketchResultRow o) {
		if (this.score < 0) {
			return -1;
		}
		if (o.getScore() < 0) {
			return 1;
		}
		return -this.score.compareTo(o.getScore());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SketchResultRow)) {
			return false;
		}
		SketchResultRow other = (SketchResultRow) obj;
		if (this.item == null) {
			if (other.item != null) {
				return false;
			}
		} else if (!this.item.equals(other.item)) {
			return false;
		}
		return true;
	}

	public Integer getFrequency() {
		return this.frequency;
	}

	public String getItem() {
		return this.item;
	}

	public Double getScore() {
		return this.score;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.item == null ? 0 : this.item.hashCode());
		return result;
	}

	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public void setScore(Double score) {
		this.score = score;
	}
}
