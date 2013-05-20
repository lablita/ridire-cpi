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
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.index.results.Sketch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.MMapDirectory;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("sketchRetriever")
@Scope(ScopeType.CONVERSATION)
public class SketchRetriever {

	@In
	private EntityManager entityManager;

	private IndexReader indexReader1;
	private IndexReader indexReader2;
	private String sketchToExtract = null;

	private String lemma;
	private String pos;

	private String domain;

	private List<SelectItem> sketchesSI = new ArrayList<SelectItem>();

	private Integer functionalMetadatum = -1;

	private Integer semanticMetadatum = -1;

	private List<List<String[]>> tableResults = new ArrayList<List<String[]>>();
	private List<SketchTable> sketchTables = new ArrayList<SketchTable>();
	private List<SketchTable> sketchTablesFirst = new ArrayList<SketchTable>();
	private List<SketchTable> sketchTablesSecond = new ArrayList<SketchTable>();
	private List<SketchTable> sketchTablesThird = new ArrayList<SketchTable>();

	static List<String> prepOrderList = new ArrayList<String>() {
		{
			this.add("pp_a");
			this.add("pp_ad");
			this.add("pp_al");
			this.add("pp_all'");
			this.add("pp_alla");
			this.add("NONE-1");
			this.add("pp_con");
			this.add("pp_col");
			this.add("NONE-2");
			this.add("pp_da");
			this.add("pp_dal");
			this.add("pp_dall'");
			this.add("pp_dalla");
			this.add("NONE-3");
			this.add("pp_di");
			this.add("pp_de");
			this.add("pp_del");
			this.add("pp_dell'");
			this.add("pp_della");
			this.add("NONE-4");
			this.add("pp_fra");
			this.add("NONE-5");
			this.add("pp_in");
			this.add("pp_nel");
			this.add("pp_nell'");
			this.add("pp_nella");
			this.add("NONE-6");
			this.add("pp_per");
			this.add("NONE-7");
			this.add("pp_su");
			this.add("pp_sul");
			this.add("pp_sull'");
			this.add("pp_sulla");
			this.add("NONE-8");
			this.add("pp_tra");
			this.add("NONEZ-9");
			this.add("pp_accanto");
			this.add("pp_assieme");
			this.add("pp_attorno");
			this.add("pp_attraverso");
			this.add("pp_causa");
			this.add("pp_circa");
			this.add("pp_contro");
			this.add("pp_da|di");
			this.add("pp_dentro");
			this.add("pp_davanti");
			this.add("pp_dinanzi");
			this.add("pp_durante");
			this.add("pp_entro");
			this.add("pp_dietro");
			this.add("pp_dopo");
			this.add("pp_fin");
			this.add("pp_fino");
			this.add("pp_fuori");
			this.add("pp_innanzi");
			this.add("pp_insieme");
			this.add("pp_intorno");
			this.add("pp_lungo");
			this.add("pp_mediante");
			this.add("pp_oltre");
			this.add("pp_nonostante");
			this.add("pp_presso");
			this.add("pp_pro");
			this.add("pp_rispetto");
			this.add("pp_secondo");
			this.add("pp_senza");
			this.add("pp_sino");
			this.add("pp_sopra");
			this.add("pp_sotto");
			this.add("pp_tramite");
			this.add("pp_tranne");
			this.add("pp_verso");
			this.add("pp_via");
		}
	};

	static List<String> nounOrderList = new ArrayList<String>() {
		{
			this.add("postN_V");
			this.add("preN_V");
			this.add("AofN");
			this.add("NONE-00");
			this.add("n_modifies");
			this.add("n_modifier");
			this.add("e_o");
			this.add("NONEY-01");
			this.addAll(SketchRetriever.prepOrderList);
		}
	};

	static List<String> adjectiveOrderList = new ArrayList<String>() {
		{
			this.add("NofA");
			this.add("e_o");
			this.add("NONEY-01");
			this.addAll(SketchRetriever.prepOrderList);
		}
	};

