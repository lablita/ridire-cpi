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

import javax.faces.event.ActionEvent;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("menuBean")
@Scope(ScopeType.EVENT)
public class MenuBean {
	@In
	private MenuState menuState;

	public MenuBean() {
	}

	public MenuState getMenuState() {
		return this.menuState;
	}

	public void select(ActionEvent event) {
		this.menuState.setSelectedMenuItem(event.getComponent().getId());
	}

	public void setMenuState(MenuState menuState) {
		this.menuState = menuState;
	}
}
