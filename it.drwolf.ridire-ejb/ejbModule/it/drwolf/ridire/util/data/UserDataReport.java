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
package it.drwolf.ridire.util.data;

import java.util.ArrayList;
import java.util.List;

public class UserDataReport implements Comparable<UserDataReport> {
	private String username;
	private String nome;
	private String cognome;
	private List<Long> values = new ArrayList<Long>();

	public UserDataReport(List<Long> values, String username, String nome,
			String cognome) {
		super();
		this.values = values;
		this.username = username;
		this.nome = nome;
		this.cognome = cognome;
	}

	public int compareTo(UserDataReport o) {
		if (this.getValues() != null && this.getValues().size() > 0
				&& o.getValues() != null && o.getValues().size() > 0) {
			return this.getValues().get(0).compareTo(o.getValues().get(0));
		}
		return 1;
	}

	public String getCognome() {
		return this.cognome;
	}

	public String getNome() {
		return this.nome;
	}

	public String getUsername() {
		return this.username;
	}

	public List<Long> getValues() {
		return this.values;
	}

	public void setCognome(String cognome) {
		this.cognome = cognome;
	}

	public void setNome(String nome) {
		this.nome = nome;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setValue(List<Long> values) {
		this.values = values;
	}
}
