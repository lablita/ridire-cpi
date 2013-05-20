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

import it.drwolf.ridire.entity.Role;
import it.drwolf.ridire.entity.User;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.jboss.seam.security.Credentials;
import org.jboss.seam.security.Identity;

@Name("authenticator")
public class Authenticator {
	@Logger
	private Log log;

	@In
	Identity identity;
	@In
	Credentials credentials;

	@In(create = true)
	LoggedUser loggedUser;

	@In
	private EntityManager entityManager;

	private void addAllChildrenRoles(Role role) {
		if (role == null) {
			return;
		}
		this.identity.addRole(role.getDescription());
		for (Role r : role.getChildrenRoles()) {
			this.addAllChildrenRoles(r);
		}
	}

	@SuppressWarnings("unchecked")
	public boolean authenticate() {
		String username = this.credentials.getUsername();
		String pw = this.credentials.getPassword();
		this.log.info("authenticating {0}", username);
		List<User> users = this.entityManager
				.createQuery(
						"from User u where u.username=:username and u.password=:password")
				.setParameter("username", username)
				.setParameter("password", pw).getResultList();
		if (users != null && users.size() == 1) {
			// if user has admin role, automatically gets all the others
			User user2Login = users.get(0);
			for (Role role : user2Login.getRoles()) {
				this.addAllChildrenRoles(role);
			}
			this.loggedUser.register(user2Login);
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	private List<Role> getAllRoles() {
		return this.entityManager.createQuery("from Role").getResultList();
	}
}
