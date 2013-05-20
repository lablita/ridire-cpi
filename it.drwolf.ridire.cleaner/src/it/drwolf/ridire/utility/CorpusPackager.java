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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.IOException;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.apache.commons.lang.text.StrTokenizer;

public class CorpusPackager {
	public static void main(String[] args) {
		new CorpusPackager(args);
	}

	private Options options;
	private String dirName;
	private String descFile;
	StrTokenizer strTokenizer = new StrTokenizer();
	private Integer minValue;
	private Integer maxValue;

	public CorpusPackager(String[] args) {
		this.createOptions();
		this.parseOptions(args);
		try {
			BufferedReader bufferedReader = new BufferedReader(new FileReader(
					this.descFile));
			String fileLine = null;
			while ((fileLine = bufferedReader.readLine()) != null) {
				this.processLine(fileLine);
			}
			bufferedReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void createOptions() {
		this.options = new Options();
		Option f = new Option("f", "descfile", true, "description file");
		this.options.addOption(f);
		Option dir = new Option("d", "destDir", true, "destination dir");
		this.options.addOption(dir);
		Option min = new Option("m", "min", true, "min value");
		this.options.addOption(min);
		Option max = new Option("M", "max", true, "max value");
		this.options.addOption(max);
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
			formatter.printHelp("CorpusPackager", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.dirName = cmdline.getOptionValue("d");
			if (this.dirName == null) {
				System.err.println("No directory provided.");
				formatter.printHelp("CorpusPackager", this.options);
				System.exit(-1);
			}
			this.descFile = cmdline.getOptionValue("f");
			if (this.descFile == null) {
				System.err.println("No description file provided.");
				formatter.printHelp("CorpusPackager", this.options);
				System.exit(-1);
			}
			String min = cmdline.getOptionValue("m");
			if (min == null) {
				System.err.println("No min value provided.");
				formatter.printHelp("CorpusPackager", this.options);
				System.exit(-1);
			}
			this.minValue = Integer.parseInt(min);
			String max = cmdline.getOptionValue("M");
			if (max == null) {
				System.err.println("No max value provided.");
				formatter.printHelp("CorpusPackager", this.options);
				System.exit(-1);
			}
			this.maxValue = Integer.parseInt(max);
		}
	}

	private void processLine(String fileLine) throws IOException {
		this.strTokenizer.setIgnoreEmptyTokens(false);
		this.strTokenizer.reset(fileLine);
		String[] tokens = this.strTokenizer.getTokenArray();
		if (tokens.length == 5) {
			Integer words = Integer.parseInt(tokens[0].trim());
			String jobName = tokens[1].trim();
			FileFilter fileFilter = new RegexFileFilter(jobName.replaceAll("_",
					"[_ ]"));
			String jobsDir = "/home/drwolf/heritrix-3.1.1-SNAPSHOT/jobs/";
			File[] jobsDirs = new File(jobsDir).listFiles(fileFilter);
			File jobDirFile = null;
			if (jobsDirs.length > 0) {
				jobDirFile = jobsDirs[0];
			} else {
				System.out.println(jobName + "\tnot found.");
				return;
			}
			String fileDigest = tokens[2].trim();
			if (words < this.minValue || words >= this.maxValue) {
				System.out.println(jobName + "\t" + fileDigest
						+ "\t skipped: out of range.");
				return;
			}
			String functional = tokens[3].trim();
			String semantic = tokens[4].trim();
			String dirname = functional.trim();
			if (dirname.trim().length() < 1) {
				dirname = semantic.trim();
			}
			if (dirname.trim().length() < 1) {
				dirname = "other";
			}
			File destDir = new File(this.dirName
					+ System.getProperty("file.separator") + dirname);
			File f = new File(jobDirFile.getAbsolutePath() + "/arcs/resources/"
					+ fileDigest + ".txt");
			if (!f.exists() || !f.canRead()) {
				f = new File(
						"/home/drwolf/heritrix-3.1.1-SNAPSHOT/jobs/completed-"
								+ jobName + "/arcs/resources/" + fileDigest
								+ ".txt");
				if (!f.exists() || !f.canRead()) {
					System.out.println(jobName + "\t" + fileDigest
							+ "\tnot found.");
					return;
				}
			}
			FileUtils.copyFileToDirectory(f, destDir);
		}
	}
}
