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

import it.drwolf.ridire.entity.User;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.security.Identity;

@Scope(ScopeType.CONVERSATION)
@Name("changePassword")
public class ChangePassword {

	@In
	FacesMessages facesMessages;

	@In
	public EntityManager entityManager;

	private String oldPassword;
	private String newPassword;
	private String confirmPassword;

	@End
	public String changePassword() {
		List<User> users = this.entityManager
				.createQuery("from User u where u.username=:username")
				.setParameter("username",
						Identity.instance().getCredentials().getUsername())
				.getResultList();
		User user = null;
		if (users != null && users.size() == 1) {
			user = users.get(0);
		}
		if (!user.getPassword().equals(this.getOldPassword())) {
			FacesMessages.instance().add("La vecchia password e' sbagliata");
			return "changePassword";
		}
		if (this.getNewPassword().equals(this.getOldPassword())) {
			FacesMessages.instance().add(
					"La nuova password deve essere diversa dalla vecchia");
			return "changePassword";
		}
		if (!this.getConfirmPassword().equals(this.getNewPassword())) {
			FacesMessages.instance().add("Le password non coincidono");
			return "changePassword";
		}
		user.setPassword(this.getNewPassword());
		this.entityManager.merge(user);
		FacesMessages.instance().add("Password aggiornata");
		return "home";

	}

	// @Length(min = 8, message =
	// "La password deve essere di almeno 8 caratteri")
	public String getConfirmPassword() {
		return this.confirmPassword;
	}

	// @Length(min = 8, message =
	// "La password deve essere di almeno 8 caratteri")
	public String getNewPassword() {
		return this.newPassword;
	}

	public String getOldPassword() {
		return this.oldPassword;
	}

	public void setConfirmPassword(String confirmPassword) {
		this.confirmPassword = confirmPassword;
	}

	public void setNewPassword(String newPassword) {
		this.newPassword = newPassword;
	}

	public void setOldPassword(String value) {
		this.oldPassword = value;
	}
}
