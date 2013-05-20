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
package it.drwolf.ridire.session;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.SESSION)
@Name("menuState")
public class MenuState {

	private Map<String, Boolean> menu;

	private String selectedMenuItem;

	@In
	private EntityManager entityManager;

	public MenuState() {
	}

	public Map<String, Boolean> getMenu() {
		return this.menu;
	}

	public String getSelectedMenuItem() {
		return this.selectedMenuItem;
	}

	@PostConstruct
	public void init() {
		this.menu = new HashMap<String, Boolean>();
		this.menu.put("ridire", true);
		this.menu.put("crawlingarea", true);
		this.menu.put("queryingarea", true);
		this.menu.put("reportingarea", true);
		this.menu.put("adminarea", true);
		this.menu.put("processingarea", true);
		this.menu.put("news", true);
		this.menu.put("users", true);
		this.menu.put("anonUsers", true);
	}

	public void setMenu(Map<String, Boolean> menu) {
		this.menu = menu;
	}

	public void setSelectedMenuItem(String selectedMenuItem) {
		this.selectedMenuItem = selectedMenuItem;
	}
}
