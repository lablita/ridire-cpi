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
package it.drwolf.ridire.entity.comparators;

import it.drwolf.ridire.entity.LocalResource;

import java.util.Comparator;

public class LocalResourceFilenameComparator implements
		Comparator<LocalResource> {

	public int compare(LocalResource arg0, LocalResource arg1) {
		if (arg0 == null || arg0.getOrigFileName() == null) {
			return -1;
		}
		if (arg1 == null || arg1.getOrigFileName() == null) {
			return 1;
		}
		return arg0.getOrigFileName().compareTo(arg1.getOrigFileName());
	}

}
