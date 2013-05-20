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
package it.drwolf.ridire.session;

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.session.async.JobMapperMonitor;
import it.drwolf.ridire.utility.RIDIREPlainTextCleaner;
import it.drwolf.ridire.utility.RIDIREReTagger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

public class JobCleaner {

	private String testAfter;
	private String testBefore;
	private String testOutput;
	private String cleanerPath;
	private String perlUser;
	private String perlPw;
	private String cleanedText;
	private String cleaningScript;
	private RIDIREPlainTextCleaner ridirePlainTextCleaner;
	private RIDIREReTagger ridireReTagger;

	public JobCleaner(String perlUser, String perlPw, String cleanerPath,
			String cleaningScript, String treeTaggerBin) {
		super();
		this.perlUser = perlUser;
		this.perlPw = perlPw;
		this.cleanerPath = cleanerPath;
		this.cleaningScript = cleaningScript;
		this.ridirePlainTextCleaner = new RIDIREPlainTextCleaner(null);
		this.ridireReTagger = new RIDIREReTagger(null);
		this.ridireReTagger.setTreetaggerBin(treeTaggerBin);
		File script;
		try {
			script = File.createTempFile("cleaner", ".pl");
			FileUtils.writeStringToFile(script, this.cleaningScript);
			// transfer script
			JSch jSch = new JSch();
			com.jcraft.jsch.Session session = jSch.getSession(this.perlUser,
					"127.0.0.1");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(this.perlPw);
			session.connect();
			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp c = (ChannelSftp) channel;
			int mode = ChannelSftp.OVERWRITE;
			c.put(script.getAbsolutePath(),
					this.cleanerPath.concat(
							System.getProperty("file.separator")).concat(
							"cleaner.pl"), mode);
			c.disconnect();
			FileUtils.deleteQuietly(script);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JSchException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void clean(List<String> arcFiles, List<String> digests) {
		try {
			List<String> origFiles = new ArrayList<String>();
			List<String> endFiles = new ArrayList<String>();
			for (int i = 0; i < arcFiles.size(); i++) {
				String origFile = FilenameUtils.getFullPath(arcFiles.get(i))
						.concat(JobMapperMonitor.RESOURCESDIR)
						.concat(digests.get(i).concat(".txt"));
				File cleanedFile = new File(origFile + ".2");
				origFiles.add(cleanedFile.getAbsolutePath());
				// duplicate orig file
				String cleanedText = this.ridirePlainTextCleaner
						.getCleanText(new File(origFile));
				FileUtils.writeStringToFile(cleanedFile, cleanedText, "UTF-8");
				String endFile = this.cleanerPath
						.concat(System.getProperty("file.separator"))
						.concat(digests.get(i)).concat(".txt.2");
				endFiles.add(endFile);
			}

			// transfer file to process
			JSch jSch = new JSch();
			com.jcraft.jsch.Session session = jSch.getSession(this.perlUser,
					"127.0.0.1");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(this.perlPw);
			session.connect();
			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp c = (ChannelSftp) channel;
			int mode = ChannelSftp.OVERWRITE;
			for (int i = 0; i < origFiles.size(); i++) {
				c.put(origFiles.get(i), endFiles.get(i), mode);
			}
			c.disconnect();
			String command = null;
			// execute script
			for (String endFile : endFiles) {
				command = "perl "
						+ this.cleanerPath.concat(System
								.getProperty("file.separator")) + "cleaner.pl ";
				channel = session.openChannel("exec");
				ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
				((ChannelExec) channel).setErrStream(errorStream);
				command += endFile;
				((ChannelExec) channel).setCommand(command);
				channel.setInputStream(null);
				InputStream inputStream = channel.getInputStream();
				channel.connect();
				byte[] tmp = new byte[1024];
				while (true) {
					while (inputStream.available() > 0) {
						int i = inputStream.read(tmp, 0, 1024);
						if (i < 0) {
							break;
						}
					}
					if (channel.isClosed()) {
						break;
					}
					try {
						Thread.sleep(200);
					} catch (Exception ee) {
					}
				}
			}
			channel.disconnect();
			// get new files
			channel = session.openChannel("sftp");
			channel.connect();
			c = (ChannelSftp) channel;
			List<File> newFiles = new ArrayList<File>();
			for (String endFile : endFiles) {
				File newFile = File.createTempFile("cleanedFile", null);
				newFiles.add(newFile);
				c.get(endFile + ".tmp", newFile.getAbsolutePath());
			}
			c.disconnect();
			// delete files from working directory
			channel = session.openChannel("exec");
			command = "rm "
					+ this.cleanerPath.concat(System
							.getProperty("file.separator"))
					+ "*.2 "
					+ this.cleanerPath.concat(System
							.getProperty("file.separator")) + "*.2.tmp";
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			channel.connect();
			channel.disconnect();
			session.disconnect();
			for (int i = 0; i < origFiles.size(); i++) {
				File origF = new File(origFiles.get(i));
				FileUtils.deleteQuietly(origF);
				FileUtils.moveFile(newFiles.get(i), origF);
			}
			for (int i = 0; i < origFiles.size(); i++) {
				this.ridireReTagger.retagFile(new File(origFiles.get(i)));
			}
		} catch (JSchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getCleanedText() {
		return this.cleanedText;
	}

	public String getTestAfter() {
		return this.testAfter;
	}

	public String getTestBefore() {
		return this.testBefore;
	}

	public String getTestOutput() {
		return this.testOutput;
	}

	private void setCleanedText(String cleanedText) {
		this.cleanedText = cleanedText;
	}

	public void testScript(CrawledResource cr, String cleaningScript) {
		JSch jSch = new JSch();
		String origFile = FilenameUtils.getFullPath(cr.getArcFile())
				.concat(JobMapperMonitor.RESOURCESDIR)
				.concat(cr.getDigest().concat(".txt"));
		String endFile = this.cleanerPath
				.concat(System.getProperty("file.separator"))
				.concat(cr.getDigest()).concat(".txt");
		String command = "perl "
				+ this.cleanerPath.concat(System.getProperty("file.separator"))
				+ "testscript.pl " + endFile;
		try {
			this.testBefore = this.ridirePlainTextCleaner
					.getCleanText(new File(origFile));
			File script = File.createTempFile("cleaner", ".pl");
			File origTemp = File.createTempFile("orig", ".temp");
			FileUtils.writeStringToFile(origTemp, this.testBefore);
			FileUtils.writeStringToFile(script, cleaningScript);
			// transfer file to process
			com.jcraft.jsch.Session session = jSch.getSession(this.perlUser,
					"127.0.0.1");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setConfig(config);
			session.setPassword(this.perlPw);
			session.connect();
			Channel channel = session.openChannel("sftp");
			channel.connect();
			ChannelSftp c = (ChannelSftp) channel;
			int mode = ChannelSftp.OVERWRITE;
			c.put(origTemp.getAbsolutePath(), endFile, mode);
			// transfer script
			c.put(script.getAbsolutePath(),
					this.cleanerPath.concat(
							System.getProperty("file.separator")).concat(
							"testscript.pl"));
			c.disconnect();
			FileUtils.deleteQuietly(script);
			// execute script
			channel = session.openChannel("exec");
			ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
			((ChannelExec) channel).setErrStream(errorStream);
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			InputStream inputStream = channel.getInputStream();
			channel.connect();
			byte[] tmp = new byte[1024];
			while (true) {
				while (inputStream.available() > 0) {
					int i = inputStream.read(tmp, 0, 1024);
					if (i < 0) {
						break;
					}
				}
				if (channel.isClosed()) {
					break;
				}
				try {
					Thread.sleep(200);
				} catch (Exception ee) {
				}
			}
			this.testOutput = errorStream.toString();
			channel.disconnect();
			// get new file
			channel = session.openChannel("sftp");
			channel.connect();
			c = (ChannelSftp) channel;
			File newFile = File.createTempFile("cleanedFile", null);
			c.get(endFile + ".tmp", newFile.getAbsolutePath());
			c.disconnect();
			// delete files from working directory
			channel = session.openChannel("exec");
			command = "rm "
					+ this.cleanerPath.concat(System
							.getProperty("file.separator")) + "testscript.pl "
					+ endFile + " " + endFile + ".tmp";
			((ChannelExec) channel).setCommand(command);
			channel.setInputStream(null);
			inputStream = channel.getInputStream();
			channel.connect();
			StringBuffer testScriptOutputStringBuffer = new StringBuffer();
			while (true) {
				while (inputStream.available() > 0) {
					int i = inputStream.read(tmp, 0, 1024);
					if (i < 0) {
						break;
					}
					testScriptOutputStringBuffer.append(tmp);
				}
				if (channel.isClosed()) {
					testScriptOutputStringBuffer.append("Exit status: "
							+ channel.getExitStatus());
					break;
				}
				try {
					Thread.sleep(200);
				} catch (Exception ee) {
				}
			}
			channel.disconnect();
			session.disconnect();
			this.testAfter = FileUtils.readFileToString(newFile);
			FileUtils.deleteQuietly(newFile);
		} catch (JSchException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SftpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
