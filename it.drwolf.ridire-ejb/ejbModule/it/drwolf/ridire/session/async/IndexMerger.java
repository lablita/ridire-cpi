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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.ContextsIndexManager;

import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Name("indexMerger")
public class IndexMerger {

	private static final int RESBATCH = 20;
	@In(create = true)
	private FlagBearer flagBearer;
	private UserTransaction userTx;
	private EntityManager entityManager1;
	// private ContextsIndexManager contextsIndexManager;

	@In
	private EntityManager entityManager;
	private ChannelSftp c;
	private JSch jSch;
	private Session session;

	private void disconnect() {
		this.c.disconnect();
		this.session.disconnect();
	}

	private boolean isAlreadyIndexed(String www1localstore, String jobName,
			CrawledResource cr) {
		File crFile = new File(www1localstore
				+ System.getProperty("file.separator") + jobName
				+ System.getProperty("file.separator") + cr.getDigest()
				+ ".txt.pos");
		return crFile.exists() && crFile.canRead();
	}

	private boolean isAlreadyMerged(Job j, long crSize, String www1localstore) {
		// check if dir exists and check indexed resources
		File jobDir = new File(www1localstore
				+ System.getProperty("file.separator") + j.getName());
		if (jobDir == null || !jobDir.exists()
				|| jobDir.listFiles().length < crSize) {
			return false;
		}
		return true;
	}

