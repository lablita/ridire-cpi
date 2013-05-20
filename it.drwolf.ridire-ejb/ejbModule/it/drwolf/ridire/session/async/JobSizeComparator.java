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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.entity.Job;

import java.io.File;
import java.util.Comparator;

public class JobSizeComparator implements Comparator<Job> {

	public int compare(Job j1, Job j2) {
		if (j1 == null || j1.getId() == null) {
			return 1;
		}
		if (j2 == null || j2.getId() == null) {
			return -1;
		}
		if (this.getJobApproxSize(j1) < this.getJobApproxSize(j2)) {
			return -1;
		}
		return 1;
	}

	public long getJobApproxSize(Job persistedJob) {
		String dir = JobMapperMonitor.JOBSDIR;
		File filename = null;
		if (persistedJob.getChildJobName() != null
				&& !persistedJob.isMappedResources()) {
			filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
					+ persistedJob.getChildJobName()
					+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
					+ JobMapperMonitor.FILE_SEPARATOR);
		} else {
			filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
					+ persistedJob.getName() + JobMapperMonitor.FILE_SEPARATOR
					+ "arcs" + JobMapperMonitor.FILE_SEPARATOR);
		}
		File[] arcFiles = filename.listFiles();
		if (arcFiles == null) {
			// try also 'completed-' for back compatibility
			filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
					+ "completed-" + persistedJob.getName()
					+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
					+ JobMapperMonitor.FILE_SEPARATOR);
			arcFiles = filename.listFiles();
			if (arcFiles == null) {
				return 0L;
			}
		}
		long sum = 0L;
		for (File f : arcFiles) {
			sum += f.length();
		}
		return sum;
	}
}
