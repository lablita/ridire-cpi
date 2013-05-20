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
package it.drwolf.ridire.session.async.factories;

import it.drwolf.ridire.session.async.Mapper;

import java.util.concurrent.ThreadFactory;

public class PoolThreadFactory implements ThreadFactory {

	private int priority;

	public PoolThreadFactory(int priority) {
		super();
		this.priority = priority;
	}

	public Thread newThread(Runnable runnable) {
		Thread t = new Thread(runnable);
		if (runnable instanceof Mapper) {
			Mapper m = (Mapper) runnable;
			t.setName(m.getJobName());
		}
		t.setPriority(this.priority);
		return t;
	}

}
