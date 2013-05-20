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
package it.drwolf.ridire.utility.test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;

public class NewsCleaner {
	class FileNameComparator implements Comparator<File> {

		public int compare(File o1, File o2) {
			return o1.getName().compareTo(o2.getName());
		}

	}

	private static final String DIR1 = "/home/drwolf/tmp/prove_ncleaner/train/big_test/html_1";

	private static final String DIR2 = "/home/drwolf/tmp/prove_ncleaner/train/big_test/clean_1";

	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		new NewsCleaner();
	}

	public NewsCleaner() throws IOException {
		// this.removeDirtyHtml();
		File dir2 = new File(DIR2);
		List<File> dir2files = new ArrayList(FileUtils.listFiles(dir2, null,
				false));
		for (File f : dir2files) {
			String content = FileUtils.readFileToString(f);
			content = StringEscapeUtils.unescapeHtml(content);
			content = content.replaceAll("\\\\\n", " ");
			File nf = new File(f.getAbsolutePath().substring(0,
					f.getAbsolutePath().length() - 5));
			FileUtils.writeStringToFile(nf, content, "UTF-8");
		}
	}

	private void removeDirtyHtml() {
		File dir1 = new File(DIR1);
		File dir2 = new File(DIR2);
		List dir1files = new ArrayList(FileUtils.listFiles(dir1, null, false));
		List dir2files = new ArrayList(FileUtils.listFiles(dir2, null, false));
		Collections.sort(dir1files, new FileNameComparator());
		Collections.sort(dir2files, new FileNameComparator());
		for (int i = 0, j = 0; i < dir1files.size(); i++) {
			File f1 = (File) dir1files.get(i);
			File f2 = (File) dir2files.get(j);
			if (f1.getName().equals(f2.getName())) {
				j++;
				continue;
			} else {
				FileUtils.deleteQuietly(f1);
			}
		}
	}
}
