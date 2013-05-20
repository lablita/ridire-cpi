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
package it.drwolf.ridire.session.async.callable;

import it.drwolf.ridire.index.ContextsIndexManager;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;

public class SingleResourceRemover extends IndexingCommand {
	private Integer crId;
	private ContextsIndexManager contextsIndexManager;
	private float percentage = 0.0f;

	public SingleResourceRemover(Integer crId) {
		super();
		this.crId = crId;
	}

	public IndexingResult call() {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		try {
			this.contextsIndexManager = (ContextsIndexManager) Component
					.getInstance("contextsIndexManager");
			this.contextsIndexManager.deleteSingleCrawledResource(this.crId);
			this.percentage = 1.0f;
		} catch (Exception e) {
			e.printStackTrace();
		}
		Lifecycle.endCall();
		this.setTerminated(true);
		return indexingResult;
	}

	public String getDescription() {
		return "Removing single resource: " + this.crId;
	}

	public float getPercentage() {
		return this.percentage;
	}

}