	static List<String> verbOrderList = new ArrayList<String>() {
		{
			this.add("preV_N");
			this.add("postV_N");
			this.add("postV_ADV");
			this.add("e_o");
			this.add("NONEY-01");
			this.addAll(SketchRetriever.prepOrderList);
		}
	};

	static List<String> adverbOrderList = new ArrayList<String>() {
		{
			this.add("preADV_V");
			this.add("e_o");
			this.add("NONEY-01");
			this.addAll(SketchRetriever.prepOrderList);
		}
	};

	private boolean noResults = false;
	private boolean allDomains = true;

	private boolean sketch1 = true;

	private void compactResults() {
		int position = 1;
		Iterator<SketchTable> itOnSketchTables = this.sketchTables.iterator();
		boolean first = true;
		boolean second = false;
		while (itOnSketchTables.hasNext()) {
			SketchTable next = itOnSketchTables.next();
			if (next.getSketchName().startsWith("NONEY")) {
				first = false;
				second = true;
				position = 1;
			} else if (next.getSketchName().startsWith("NONEZ")) {
				first = false;
				second = false;
				position = 1;
			}
			next.setPosition(position);
			if (!next.getSketchName().startsWith("NONE")
					&& next.getRows().size() < 1) {
				itOnSketchTables.remove();
			} else {
				if (first) {
					this.sketchTablesFirst.add(next);
				} else if (second) {
					this.sketchTablesSecond.add(next);
					if (position % 4 == 0
							|| next.getSketchName().startsWith("NONE")) {
						position = 1;
						if (!next.getSketchName().startsWith("NONE")) {
							this.sketchTablesSecond.add(new SketchTable(
									"NONE-S"));
						}
					}
				} else {
					this.sketchTablesThird.add(next);
				}
				if (!next.getSketchName().startsWith("NONE")) {
					++position;
				}
			}
		}

	}

	public String getDomain() {
		return this.domain;
	}

	public Integer getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public String getLemma() {
		return this.lemma;
	}

	public String getPos() {
		return this.pos;
	}

