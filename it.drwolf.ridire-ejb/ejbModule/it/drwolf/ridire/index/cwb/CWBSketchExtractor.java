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
package it.drwolf.ridire.index.cwb;

import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.index.results.Sketch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.Executor;
import org.apache.commons.io.FileUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("cwbSketchExtractor")
@Scope(ScopeType.CONVERSATION)
public class CWBSketchExtractor {
	private String firstWord;
	private String secondWord;
	private List<Sketch> sketches = new ArrayList<Sketch>();
	private String firstPoS = Sketch.NOUN;
	private String secondPoS = Sketch.NOUN;
	@In
	private EntityManager entityManager;

	private String cqpExecutable;

	private String cqpCorpusName;
	private String cqpRegistry;

	@In
	private CorpusSizeParams corpusSizeParams;

	private String score = CWBCollocatesExtractor.LOGDICE_SCORE;

	private Integer semanticMetadatum = -1;
	private Integer functionalMetadatum = -1;
	private String secondMetadatum = "Tutti";
	private boolean confSemantic = false;
	private boolean confFunctional = false;
	public static final String ADVERB = "avverbio";
	public static final String ADJECTIVE = "aggettivo";
	public static final String NOUN = "nome";
	public static final String VERB = "verbo";
	private static final String ALL = "all";
	private List<String> availableGoodFors = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -7105422014332362200L;

