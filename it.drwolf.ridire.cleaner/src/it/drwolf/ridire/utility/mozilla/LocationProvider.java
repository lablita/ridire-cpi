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
package it.drwolf.ridire.utility.mozilla;

import java.io.File;

import org.mozilla.xpcom.IAppFileLocProvider;

public class LocationProvider implements IAppFileLocProvider {

	private final File libXULPath;

	public LocationProvider(File grePath) {
		this.libXULPath = grePath;
	}

	public File getFile(String aProp, boolean[] aPersistent) {
		File file = null;
		if (aProp.equals("GreD") || aProp.equals("GreComsD")) {
			file = this.libXULPath;
			if (aProp.equals("GreComsD")) {
				file = new File(file, "components");
			}
		} else if (aProp.equals("MozBinD") || aProp.equals("CurProcD")
				|| aProp.equals("ComsD") || aProp.equals("ProfD")) {
			file = this.libXULPath;
			if (aProp.equals("ComsD")) {
				file = new File(file, "components");
			}
		}
		return file;
	}

	public File[] getFiles(String aProp) {
		File[] files = null;
		if (aProp.equals("APluginsDL")) {
			files = new File[1];
			files[0] = new File(this.libXULPath, "plugins");
		}
		return files;
	}

}
