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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;

import org.archive.io.GzippedInputStream;
import org.archive.io.RepositionableInputStream;

public class GunzipTest {

	private static final String INPUTFILE = "/home/drwolf/heritrix-2.0.2/jobs/completed-b__5/arcs/IAH-20090714151103-00000-mia.arc.gz";

	public static void main(String[] args) throws FileNotFoundException,
			IOException {
		new GunzipTest();
	}

	public GunzipTest() throws FileNotFoundException, IOException {
		GzippedInputStream gzis = new GzippedInputStream(
				new RepositionableInputStream(new FileInputStream(new File(
						INPUTFILE))));
		Iterator itOnGzis = gzis.iterator();
		int curs = 0;
		while (itOnGzis.hasNext()) {
			InputStream is = (InputStream) itOnGzis.next();
			byte[] buf = new byte[1024];
			int count = 0;
			GZIPOutputStream baos = new GZIPOutputStream(new FileOutputStream(
					new File(
							"/home/drwolf/heritrix-2.0.2/jobs/completed-b__5/arcs/"
									+ curs + ".gz")));
			while ((count = is.read(buf)) != -1) {
				baos.write(buf, 0, count);
			}
			baos.finish();
			baos.close();
			baos.close();
			++curs;
			// is.close();
		}
		gzis.close();
	}
}
