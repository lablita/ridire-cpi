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
package it.drwolf.ridire.utility;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;

public class RIDIREPlainTextCleaner {

	private class PlainTextFileFilter implements FileFilter {

		public boolean accept(File f) {
			if (f != null && f.canRead() && f.getAbsolutePath().endsWith("txt")) {
				return true;
			}
			return false;
		}

	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RIDIREPlainTextCleaner(args);

	}

	private Options options;
	private String dirName;
	private boolean onlyDirsWithSpaces;

	public RIDIREPlainTextCleaner(String[] args) {
		if (args != null) {
			this.createOptions();
			this.parseOptions(args);
			File[] files = this.getPlainTextFiles();
			for (File f : files) {
				System.out.print("Cleaning file: " + f.getName() + "...");
				try {
					this.cleanTextFile(f);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(" done.");
			}
		}
	}

	public void cleanTextFile(File f) throws IOException {
		File tmpFile = new File(f.getCanonicalPath() + ".tmp");
		FileUtils.writeStringToFile(tmpFile, this.getCleanText(f));
		FileUtils.deleteQuietly(f);
		FileUtils.moveFile(tmpFile, f);
	}

	private void createOptions() {
		this.options = new Options();
		Option dir = new Option("d", "dir", true, "input directory");
		this.options.addOption(dir);
		Option onlyDirsWithSpaces = new Option("s", "dirWithSpaces", false,
				"Process only dirs with spaces");
		this.options.addOption(onlyDirsWithSpaces);
	}

	public String getCleanText(File f) throws IOException {
		String fileContent = FileUtils.readFileToString(f, "UTF-8");
		fileContent = this.substitute(fileContent, "â€œ", "\"");
		fileContent = this.substitute(fileContent, "â€�", "\"");
		fileContent = this.substitute(fileContent, "â€™", "\"");
		fileContent = this.substitute(fileContent, "â\\u0080\\u0099", "'");
		fileContent = this.substitute(fileContent, "â\\u0080\\u009c", "\"");
		fileContent = this.substitute(fileContent, "â\\u0080\\u009d", "\"");
		fileContent = this.substitute(fileContent, "â\\u0080\\u0093", "-");
		fileContent = this.substitute(fileContent, "Â\\u0092", "'");
		fileContent = this.substitute(fileContent, "Â\\u0093", "'");
		fileContent = this.substitute(fileContent, "Â«", "«");
		fileContent = this.substitute(fileContent, "Â»", "»");
		fileContent = this.substitute(fileContent, "Ã¹", "ù");
		fileContent = this.substitute(fileContent, "Ã ", "à");
		fileContent = this.substitute(fileContent, "Ã¨", "è");
		fileContent = this.substitute(fileContent, "Ã©", "é");
		fileContent = this.substitute(fileContent, "Ã¬", "ì");
		fileContent = this.substitute(fileContent, "Ã²", "ò");
		fileContent = this.substitute(fileContent, "Ãˆ", "È");
		fileContent = this.substitute(fileContent, "Ã", "È");
		fileContent = this.substitute(fileContent, "Â", "...");
		fileContent = this
				.substitute(fileContent, "Follow us on Twitter »", "");
		return fileContent;
	}

	private File[] getPlainTextFiles() {
		if (this.onlyDirsWithSpaces && !this.dirName.contains(" ")) {
			return new File[0];
		}
		File dir = new File(this.dirName);
		FileFilter filter = new PlainTextFileFilter();
		if (dir != null && dir.isDirectory()) {
			return dir.listFiles(filter);
		} else {
			return null;
		}
	}

	private void parseOptions(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new GnuParser();
		CommandLine cmdline = null;
		try {
			// parse the command line arguments
			cmdline = parser.parse(this.options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp("RIDIREPlainTextCleaner", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.dirName = cmdline.getOptionValue("d");
			if (this.dirName == null) {
				System.err.println("No directory provided.");
				formatter.printHelp("RIDIREPlainTextCleaner", this.options);
				System.exit(-1);
			}
			this.onlyDirsWithSpaces = cmdline.hasOption("s");
		}
	}

	private String substitute(String original, String regex, String subst) {
		Pattern p = Pattern.compile(regex, Pattern.MULTILINE);
		Matcher m = p.matcher(original);
		return m.replaceAll(subst);
	}

}
