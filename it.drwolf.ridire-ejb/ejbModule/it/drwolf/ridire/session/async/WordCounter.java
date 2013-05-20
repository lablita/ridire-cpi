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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.util.MD5DigestCreator;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

@Name("wordCounter")
public class WordCounter {
	private UserTransaction userTx;

	@In
	private EntityManager entityManager;

	private List<String> notWordPoSs = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 9183543723833790171L;

		{
			this.add("PON");
			this.add("PUN");
			this.add("NOCAT");
			this.add("SENT");
			this.add("SYM");
		}
	};

	@Asynchronous
	public void countWords(Integer jobId) {
		try {
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			String resourcesDir = null;
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String dir = this.entityManager.find(Parameter.class,
					Parameter.JOBS_DIR.getKey()).getValue();
			Job curJ = this.entityManager.find(Job.class, jobId);
			if (curJ.getChildJobName() != null && !curJ.isMappedResources()) {
				resourcesDir = dir + JobMapperMonitor.FILE_SEPARATOR
						+ curJ.getChildJobName()
						+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
						+ JobMapperMonitor.FILE_SEPARATOR
						+ JobMapperMonitor.RESOURCESDIR;
			} else {
				resourcesDir = dir + JobMapperMonitor.FILE_SEPARATOR
						+ curJ.getName() + JobMapperMonitor.FILE_SEPARATOR
						+ "arcs" + JobMapperMonitor.FILE_SEPARATOR
						+ JobMapperMonitor.RESOURCESDIR;
			}

			// back compatibility with Heritrix 2
			File resourcesDirFile = new File(resourcesDir);
			if (resourcesDirFile == null || !resourcesDirFile.isDirectory()) {
				resourcesDir = "completed-" + resourcesDir;
			}
			this.entityManager.flush();
			this.userTx.commit();
			Integer jobWords = 0;
			for (CrawledResource cr : curJ.getCrawledResources()) {
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				this.entityManager.joinTransaction();
				this.entityManager.refresh(cr);
				String digest = cr.getDigest();
				if (digest != null) {
					File posFile = new File(resourcesDir
							+ JobMapperMonitor.FILE_SEPARATOR + digest
							+ ".txt.pos");
					if (posFile != null && posFile.exists()
							&& posFile.canRead()) {
						try {
							Integer countWordsFromPoSTagResource = this
									.countWordsFromPoSTagResource(posFile);
							jobWords += countWordsFromPoSTagResource;
							cr.setWordsNumber(countWordsFromPoSTagResource);
							File f = new File(FilenameUtils.getFullPath(cr
									.getArcFile())
									+ JobMapperMonitor.RESOURCESDIR
									+ cr.getDigest() + ".txt");
							if (f.exists() && f.canRead()) {
								cr.setExtractedTextHash(MD5DigestCreator
										.getMD5Digest(f));
							}
							this.entityManager.merge(cr);
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						} catch (NoSuchAlgorithmException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}
				}
				this.entityManager.flush();
				this.userTx.commit();
			}
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			Job j = this.entityManager.find(Job.class, jobId);
			j.setWordsNumber(jobWords);
			this.entityManager.persist(j);
			this.entityManager.flush();
			this.userTx.commit();
		} catch (NotSupportedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (SystemException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
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
		}
	}

	private Integer countWordsFromPoSTagResource(File posTagResourceFile)
			throws IOException {
		List<String> lines = FileUtils.readLines(posTagResourceFile);
		Integer count = 0;
		StrTokenizer tokenizer = StrTokenizer.getTSVInstance();
		for (String l : lines) {
			tokenizer.reset(l);
			String[] tokens = tokenizer.getTokenArray();
			if (tokens.length == 3) {
				if (this.isValidPos(tokens[1].trim())) {
					++count;
				}
			}
		}
		return count;
	}

	private boolean isValidPos(String pos) {
		if (this.notWordPoSs.contains(pos)) {
			return false;
		}
		return true;
	}
}
