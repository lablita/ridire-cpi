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

public class SketchDiffRow implements Comparable<SketchDiffRow> {
	private String item;

	private Integer frequency1 = 0;

	private Double score1 = 0.0;
	private Integer frequency2 = 0;
	private Double score2 = 0.0;
	private Double difference;
	private Double summedScores = 0.0;

	public int compareTo(SketchDiffRow o) {
		if (o != null) {
			return -this.getDifference().compareTo(o.getDifference());
		}
		return -1;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof SketchDiffRow)) {
			return false;
		}
		SketchDiffRow other = (SketchDiffRow) obj;
		if (this.item == null) {
			if (other.item != null) {
				return false;
			}
		} else if (!this.item.equals(other.item)) {
			return false;
		}
		return true;
	}

	public String getColor() {
		Double diff = this.getDifference();
		if (diff > 6.0) {
			diff = 6.0;
		} else if (diff < -6.0) {
			diff = -6.0;
		}
		if (diff > 6.0) {
			return "#00FF00";
		}
		if (diff > 4.0) {
			return "#55FF55";
		}
		if (diff > 2.0) {
			return "#AAFFAA";
		}
		if (diff > -2.0) {
			return "#FFFFFF";
		}
		if (diff > -4.0) {
			return "#FFAAAA";
		}
		if (diff > -6.0) {
			return "#FF5555";
		}
		return "#FF0000";
	}

	public Double getDifference() {
		return this.difference;
	}

	public Integer getFrequency1() {
		return this.frequency1;
	}

	public Integer getFrequency2() {
		return this.frequency2;
	}

	public String getItem() {
		return this.item;
	}

	public Double getScore1() {
		return this.score1;
	}

	public Double getScore2() {
		return this.score2;
	}

	public Double getSummedScores() {
		return this.summedScores;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.item == null ? 0 : this.item.hashCode());
		return result;
	}

	public void setDifference(Double difference) {
		this.difference = difference;
	}

	public void setFrequency1(Integer frequency1) {
		this.frequency1 = frequency1;
	}

	public void setFrequency2(Integer frequency2) {
		this.frequency2 = frequency2;
	}

	public void setItem(String item) {
		this.item = item;
	}

	public void setScore1(Double score1) {
		this.score1 = score1;
	}

	public void setScore2(Double score2) {
		this.score2 = score2;
	}

	public void setSummedScores(Double summedScores) {
		this.summedScores = summedScores;
	}
}
