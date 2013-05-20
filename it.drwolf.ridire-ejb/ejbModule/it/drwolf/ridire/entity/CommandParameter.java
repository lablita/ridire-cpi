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
package it.drwolf.ridire.entity;

import java.io.Serializable;

import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class CommandParameter implements Serializable {

	public static final String HERITRIX_LAUNCH_KEY = "heritrix.launch";
	public static final String HERITRIX_BINDIR_KEY = "heritrix.bindir";
	public static final String HERITRIX_DIR_KEY = "heritrix.dir";
	public static final String HERITRIX_ADMINWUIPW_KEY = "heritrix.adminwuipw";
	public static final String HERITRIX_ADMINPW_KEY = "heritrix.adminwpw";
	public static final String HERITRIX_WEBUIPORT_KEY = "heritrix.webuiport";
	public static final String HERITRIX_JMXURL_KEY = "heritrix.jmxurl";
	public static final String PDFTOHTML_EXECUTABLE_KEY = "pdftohtml.bin";
	public static final String TREETAGGER_EXECUTABLE_KEY = "treetagger.bin";
	public static final String TREETAGGER_EXECUTABLE_UTF8_KEY = "treetaggerutf8.bin";
	public static final String TREETAGGER_PARAMETER_FILE_KEY = "treetagger.parfile";
	public static final String RIDIRE_CLEANER_EXECUTABLE_KEY = "ridirecleaner.jar";
	public static final String PDFCLEANER_EXECUTABLE_KEY = "pdfcleaner.jar";
	/**
	 * 
	 */
	private static final long serialVersionUID = -4257159499398423767L;
	private String commandName;
	private String commandValue;

	private final static CommandParameter HERITRIX_DIR = new CommandParameter(
			CommandParameter.HERITRIX_DIR_KEY, "/home/drwolf/heritrix-2.0.2");
	private final static CommandParameter HERITRIX_BINDIR = new CommandParameter(
			CommandParameter.HERITRIX_BINDIR_KEY, "bin");
	private final static CommandParameter HERITRIX_LAUNCH_COMMAND = new CommandParameter(
			CommandParameter.HERITRIX_LAUNCH_KEY, "heritrix");
	private final static CommandParameter HERITRIX_ADMINWUIPW_COMMAND = new CommandParameter(
			CommandParameter.HERITRIX_ADMINWUIPW_KEY, "changeme");
	private final static CommandParameter HERITRIX_ADMINPW_COMMAND = new CommandParameter(
			CommandParameter.HERITRIX_ADMINPW_KEY, "admin");
	private final static CommandParameter HERITRIX_WEBUIPORT_COMMAND = new CommandParameter(
			CommandParameter.HERITRIX_WEBUIPORT_KEY, "8999");
	private final static CommandParameter HERITRIX_JMXURL = new CommandParameter(
			CommandParameter.HERITRIX_JMXURL_KEY,
			"service:jmx:rmi://localhost:8849/jndi/rmi://localhost:8849/jmxrmi");
	private static final CommandParameter PDFTOHTML_EXECUTABLE = new CommandParameter(
			CommandParameter.PDFTOHTML_EXECUTABLE_KEY, "pdftohtml");
	private static final CommandParameter TREETAGGER_EXECUTABLE = new CommandParameter(
			CommandParameter.TREETAGGER_EXECUTABLE_KEY,
			"/home/drwolf/tree-tagger/cmd/tree-tagger-italian");
	private static final CommandParameter TREETAGGER_EXECUTABLE_UTF8 = new CommandParameter(
			CommandParameter.TREETAGGER_EXECUTABLE_UTF8_KEY,
			"/home/drwolf/tree-tagger/cmd/tree-tagger-italian-utf8");
	private static final CommandParameter TREETAGGER_PARAMETER_FILE = new CommandParameter(
			CommandParameter.TREETAGGER_PARAMETER_FILE_KEY,
			"/home/drwolf/tree-tagger/lib/italian-utf8.par");
	private static final CommandParameter RIDIRE_CLEANER_EXECUTABLE = new CommandParameter(
			CommandParameter.RIDIRE_CLEANER_EXECUTABLE_KEY,
			"/home/drwolf/ridirecleaner/ridirecleaner.jar");
	private static final CommandParameter PDFCLEANER_EXECUTABLE = new CommandParameter(
			CommandParameter.PDFCLEANER_EXECUTABLE_KEY,
			"/home/drwolf/ridirecleaner/PdfCleaner.jar");
	public static final CommandParameter[] defaults = new CommandParameter[] {
			CommandParameter.HERITRIX_DIR, CommandParameter.HERITRIX_BINDIR,
			CommandParameter.HERITRIX_LAUNCH_COMMAND,
			CommandParameter.HERITRIX_ADMINPW_COMMAND,
			CommandParameter.HERITRIX_ADMINWUIPW_COMMAND,
			CommandParameter.HERITRIX_WEBUIPORT_COMMAND,
			CommandParameter.HERITRIX_JMXURL,
			CommandParameter.PDFTOHTML_EXECUTABLE,
			CommandParameter.TREETAGGER_EXECUTABLE,
			CommandParameter.TREETAGGER_EXECUTABLE_UTF8,
			CommandParameter.TREETAGGER_PARAMETER_FILE,
			CommandParameter.RIDIRE_CLEANER_EXECUTABLE,
			CommandParameter.PDFCLEANER_EXECUTABLE };

	public CommandParameter() {
	}

	public CommandParameter(String commandName, String commandValue) {
		this.commandName = commandName;
		this.commandValue = commandValue;
	}

	@Id
	public String getCommandName() {
		return this.commandName;
	}

	public String getCommandValue() {
		return this.commandValue;
	}

	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}

	public void setCommandValue(String commandValue) {
		this.commandValue = commandValue;
	}
}
