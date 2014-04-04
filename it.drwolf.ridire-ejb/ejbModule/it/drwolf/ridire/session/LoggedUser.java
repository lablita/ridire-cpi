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

import it.drwolf.ridire.entity.User;

import java.util.Date;
import java.util.UUID;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.Identity;

@Scope(ScopeType.SESSION)
@Name("loggedUser")
public class LoggedUser {

	@In(required = true)
	private Identity identity;

	private User user;

	private UUID randomID;
	private Date logTime = null;
	@In(required = true, create = true)
	private LoggedUsersPool loggedUsersPool;

	@Destroy
	public void destroy() {
		this.loggedUsersPool.remove(this);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof LoggedUser)) {
			return false;

		}
		LoggedUser other = (LoggedUser) obj;
		if (this.randomID == null) {
			if (other.randomID != null) {
				return false;
			}
		} else if (!this.randomID.equals(other.randomID)) {
			return false;
		}
		return true;
	}

	public Date getLogTime() {
		return this.logTime;
	}

	public User getUser() {
		return this.user;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.randomID == null ? 0 : this.randomID.hashCode());
		return result;
	}

	public void register(User user) {
		this.randomID = UUID.randomUUID();
		this.user = user;
		this.logTime = new Date();
		this.loggedUsersPool.add(this);
	}
}
