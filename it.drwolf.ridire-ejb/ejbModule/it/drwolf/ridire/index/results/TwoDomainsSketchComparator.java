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

import java.util.Comparator;

public class TwoDomainsSketchComparator implements Comparator<SketchResult> {

	public int compare(SketchResult o1, SketchResult o2) {
		if (o1 == null) {
			return -1;
		}
		if (o2 == null) {
			return 1;
		}
		double o1score = o1.getScore() - o1.getScore();
		double o2score = o2.getScore() - o2.getScore();
		if (o1score < o2score) {
			return -1;
		}
		if (o1score > o2score) {
			return 1;
		}
		return 0;
	}

}
