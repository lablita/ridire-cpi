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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Transient;

import org.hibernate.validator.Email;

@Entity
public class User {
	private String name;
	private String surname;
	private String body;
	private String username;
	private String password;
	private Long quota = 4000000000L; // default to 4GB per user
	private Set<Role> roles = new HashSet<Role>(0);
	private Set<Job> jobs = new HashSet<Job>(0);
	private Set<LocalResource> localResources = new HashSet<LocalResource>(0);
	private Set<User> assignedUsers = new HashSet<User>(0);
	private User supervisor;
	private Integer id;
	private String email;

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof User)) {
			return false;
		}
		User other = (User) obj;
		if (this.getId() == null) {
			if (other.getId() != null) {
				return false;
			}
		} else if (!this.getId().equals(other.getId())) {
			return false;
		}
		if (this.getUsername() == null) {
			if (other.getUsername() != null) {
				return false;
			}
		} else if (!this.getUsername().equals(other.getUsername())) {
			return false;
		}
		return true;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "supervisor")
	public Set<User> getAssignedUsers() {
		return this.assignedUsers;
	}

	@Transient
	public List<User> getAssignedUsersList() {
		return new ArrayList<User>(this.getAssignedUsers());
	}

	public String getBody() {
		return this.body;
	}

	@Email
	public String getEmail() {
		return this.email;
	}

	@Id
	@GeneratedValue(strategy = AUTO)
	public Integer getId() {
		return this.id;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "crawlerUser")
	public Set<Job> getJobs() {
		return this.jobs;
	}

	@OneToMany(fetch = FetchType.LAZY, mappedBy = "crawlerUser")
	public Set<LocalResource> getLocalResources() {
		return this.localResources;
	}

	public String getName() {
		return this.name;
	}

	public String getPassword() {
		return this.password;
	}

	public Long getQuota() {
		return this.quota;
	}

	@ManyToMany(fetch = FetchType.EAGER)
	@OrderBy("description")
	public Set<Role> getRoles() {
		return this.roles;
	}

	@Transient
	public List<Role> getRolesList() {
		return new ArrayList<Role>(this.getRoles());
	}

	@ManyToOne(fetch = FetchType.LAZY)
	public User getSupervisor() {
		return this.supervisor;
	}

	public String getSurname() {
		return this.surname;
	}

	public String getUsername() {
		return this.username;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (this.getId() == null ? 0 : this.getId().hashCode());
		result = prime
				* result
				+ (this.getUsername() == null ? 0 : this.getUsername()
						.hashCode());
		return result;
	}

	@Transient
	public boolean hasRole(String roleDescription) {
		for (Role r : this.getRoles()) {
			if (r.getDescription().equals(roleDescription)) {
				return true;
			}
		}
		return false;
	}

	public void setAssignedUsers(Set<User> assignedUsers) {
		this.assignedUsers = assignedUsers;
	}

	public void setAssignedUsersList(List<User> assignedUsers) {
		this.assignedUsers.clear();
		this.assignedUsers.addAll(assignedUsers);
	}

	public void setBody(String body) {
		this.body = body;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	public void setJobs(Set<Job> jobs) {
		this.jobs = jobs;
	}

	public void setLocalResources(Set<LocalResource> localResources) {
		this.localResources = localResources;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setQuota(Long quota) {
		this.quota = quota;
	}

	public void setRoles(Set<Role> roles) {
		this.roles = roles;
	}

	public void setRolesList(List<Role> rolesList) {
		this.getRoles().clear();
		this.getRoles().addAll(rolesList);
	}

	public void setSupervisor(User supervisor) {
		this.supervisor = supervisor;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@Override
	public String toString() {
		return this.getUsername();
	}
}
