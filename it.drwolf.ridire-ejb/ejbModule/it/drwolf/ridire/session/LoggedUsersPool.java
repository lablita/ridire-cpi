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
package it.drwolf.ridire.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Scope(ScopeType.APPLICATION)
@Name("loggedUsersPool")
public class LoggedUsersPool {

	private class LoggedUsersComparator implements Comparator<LoggedUser> {

		public int compare(LoggedUser arg0, LoggedUser arg1) {
			String s0 = arg0.getUser().getSurname();
			String s1 = arg1.getUser().getSurname();
			if (s1 == null) {
				return -1;
			}
			if (s0 == null) {
				return 1;
			}
			return s0.compareToIgnoreCase(s1);
		}

	}

	Set<LoggedUser> loggedUsers = new HashSet<LoggedUser>();
	private Comparator<LoggedUser> loggedUsersComparator = new LoggedUsersComparator();

	public void add(LoggedUser loggedUser) {
		this.loggedUsers.add(loggedUser);
	}

	public List<LoggedUser> getLoggedUsers() {
		List<LoggedUser> orderedLoggedUsers = new ArrayList<LoggedUser>(
				this.loggedUsers);
		Collections.sort(orderedLoggedUsers, this.loggedUsersComparator);
		return orderedLoggedUsers;
	}

	public void remove(LoggedUser loggedUser) {
		this.loggedUsers.remove(loggedUser);

	}
}
