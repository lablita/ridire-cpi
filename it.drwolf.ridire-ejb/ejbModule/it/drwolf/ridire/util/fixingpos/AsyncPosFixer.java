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
package it.drwolf.ridire.util.fixingpos;

import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.index.cwb.scripts.VRTFilesBuilder;
import it.drwolf.ridire.session.async.JobMapperMonitor;
import it.drwolf.ridire.session.async.WordCounter;
import it.drwolf.ridire.utility.RIDIREReTagger;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
import org.jboss.seam.Component;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

@Name("asyncPosFixer")
public class AsyncPosFixer implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3068568850090727817L;
	private EntityManager entityManager;
	private UserTransaction userTx;
	private RIDIREReTagger ridireReTagger;

	@In(create = true)
	private VRTFilesBuilder vrtFilesBuilder;

	@In(create = true)
	private WordCounter wordCounter;

	@SuppressWarnings("unchecked")
	@Asynchronous
	public void doAsyncFix(PosFixerData posFixerData) {
		StrTokenizer strTokenizer = new StrTokenizer("\t");
		File destDir = new File(posFixerData.getDestDir());
		File reverseDestDir = new File(posFixerData.getReverseDestDir());
		if (!destDir.exists() || !destDir.isDirectory()
				|| !reverseDestDir.exists() || !reverseDestDir.isDirectory()) {
			System.err.println("Not valid destination folder.");
			return;
		}
		this.ridireReTagger = new RIDIREReTagger(null);
		try {
			this.entityManager = (EntityManager) Component
					.getInstance("entityManager");
			this.userTx = (UserTransaction) org.jboss.seam.Component
					.getInstance("org.jboss.seam.transaction.transaction");
			this.userTx.setTransactionTimeout(1000 * 10 * 60);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String treeTaggerBin = this.entityManager.find(
					CommandParameter.class,
					CommandParameter.TREETAGGER_EXECUTABLE_KEY)
					.getCommandValue();
			this.ridireReTagger.setTreetaggerBin(treeTaggerBin);
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			List<String> lines = FileUtils.readLines(new File(posFixerData
					.getFile()));
			int count = 0;
			for (String l : lines) {
				if (l == null || l.trim().length() < 1) {
					continue;
				}
				String digest = l.replaceAll("\\./", "").replaceAll("\\.vrt",
						"");
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				this.entityManager.joinTransaction();
				List<CrawledResource> crs = this.entityManager.createQuery(
						"from CrawledResource cr where cr.digest=:digest")
						.setParameter("digest", digest).getResultList();
				if (crs.size() != 1) {
					System.err.println("PosFixer: " + l
							+ " resource not found.");
				} else {
					CrawledResource cr = crs.get(0);
					String origFile = FilenameUtils
							.getFullPath(cr.getArcFile()).concat(
									JobMapperMonitor.RESOURCESDIR).concat(
									digest.concat(".txt"));
					File toBeRetagged = new File(origFile);
					if (toBeRetagged.exists() && toBeRetagged.canRead()) {
						String retaggedFile = this.ridireReTagger
								.retagFile(toBeRetagged);
						int wordsNumber = this.wordCounter
								.countWordsFromPoSTagResource(new File(
										retaggedFile));
						cr.setWordsNumber(wordsNumber);
						this.entityManager.persist(cr);
						this.vrtFilesBuilder.createVRTFile(retaggedFile,
								strTokenizer, cr, destDir);
						String vrtFileName = destDir
								+ System.getProperty("file.separator") + digest
								+ ".vrt";
						File vrtFile = new File(vrtFileName);
						this.vrtFilesBuilder.reverseFile(reverseDestDir,
								vrtFile);
					}
				}
				this.userTx.commit();
				System.out.println(" Processed " + (++count) + " of "
						+ lines.size());
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
		} catch (IOException e) {
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
}