		{
			this.add(CWBSketchExtractor.NOUN);
			this.add(CWBSketchExtractor.VERB);
			this.add(CWBSketchExtractor.ADJECTIVE);
			this.add(CWBSketchExtractor.ADVERB);
		}
	};

	private String sketchToExtract;
	private List<SelectItem> goodSketches = new ArrayList<SelectItem>();

	private Sketch chosenSketch = null;

	private String firstDomain;

	private String secondDomain;

	private List<String> sketchQueries = new ArrayList<String>();

	private String createQueryForCQP(String gramRel, File resTblFile,
			String stringToAdd) {
		String realQuery = String.format(gramRel, this.getFirstWord());
		if (stringToAdd == null || stringToAdd.trim().length() < 1) {
			if (this.getFunctionalMetadatum() >= 0) {
				FunctionalMetadatum fm = this.entityManager.find(
						FunctionalMetadatum.class,
						this.getFunctionalMetadatum());
				if (realQuery.indexOf("::") != -1) {
					realQuery += " & ";
				} else {
					realQuery += " :: ";
				}
				realQuery += "target.text_functional='"
						+ fm.getDescription().replaceAll("\\s", "_") + "';";
			} else if (this.getSemanticMetadatum() >= 0) {
				SemanticMetadatum sm = this.entityManager.find(
						SemanticMetadatum.class, this.getSemanticMetadatum());
				if (realQuery.indexOf("::") != -1) {
					realQuery += " & ";
				} else {
					realQuery += " :: ";
				}
				realQuery += " target.text_semantic='"
						+ sm.getDescription().replaceAll("\\s", "_") + "';";
			}
		} else {
			realQuery += stringToAdd;
		}
		this.getSketchQueries().add(realQuery);
		realQuery += "\n";
		String q = "set AutoShow off;\n" + "set ProgressBar off;\n"
				+ "set PrettyPrint off;\n" + "set Context 10 words;\n"
				+ "set LeftKWICDelim '--%%%--';\n"
				+ "set RightKWICDelim '--%%%--';\n" + "show -cpos;\n"
				+ realQuery;
		q += ";\n";
		String queryToTabulate = "A1";
		q += "tabulate " + queryToTabulate
				+ " target lemma > \"| sort | uniq -c | sort -nr > '"
				+ resTblFile.getAbsolutePath() + "'\"; ";
		return q;
	}

	private void executeCQPQuery(File queryFile) throws ExecuteException,
			IOException {
		Executor executor = new DefaultExecutor();
		File tempSh = File.createTempFile("ridireSH", ".sh");
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("export LC_ALL=C\n");
		stringBuffer.append(this.cqpExecutable + " -f "
				+ queryFile.getAbsolutePath() + " -D " + this.cqpCorpusName
				+ " -r " + this.cqpRegistry + "\n");
		FileUtils.writeStringToFile(tempSh, stringBuffer.toString());
		tempSh.setExecutable(true);
		CommandLine commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tempSh);
	}

	private void extractSingleLemmaSketches(String lemma, boolean firstWord,
			boolean twoDomains) {

	}

	private void extractSingleLemmaTwoDomainsSketches(String term) {
		this.extractSingleLemmaSketches(term, true, true);
		this.extractSingleLemmaSketches(term, false, true);
		System.out.println("");
	}

	public void extractSketches() {
		this.getSketchQueries().clear();
		for (Sketch sketch : this.sketches) {
			sketch.getSketchResults1().clear();
			sketch.getSketchResults2().clear();
		}
		this.setFirstDomain(null);
		this.setSecondDomain(null);
		if (this.getFirstWord() != null
				&& this.getFirstWord().trim().length() > 0) {
			if (this.getSecondWord() != null
					&& this.getSecondWord().trim().length() > 0) {
				this.extractTwoLemmasSketches(this.getFirstWord().trim(), this
						.getSecondWord().trim());
			} else {
				if (this.isConfFunctional() || this.isConfSemantic()) {
					this.extractSingleLemmaTwoDomainsSketches(this
							.getFirstWord().trim());
				} else {
					this.extractSingleLemmaSketches(this.getFirstWord().trim(),
							true, false);
				}
			}
		}
	}

	private void extractTwoLemmasSketches(String lemma1, String lemma2) {
		this.extractSingleLemmaSketches(lemma1, true, false);
		this.extractSingleLemmaSketches(lemma2, false, false);
	}

	public List<String> getAvailableGoodFors() {
		return this.availableGoodFors;
	}

	public Sketch getChosenSketch() {
		return this.chosenSketch;
	}

	public String getFirstDomain() {
		return this.firstDomain;
	}

	public String getFirstPoS() {
		return this.firstPoS;
	}

	public String getFirstWord() {
		return this.firstWord;
	}

	public Integer getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public List<SelectItem> getGoodSketches() {
		this.goodSketches.clear();
		Map<String, SelectItem> goodSketchesMap = new HashMap<String, SelectItem>();
		for (Sketch s : this.sketches) {
			if (s.getGoodFor().equals(this.getFirstPoS())) {
				SelectItem si = new SelectItem(s.getName());
				goodSketchesMap.put(s.getName(), si);
			}
		}
		this.goodSketches.addAll(goodSketchesMap.values());
		return this.goodSketches;
	}

	public String getScore() {
		return this.score;
	}

	public String getSecondDomain() {
		return this.secondDomain;
	}

	public String getSecondMetadatum() {
		return this.secondMetadatum;
	}

	public String getSecondPoS() {
		return this.secondPoS;
	}

	public String getSecondWord() {
		return this.secondWord;
	}

	public Integer getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public List<String> getSketchQueries() {
		return this.sketchQueries;
	}

	private double getSketchScore(long fa, long fb, long fab, long n) {
		if (this.getScore().equals(CWBCollocatesExtractor.MI_SCORE)) {
			return Math.log(fab * 1.0 * n / (fa * fb)) / Math.log(2.0);
		}
		if (this.getScore().equals(CWBCollocatesExtractor.LOG_LIKELIHOOD)) {
			return this.xlx(fab) + this.xlx(fa - fab) + this.xlx(fb - fab)
					+ this.xlx(n) + this.xlx(n + fab - fa - fb) - this.xlx(fa)
					- this.xlx(fb) - this.xlx(n - fa) - this.xlx(n - fb);
		}
		if (this.getScore().equals(CWBCollocatesExtractor.T_SCORE)) {
			return (fab - fa * fb / n) / Math.sqrt(fab);
		}
		// default is logDice
		double a = 2.0 * fab / (fa + fb);
		return 14 + Math.log(a) / Math.log(2);
	}

	public String getSketchToExtract() {
		return this.sketchToExtract;
	}

	@Create
	public void init() {
		this.cqpExecutable = this.entityManager.find(Parameter.class,
				Parameter.CQP_EXECUTABLE.getKey()).getValue();
		this.cqpCorpusName = this.entityManager.find(Parameter.class,
				Parameter.CQP_CORPUSNAME.getKey()).getValue();
		this.cqpRegistry = this.entityManager.find(Parameter.class,
				Parameter.CQP_REGISTRY.getKey()).getValue();
	}

	public boolean isConfFunctional() {
		return this.confFunctional;
	}

	public boolean isConfSemantic() {
		return this.confSemantic;
	}

	private boolean isSemanticMetadatum(String secondMetadatum2) {
		Number n = (Number) this.entityManager
				.createQuery(
						"select count(fm.id) from FunctionalMetadatum fm where fm.description=:des")
				.setParameter("des", secondMetadatum2).getSingleResult();
		if (n.intValue() > 0) {
			return false;
		}
		return true;
	}

	public void setChosenSketch(Sketch chosenSketch) {
		this.chosenSketch = chosenSketch;
	}

	public void setConfFunctional(boolean confFunctional) {
		this.confFunctional = confFunctional;
		if (confFunctional) {
			this.confSemantic = false;
		}
	}

	public void setConfSemantic(boolean confSemantic) {
		this.confSemantic = confSemantic;
		if (confSemantic) {
			this.confFunctional = false;
		}
	}

	public void setFirstDomain(String firstDomain) {
		this.firstDomain = firstDomain;
	}

	public void setFirstPoS(String firstPoS) {
		this.firstPoS = firstPoS;
	}

	public void setFirstWord(String firstWord) {
		this.firstWord = firstWord;
	}

	public void setFunctionalMetadatum(Integer functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setGoodSketches(List<SelectItem> goodSketches) {
		this.goodSketches = goodSketches;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public void setSecondDomain(String secondDomain) {
		this.secondDomain = secondDomain;
	}

	public void setSecondMetadatum(String secondMetadatum) {
		this.secondMetadatum = secondMetadatum;
	}

	public void setSecondPoS(String secondPoS) {
		this.secondPoS = secondPoS;
	}

	public void setSecondWord(String secondWord) {
		this.secondWord = secondWord;
	}

	public void setSemanticMetadatum(Integer semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSketchQueries(List<String> sketchQueries) {
		this.sketchQueries = sketchQueries;
	}

	public void setSketchToExtract(String sketchToExtract) {
		this.sketchToExtract = sketchToExtract;
		for (Sketch sk : this.sketches) {
			if (sk.getName().equals(sketchToExtract)
					&& this.getFirstPoS().equals(sk.getGoodFor())) {
				this.setChosenSketch(sk);
				break;
			}
		}
	}

	private double xlx(long l) {
		if (l == 0) {
			return 0;
		}
		return l * Math.log(l);
	}
}
