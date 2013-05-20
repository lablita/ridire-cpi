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
package it.drwolf.ridire.index.sketch;

import it.drwolf.ridire.index.cwb.CWBCollocatesExtractor;
import it.drwolf.ridire.index.cwb.CorpusSizeParams;
import it.drwolf.ridire.index.cwb.SketchComparator;
import it.drwolf.ridire.index.results.GramRel;
import it.drwolf.ridire.index.results.Sketch;
import it.drwolf.ridire.index.results.SketchResult;
import it.drwolf.ridire.session.LocalResourcesManager;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.EntityManager;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.async.QuartzTriggerHandle;

@Name("asyncSketchCreator")
public class AsyncSketchCreator implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2325775470512155140L;

	@In
	private EntityManager entityManager;

	@In
	private CorpusSizeParams corpusSizeParams;

	private String cqpExecutable;

	private String cqpCorpusName;

	private String cqpRegistry;

	@In(create = true)
	private LocalResourcesManager localResourcesManager;

	private void addDocument(Map<String, SketchResult> sr, String lemma,
			IndexWriter indexWriter, String sketch, String type,
			String functional, String semantic, String goodFor)
			throws CorruptIndexException, IOException {
		Document d = new Document();
		List<SketchResult> orderedSketchResults = this
				.getOrderedSketchResults(sr.values());
		StringBuffer sb = new StringBuffer();
		long fA = 0L;
		for (SketchResult r : orderedSketchResults) {
			fA = r.getfA();
			sb.append(r.getCollocata() + "\t" + r.getfAB() + "\t"
					+ r.getScore() + "\t" + fA + "\t" + r.getfB() + "\t" + "\n");
		}
		d.add(new Field("overallfrequency", fA + "", Store.YES, Index.NO));
		d.add(new Field("tabella", sb.toString(), Store.YES, Index.NO));
		d.add(new Field("lemma", lemma, Store.NO, Index.NOT_ANALYZED_NO_NORMS));
		d.add(new Field("sketch", sketch, Field.Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));
		d.add(new Field("goodFor", goodFor, Field.Store.YES,
				Index.NOT_ANALYZED_NO_NORMS));
		d.add(new Field("type", type, Field.Store.NO,
				Index.NOT_ANALYZED_NO_NORMS));
		if (functional != null) {
			d.add(new Field("functional", functional, Field.Store.NO,
					Index.NOT_ANALYZED_NO_NORMS));
		}
		if (semantic != null) {
			d.add(new Field("semantic", semantic, Field.Store.NO,
					Index.NOT_ANALYZED_NO_NORMS));
		}
		if (functional == null && semantic == null) {
			d.add(new Field("allcorpora", "yes", Field.Store.NO,
					Index.NOT_ANALYZED_NO_NORMS));
		}
		indexWriter.addDocument(d);
		indexWriter.commit();
	}

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

	private void compactLines(List<File> tableFile, File finalTable)
			throws ExecuteException, IOException {
		Executor executor = new DefaultExecutor();
		File tempTabTot = File.createTempFile("ridireTABTOT", ".tbl");
		File tempSh = File.createTempFile("ridireSH", ".sh");
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("export LC_ALL=C\n");
		stringBuffer.append("cat " + StringUtils.join(tableFile, " ") + " > "
				+ tempTabTot.getAbsolutePath() + "\n");
		stringBuffer
				.append("awk '{a[$2]+= $1; s+=$1}END{for(i in a){print a[i],i;}; print s \"\t\"}' "
						+ tempTabTot.getAbsolutePath()
						+ " | sort -k1nr -k2 > "
						+ finalTable.getAbsolutePath());
		FileUtils.writeStringToFile(tempSh, stringBuffer.toString());
		tempSh.setExecutable(true);
		CommandLine commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tempTabTot);
		FileUtils.deleteQuietly(tempSh);
	}

	private String createQueryForCQP(File resTblFile, String stringToAdd,
			String functionalMetadatum, String semanticMetadatum,
			String realQuery, boolean trinary) {
		if (stringToAdd == null || stringToAdd.trim().length() < 1) {
			if (functionalMetadatum != null) {
				if (realQuery.indexOf("::") != -1) {
					realQuery += " & ";
				} else {
					realQuery += " :: ";
				}
				realQuery += "target.text_functional='"
						+ functionalMetadatum.replaceAll("\\s", "_") + "';";
			} else if (semanticMetadatum != null) {

				if (realQuery.indexOf("::") != -1) {
					realQuery += " & ";
				} else {
					realQuery += " :: ";
				}
				realQuery += " target.text_semantic='"
						+ semanticMetadatum.replaceAll("\\s", "_") + "';";
			}
		} else {
			realQuery += stringToAdd;
		}
		realQuery += "\n";
		String q = "set AutoShow off;\n" + "set ProgressBar off;\n"
				+ "set PrettyPrint off;\n" + "set Context 10 words;\n"
				+ "set LeftKWICDelim '--%%%--';\n"
				+ "set RightKWICDelim '--%%%--';\n" + "show -cpos;\n"
				+ realQuery;
		q += ";\n";
		String queryToTabulate = "A1";
		if (trinary) {
			q += "tabulate "
					+ queryToTabulate
					+ " match .. matchend lemma, match .. matchend pos, target lemma > '"
					+ resTblFile.getAbsolutePath() + "'; ";
		} else {
			q += "tabulate " + queryToTabulate
					+ " target lemma > \"| sort | uniq -c | sort -nr > '"
					+ resTblFile.getAbsolutePath() + "'\"; ";
		}
		return q;
	}

	private Map<String, Map<String, Number>> createResTable(List<String> lines,
			StrTokenizer strTokenizer) {
		Map<String, Map<String, Number>> resTable = new HashMap<String, Map<String, Number>>();
		for (String line : lines) {
			String[] tokens = strTokenizer.reset(line).getTokenArray();
			if (tokens.length != 3) {
				continue;
			}
			String[] lemmas = tokens[0].split("\\s");
			String[] poss = tokens[1].split("\\s");
			if (lemmas.length != poss.length || poss.length < 2) {
				continue;
			}
			String target = tokens[2].trim();
			String preArtpre = null;
			for (int i = 1; i < poss.length; i++) {
				if (poss[i].trim().matches("PRE|ARTPRE")) {
					preArtpre = lemmas[i].trim();
					break;
				}
			}
			if (preArtpre == null) {
				continue;
			}
			Map<String, Number> tableForPre = resTable.get(preArtpre);
			if (tableForPre == null) {
				tableForPre = new HashMap<String, Number>();
			}
			Number n = tableForPre.get(target);
			if (n == null) {
				tableForPre.put(target, 1);
			} else {
				tableForPre.put(target, n.intValue() + 1);
			}
			resTable.put(preArtpre, tableForPre);
		}
		return resTable;
	}

	@Asynchronous
	public QuartzTriggerHandle createSketches(IndexWriter indexWriter,
			List<String> nounsToBeProcessed, String cqpExecutable,
			String cqpRegistry, String cqpCorpusName, String goodFor) {
		int toBeProcessed = nounsToBeProcessed.size();
		int count = 0;
		this.cqpExecutable = cqpExecutable;
		this.cqpRegistry = cqpRegistry;
		this.cqpCorpusName = cqpCorpusName;
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE sketchcreator");
		Set<String> functionalMetadata = this.localResourcesManager
				.getAllFunctionalMetadataMap().keySet();
		Set<String> semanticMetadata = this.localResourcesManager
				.getAllSemanticMetadataMap().keySet();
		try {
			System.out.println("Sketchcreator running " + this.toString());
			for (String lemma : nounsToBeProcessed) {
				++count;
				IndexReader indexReader = IndexReader.open(indexWriter, false);
				IndexSearcher indexSearcher = new IndexSearcher(indexReader);
				for (Sketch s : SketchList.getSketches()) {
					if (!s.getGoodFor().equals(goodFor)) {
						continue;
					}
					for (String functionalMetadatum : functionalMetadata) {
						if (this.alreadyIndexed(lemma, functionalMetadatum,
								null, s.getName(), indexSearcher)) {
							System.out.println("Skipped: " + lemma
									+ functionalMetadatum + s.getName());
							continue;
						}
						HashMap<String, SketchResult> sr = this
								.extractSingleLemmaSketches(lemma,
										functionalMetadatum, null, s,
										indexWriter);
						if (!s.isTrinary()) {
							this.addDocument(sr, lemma, indexWriter,
									s.getName(), "WORD_SKETCH",
									functionalMetadatum, null, s.getGoodFor());
						}
					}
					for (String semanticMetadatum : semanticMetadata) {
						if (this.alreadyIndexed(lemma, null, semanticMetadatum,
								s.getName(), indexSearcher)) {
							System.out.println("Skipped: " + lemma
									+ semanticMetadatum + s.getName());
							continue;
						}
						HashMap<String, SketchResult> sr = this
								.extractSingleLemmaSketches(lemma, null,
										semanticMetadatum, s, indexWriter);
						if (!s.isTrinary()) {
							this.addDocument(sr, lemma, indexWriter,
									s.getName(), "WORD_SKETCH", null,
									semanticMetadatum, s.getGoodFor());
						}
					}
					if (this.alreadyIndexed(lemma, null, null, s.getName(),
							indexSearcher)) {
						System.out.println("Skipped: " + lemma + " all "
								+ s.getName());
						continue;
					}
					HashMap<String, SketchResult> sr = this
							.extractSingleLemmaSketches(lemma, null, null, s,
									indexWriter);
					if (!s.isTrinary()) {
						this.addDocument(sr, lemma, indexWriter, s.getName(),
								"WORD_SKETCH", null, null, s.getGoodFor());
					}
				}
				System.out.println(goodFor + " elaborati: " + count + " su "
						+ toBeProcessed);
			}
			System.out.println("Sketchcreator done " + this.toString());
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Sketchcreator done");
		return handle;
	}

	private void executeCQPQuery(File queryFile, boolean inverse)
			throws ExecuteException, IOException {
		Executor executor = new DefaultExecutor();
		File tempSh = File.createTempFile("ridireSH", ".sh");
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("export LC_ALL=C\n");
		String corpusName = this.cqpCorpusName;
		if (inverse) {
			corpusName += "INV";
		}
		stringBuffer.append(this.cqpExecutable + " -f "
				+ queryFile.getAbsolutePath() + " -D " + corpusName + " -r "
				+ this.cqpRegistry + "\n");
		FileUtils.writeStringToFile(tempSh, stringBuffer.toString());
		tempSh.setExecutable(true);
		CommandLine commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tempSh);
	}

	private HashMap<String, SketchResult> extractSingleLemmaSketches(
			String lemma, String functionalMetadatum, String semanticMetadatum,
			Sketch s, IndexWriter indexWriter) {
		HashMap<String, SketchResult> sr = new HashMap<String, SketchResult>();
		String freqTable = "freq_lemma_all";
		if (functionalMetadatum != null) {
			freqTable = "freq_lemma_"
					+ functionalMetadatum.trim().replaceAll("\\s", "_");
		}
		if (semanticMetadatum != null) {
			freqTable = "freq_lemma_"
					+ semanticMetadatum.trim().replaceAll("\\s", "_");
		}
		List<Number> firstFreqList = this.entityManager
				.createNativeQuery(
						"select freq from " + freqTable + " where item=:item")
				.setParameter("item", lemma).getResultList();
		if (firstFreqList != null && firstFreqList.size() > 0
				&& firstFreqList.get(0).longValue() > 0) {
			long firstFreq = firstFreqList.get(0).longValue();
			StrTokenizer strTokenizer = new StrTokenizer();
			try {
				List<File> tableFiles = new ArrayList<File>();
				String queryString = null;
				String stringToAdd = null;
				String realQuery = "";
				for (GramRel gramRel : s.getGramrels()) {
					File resTblFile = File.createTempFile("ridireTBL", ".tbl");
					tableFiles.add(resTblFile);
					String rel = gramRel.getRel();
					realQuery = String.format(rel, lemma);
					String subquery = gramRel.getSubquery();
					if (subquery != null) {
						realQuery += ";\nASUB;\n"
								+ String.format(subquery, lemma);
					}
					queryString = this.createQueryForCQP(resTblFile,
							stringToAdd, functionalMetadatum,
							semanticMetadatum, realQuery, s.isTrinary());
					File queryFile = File.createTempFile("ridireQ", ".query");
					FileUtils.writeStringToFile(queryFile, queryString);
					long start = System.currentTimeMillis();
					this.executeCQPQuery(queryFile, gramRel.isInverse());
					System.out.println("CQP exec time for "
							+ realQuery.replaceAll("\n", " ") + " "
							+ functionalMetadatum + " " + semanticMetadatum
							+ " : " + (System.currentTimeMillis() - start));
					if (!resTblFile.exists() || !resTblFile.canRead()) {
						continue;
					}
					FileUtils.deleteQuietly(queryFile);
				}
				List<String> lines = null;
				if (!s.isTrinary()) {
					File resTblFile = File.createTempFile("ridireTBLFINAL",
							".tbl");
					this.compactLines(tableFiles, resTblFile);
					lines = FileUtils.readLines(resTblFile);
					FileUtils.deleteQuietly(resTblFile);
				} else if (tableFiles.size() > 0) {
					lines = FileUtils.readLines(tableFiles.get(0));
				}
				for (File tableFile : tableFiles) {
					FileUtils.deleteQuietly(tableFile);
				}
				if (s.isTrinary()) {
					strTokenizer.setDelimiterString("@@##");
					this.processTrinaryTable(freqTable, firstFreq,
							strTokenizer, lines, lemma, indexWriter,
							s.getName(), functionalMetadatum,
							semanticMetadatum, s.getGoodFor());
				} else {
					strTokenizer.setDelimiterString(" ");
					this.processNotTrinaryTable(sr, freqTable, firstFreq,
							strTokenizer, lines);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return sr;
	}

	private List<SketchResult> getOrderedSketchResults(
			Collection<SketchResult> collection) {
		List<SketchResult> ret = new ArrayList<SketchResult>();
		ret.addAll(collection);
		Collections.sort(ret, new SketchComparator());
		// return ret.subList(0, Math.min(20, ret.size()));
		return ret;
	}

	private double getSketchScore(String score, long fa, long fb, long fab,
			long n) {
		if (score.equals(CWBCollocatesExtractor.MI_SCORE)) {
			return Math.log(fab * 1.0 * n / (fa * fb)) / Math.log(2.0);
		}
		if (score.equals(CWBCollocatesExtractor.LOG_LIKELIHOOD)) {
			return this.xlx(fab) + this.xlx(fa - fab) + this.xlx(fb - fab)
					+ this.xlx(n) + this.xlx(n + fab - fa - fb) - this.xlx(fa)
					- this.xlx(fb) - this.xlx(n - fa) - this.xlx(n - fb);
		}
		if (score.equals(CWBCollocatesExtractor.T_SCORE)) {
			return (fab - fa * fb / n) / Math.sqrt(fab);
		}
		// default is logDice
		double a = 2.0 * fab / (fa + fb);
		return 14 + Math.log(a) / Math.log(2);
	}

	private void processNotTrinaryTable(HashMap<String, SketchResult> sr,
			String freqTable, long firstFreq, StrTokenizer strTokenizer,
			List<String> lines) {
		if (lines != null && lines.size() > 0
				&& lines.get(0).trim().length() > 0) {
			// fA = first line
			Number fA = Long.valueOf(lines.get(0).trim());
			for (String l : lines) {
				String[] tokens = strTokenizer.reset(l).getTokenArray();
				if (tokens.length != 2) {
					continue;
				}
				String f = tokens[1];
				List<Number> fBs = this.entityManager
						.createNativeQuery(
								"select freq from " + freqTable
										+ " where item=:item")
						.setParameter("item", f).getResultList();
				if (fBs == null || fBs.size() < 1) {
					continue;
				}
				SketchResult res = sr.get(f);
				if (res == null) {
					res = new SketchResult();
				}
				long fB = fBs.get(0).longValue();
				if (fBs != null && fBs.size() > 0 && fB > 0) {
					res.setCollocata(f);
					long n = this.corpusSizeParams.getCorpusSize(
							freqTable.substring(5)).longValue();
					long fAB = Long.parseLong(tokens[0]);
					double score = this.getSketchScore(
							CWBCollocatesExtractor.LOGDICE_SCORE,
							fA.longValue(), fB, fAB, n);
					res.setScore(score);
					res.setfA(fA.longValue());
					res.setfAB(fAB);
					res.setfB(fB);
					sr.put(f, res);
				}
			}
		}
	}

	private void processTrinaryTable(String freqTable, long firstFreq,
			StrTokenizer strTokenizer, List<String> lines, String lemma,
			IndexWriter indexWriter, String sketch, String functional,
			String semantic, String goodFor) throws CorruptIndexException,
			IOException {
		Map<String, Map<String, Number>> resTable = this.createResTable(lines,
				strTokenizer);
		HashMap<String, Map<String, SketchResult>> sr = new HashMap<String, Map<String, SketchResult>>();
		for (String pre : resTable.keySet()) {
			Map<String, Number> preTable = resTable.get(pre);
			Number fA = 0;
			for (Number pfA : preTable.values()) {
				fA = fA.longValue() + pfA.longValue();
			}
			Map<String, SketchResult> preRes = sr.get(pre);
			if (preRes == null) {
				preRes = new HashMap<String, SketchResult>();
			}
			for (String target : preTable.keySet()) {
				List<Number> fBs = this.entityManager
						.createNativeQuery(
								"select freq from " + freqTable
										+ " where item=:item")
						.setParameter("item", target).getResultList();
				SketchResult res = preRes.get(target);
				if (res == null) {
					res = new SketchResult();
				}
				if (fBs != null && fBs.size() > 0) {
					long fB = fBs.get(0).longValue();
					if (fBs != null && fBs.size() > 0 && fB > 0) {
						res.setCollocata(target);
						long n = this.corpusSizeParams.getCorpusSize(
								freqTable.substring(5)).longValue();
						long fAB = preTable.get(target).longValue();
						double score = this.getSketchScore(
								CWBCollocatesExtractor.LOGDICE_SCORE,
								fA.longValue(), fB, fAB, n);
						res.setScore(score);
						res.setfA(fA.longValue());
						res.setfB(fB);
						res.setfAB(fAB);
						preRes.put(target, res);
					}
				} else {
					continue;
				}
			}
			sr.put(pre, preRes);
		}
		for (String pre : sr.keySet()) {
			this.addDocument(sr.get(pre), lemma, indexWriter,
					String.format(sketch, pre), "WORD_SKETCH", functional,
					semantic, goodFor);
		}
	}

	@Asynchronous
	public QuartzTriggerHandle updateSketches(IndexWriter indexWriter,
			List<String> itemsToBeProcessed, String cqpExecutable,
			String cqpRegistry, String cqpCorpusName, String goodFor1,
			String goodFor2) {
		int toBeProcessed = itemsToBeProcessed.size();
		int count = 0;
		this.cqpExecutable = cqpExecutable;
		this.cqpRegistry = cqpRegistry;
		this.cqpCorpusName = cqpCorpusName;
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE sketchcreator");
		Set<String> functionalMetadata = this.localResourcesManager
				.getAllFunctionalMetadataMap().keySet();
		Set<String> semanticMetadata = this.localResourcesManager
				.getAllSemanticMetadataMap().keySet();
		try {
			System.out.println("Sketchcreator update running "
					+ this.toString());
			for (String lemma : itemsToBeProcessed) {
				++count;
				for (Sketch s : SketchList.getSketchesToUpdate()) {
					BooleanQuery bq = new BooleanQuery();
					TermQuery tqLemma = new TermQuery(new Term("lemma", lemma));
					bq.add(tqLemma, Occur.MUST);
					if (s.getName().startsWith("pp_")) {
						PrefixQuery prefixQuery = new PrefixQuery(new Term(
								"sketch", "pp_"));
						bq.add(prefixQuery, Occur.MUST);
					} else {
						TermQuery sq = new TermQuery(new Term("sketch",
								s.getName()));
						bq.add(sq, Occur.MUST);
					}
					// remove wrongly assigned sketches
					indexWriter.deleteDocuments(bq);
				}
				indexWriter.commit();
				for (Sketch s : SketchList.getSketchesToUpdate()) {
					// recreate sketches
					for (String functionalMetadatum : functionalMetadata) {
						HashMap<String, SketchResult> sr = this
								.extractSingleLemmaSketches(lemma,
										functionalMetadatum, null, s,
										indexWriter);
						if (!s.isTrinary()) {
							this.addDocument(sr, lemma, indexWriter,
									s.getName(), "WORD_SKETCH",
									functionalMetadatum, null, s.getGoodFor());
						}
					}
					for (String semanticMetadatum : semanticMetadata) {
						HashMap<String, SketchResult> sr = this
								.extractSingleLemmaSketches(lemma, null,
										semanticMetadatum, s, indexWriter);
						if (!s.isTrinary()) {
							this.addDocument(sr, lemma, indexWriter,
									s.getName(), "WORD_SKETCH", null,
									semanticMetadatum, s.getGoodFor());
						}
					}
					HashMap<String, SketchResult> sr = this
							.extractSingleLemmaSketches(lemma, null, null, s,
									indexWriter);
					if (!s.isTrinary()) {
						this.addDocument(sr, lemma, indexWriter, s.getName(),
								"WORD_SKETCH", null, null, s.getGoodFor());
					}
				}
				System.out.println(goodFor1 + "-" + goodFor2 + " elaborati: "
						+ count + " su " + toBeProcessed);
			}
			System.out.println("Sketchcreator update done " + this.toString());
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("Sketchcreator done");
		return handle;
	}

	private double xlx(long l) {
		if (l == 0) {
			return 0;
		}
		return l * Math.log(l);
	}
}
