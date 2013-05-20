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
package it.drwolf.ridire.index.cwb;

public class CWBCollocateResult {
	private String term;
	private long freqInCorpus;
	private double expectedCollocateFreq;
	private long observedCollocateFreq;
	private long differentRes;

	private double scoreValue;
	private String measure;

	public CWBCollocateResult(String term, long freqInCorpus,
			double expectedCollocateFreq, long observedCollocateFreq,
			long differentRes, double scoreValue, String measure) {
		super();
		this.term = term;
		this.freqInCorpus = freqInCorpus;
		this.expectedCollocateFreq = expectedCollocateFreq;
		this.observedCollocateFreq = observedCollocateFreq;
		this.differentRes = differentRes;
		this.scoreValue = scoreValue;
		this.measure = measure;
	}

	public long getDifferentRes() {
		return this.differentRes;
	}

	public double getExpectedCollocateFreq() {
		return this.expectedCollocateFreq;
	}

	public long getFreqInCorpus() {
		return this.freqInCorpus;
	}

	public String getMeasure() {
		return this.measure;
	}

	public long getObservedCollocateFreq() {
		return this.observedCollocateFreq;
	}

	public double getScoreValue() {
		return this.scoreValue;
	}

	public String getTerm() {
		return this.term;
	}

	public void setDifferentRes(long differentRes) {
		this.differentRes = differentRes;
	}

	public void setExpectedCollocateFreq(double expectedCollocateFreq) {
		this.expectedCollocateFreq = expectedCollocateFreq;
	}

	public void setFreqInCorpus(long freqInCorpus) {
		this.freqInCorpus = freqInCorpus;
	}

	public void setMeasure(String measure) {
		this.measure = measure;
	}

	public void setObservedCollocateFreq(long observedCollocateFreq) {
		this.observedCollocateFreq = observedCollocateFreq;
	}

	public void setScoreValue(double scoreValue) {
		this.scoreValue = scoreValue;
	}

	public void setTerm(String term) {
		this.term = term;
	}
}
