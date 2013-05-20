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
package it.drwolf.ridire.entity;

import static javax.persistence.GenerationType.AUTO;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Transient;

@Entity
public class Role {
	@Transient
	public static String CRAWLERUSER = "Crawler User";
	@Transient
	public static String CRAWLERSUPERVISOR = "Crawler Supervisor";
	@Transient
	public static String ADMIN = "Admin";
	@Transient
	public static String INDEXER = "Indexer";
	@Transient
	public static String GUEST = "Guest";
	@Transient
	public static String FINAL_USER = "Final user";
	private String description;
	private Set<User> users = new HashSet<User>(0);
	@Transient
	public static final String[] DEFAULT_ROLES = new String[] { Role.ADMIN,
			Role.CRAWLERSUPERVISOR, Role.CRAWLERUSER, Role.GUEST, Role.INDEXER,
			Role.FINAL_USER };

	private Integer id;

	private Role superRole;
	private Set<Role> childrenRoles = new HashSet<Role>();

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof Role)) {
			return false;
		}
		Role other = (Role) obj;
		if (this.getDescription() == null) {
			if (other.getDescription() != null) {
				return false;
			}
		} else if (!this.getDescription().equals(other.getDescription())) {
			return false;
		}
		return true;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "superRole")
	public Set<Role> getChildrenRoles() {
		return this.childrenRoles;
	}

	public String getDescription() {
		return this.description;
	}

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public Role getSuperRole() {
		return this.superRole;
	}

	@ManyToMany(fetch = FetchType.EAGER, mappedBy = "roles")
	public Set<User> getUsers() {
		return this.users;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ (this.getDescription() == null ? 0 : this.getDescription()
						.hashCode());
		return result;
	}

	@Transient
	public boolean hasSuperRole(Role superRole) {
		Role sRole = this.getSuperRole();
		while (sRole != null) {
			if (sRole.equals(superRole)) {
				return true;
			}
			sRole = sRole.getSuperRole();
		}
		return false;
	}

	public void setChildrenRoles(Set<Role> childrenRoles) {
		this.childrenRoles = childrenRoles;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setSuperRole(Role superRole) {
		this.superRole = superRole;
	}

	public void setUsers(Set<User> users) {
		this.users = users;
	}

	@Override
	public String toString() {
		return this.getDescription();
	}
}
