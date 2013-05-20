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

public class SketchResult {
	private String collocata;
	private long fA;
	private long fB;
	private long fAB;
	private double score;

	public String getCollocata() {
		return this.collocata;
	}

	public long getfA() {
		return this.fA;
	}

	public long getfAB() {
		return this.fAB;
	}

	public long getfB() {
		return this.fB;
	}

	public double getScore() {
		return this.score;
	}

	public void setCollocata(String collocata) {
		this.collocata = collocata;
	}

	public void setfA(long fA) {
		this.fA = fA;
	}

	public void setfAB(long fAB) {
		this.fAB = fAB;
	}

	public void setfB(long fB) {
		this.fB = fB;
	}

	public void setScore(double score) {
		this.score = score;
	}

}
