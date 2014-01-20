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
package it.drwolf.ridire.util;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.Name;

@Name("frequencyListManager")
public class FrequencyListManager {
	private final List<DownloadableFrequencyList> listsFunctional = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -5706807262990007662L;

		{
			this.add(new DownloadableFrequencyList(
					"Amministrazione_e_Legislazione", 4975230,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Economia_e_Affari",
					4519731, DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Informazione", 7573329,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Informazione (TSV)",
					5373309, DownloadableFrequencyList.ZIP));
		}
	};

	private final List<DownloadableFrequencyList> listsSemantic = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7342164625660265707L;

		{
			this.add(new DownloadableFrequencyList("Architettura_e_Design",
					4975230, DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Arti_figurative", 6461302,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Cinema", 3021985,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Cucina", 2644094,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Letteratura_e_Teatro",
					4519731, DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Moda", 7573329,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Musica", 5181815,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Religione", 3570831,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Sport", 5238954,
					DownloadableFrequencyList.ODS));
		}
	};

	private final List<DownloadableFrequencyList> listsAllDomains = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6168493729873494830L;

		{
			this.add(new DownloadableFrequencyList("TOTALE", 8619943,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("TOTALE (TSV)", 18915460,
					DownloadableFrequencyList.ZIP));
		}
	};

	private final List<DownloadableFrequencyList> listsAll = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7878253616690377456L;

		{
			this.add(new DownloadableFrequencyList("RIDIRE_LF", 3680314,
					DownloadableFrequencyList.ZIP));
		}
	};

	public List<DownloadableFrequencyList> getListsAll() {
		return this.listsAll;
	}

	public List<DownloadableFrequencyList> getListsAllDomains() {
		return this.listsAllDomains;
	}

	public List<DownloadableFrequencyList> getListsFunctional() {
		return this.listsFunctional;
	}

	public List<DownloadableFrequencyList> getListsSemantic() {
		return this.listsSemantic;
	}

}
