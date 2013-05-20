package it.drwolf.ridire.utility;

import it.drwolf.ridire.utility.mozilla.LocationProvider;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.mozilla.xpcom.GREVersionRange;
import org.mozilla.xpcom.Mozilla;
import org.mozilla.xpcom.XPCOMException;

public class MozillaReadabilityCleaner {
	public static void main(String args[]) {
		new MozillaReadabilityCleaner(args);
	}

	private Options options;
	private String fileName;
	private String bookmark;
	private String host;
	private String encoding;

	public MozillaReadabilityCleaner(String[] args) {
		Mozilla mozilla = Mozilla.getInstance();
		GREVersionRange[] range = new GREVersionRange[1];
		range[0] = new GREVersionRange("1.9.0", true, "2.0", true);
		try {
			File grePath = Mozilla.getGREPathWithProperties(range, null);
			LocationProvider locProvider = new LocationProvider(grePath);
			mozilla.initialize(grePath);
			mozilla.initEmbedding(grePath, grePath, locProvider);
		} catch (FileNotFoundException e) {
			// this exception is thrown if greGREPathWithProperties cannot find
			// a GRE
			e.printStackTrace();
		} catch (XPCOMException e) {
			// this exception is thrown if initEmbedding failed
			e.printStackTrace();
		}
		this.createOptions();
		this.parseOptions(args);
		try {
			mozilla.termEmbedding();
		} catch (XPCOMException e) {
			// this exception is thrown if termEmbedding failed
		}
	}

	private void createOptions() {
		this.options = new Options();
		Option file = new Option("f", "file", true, "input file");
		this.options.addOption(file);
		Option bookmark = new Option("b", "bookmark", true,
				"bookmarks location");
		this.options.addOption(bookmark);
		Option host = new Option("h", "host", true, "host");
		this.options.addOption(host);
		Option encoding = new Option("e", "encoding", true, "encoding");
		this.options.addOption(encoding);
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
			formatter.printHelp("MozillaReadabilityCleaner", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.fileName = cmdline.getOptionValue("f");
			if (this.fileName == null) {
				System.err.println("No file provided.");
				formatter.printHelp("MozillaReadabilityCleaner", this.options);
				System.exit(-1);
			}
			this.bookmark = cmdline.getOptionValue("b");
			if (this.bookmark == null) {
				System.err.println("No bookmark script.");
				formatter.printHelp("MozillaReadabilityCleaner", this.options);
				System.exit(-1);
			}
			this.host = cmdline.getOptionValue("h");
			if (this.host == null) {
				System.err.println("No host.");
				formatter.printHelp("MozillaReadabilityCleaner", this.options);
				System.exit(-1);
			}
			this.encoding = cmdline.getOptionValue("e");
			if (this.encoding == null) {
				System.err.println("No encoding.");
				formatter.printHelp("MozillaReadabilityCleaner", this.options);
				System.exit(-1);
			}
		}
	}
}
