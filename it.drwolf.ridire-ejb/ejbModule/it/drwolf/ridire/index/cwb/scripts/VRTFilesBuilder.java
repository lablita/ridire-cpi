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
package it.drwolf.ridire.index.cwb.scripts;

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.session.async.JobMapperMonitor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.log.Log;
import org.jboss.seam.transaction.UserTransaction;

@Name("vrtFilesBuilder")
public class VRTFilesBuilder {

	private static final int MAXRESULTS = 100;
	private EntityManager entityManager;
	private UserTransaction userTx;

	Map<String, String> easyPos = new HashMap<String, String>() {
		{
			this.put("ARTPRE", "PREP");
			this.put("AUX:fin", "VERB");
			this.put("AUX:fin:cli", "VERB");
			this.put("AUX:geru", "VERB");
			this.put("AUX:geru:cli", "VERB");
			this.put("AUX:infi", "VERB");
			this.put("AUX:infi:cli", "VERB");
			this.put("AUX:ppast", "VERB");
			this.put("AUX:ppre", "VERB");
			this.put("DET:demo", "ADJPRO");
			this.put("DET:indef", "ADJPRO");
			this.put("DET:num", "ADJPRO");
			this.put("DET:poss", "ADJPRO");
			this.put("DET:wh", "ADJPRO");
			this.put("PRE", "PREP");
			this.put("PRO:demo", "PRON");
			this.put("PRO:indef", "PRON");
			this.put("PRO:num", "PRON");
			this.put("PRO:pers", "PRON");
			this.put("PRO:poss", "PRON");
			this.put("VER2:fin", "VERB");
			this.put("VER2:fin:cli", "VERB");
			this.put("VER2:geru", "VERB");
			this.put("VER2:geru:cli", "VERB");
			this.put("VER2:infi", "VERB");
			this.put("VER2:infi:cli", "VERB");
			this.put("VER2:ppast", "VERB");
			this.put("VER2:ppre", "VERB");
			this.put("VER:fin", "VERB");
			this.put("VER:fin:cli", "VERB");
			this.put("VER:geru", "VERB");
			this.put("VER:geru:cli", "VERB");
			this.put("VER:infi", "VERB");
			this.put("VER:infi:cli", "VERB");
			this.put("VER:ppast", "VERB");
			this.put("VER:ppast:cli", "VERB");
			this.put("VER:ppre", "VERB");
		}
	};

	@Logger
	private Log log;

