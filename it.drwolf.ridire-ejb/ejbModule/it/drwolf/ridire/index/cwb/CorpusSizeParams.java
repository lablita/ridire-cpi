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
package it.drwolf.ridire.index.cwb;

import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.SemanticMetadatum;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("corpusSizeParams")
@Scope(ScopeType.APPLICATION)
public class CorpusSizeParams {
	private Map<String, Number> corporaSize = new HashMap<String, Number>();
	private List<String> availablePoSs = new ArrayList<String>();
	private List<String> availableEasyposs = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -8966088492523974606L;

		{
			this.add("Tutti");
			this.add("ADJ");
			this.add("ADJPRO");
			this.add("ADV");
			this.add("ADV:mente");
			this.add("ART");
			this.add("CHE");
			this.add("CL");
			this.add("CON");
			this.add("NEG");
			this.add("NOCAT");
			this.add("NOUN");
			this.add("NPR");
			this.add("NUM");
			this.add("PREP");
			this.add("PRON");
			this.add("PUN");
			this.add("SENT");
			this.add("VERB");
			this.add("WH");
		}
	};

	@In
	private EntityManager entityManager;

	public List<String> getAvailableEasyposs() {
		return this.availableEasyposs;
	}

	public List<String> getAvailablePoSs() {
		Collections.sort(this.availablePoSs);
		this.availablePoSs.remove("Tutti");
		this.availablePoSs.add(0, "Tutti");
		return this.availablePoSs;
	}

	public Number getCorpusSize(String name) {
		if (name != null && this.corporaSize.containsKey(name)) {
			return this.corporaSize.get(name);
		}
		return 0;
	}

	@SuppressWarnings("unchecked")
	public void init() {
		try {
			this.corporaSize.put("lemma_all", ((Number) this.entityManager
					.createNativeQuery(
							"select sum(freq) as somma from freq_lemma_all")
					.getSingleResult()).longValue());
			this.corporaSize.put("forma_all", ((Number) this.entityManager
					.createNativeQuery(
							"select sum(freq) as somma from freq_forma_all")
					.getSingleResult()).longValue());
			this.corporaSize.put("PoS_all", ((Number) this.entityManager
					.createNativeQuery(
							"select sum(freq) as somma from freq_PoS_all")
					.getSingleResult()).longValue());
			List<SemanticMetadatum> resultList = this.entityManager
					.createQuery("from SemanticMetadatum sm").getResultList();
			for (SemanticMetadatum sm : resultList) {
				String smDes = sm.getDescription().replaceAll("\\s", "_");
				this.corporaSize.put("lemma_" + smDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_lemma_"
										+ smDes).getSingleResult());
				this.corporaSize.put("forma_" + smDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_forma_"
										+ smDes).getSingleResult());
				this.corporaSize.put("PoS_" + smDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_PoS_"
										+ smDes).getSingleResult());
			}
			List<FunctionalMetadatum> resultList2 = this.entityManager
					.createQuery("from FunctionalMetadatum fm").getResultList();
			for (FunctionalMetadatum fm : resultList2) {
				String fmDes = fm.getDescription().replaceAll("\\s", "_");
				this.corporaSize.put("lemma_" + fmDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_lemma_"
										+ fmDes).getSingleResult());
				this.corporaSize.put("lemma_" + fmDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_forma_"
										+ fmDes).getSingleResult());
				this.corporaSize.put("lemma_" + fmDes,
						(Number) this.entityManager.createNativeQuery(
								"select sum(freq) as somma from freq_PoS_"
										+ fmDes).getSingleResult());
			}
			this.availablePoSs
					.addAll(this.entityManager
							.createNativeQuery(
									"select item from freq_PoS_all where item not like 'VER%' and item not like 'AUX%'")
							.getResultList());
		} catch (Exception e) {
			e.printStackTrace();
		}
		this.availablePoSs.add("VER");
		this.availablePoSs.add("AUX");
	}

	public void setAvailableEasyposs(List<String> availableEasyposs) {
		this.availableEasyposs = availableEasyposs;
	}
}
