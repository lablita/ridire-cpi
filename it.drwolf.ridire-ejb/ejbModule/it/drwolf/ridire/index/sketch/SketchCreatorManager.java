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
package it.drwolf.ridire.index.sketch;

import it.drwolf.ridire.entity.Parameter;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;

import org.apache.lucene.analysis.KeywordAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.util.Version;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("sketchCreatorManager")
@Scope(ScopeType.CONVERSATION)
public class SketchCreatorManager {

	@In
	private EntityManager entityManager;

	@In(create = true)
	private SketchCreatorData sketchCreatorData;

	private String destDir;

	private String workingDir;

	private Integer processNumber;

	@In(create = true)
	private SketchCreator sketchCreator;

	public void closeIndex() {
		try {
			if (this.sketchCreatorData.getIndexWriter() != null) {
				this.sketchCreatorData.getIndexWriter().close();
			} else {
				String indexLocation = this.entityManager.find(Parameter.class,
						Parameter.SKETCH_INDEX_LOCATION.getKey()).getValue();
				IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
						Version.LUCENE_33, new KeywordAnalyzer());
				indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
				IndexWriter indexWriter = new IndexWriter(new MMapDirectory(
						new File(indexLocation)), indexWriterConfig);
				if (indexWriter != null) {
					indexWriter.close();

				}
			}
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createSketches() {
		// sketches are created not in the same dir of the currently queried
		// ones.
		String indexLocation = this.entityManager.find(Parameter.class,
				Parameter.SKETCH_INDEX_LOCATION2.getKey()).getValue();
		this.doCreateSketches(indexLocation);
	}

	private void doCreateSketches(String indexLocation) {
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
				Version.LUCENE_33, new KeywordAnalyzer());
		indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
		try {
			IndexWriter indexWriter = new IndexWriter(new MMapDirectory(
					new File(indexLocation)), indexWriterConfig);
			this.sketchCreatorData.setIndexWriter(indexWriter);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.sketchCreatorData.setProcessNumber(this.processNumber);
		this.sketchCreatorData.setWorkingDir(this.workingDir);
		this.sketchCreator.createSketches(this.sketchCreatorData);
	}

	// public void fixSketches() {
	// String indexLocation2 = this.entityManager.find(Parameter.class,
	// Parameter.SKETCH_INDEX_LOCATION2.getKey()).getValue();
	// IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
	// Version.LUCENE_33, new KeywordAnalyzer());
	// indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
	// try {
	// IndexWriter indexWriter = new IndexWriter(new MMapDirectory(
	// new File(indexLocation2)), indexWriterConfig);
	// this.sketchCreatorData.setIndexWriter(indexWriter);
	// } catch (CorruptIndexException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (LockObtainFailedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// this.sketchCreatorData.setDestDir(this.destDir);
	// this.sketchCreatorData.setProcessNumber(this.processNumber);
	// this.sketchCreatorData.setWorkingDir(this.workingDir);
	// this.sketchCreator.fixSketches(this.sketchCreatorData);
	// }

	public String getDestDir() {
		return this.destDir;
	}

	public Integer getProcessNumber() {
		return this.processNumber;
	}

	public String getWorkingDir() {
		return this.workingDir;
	}

	public void setDestDir(String destDir) {
		this.destDir = destDir;
	}

	public void setProcessNumber(Integer processNumber) {
		this.processNumber = processNumber;
	}

	public void setWorkingDir(String workingDir) {
		this.workingDir = workingDir;
	}

	// private void updateIndex(String cqpExecutable, String cqpRegistry,
	// String cqpCorpusName, File file) {
	// List<String> allItems = null;
	// try {
	// allItems = FileUtils.readLines(file);
	// } catch (IOException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	// int allItemsSize = allItems.size();
	// double itemsPerProcess = Math.floor(1.0 * allItemsSize
	// / this.getProcessNumber());
	// System.out.println("Update "
	// + FilenameUtils.getBaseName(file.getName()) + "\t"
	// + allItemsSize + "\t" + itemsPerProcess);
	// int resto = allItemsSize % this.getProcessNumber();
	// int fromIndex = 0;
	// int endIndex = 0;
	// String[] poss = FilenameUtils.getBaseName(file.getName()).split("_");
	// for (int i = 0; i < this.getProcessNumber(); i++) {
	// // List<String> nounsToBeProcessed = allNouns.subList(0, 100);
	// double slice = itemsPerProcess;
	// if (i < resto) {
	// slice++;
	// }
	// endIndex = (int) Math.min(fromIndex + slice, allItemsSize);
	// List<String> nounsToBeProcessed = allItems.subList(fromIndex,
	// endIndex);
	// System.out.println("Update " + i + "\t" + fromIndex + "\t"
	// + endIndex);
	// this.asyncSketchCreator.updateSketches(this.indexWriter,
	// nounsToBeProcessed, cqpExecutable, cqpRegistry,
	// cqpCorpusName, poss[0], poss[1]);
	// fromIndex = endIndex;
	// }
	// System.out.println("Update "
	// + FilenameUtils.getBaseName(file.getName()) + " done.");
	// }
	//
	// public void updateSketches() {
	// String cqpExecutable = this.entityManager.find(Parameter.class,
	// Parameter.CQP_EXECUTABLE.getKey()).getValue();
	// String cqpRegistry = this.entityManager.find(Parameter.class,
	// Parameter.CQP_REGISTRY.getKey()).getValue();
	// String cqpCorpusName = this.entityManager.find(Parameter.class,
	// Parameter.CQP_CORPUSNAME.getKey()).getValue();
	// String indexLocation = this.entityManager.find(Parameter.class,
	// Parameter.SKETCH_INDEX_LOCATION.getKey()).getValue();
	// IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
	// Version.LUCENE_33, new KeywordAnalyzer());
	// indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
	// indexWriterConfig.setMaxThreadStates(this.getProcessNumber());
	// try {
	// this.indexWriter = new IndexWriter(new MMapDirectory(new File(
	// indexLocation)), indexWriterConfig);
	// } catch (CorruptIndexException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (LockObtainFailedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// Collection<File> files = FileUtils.listFiles(
	// new File(this.getWorkingDir()), new String[] { "change" },
	// false);
	// for (File f : files) {
	// this.updateIndex(cqpExecutable, cqpRegistry, cqpCorpusName, f);
	// }
	// }

}
