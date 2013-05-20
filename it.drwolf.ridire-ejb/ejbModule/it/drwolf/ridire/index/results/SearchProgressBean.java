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
package it.drwolf.ridire.index.results;

import java.util.ArrayList;
import java.util.List;

import org.jboss.seam.annotations.Name;

@Name("searchProgressBean")
public class SearchProgressBean {
	private List<String> messages = new ArrayList<String>();

	public void addMessage(String message) {
		this.messages.add(message);
	}

	public void clearMessages() {
		this.messages.clear();
	}

	public List<String> getMessages() {
		return this.messages;
	}

	public void setMessages(List<String> messages) {
		this.messages = messages;
	}
}
