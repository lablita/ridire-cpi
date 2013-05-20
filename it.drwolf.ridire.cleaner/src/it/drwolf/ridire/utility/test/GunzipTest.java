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
