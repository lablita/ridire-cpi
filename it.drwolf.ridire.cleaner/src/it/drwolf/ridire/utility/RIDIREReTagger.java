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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.LogOutputStream;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;

public class RIDIREReTagger {

	private class PlainTextFileFilter implements FileFilter {

		public boolean accept(File f) {
			if (f != null && f.canRead() && f.getAbsolutePath().endsWith("txt")) {
				return true;
			}
			return false;
		}

	}

	private class TreeTaggerLog extends LogOutputStream {
		private List<String> lines = new ArrayList<String>();

		public List<String> getLines() {
			return this.lines;
		}

		@Override
		protected void processLine(String line, int arg1) {
			if (line != null) {
				// System.out.println(line);
				this.getLines().add(line);
			}
		}

	}

	private static final long TREETAGGER_TIMEOUT = 240000; // 4 mins

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new RIDIREReTagger(args);

	}

	private Options options;

	private String dirName;
	private String treeTaggerBin;

	private boolean onlyDirsWithSpaces;

	public RIDIREReTagger(String[] args) {
		if (args != null) {
			this.createOptions();
			this.parseOptions(args);
			File[] files = this.getPlainTextFiles();
			for (File f : files) {
				System.out.print("Retagging file: " + f.getName() + "...");
				try {
					this.retagFile(f);
				} catch (ExecuteException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				System.out.println(" done.");
			}
		}
	}

	private void createOptions() {
		this.options = new Options();
		Option dir = new Option("d", "dir", true, "input directory");
		this.options.addOption(dir);
		Option treeTaggerBin = new Option("b", "treeTaggerBin", true,
				"TreeTaggerBin");
		this.options.addOption(treeTaggerBin);
		Option onlyDirsWithSpaces = new Option("s", "dirWithSpaces", false,
				"Process only dirs with spaces");
		this.options.addOption(onlyDirsWithSpaces);
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
		org.apache.commons.cli.CommandLine cmdline = null;
		try {
			// parse the command line arguments
			cmdline = parser.parse(this.options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp("RIDIREReTagger", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.dirName = cmdline.getOptionValue("d");
			if (this.dirName == null) {
				System.err.println("No directory provided.");
				formatter.printHelp("RIDIREReTagger", this.options);
				System.exit(-1);
			}
			this.treeTaggerBin = cmdline.getOptionValue("b");
			if (this.treeTaggerBin == null) {
				System.err.println("No executable provided.");
				formatter.printHelp("RIDIREReTagger", this.options);
				System.exit(-1);
			}
			this.onlyDirsWithSpaces = cmdline.hasOption("s");
		}
	}

	public String retagFile(File f) throws ExecuteException, IOException {
		// Map<String, File> map = new HashMap<String, File>();
		String fileIN = f.getAbsolutePath();
		String fileOut = f.getAbsolutePath() + ".iso";
		String posOld = f.getAbsolutePath() + ".iso.pos";
		String posNew = f.getAbsolutePath() + ".pos";
		// first convert from utf8 to iso8859-1
		CommandLine commandLine = CommandLine.parse("iconv");
		commandLine.addArgument("-c").addArgument("-s").addArgument("-f")
				.addArgument("utf8").addArgument("-t").addArgument(
						"iso8859-1//TRANSLIT").addArgument("-o").addArgument(
						fileOut, false).addArgument(fileIN, false);
		DefaultExecutor executor = new DefaultExecutor();
		ExecuteWatchdog watchdog = new ExecuteWatchdog(
				RIDIREReTagger.TREETAGGER_TIMEOUT);
		executor.setWatchdog(watchdog);
		int exitValue = executor.execute(commandLine);
		if (exitValue == 0) {
			// tag using latin1 and Baroni's tagset
			commandLine = CommandLine.parse(this.treeTaggerBin);
			commandLine.addArgument(fileOut, false);
			executor = new DefaultExecutor();
			executor.setExitValue(0);
			watchdog = new ExecuteWatchdog(RIDIREReTagger.TREETAGGER_TIMEOUT);
			executor.setWatchdog(watchdog);
			TreeTaggerLog treeTaggerLog = new TreeTaggerLog();
			PumpStreamHandler executeStreamHandler = new PumpStreamHandler(
					treeTaggerLog, null);
			executor.setStreamHandler(executeStreamHandler);
			int exitValue2 = executor.execute(commandLine);
			if (exitValue2 == 0) {
				// FileUtils.deleteQuietly(new File(fileOut));
				File posTagFile = new File(posOld);
				FileUtils.writeLines(posTagFile, treeTaggerLog.getLines());
			}
			// reconvert to utf8
			commandLine = CommandLine.parse("iconv");
			commandLine.addArgument("-s").addArgument("-f").addArgument(
					"iso8859-1").addArgument("-t")
					.addArgument("utf8//TRANSLIT").addArgument("-o")
					.addArgument(posNew, false).addArgument(posOld, false);
			executor = new DefaultExecutor();
			watchdog = new ExecuteWatchdog(RIDIREReTagger.TREETAGGER_TIMEOUT);
			executor.setWatchdog(watchdog);
			int exitValue3 = executor.execute(commandLine);
			if (exitValue3 == 0) {
				// FileUtils.deleteQuietly(new File(f.getPath() + ".iso.pos"));
				return new File(posNew).getCanonicalPath();
			}
		}
		return null;
	}

	public void setTreetaggerBin(String treeTaggerBin) {
		this.treeTaggerBin = treeTaggerBin;
	}
}
