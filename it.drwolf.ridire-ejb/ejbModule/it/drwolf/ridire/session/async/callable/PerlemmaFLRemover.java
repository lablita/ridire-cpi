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
package it.drwolf.ridire.session.async.callable;

import it.drwolf.ridire.index.ContextsIndexManager;

import org.jboss.seam.Component;
import org.jboss.seam.contexts.Lifecycle;

public class PerlemmaFLRemover extends IndexingCommand {

	private ContextsIndexManager contextsIndexManager;

	public IndexingResult call() throws Exception {
		IndexingResult indexingResult = new IndexingResult();
		Lifecycle.beginCall();
		System.out.println("Removing perlemmaFL started.");
		this.contextsIndexManager = (ContextsIndexManager) Component
				.getInstance("contextsIndexManager");
		this.contextsIndexManager.openIndexWriterWithoutCreating();
		this.contextsIndexManager.removePerlemmaFl();
		this.contextsIndexManager.closeIndexWriter();
		Lifecycle.endCall();
		this.setTerminated(true);
		System.out.println("Removing perlemmaFL finished.");
		return indexingResult;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public float getPercentage() {
		// TODO Auto-generated method stub
		return 0;
	}

}
