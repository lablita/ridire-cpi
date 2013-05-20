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
package it.drwolf.ridire.entity;

import static javax.persistence.GenerationType.AUTO;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

@Entity
public class FunctionalMetadatum {
	private static final String ECONOMY = "Economia e Affari";

	private static final String NEWS = "Informazione";

	private static final String LAW = "Amministrazione e Legislazione";

	private Integer id;
	private String description;
	private Set<LocalResource> localResources = new HashSet<LocalResource>(0);
	public static String[] DEFAULT = new String[] { ECONOMY, NEWS, LAW };

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof FunctionalMetadatum)) {
			return false;
		}
		FunctionalMetadatum other = (FunctionalMetadatum) obj;
		if (this.id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!this.id.equals(other.id)) {
			return false;
		}
		return true;
	}

	public String getDescription() {
		return this.description;
	}

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "functionalMetadatum")
	public Set<LocalResource> getLocalResources() {
		return this.localResources;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (this.id == null ? 0 : this.id.hashCode());
		return result;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setLocalResources(Set<LocalResource> localResources) {
		this.localResources = localResources;
	}

}
