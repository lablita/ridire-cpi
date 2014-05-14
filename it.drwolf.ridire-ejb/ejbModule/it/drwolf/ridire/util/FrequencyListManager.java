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
					"Amministrazione_e_Legislazione", 358199,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Economia_e_Affari", 370486,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Informazione", 378166,
					DownloadableFrequencyList.ODS));
		}
	};

	private final List<DownloadableFrequencyList> listsSemantic = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7342164625660265707L;

		{
			this.add(new DownloadableFrequencyList("Architettura_e_Design",
					344014, DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Arti_figurative", 339527,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Cinema", 315096,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Cucina", 306136,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Letteratura_e_Teatro",
					387953, DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Moda", 301495,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Musica", 346376,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Religione", 292900,
					DownloadableFrequencyList.ODS));
			this.add(new DownloadableFrequencyList("Sport", 321673,
					DownloadableFrequencyList.ODS));
		}
	};

	private final List<DownloadableFrequencyList> listsAllDomains = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -6168493729873494830L;

		{
			this.add(new DownloadableFrequencyList("TOTALE", 536844,
					DownloadableFrequencyList.ODS));
		}
	};

	private final List<DownloadableFrequencyList> listsAll = new ArrayList<DownloadableFrequencyList>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 7878253616690377456L;

		{
			this.add(new DownloadableFrequencyList("RIDIRE_LF", 4561682,
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
