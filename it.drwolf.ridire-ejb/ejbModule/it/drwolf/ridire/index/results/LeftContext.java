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

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;

public class LeftContext {
	private String context;

	private StrTokenizer strTokenizer = new StrTokenizer();

	public LeftContext(String leftContext) {
		this.context = leftContext;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LeftContext)) {
			return false;
		}
		LeftContext other = (LeftContext) obj;
		if (this.context == null) {
			if (other.context != null) {
				return false;
			}
		} else if (!this.context.equals(other.context)) {
			return false;
		}
		return true;
	}

	public String getContext() {
		return this.context;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.context == null ? 0 : this.context.hashCode());
		return result;
	}

	public void setContext(String context) {
		this.context = context;
	}

	@Override
	public String toString() {
		// return super.toString();
		List<String> l1 = this.strTokenizer.reset(this.context).getTokenList();
		Collections.reverse(l1);
		return StringUtils.join(l1, " ").replaceAll("_", " ");
	}
}
