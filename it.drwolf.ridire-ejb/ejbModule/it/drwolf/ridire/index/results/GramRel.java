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

public class GramRel {
	private String rel;
	private String origRel;
	private String subquery;
	private boolean inverse = false;

	public GramRel(String rel, boolean inverse, String subquery, String origRel) {
		this.rel = rel;
		this.inverse = inverse;
		this.subquery = subquery;
		this.origRel = origRel;
	}

	public String getOrigRel() {
		return this.origRel;
	}

	public String getRel() {
		return this.rel;
	}

	public String getSubquery() {
		return this.subquery;
	}

	public boolean isInverse() {
		return this.inverse;
	}

	public void setInverse(boolean inverse) {
		this.inverse = inverse;
	}

	public void setOrigRel(String origRel) {
		this.origRel = origRel;
	}

	public void setRel(String rel) {
		this.rel = rel;
	}

	public void setSubquery(String subquery) {
		this.subquery = subquery;
	}
}
