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