	public Integer getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	private void getSketchesFromIndex(IndexReader reader) {
		this.noResults = false;
		BooleanQuery bq = new BooleanQuery();
		TermQuery tqLemma = new TermQuery(new Term("lemma", this.getLemma()));
		bq.add(tqLemma, Occur.MUST);
		if (this.getFunctionalMetadatum() >= 0) {
			FunctionalMetadatum fm = this.entityManager.find(
					FunctionalMetadatum.class, this.getFunctionalMetadatum());
			TermQuery funcQuery = new TermQuery(new Term("functional",
					fm.getDescription()));
			bq.add(funcQuery, Occur.MUST);
		} else if (this.getSemanticMetadatum() >= 0) {
			SemanticMetadatum sm = this.entityManager.find(
					SemanticMetadatum.class, this.getSemanticMetadatum());
			TermQuery semQuery = new TermQuery(new Term("semantic",
					sm.getDescription()));
			bq.add(semQuery, Occur.MUST);
		}
		if (this.getSemanticMetadatum() < 0
				&& this.getFunctionalMetadatum() < 0) {
			TermQuery allCorporaQuery = new TermQuery(new Term("allcorpora",
					"yes"));
			bq.add(allCorporaQuery, Occur.MUST);
		}
		if (!this.getSketchToExtract().equals("Tutti")) {
			if (this.getSketchToExtract().startsWith("pp_")) {
				PrefixQuery prefixQuery = new PrefixQuery(new Term("sketch",
						"pp_"));
				bq.add(prefixQuery, Occur.MUST);
			} else {
				TermQuery sq = new TermQuery(new Term("sketch",
						this.getSketchToExtract()));
				bq.add(sq, Occur.MUST);
			}
		}
		IndexSearcher indexSearcher = new IndexSearcher(reader);
		TopDocs results = null;
		try {
			results = indexSearcher.search(bq, Integer.MAX_VALUE);
			if (results != null) {
				if (results.totalHits == 0) {
					this.noResults = true;
				}
				List<String> orderList = SketchRetriever.nounOrderList;
				if (this.getPos().equals("verbo")) {
					orderList = SketchRetriever.verbOrderList;
				} else if (this.getPos().equals("aggettivo")) {
					orderList = SketchRetriever.adjectiveOrderList;
				} else if (this.getPos().equals("avverbio")) {
					orderList = SketchRetriever.adverbOrderList;
				}
				this.sketchTables.clear();
				this.sketchTablesFirst.clear();
				this.sketchTablesSecond.clear();
				this.sketchTablesThird.clear();
				for (String n : orderList) {
					this.sketchTables.add(new SketchTable(n));
				}
				for (int i = 0; i < results.totalHits; i++) {
					Document d = reader.document(results.scoreDocs[i].doc);
					String sketch = d.get("sketch");
					String tabella = d.get("tabella");
					String overallFrequency = d.get("overallfrequency");
					String goodFor = d.get("goodFor");
					if (goodFor != null && !goodFor.equals(this.getPos())) {
						continue;
					}
					// HACK: change table names
					String sketchName = sketch.trim();
					if (this.sketch1) {
						if (sketchName.equals("AofN")) {
							sketchName = "NofA";
						} else if (sketchName.equals("NofA")) {
							sketchName = "AofN";
						} else if (sketchName.equals("preADV_V")) {
							sketchName = "postV_ADV";
						} else if (sketchName.equals("postV_ADV")) {
							sketchName = "preADV_V";
						}
					}
					if (!SketchList.isSketchNameGoodFor(sketchName,
							this.getPos())) {
						continue;
					}
					int index = orderList.indexOf(sketchName);
					SketchTable sketchTable = this.sketchTables.get(index);
					sketchTable.setGlobalFrequency(Integer
							.parseInt(overallFrequency.trim()));
					String[] righe = StringUtils.split(tabella, "\n");
					for (int j = 0; j < Math.min(20, righe.length); j++) {
						SketchResultRow sketchResultRow = new SketchResultRow();
						String[] tokens = StringUtils.split(righe[j], "\t");
						sketchResultRow.setItem(tokens[0].trim());
						sketchResultRow.setFrequency(Integer.parseInt(tokens[1]
								.trim()));
						sketchResultRow.setScore(Double.parseDouble(tokens[2]
								.trim()));
						sketchTable.getRows().add(sketchResultRow);
					}
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.compactResults();
	}

	public void getSketchesFromIndex1() {
		this.sketch1 = true;
		try {
			IndexReader newReader = IndexReader
					.openIfChanged(this.indexReader1);
			if (newReader != null) {
				this.indexReader1.close();
				this.indexReader1 = newReader;
			}
			this.getSketchesFromIndex(this.indexReader1);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public void getSketchesFromIndex2() {
		this.sketch1 = false;
		try {
			IndexReader newReader = IndexReader
					.openIfChanged(this.indexReader2);
			if (newReader != null) {
				this.indexReader2.close();
				this.indexReader2 = newReader;
			}
			this.getSketchesFromIndex(this.indexReader2);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public List<SelectItem> getSketchesSI() {
		return this.sketchesSI;
	}

	public List<SketchTable> getSketchTables() {
		return this.sketchTables;
	}

	public List<SketchTable> getSketchTablesFirst() {
		return this.sketchTablesFirst;
	}

	public List<SketchTable> getSketchTablesSecond() {
		return this.sketchTablesSecond;
	}

	public List<SketchTable> getSketchTablesThird() {
		return this.sketchTablesThird;
	}

	public String getSketchToExtract() {
		return this.sketchToExtract;
	}

	public List<List<String[]>> getTableResults() {
		return this.tableResults;
	}

	@Create
	public void init() {
		try {
			String indexLocation1 = this.entityManager.find(Parameter.class,
					Parameter.SKETCH_INDEX_LOCATION.getKey()).getValue();
			String indexLocation2 = this.entityManager.find(Parameter.class,
					Parameter.SKETCH_INDEX_LOCATION2.getKey()).getValue();
			this.indexReader1 = IndexReader.open(new MMapDirectory(new File(
					indexLocation1)), true);
			this.indexReader2 = IndexReader.open(new MMapDirectory(new File(
					indexLocation2)), true);
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.initSketchesSI();
	}

	private void initSketchesSI() {
		this.sketchesSI.add(new SelectItem("Tutti"));
		Set<String> skeNames = new HashSet<String>();
		for (Sketch s : SketchList.getSketches()) {
			skeNames.add(s.getName());
		}
		this.sketchesSI.add(new SelectItem("postV_N"));
		this.sketchesSI.add(new SelectItem("preV_N"));
		this.sketchesSI.add(new SelectItem("preN_V"));
		this.sketchesSI.add(new SelectItem("postN_V"));
		this.sketchesSI.add(new SelectItem("pp_%"));
		this.sketchesSI.add(new SelectItem("n_modifies"));
		this.sketchesSI.add(new SelectItem("n_modifier"));
		this.sketchesSI.add(new SelectItem("NofA"));
		this.sketchesSI.add(new SelectItem("AofN"));
		this.sketchesSI.add(new SelectItem("postV_ADV"));
		this.sketchesSI.add(new SelectItem("preADV_V"));
		this.sketchesSI.add(new SelectItem("e_o"));
	}

	public boolean isAllDomains() {
		return this.allDomains;
	}

	public boolean isNoResults() {
		return this.noResults;
	}

	public void setAllDomains(boolean allDomains) {
		this.allDomains = allDomains;
	}

	public void setDomain(String domain) {
		this.domain = domain;
		if (domain != null) {
			List<FunctionalMetadatum> fs = this.entityManager
					.createQuery(
							"from FunctionalMetadatum f where f.description=:d")
					.setParameter("d", domain).getResultList();
			if (fs.size() == 1) {
				this.setFunctionalMetadatum(fs.get(0).getId());
				this.setSemanticMetadatum(-1);
				return;
			}
			List<SemanticMetadatum> ss = this.entityManager
					.createQuery(
							"from SemanticMetadatum s where s.description=:d")
					.setParameter("d", domain).getResultList();
			if (ss.size() == 1) {
				this.setSemanticMetadatum(ss.get(0).getId());
				this.setFunctionalMetadatum(-1);
				return;
			}
		}
		this.setFunctionalMetadatum(-1);
		this.setSemanticMetadatum(-1);
	}

	public void setFunctionalMetadatum(Integer functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;
	}

	public void setNoResults(boolean noResults) {
		this.noResults = noResults;
	}

	public void setPos(String pos) {
		this.pos = pos;
	}

	public void setSemanticMetadatum(Integer semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSketchesSI(List<SelectItem> sketchesSI) {
		this.sketchesSI = sketchesSI;
	}

	public void setSketchTables(List<SketchTable> sketchTables) {
		this.sketchTables = sketchTables;
	}

	public void setSketchTablesFirst(List<SketchTable> sketchTablesFirst) {
		this.sketchTablesFirst = sketchTablesFirst;
	}

	public void setSketchTablesSecond(List<SketchTable> sketchTablesSecond) {
		this.sketchTablesSecond = sketchTablesSecond;
	}

	public void setSketchTablesThird(List<SketchTable> sketchTablesThird) {
		this.sketchTablesThird = sketchTablesThird;
	}

	public void setSketchToExtract(String sketchToExtract) {
		this.sketchToExtract = sketchToExtract;
	}

	public void setTableResults(List<List<String[]>> tableResults) {
		this.tableResults = tableResults;
	}
}
