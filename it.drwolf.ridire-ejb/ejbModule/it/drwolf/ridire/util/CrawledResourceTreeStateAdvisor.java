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

import it.drwolf.ridire.entity.CrawledResource;

import org.jboss.seam.annotations.Name;
import org.richfaces.component.UITree;
import org.richfaces.component.state.TreeStateAdvisor;

@Name("crawledResourceTreeStateAdvisor")
public class CrawledResourceTreeStateAdvisor implements TreeStateAdvisor {

	public Boolean adviseNodeOpened(UITree tree) {
		CrawledResource item = (CrawledResource) tree.getNodeFacet()
				.getDragValue();
		if (item.getChildren().size() > 0) {
			return Boolean.TRUE;
		}
		return Boolean.FALSE;
	}

	public Boolean adviseNodeSelected(UITree tree) {
		return null;
	}

}
