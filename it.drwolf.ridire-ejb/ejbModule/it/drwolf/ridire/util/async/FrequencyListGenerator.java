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
package it.drwolf.ridire.util.async;

import it.drwolf.ridire.index.ContextAnalyzer;
import it.drwolf.ridire.index.ContextsIndexManager;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermFreqVector;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PrefixQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHitCountCollector;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;

@Name("frequencyListGenerator")
public class FrequencyListGenerator {

	private static final int BATCH_SIZE = 10000;
	@In(create = true)
	private ContextsIndexManager contextsIndexManager;

	private Map<String, Integer> getBareTable(List<String> corporaNames,
			String functionalMetadatumDescription,
			String semanticMetadatumDescription, String frequencyBy)
			throws IOException {
		Map<String, Integer> fl = new HashMap<String, Integer>();
		Query q = new BooleanQuery();
		if (corporaNames != null && corporaNames.size() > 0
				&& !(corporaNames.size() == 1 && corporaNames.get(0) == null)) {
			BooleanQuery corporaQuery = new BooleanQuery();
			for (String cn : corporaNames) {
				if (cn != null) {
					corporaQuery.add(new TermQuery(new Term("corpus", cn)),
							Occur.SHOULD);
				}
			}
			((BooleanQuery) q).add(corporaQuery, Occur.MUST);
		}
		if (functionalMetadatumDescription != null) {
			TermQuery funcQuery = new TermQuery(new Term("functionalMetadatum",
					functionalMetadatumDescription));
			((BooleanQuery) q).add(funcQuery, Occur.MUST);
		}
		if (semanticMetadatumDescription != null) {
			TermQuery semaQuery = new TermQuery(new Term("semanticMetadatum",
					semanticMetadatumDescription));
			((BooleanQuery) q).add(semaQuery, Occur.MUST);
		}
		PrefixQuery prefixQuery = new PrefixQuery(new Term("performaFL", ""));
		((BooleanQuery) q).add(prefixQuery, Occur.MUST);
		IndexSearcher indexSearcher = this.contextsIndexManager
				.getIndexSearcherR();
		System.out.println("Starting FL calculation");
		TotalHitCountCollector totalHitCountCollector = new TotalHitCountCollector();
		indexSearcher.search(q, null, totalHitCountCollector);
		int totalHits = totalHitCountCollector.getTotalHits();
		System.out.println("Frequency list calculation. Docs to be processed: "
				+ totalHits);
		ScoreDoc after = null;
		int docsProcessed = 0;
		for (int j = 0; j < totalHits; j += FrequencyListGenerator.BATCH_SIZE) {
			TopDocs topDocs = null;
			if (after == null) {
				topDocs = indexSearcher.search(q,
						FrequencyListGenerator.BATCH_SIZE);
			} else {
				topDocs = indexSearcher.searchAfter(after, q,
						FrequencyListGenerator.BATCH_SIZE);
			}
			StrTokenizer strTokenizer = new StrTokenizer();
			strTokenizer.setDelimiterString(ContextAnalyzer.SEPARATOR);
			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			if (scoreDocs != null) {
				for (ScoreDoc scoreDoc : scoreDocs) {
					++docsProcessed;
					after = scoreDoc;
					TermFreqVector termFreqVector = indexSearcher
							.getIndexReader().getTermFreqVector(scoreDoc.doc,
									"performaFL");
					if (termFreqVector == null) {
						continue;
					}
					String[] terms = termFreqVector.getTerms();
					int[] frequencies = termFreqVector.getTermFrequencies();
					for (int i = 0; i < terms.length; i++) {
						String term = terms[i];
						String[] tokenArray = strTokenizer.reset(term)
								.getTokenArray();
						if (tokenArray.length != 3) {
							continue;
						}
						String pos = tokenArray[1];
						String lemma = tokenArray[2];
						if (lemma.equals("<unknown>")) {
							lemma = tokenArray[0];
						}
						if (frequencyBy.equals("forma")) {
							term = tokenArray[0];
						} else if (frequencyBy.equals("lemma")) {
							term = lemma;
						} else if (frequencyBy.equals("PoS-lemma")) {
							if (pos.startsWith("VER")) {
								pos = "VER";
							}
							term = pos + " / " + lemma;
						} else if (frequencyBy.equals("PoS-forma")) {
							if (pos.startsWith("VER")) {
								pos = "VER";
							}
							term = pos + " / " + tokenArray[0];
						} else {
							term = tokenArray[1];
						}
						Integer count = fl.get(term);
						if (count == null) {
							fl.put(term, frequencies[i]);
						} else {
							fl.put(term, frequencies[i] + count);
						}
					}
					if (docsProcessed % 1000 == 0) {
						System.out
								.println("Frequency list calculation. Docs processed: "
										+ docsProcessed
										+ " on total: "
										+ totalHits
										+ " ("
										+ docsProcessed
										* 100.0f / totalHits + "%)");
					}
				}
			}
		}
		return fl;
	}

	@Asynchronous
	public String saveFrequencyListToFile(
			FrequencyListDataGenerator frequencyListDataGenerator)
			throws IOException {
		Map<String, Integer> fl = this.getBareTable(
				frequencyListDataGenerator.getCorporaNames(),
				frequencyListDataGenerator.getFunctionalMetadatumDescription(),
				frequencyListDataGenerator.getSemanticMetadatumDescription(),
				frequencyListDataGenerator.getFrequencyBy());
		File file = File.createTempFile("ridireFL-"
				+ frequencyListDataGenerator + "-" + new Date(), ".txt");
		for (String k : fl.keySet()) {
			FileUtils.writeStringToFile(file, k + "\t" + fl.get(k) + "\n",
					null, true);
		}
		System.out.println("Frequency list calculation. Done.");
		return "OK";
	}
}
