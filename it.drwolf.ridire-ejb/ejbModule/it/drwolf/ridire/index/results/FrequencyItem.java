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

public class FrequencyItem {
	private String formaPosLemma;
	private String pos;
	private int frequency;

	public FrequencyItem(String formaPosLemma, int frequency) {
		super();
		this.formaPosLemma = formaPosLemma;
		this.frequency = frequency;
	}

	public FrequencyItem(String formaPosLemma, String pos, int frequency) {
		super();
		this.formaPosLemma = formaPosLemma;
		this.pos = pos;
		this.frequency = frequency;
	}

	public String getFormaPosLemma() {
		return this.formaPosLemma;
	}

	public int getFrequency() {
		return this.frequency;
	}

	public String getPos() {
		return this.pos;
	}

	public void setFormaPosLemma(String formaPosLemma) {
		this.formaPosLemma = formaPosLemma;
	}

	public void setFrequency(int frequency) {
		this.frequency = frequency;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

}
