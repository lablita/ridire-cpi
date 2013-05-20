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

import java.util.concurrent.Callable;

public abstract class IndexingCommand implements Callable<IndexingResult> {
	protected boolean terminated = false;

	public abstract String getDescription();

	public abstract float getPercentage();

	public Integer getPercentageInt() {
		return Math.round(this.getPercentage() * 100);
	}

	public boolean isTerminated() {
		return this.terminated;
	}

	public void setTerminated(boolean terminated) {
		this.terminated = terminated;
	}
}
