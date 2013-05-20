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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.session.async.callable.IndexingCommand;
import it.drwolf.ridire.session.async.callable.IndexingResult;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.collections.buffer.BoundedFifoBuffer;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.security.Restrict;
import org.primefaces.model.LazyDataModel;

@Name("indexerService")
@Scope(ScopeType.APPLICATION)
public class IndexerService {

	private CompletionService<IndexingResult> completionService;
	private LazyDataModel<IndexingCommand> lazyIndexingCommand = new LazyDataModel<IndexingCommand>() {

		@Override
		public List<IndexingCommand> fetchLazyData(int arg0, int arg1) {
			if (arg0 >= 0 && arg0 < IndexerService.this.indexingCommands.size()
					&& arg1 > 0
					&& arg1 < IndexerService.this.indexingCommands.size()
					&& arg0 < arg1) {
				return IndexerService.this.indexingCommands.subList(arg0, arg1);
			}
			return null;
		}

		@Override
		public int getRowCount() {
			return IndexerService.this.indexingCommands.size();
		}
	};

	private List<IndexingCommand> indexingCommands = new ArrayList<IndexingCommand>();

	public List<IndexingCommand> getIndexingCommands() {
		List<IndexingCommand> nl = new ArrayList<IndexingCommand>();
		nl.addAll(this.indexingCommands);
		return nl;
	}

	public LazyDataModel<IndexingCommand> getLazyIndexingCommand() {
		return this.lazyIndexingCommand;
	}

	@Create
	public void init() {
		ExecutorService executorService = Executors.newSingleThreadExecutor();
		this.completionService = new ExecutorCompletionService<IndexingResult>(
				executorService);
	}

	@SuppressWarnings("unchecked")
	@Restrict("#{s:hasRole('Indexer')}")
	public void removeCompletedCommands() {
		List<IndexingCommand> notToBePurgedCommands = new ArrayList<IndexingCommand>();
		Iterator<IndexingCommand> itOnCommands = this.indexingCommands
				.iterator();
		while (itOnCommands.hasNext()) {
			IndexingCommand ic = itOnCommands.next();
			if (!ic.isTerminated()) {
				notToBePurgedCommands.add(0, ic);
			}
		}
		this.indexingCommands.clear();
		this.indexingCommands.addAll(notToBePurgedCommands);
	}

	public void setIndexingCommands(BoundedFifoBuffer indexingCommands) {
		return;
	}

	public void setLazyIndexingCommand(
			LazyDataModel<IndexingCommand> lazyIndexingCommand) {
		this.lazyIndexingCommand = lazyIndexingCommand;
	}

	public void submitCallable(IndexingCommand task) {
		this.completionService.submit(task);
		this.indexingCommands.add(task);
	}
}
