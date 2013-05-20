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

import it.drwolf.ridire.index.cwb.SketchComparator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class Sketch {

	public static final String ADVERB = "avverbio";
	public static final String ADJECTIVE = "aggettivo";
	public static final String NOUN = "nome";
	public static final String VERB = "verbo";
	private String firstWord;
	private String secondWord;
	private List<GramRel> gramrels = new ArrayList<GramRel>();
	private String name;
	private long firstFreq;
	private long secondFreq;
	private double firstScore;
	private double secondScore;
	private String goodFor;
	private boolean trinary = false;
	private HashMap<String, SketchResult> sketchResults1 = new HashMap<String, SketchResult>();
	private HashMap<String, SketchResult> sketchResults2 = new HashMap<String, SketchResult>();

	public void addGramrel(String rel, boolean inverse, String subquery,
			String origRel) {
		GramRel gramrel = new GramRel(rel, inverse, subquery, origRel);
		this.getGramrels().add(gramrel);
	}

	public long getFirstFreq() {
		return this.firstFreq;
	}

	public double getFirstScore() {
		return this.firstScore;
	}

	public String getFirstWord() {
		return this.firstWord;
	}

	public String getGoodFor() {
		return this.goodFor;
	}

	public List<GramRel> getGramrels() {
		return this.gramrels;
	}

	public String getName() {
		return this.name;
	}

	public List<SketchResult> getOrderedSketchResults() {
		List<SketchResult> ret = new ArrayList<SketchResult>();
		ret.addAll(this.getSketchResults1().values());
		Collections.sort(ret, new SketchComparator());
		return ret.subList(0, Math.min(20, ret.size()));
	}

	public long getSecondFreq() {
		return this.secondFreq;
	}

	public double getSecondScore() {
		return this.secondScore;
	}

	public String getSecondWord() {
		return this.secondWord;
	}

	public HashMap<String, SketchResult> getSketchResults1() {
		return this.sketchResults1;
	}

	public HashMap<String, SketchResult> getSketchResults2() {
		return this.sketchResults2;
	}

	public List<SketchResult> getSketchResultsCoupled() {
		List<SketchResult> ret = new ArrayList<SketchResult>();
		ret.addAll(this.getSketchResults1().values());
		Collections.sort(ret, new CoupledSketchComparator());
		for (SketchResult sketchResult : ret) {
			System.out.println(sketchResult.getCollocata() + "\t"
					+ sketchResult.getScore() + "\t" + sketchResult.getScore());
		}
		return ret;
	}

	public List<SketchResult> getTwoDomainsSketchResults(int domainNumber) {
		List<SketchResult> ret = new ArrayList<SketchResult>();
		if (domainNumber == 1) {
			ret.addAll(this.getSketchResults1().values());
		} else {
			ret.addAll(this.getSketchResults2().values());
		}
		Collections.sort(ret, new SketchComparator());
		for (SketchResult sketchResult : ret) {
			System.out.println(sketchResult.getCollocata() + "\t"
					+ sketchResult.getScore() + "\t" + sketchResult.getScore());
		}
		return ret.subList(0, Math.min(20, ret.size()));
	}

	public boolean isTrinary() {
		return this.trinary;
	}

	public void setFirstFreq(long firstFreq) {
		this.firstFreq = firstFreq;
	}

	public void setFirstScore(double firstScore) {
		this.firstScore = firstScore;
	}

	public void setFirstWord(String firstWord) {
		this.firstWord = firstWord;
	}

	public void setGoodFor(String goodFor) {
		this.goodFor = goodFor;
	}

	public void setGramrels(List<GramRel> gramrels) {
		this.gramrels = gramrels;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setSecondFreq(long secondFreq) {
		this.secondFreq = secondFreq;
	}

	public void setSecondScore(double secondScore) {
		this.secondScore = secondScore;
	}

	public void setSecondWord(String secondWord) {
		this.secondWord = secondWord;
	}

	public void setSketchResults1(HashMap<String, SketchResult> sketchResults1) {
		this.sketchResults1 = sketchResults1;
	}

	public void setSketchResults2(HashMap<String, SketchResult> sketchResults2) {
		this.sketchResults2 = sketchResults2;
	}

	public void setTrinary(boolean trinary) {
		this.trinary = trinary;
	}
}
