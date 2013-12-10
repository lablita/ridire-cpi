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

import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.results.Sketch;
import it.drwolf.ridire.session.LocalResourcesManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.jboss.seam.Component;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

@Name("sketchCreator")
public class SketchCreator {

	private class SketchFix {
		private String collocata;
		private Number scoreOld;
		private Number scoreNew;
		private Number fAOld;
		private Number fANew;
		private Number fB;
		private Number fABOld;
		private Number fABNew;
	}

	private class SketchFixComparator implements Comparator<SketchFix> {
		public int compare(SketchFix s1, SketchFix s2) {
			if (s1 == null) {
				return -1;
			}
			if (s2 == null) {
				return 1;
			}
			return s1.scoreNew.doubleValue() > s2.scoreNew.doubleValue() ? -1
					: 1;
		}

	}

	private EntityManager entityManager;
	private UserTransaction userTx;

	public static final String[] POSS = new String[] { Sketch.NOUN,
			Sketch.VERB, Sketch.ADJECTIVE, Sketch.ADVERB };

	private boolean alreadyIndexed(String lemma, String functionalMetadatum,
			String semanticMetadatum, String name, IndexSearcher indexSearcher) {
		BooleanQuery bq = new BooleanQuery();
		TermQuery tqLemma = new TermQuery(new Term("lemma", lemma));
		bq.add(tqLemma, Occur.MUST);
		if (functionalMetadatum != null) {
			TermQuery funcQuery = new TermQuery(new Term("functional",
					functionalMetadatum));
			bq.add(funcQuery, Occur.MUST);
		} else if (semanticMetadatum != null) {
			TermQuery semQuery = new TermQuery(new Term("semantic",
					semanticMetadatum));
			bq.add(semQuery, Occur.MUST);
		}
		if (semanticMetadatum == null && functionalMetadatum == null) {
			TermQuery allCorporaQuery = new TermQuery(new Term("allcorpora",
					"yes"));
			bq.add(allCorporaQuery, Occur.MUST);
		}
		if (!name.equals("Tutti")) {
			if (name.startsWith("pp_")) {
				PrefixQuery prefixQuery = new PrefixQuery(new Term("sketch",
						"pp_"));
				bq.add(prefixQuery, Occur.MUST);
			} else {
				TermQuery sketchQuery = new TermQuery(new Term("sketch", name));
				bq.add(sketchQuery, Occur.MUST);
			}
		}
		TopDocs results = null;
		try {
			results = indexSearcher.search(bq, Integer.MAX_VALUE);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (results != null && results.totalHits > 0) {
			return true;
		}
		return false;
	}

	private void assignNewScore(SketchFix sf, Integer fANew) {
		double a = 2.0 * sf.fABOld.doubleValue()
				/ (fANew + sf.fB.doubleValue());
		sf.fANew = fANew;
		sf.scoreNew = 14 + Math.log(a) / Math.log(2);
	}

	private Number calculateOldFAB(SketchFix sketchFix) {
		double t = Math.pow(2.0, sketchFix.scoreOld.doubleValue() - 14);
		return Math.round(t
				* (sketchFix.fAOld.doubleValue() + sketchFix.fB.doubleValue())
				/ 2);
	}

	@Asynchronous
	public void createSketches(SketchCreatorData sketchCreatorData) {
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
			String cqpExecutable = this.entityManager.find(Parameter.class,
					Parameter.CQP_EXECUTABLE.getKey()).getValue();
			String cqpRegistry = this.entityManager.find(Parameter.class,
					Parameter.CQP_REGISTRY.getKey()).getValue();
			String cqpCorpusName = this.entityManager.find(Parameter.class,
					Parameter.CQP_CORPUSNAME_FOR_SKETCHES.getKey()).getValue();
			LocalResourcesManager localResourcesManager = (LocalResourcesManager) Component
					.getInstance("localResourcesManager");
			AsyncSketchCreator asyncSketchCreator = (AsyncSketchCreator) Component
					.getInstance("asyncSketchCreator");
			Set<String> functionalMetadata = localResourcesManager
					.getAllFunctionalMetadataMap().keySet();
			Set<String> semanticMetadata = localResourcesManager
					.getAllSemanticMetadataMap().keySet();
			Map<String, List<String>> toBeProcessed = new HashMap<String, List<String>>();
			for (String pos : SketchCreator.POSS) {
				toBeProcessed.put(pos, this.getToBeProcessed(pos,
						sketchCreatorData.getIndexWriter(), functionalMetadata,
						semanticMetadata, sketchCreatorData.getWorkingDir()));
			}
			for (String pos : SketchCreator.POSS) {
				this.processList(cqpExecutable, cqpRegistry, cqpCorpusName,
						toBeProcessed.get(pos), pos, sketchCreatorData,
						asyncSketchCreator);
			}
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
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

	// private void fixSingleLemmaDomainSketches(String domain, String lemma,
	// IndexWriter indexWriter) throws CorruptIndexException, IOException,
	// SystemException, NotSupportedException, SecurityException,
	// IllegalStateException, RollbackException, HeuristicMixedException,
	// HeuristicRollbackException {
	// String type = "WORD_SKETCH";
	// BooleanQuery bq = new BooleanQuery();
	// boolean allcorpora = false;
	// if (domain == null) {
	// allcorpora = true;
	// }
	// bq.add(new TermQuery(new Term("type", type)), Occur.MUST);
	// bq.add(new TermQuery(new Term("lemma", lemma)), Occur.MUST);
	// boolean functional = this.isFunctional(domain);
	// if (allcorpora) {
	// bq.add(new TermQuery(new Term("allcorpora", "yes")), Occur.MUST);
	// } else {
	// if (functional) {
	// bq.add(new TermQuery(new Term("functional", domain)),
	// Occur.MUST);
	// } else {
	// bq.add(new TermQuery(new Term("semantic", domain)), Occur.MUST);
	// }
	// }
	// String freqTable = "freq_lemma_all";
	// if (!allcorpora) {
	// freqTable = "freq_lemma_" + domain.replaceAll("\\s", "_");
	// }
	// if (!this.userTx.isActive()) {
	// this.userTx.begin();
	// }
	// this.entityManager.joinTransaction();
	// List<Number> firstFreqList = this.entityManager
	// .createNativeQuery(
	// "select freq from " + freqTable + " where item=:item")
	// .setParameter("item", lemma).getResultList();
	// this.userTx.commit();
	// if (firstFreqList != null && firstFreqList.size() > 0
	// && firstFreqList.get(0).longValue() > 0) {
	// Long fA = firstFreqList.get(0).longValue();
	// TopDocs topDocs = this.indexSearcher.search(bq, Integer.MAX_VALUE);
	// for (ScoreDoc sd : topDocs.scoreDocs) {
	// Document d_orig = this.indexReader.document(sd.doc);
	// String table = d_orig.get("tabella");
	// String sketch = d_orig.get("sketch");
	// String goodFor = d_orig.get("goodFor");
	// this.tokLines.reset(table);
	// List<String> lines = this.tokLines.getTokenList();
	// Integer fANew = 0;
	// Map<String, SketchFix> map = new HashMap<String,
	// SketchCreator.SketchFix>();
	// for (String line : lines) {
	// this.tokCells.reset(line);
	// String[] cells = this.tokCells.getTokenArray();
	// SketchFix sketchFix = new SketchFix();
	// String collocata = cells[0];
	// sketchFix.collocata = collocata;
	// sketchFix.scoreOld = Double.valueOf(cells[2]);
	// sketchFix.fAOld = fA;
	// sketchFix.fB = Long.valueOf(cells[1]);
	// Number F1 = this.calculateOldFAB(sketchFix);
	// sketchFix.fABOld = F1;
	// fANew += F1.intValue();
	// map.put(lemma, sketchFix);
	// }
	// for (String l : map.keySet()) {
	// SketchFix sf = map.get(l);
	// this.assignNewScore(sf, fANew);
	// }
	// List<SketchFix> orderedSketchFix = this
	// .getOrderedSketchFixes(map.values());
	// StringBuffer newTab = new StringBuffer();
	// for (SketchFix sf : orderedSketchFix) {
	// newTab.append(sf.collocata + "\t" + sf.fB + "\t"
	// + sf.scoreNew + "\t" + sf.fABNew + "\n");
	// }
	// Document d = new Document();
	// d.add(new Field("overallfrequency", fANew + "", Store.YES,
	// Index.NO));
	// d.add(new Field("tabella", newTab.toString(), Store.YES,
	// Index.NO));
	// d.add(new Field("lemma", lemma, Store.NO,
	// Index.NOT_ANALYZED_NO_NORMS));
	// d.add(new Field("sketch", sketch, Field.Store.YES,
	// Index.NOT_ANALYZED_NO_NORMS));
	// d.add(new Field("goodFor", goodFor, Field.Store.YES,
	// Index.NOT_ANALYZED_NO_NORMS));
	// d.add(new Field("type", type, Field.Store.NO,
	// Index.NOT_ANALYZED_NO_NORMS));
	// if (functional) {
	// d.add(new Field("functional", domain, Field.Store.NO,
	// Index.NOT_ANALYZED_NO_NORMS));
	// } else if (!allcorpora) {
	// d.add(new Field("semantic", domain, Field.Store.NO,
	// Index.NOT_ANALYZED_NO_NORMS));
	// } else {
	// d.add(new Field("allcorpora", "yes", Field.Store.NO,
	// Index.NOT_ANALYZED_NO_NORMS));
	// }
	// indexWriter.addDocument(d);
	// ++SketchCreator.counter;
	// System.out.println("Fixing sketch: " + SketchCreator.counter);
	// }
	// indexWriter.commit();
	// }
	// }

	// @Asynchronous
	// public void fixSketches(SketchCreatorData sketchCreatorData) {
	// try {
	// this.entityManager = (EntityManager) Component
	// .getInstance("entityManager");
	// this.userTx = (UserTransaction) org.jboss.seam.Component
	// .getInstance("org.jboss.seam.transaction.transaction");
	// this.userTx.setTransactionTimeout(1000 * 10 * 60);
	// if (!this.userTx.isActive()) {
	// this.userTx.begin();
	// }
	// this.entityManager.joinTransaction();
	// String indexLocation_orig = this.entityManager.find(
	// Parameter.class, Parameter.SKETCH_INDEX_LOCATION.getKey())
	// .getValue();
	// this.indexReader = IndexReader.open(new MMapDirectory(new File(
	// indexLocation_orig)));
	// this.indexSearcher = new IndexSearcher(this.indexReader);
	// this.userTx.commit();
	// IndexWriterConfig indexWriterConfig = new IndexWriterConfig(
	// Version.LUCENE_33, new KeywordAnalyzer());
	// indexWriterConfig.setOpenMode(OpenMode.CREATE_OR_APPEND);
	// LocalResourcesManager localResourcesManager = (LocalResourcesManager)
	// Component
	// .getInstance("localResourcesManager");
	// Set<String> functionalMetadata = localResourcesManager
	// .getAllFunctionalMetadataMap().keySet();
	// Set<String> semanticMetadata = localResourcesManager
	// .getAllSemanticMetadataMap().keySet();
	// Set<String> allDomains = new HashSet<String>();
	// allDomains.addAll(semanticMetadata);
	// allDomains.addAll(functionalMetadata);
	// for (String pos : SketchCreator.POSS) {
	// List<String> lemmiOfPos = FileUtils.readLines(new File(
	// sketchCreatorData.getWorkingDir()
	// + System.getProperty("file.separator") + pos
	// + ".txt"));
	// for (String lemma : lemmiOfPos) {
	// for (String domain : allDomains) {
	// this.fixSingleLemmaDomainSketches(domain, lemma,
	// sketchCreatorData.getIndexWriter());
	// }
	// // all domains
	// this.fixSingleLemmaDomainSketches(null, lemma,
	// sketchCreatorData.getIndexWriter());
	// }
	// }
	// } catch (SystemException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (CorruptIndexException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (LockObtainFailedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IOException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (NotSupportedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (SecurityException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (IllegalStateException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (RollbackException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (HeuristicMixedException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } catch (HeuristicRollbackException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// } finally {
	// try {
	// if (this.userTx != null && this.userTx.isActive()) {
	// this.userTx.rollback();
	// }
	// } catch (IllegalStateException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// } catch (SecurityException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// } catch (SystemException e1) {
	// // TODO Auto-generated catch block
	// e1.printStackTrace();
	// }
	// }
	// }

	private List<SketchFix> getOrderedSketchFixes(
			Collection<SketchFix> collection) {
		List<SketchFix> ret = new ArrayList<SketchFix>();
		ret.addAll(collection);
		Collections.sort(ret, new SketchFixComparator());
		// return ret.subList(0, Math.min(20, ret.size()));
		return ret;
	}

	private List<String> getToBeProcessed(String pos, IndexWriter indexWriter,
			Set<String> functionalMetadata, Set<String> semanticMetadata,
			String workingDir) {
		List<String> allItems = null;
		List<String> toBeProcessed = new ArrayList<String>();
		try {
			IndexReader indexReader = IndexReader.open(indexWriter, false);
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			allItems = FileUtils.readLines(new File(workingDir
					+ System.getProperty("file.separator") + pos + ".txt"));
			int count = 0;
			int allItemsSize = allItems.size();
			int added = 0;
			System.out.println("Presketch: " + allItemsSize + "\t" + pos);
			for (String lemma : allItems) {
				if (this.isLemmaToBeAdded(pos, functionalMetadata,
						semanticMetadata, indexSearcher, lemma)) {
					toBeProcessed.add(lemma);
					added++;
				}
				count++;
				System.out.println("Presketch: " + count + "\t" + added);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return toBeProcessed;
	}

	private boolean isFunctional(String domain) throws SystemException,
			NotSupportedException, SecurityException, IllegalStateException,
			RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		if (domain != null) {
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<FunctionalMetadatum> fs = this.entityManager.createQuery(
					"from FunctionalMetadatum f where f.description=:d")
					.setParameter("d", domain).getResultList();
			this.userTx.commit();
			if (fs.size() == 1) {
				return true;
			}
		}
		return false;
	}

	private boolean isLemmaToBeAdded(String pos,
			Set<String> functionalMetadata, Set<String> semanticMetadata,
			IndexSearcher indexSearcher, String lemma) {
		for (Sketch s : SketchList.getSketches()) {
			if (!s.getGoodFor().equals(pos)) {
				continue;
			}
			for (String functionalMetadatum : functionalMetadata) {
				if (!this.alreadyIndexed(lemma, functionalMetadatum, null, s
						.getName(), indexSearcher)) {
					return true;
				}
			}
			for (String semanticMetadatum : semanticMetadata) {
				if (!this.alreadyIndexed(lemma, null, semanticMetadatum, s
						.getName(), indexSearcher)) {
					return true;
				}
			}
			if (!this.alreadyIndexed(lemma, null, null, s.getName(),
					indexSearcher)) {
				return true;
			}
		}
		return false;
	}

	private void processList(String cqpExecutable, String cqpRegistry,
			String cqpCorpusName, List<String> list, String pos,
			SketchCreatorData sketchCreatorData,
			AsyncSketchCreator asyncSketchCreator) {
		int allItemsSize = list.size();
		double itemsPerProcess = Math.floor(1.0 * allItemsSize
				/ sketchCreatorData.getProcessNumber());
		System.out.println("Processing " + pos + "\t" + allItemsSize + "\t"
				+ itemsPerProcess);
		int resto = allItemsSize % sketchCreatorData.getProcessNumber();
		int fromIndex = 0;
		int endIndex = 0;
		for (int i = 0; i < sketchCreatorData.getProcessNumber(); i++) {
			// List<String> nounsToBeProcessed = allNouns.subList(0, 100);
			double slice = itemsPerProcess;
			if (i < resto) {
				slice++;
			}
			endIndex = (int) Math.min(fromIndex + slice, allItemsSize);
			List<String> itemsToBeProcessed = list.subList(fromIndex, endIndex);
			System.out.println("Process " + i + "\t" + fromIndex + "\t"
					+ endIndex);
			asyncSketchCreator.createSketches(sketchCreatorData
					.getIndexWriter(), itemsToBeProcessed, cqpExecutable,
					cqpRegistry, cqpCorpusName, pos);
			fromIndex = endIndex;
		}
		System.out.println("Processing " + pos + " done.");
	}
}
