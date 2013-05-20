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

import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.SESSION)
@Name("frequencyListDataGenerator")
public class FrequencyListDataGenerator {
	private List<String> corporaNames;
	private String functionalMetadatumDescription;
	private String semanticMetadatumDescription;
	private String frequencyBy;

	public List<String> getCorporaNames() {
		return this.corporaNames;
	}

	public String getFrequencyBy() {
		return this.frequencyBy;
	}

	public String getFunctionalMetadatumDescription() {
		return this.functionalMetadatumDescription;
	}

	public String getSemanticMetadatumDescription() {
		return this.semanticMetadatumDescription;
	}

	public void setCorporaNames(List<String> corporaNames) {
		this.corporaNames = corporaNames;
	}

	public void setFrequencyBy(String frequencyBy) {
		this.frequencyBy = frequencyBy;
	}

	public void setFunctionalMetadatumDescription(
			String functionalMetadatumDescription) {
		this.functionalMetadatumDescription = functionalMetadatumDescription;
	}

	public void setSemanticMetadatumDescription(
			String semanticMetadatumDescription) {
		this.semanticMetadatumDescription = semanticMetadatumDescription;
	}

	@Override
	public String toString() {
		String join = StringUtils.join(this.getCorporaNames(), "-");
		String string = join
				+ (this.getSemanticMetadatumDescription() == null ? "" : this
						.getSemanticMetadatumDescription())
				+ "-"
				+ (this.getFunctionalMetadatumDescription() == null ? "" : this
						.getFunctionalMetadatumDescription()) + "-"
				+ this.getFrequencyBy();
		return string;
	}
}