	@Asynchronous
	public void buildFiles(VRTFilesBuilderData vrtFilesBuilderData) {
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(1000 * 10 * 60);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<Integer> validatedJobsId = this.entityManager
					.createQuery(
							"select j.id from Job j where j.validationStatus=:validated")
					.setParameter("validated", Job.VALIDATED_OK)
					.getResultList();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			int countJob = 1;
			for (Integer jobId : validatedJobsId) {
				System.out.println("Creating VRT files; job " + countJob
						+ " of " + validatedJobsId.size());
				++countJob;
				this.processResourcesOfJob(jobId, vrtFilesBuilderData);
			}
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
				}
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Asynchronous
	public void buildFilesFromFiles(VRTFilesBuilderData vrtFilesBuilderData) {
		String origDir = vrtFilesBuilderData.getOrigDir();
		Collection<File> files = FileUtils.listFiles(new File(origDir),
				new String[] { "pos" }, true);
		System.out.println("Files to be processed: " + files.size());
		File destDir = new File(vrtFilesBuilderData.getDestDir());
		destDir.mkdir();
		int i = 0;
		for (File f : files) {
			++i;
			List<String> lines = null;
			try {
				lines = FileUtils.readLines(f);
				List<String> newLines = new ArrayList<String>();
				for (String l : lines) {
					newLines.add(l.replaceAll(":", ""));
				}
				String header = this.getHeaderFromFile(i, f);
				newLines.add(0, header);
				newLines.add("</text>");
				File vrtFile = new File(destDir, FilenameUtils.getBaseName(f
						.getName())
						+ ".vrt");
				FileUtils.writeLines(vrtFile, newLines);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (i % 100 == 0) {
				System.out.println("Processed files: " + i);
			}
		}
	}

	@Asynchronous
	public void buildFilesFromJobNames(VRTFilesBuilderData vrtFilesBuilderData) {
		String[] jobNames = StringUtils.split(
				vrtFilesBuilderData.getJobsList(), "\n");
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(1000 * 10 * 60);
			List<Integer> jobIds = new ArrayList<Integer>();
			int countJob = 1;
			for (String jobName : jobNames) {
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				this.entityManager.joinTransaction();
				jobName = jobName.replaceAll("completed-", "");
				jobIds = this.entityManager.createQuery(
						"select j.id from Job j where j.name=:name ")
						.setParameter("name", jobName.trim()).getResultList();
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
				if (jobIds.size() == 1) {
					System.out.println("Creating VRT files; job " + countJob
							+ " of " + jobNames.length);
					++countJob;
					this.processResourcesOfJob(jobIds.get(0),
							vrtFilesBuilderData);
				}
			}
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
				}
			} catch (IllegalStateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SecurityException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (SystemException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	private String getEasyPos(String pos) {
		if (this.easyPos.containsKey(pos)) {
			return this.easyPos.get(pos);
		} else {
			return pos;
		}
	}

	private String getHeaderFromFile(int i, File f) {
		String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n";
		header += "<text id=\""
				+ FilenameUtils.getBaseName(FilenameUtils.getBaseName(f
						.getAbsolutePath())) + "\" functional=\"";
		if (i % 5 == 0) {
			header += "Informazione\"";
		} else if (i % 4 == 0) {
			header += "Amministrazione_e_Legislazione\"";
		} else {
			header += "Economia_e_Affari\"";
		}
		header += " semantic=\"";
		if (i % 5 == 0) {
			header += "Cinema\"";
		} else if (i % 4 == 0) {
			header += "Moda\"";
		} else {
			header += "Religione\"";
		}
		String jobname = FilenameUtils.getFullPathNoEndSeparator(
				f.getAbsolutePath()).substring(
				FilenameUtils.getFullPathNoEndSeparator(f.getAbsolutePath())
						.lastIndexOf(System.getProperty("file.separator")) + 1);
		header += " jobname=\"" + jobname.replaceAll("\\s", "_") + "\">";
		return header;
	}

	private String getHeaderFromResource(String jobName,
			String functionalMetadatum, String semanticMetadatum, String url,
			File f) {
		String header = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\" ?>\n";
		header += "<text id=\""
				+ FilenameUtils.getBaseName(FilenameUtils.getBaseName(f
						.getAbsolutePath())) + "\"";
		header += " url=\"" + StringEscapeUtils.escapeXml(url) + "\"";
		header += " functional=\"" + functionalMetadatum.replaceAll("\\s", "_")
				+ "\"";
		header += " semantic=\"" + semanticMetadatum.replaceAll("\\s", "_")
				+ "\"";
		;
		header += " jobname=\"" + jobName.replaceAll("\\s", "_") + "\">";
		return header;
	}

	private boolean haveStrangeChars(List<String> posFileLines) {
		for (String posFileLine : posFileLines) {
			if (posFileLine.contains("�") || posFileLine.contains("â€œ")
					|| posFileLine.contains("â€�")
					|| posFileLine.contains("â€™")
					|| posFileLine.contains("â\\u0080\\u0099")
					|| posFileLine.contains("â\\u0080\\u009c")
					|| posFileLine.contains("â\\u0080\\u009d")
					|| posFileLine.contains("â\\u0080\\u0093")
					|| posFileLine.contains("Â\\u0092")
					|| posFileLine.contains("Â\\u0093")
					|| posFileLine.contains("Â«") || posFileLine.contains("Â»")
					|| posFileLine.contains("Ã¹") || posFileLine.contains("Ã ")
					|| posFileLine.contains("Ã¨") || posFileLine.contains("Ã©")
					|| posFileLine.contains("Ã¬") || posFileLine.contains("Ã²")
					|| posFileLine.contains("Ãˆ") || posFileLine.contains("Ã")
					|| posFileLine.contains("Â")
					|| posFileLine.contains("Follow us on Twitter »")) {
				return true;
			}
		}
		return false;
	}

	private void processResourcesOfJob(Integer jobId,
			VRTFilesBuilderData vrtFilesBuilderData) throws SystemException,
			NotSupportedException, SecurityException, IllegalStateException,
			RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		File destDir = new File(vrtFilesBuilderData.getDestDir());
		if (!this.userTx.isActive()) {
			this.userTx.begin();
		}
		this.entityManager.joinTransaction();
		Long totResources = (Long) this.entityManager
				.createQuery(
						"select count (cr.id) from CrawledResource cr where cr.deleted is false and cr.job.id=:jobId and cr.wordsNumber>0")
				.setParameter("jobId", jobId).getSingleResult();
		this.entityManager.flush();
		this.entityManager.clear();
		this.userTx.commit();
		StrTokenizer strTokenizer = new StrTokenizer("\t");
		for (int k = 0; k < totResources; k += VRTFilesBuilder.MAXRESULTS) {
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<CrawledResource> crawledResources = this.entityManager
					.createQuery(
							"from CrawledResource cr where cr.deleted is false and cr.job.id=:jobId and cr.wordsNumber>0 order by cr.id")
					.setParameter("jobId", jobId).setFirstResult(k)
					.setMaxResults(VRTFilesBuilder.MAXRESULTS).getResultList();
			for (CrawledResource cr : crawledResources) {
				String posFileName = FilenameUtils.getFullPath(cr.getArcFile())
						+ JobMapperMonitor.RESOURCESDIR + cr.getDigest()
						+ ".txt.pos";
				File posFile = new File(posFileName);
				if (posFile.exists() && posFile.canRead()) {
					try {
						List<String> posFileLines = FileUtils
								.readLines(posFile);
						if (this.haveStrangeChars(posFileLines)) {
							this.log.warn("File with strange chars {0}",
									posFileName);
							continue;
						}
						List<String> newLines = new ArrayList<String>();
						for (String l : posFileLines) {
							strTokenizer.reset(l);
							String[] tokens = strTokenizer.getTokenArray();
							if (tokens.length != 3) {
								System.err.println("File: " + posFileName
										+ " Stringa malformed: " + l);
								continue;
							}
							String nl = tokens[0] + "\t";
							nl += tokens[1].replaceAll(":", "") + "\t";
							nl += this.getEasyPos(tokens[1]) + "\t";
							nl += tokens[2];
							newLines.add(nl);
						}
						String functionalMetadatum = cr
								.getFunctionalMetadatum() != null ? cr
								.getFunctionalMetadatum().getDescription() : "";
						String semanticMetadatum = cr.getSemanticMetadatum() != null ? cr
								.getSemanticMetadatum().getDescription()
								: "";
						String url = cr.getUrl();
						if (url == null) {
							url = "";
						}
						String header = this.getHeaderFromResource(cr.getJob()
								.getName(), functionalMetadatum,
								semanticMetadatum, url, posFile);
						newLines.add(0, header);
						newLines.add("</text>");
						File vrtFile = new File(destDir, cr.getDigest()
								+ ".vrt");
						FileUtils.writeLines(vrtFile, newLines);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				} else {
					System.err.println("Warning - File " + posFileName
							+ " doesn't exist.");
				}
			}
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			System.out.println("VRT - Processing resources " + k + " of "
					+ totResources);
		}
	}

	public void reverseVRTFiles(VRTFilesBuilderData vrtFilesBuilderData) {
		String origDir = vrtFilesBuilderData.getOrigDir();
		Collection<File> files = FileUtils.listFiles(new File(origDir),
				new String[] { "vrt" }, true);
		System.out.println("Files to be processed: " + files.size());
		File destDir = new File(vrtFilesBuilderData.getDestDir());
		destDir.mkdir();
		int i = 0;
		for (File f : files) {
			++i;
			List<String> lines = null;
			try {
				lines = FileUtils.readLines(f);
				List<String> newLines = new ArrayList<String>();
				newLines.add(lines.remove(0));
				newLines.add(lines.remove(0));
				String tail = lines.remove(lines.size() - 1);
				Collections.reverse(lines);
				for (String l : lines) {
					newLines.add(l);
				}
				newLines.add(tail);
				File vrtFile = new File(destDir, FilenameUtils.getBaseName(f
						.getName())
						+ ".vrt");
				FileUtils.writeLines(vrtFile, newLines);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (i % 100 == 0) {
				System.out.println("Processed files: " + i);
			}
		}

	}

}