	@Asynchronous
	public void mergeIndexes() {
		final int CORPUS_SIZE_LIMIT = 100000000;
		this.flagBearer.setMergingStopped(false);
		String username = this.entityManager.find(Parameter.class,
				Parameter.WWW1_USERNAME.getKey()).getValue();
		String password = this.entityManager.find(Parameter.class,
				Parameter.WWW1_PW.getKey()).getValue();
		String host = this.entityManager.find(Parameter.class,
				Parameter.WWW1_HOST.getKey()).getValue();
		String www1localstore = this.entityManager.find(Parameter.class,
				Parameter.WWW1_LOCAL_STORE.getKey()).getValue();
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		int transferredWords = 0;
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			// 10 mins
			this.userTx.begin();
			this.entityManager1 = (EntityManager) Component
					.getInstance("entityManager1");
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.entityManager1.joinTransaction();
			List<Integer> jobs = this.entityManager1
					.createQuery(
							"select j.id from Job j where j.validationStatus=:procstatus and indexed=true")
					.setParameter("procstatus", Job.VALIDATED_OK)
					.getResultList();
			this.entityManager1.flush();
			this.entityManager1.clear();
			this.userTx.commit();
			int transferredFiles = 0;
			for (long jobCount = 0; jobCount < jobs.size(); jobCount++) {
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				int jobResourcesCount = 0;
				this.entityManager1.joinTransaction();
				Job j = this.entityManager1.find(Job.class,
						jobs.get((int) jobCount));
				long crawledResourcesSize = (Long) this.entityManager1
						.createQuery(
								"select count(cr.id) from CrawledResource cr where cr.job.id=:jobid and cr.processed=:indexed")
						.setParameter("jobid", j.getId())
						.setParameter("indexed", Parameter.INDEXED)
						.getSingleResult();
				System.out.println("Merging job: " + j.getName() + " "
						+ crawledResourcesSize + "(" + jobCount + "/"
						+ jobs.size() + ")");
				if (this.isAlreadyMerged(j, crawledResourcesSize,
						www1localstore)) {
					System.out.println("Merging job " + j.getName()
							+ ". Already merged.");
					this.entityManager1.flush();
					this.entityManager1.clear();
					this.userTx.commit();
					continue;
				}
				this.entityManager1.flush();
				this.entityManager1.clear();
				this.userTx.commit();
				for (int resCount = 0; resCount < crawledResourcesSize; resCount += IndexMerger.RESBATCH) {
					if (!this.userTx.isActive()) {
						this.userTx.begin();
					}
					this.entityManager1.joinTransaction();
					List<CrawledResource> crawledResources = this.entityManager1
							.createQuery(
									"from CrawledResource cr where cr.job.id=:jobid and cr.processed=:indexed order by cr.id")
							.setParameter("jobid", j.getId())
							.setParameter("indexed", Parameter.INDEXED)
							.setFirstResult(resCount)
							.setMaxResults(IndexMerger.RESBATCH)
							.getResultList();
					for (CrawledResource cr : crawledResources) {
						jobResourcesCount++;
						if (jobResourcesCount % 100 == 0) {
							System.out.println("Merging status: "
									+ jobResourcesCount
									/ (crawledResourcesSize * 1.0f) * 100 + "%"
									+ " (" + jobResourcesCount + "/"
									+ crawledResourcesSize + ")");
						}
						if (this.isAlreadyIndexed(www1localstore, j.getName(),
								cr)) {
							continue;
						}
						if (transferredFiles % 30 == 0) {
							this.openConnection(username, password, host);
						}
						try {
							this.transferFile(www1localstore, j.getName(), cr);
							this.contextsIndexManager
									.indexExternalCrawledResource(
											j.getName(),
											cr.getSemanticMetadatum() == null ? ""
													: cr.getSemanticMetadatum()
															.getDescription(),
											cr.getFunctionalMetadatum() == null ? ""
													: cr.getFunctionalMetadatum()
															.getDescription(),
											j.getId(), cr.getDigest());
						} catch (JSchException jSchException) {
							jSchException.printStackTrace();
						} catch (SftpException sftpException) {
							System.err.println("No such file: "
									+ cr.getDigest());
						} catch (IOException ioException) {
							ioException.printStackTrace();
						}
						transferredFiles++;
						transferredWords += cr.getWordsNumber();
						if (transferredFiles % 30 == 0) {
							this.disconnect();
						}
					}
					this.entityManager1.flush();
					this.entityManager1.clear();
					this.userTx.commit();
					if (this.flagBearer.isMergingStopped()) {
						this.disconnect();
						this.contextsIndexManager.closeIndexWriter();
						return;
					}
					if (transferredWords > CORPUS_SIZE_LIMIT) {
						break;
					}
				}
				if (transferredWords > CORPUS_SIZE_LIMIT) {
					break;
				}
			}
			// this.contextsIndexManager = (ContextsIndexManager) Component
			// .getInstance("contextsIndexManager");
			this.entityManager1.flush();
			this.entityManager1.clear();
			this.userTx.commit();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (this.userTx != null && this.userTx.isActive()) {
					this.userTx.rollback();
				}
			} catch (IllegalStateException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SecurityException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SystemException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void openConnection(String username, String password, String host)
			throws JSchException {
		if (this.jSch == null) {
			this.jSch = new JSch();
		}
		this.session = this.jSch.getSession(username, host);
		java.util.Properties config = new java.util.Properties();
		config.put("StrictHostKeyChecking", "no");
		this.session.setConfig(config);
		this.session.setPassword(password);
		this.session.connect();
		Channel channel = this.session.openChannel("sftp");
		channel.connect();
		this.c = (ChannelSftp) channel;
	}

	private void transferFile(String www1localstore, String jobName,
			CrawledResource cr) throws JSchException, SftpException,
			IOException {
		int mode = ChannelSftp.OVERWRITE;
		String localDir = www1localstore + System.getProperty("file.separator")
				+ jobName;
		FileUtils.forceMkdir(new File(localDir));
		this.c.get(
				FilenameUtils.getFullPath(cr.getArcFile())
						+ System.getProperty("file.separator") + "resources"
						+ System.getProperty("file.separator") + cr.getDigest()
						+ ".txt.pos",
				localDir + System.getProperty("file.separator")
						+ cr.getDigest() + ".txt.pos", null, mode);
	}
}
