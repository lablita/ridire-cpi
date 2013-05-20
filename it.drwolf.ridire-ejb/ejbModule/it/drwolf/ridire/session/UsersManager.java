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

import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Role;
import it.drwolf.ridire.entity.User;

import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.End;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Roles;
import org.jboss.seam.annotations.Scope;
import org.richfaces.model.SortOrder;
import org.richfaces.model.selection.SimpleSelection;

@Scope(ScopeType.CONVERSATION)
@Name("usersManager")
public class UsersManager {
	@In
	private EntityManager entityManager;
	private SortOrder order = new SortOrder();
	private int scrollerPage;
	private SimpleSelection selection = new SimpleSelection();
	private List<User> allUsers = null;
	private int rows = 10;
	private User currentItem = new User();

	@End(beforeRedirect = true)
	public void deleteUser(User user2delete) {
		for (Job j : user2delete.getJobs()) {
			for (CrawledResource cr : j.getCrawledResources()) {
				this.entityManager.remove(cr);
			}
			this.entityManager.remove(j);
		}
		User superv = user2delete.getSupervisor();
		if (superv != null) {
			superv.getAssignedUsers().remove(user2delete);
			this.entityManager.merge(superv);
		}
		// System.out.println(this.entityManager.contains(this.currentItem));
		for (Role r : user2delete.getRoles()) {
			r.getUsers().remove(user2delete);
			// this.entityManager.merge(r);
		}
		this.entityManager.remove(user2delete);
		this.allUsers.remove(user2delete);
		this.currentItem = new User();
	}

	@SuppressWarnings("unchecked")
	public List<Roles> getAllRoles() {
		return this.entityManager.createQuery(
				"from Role r order by r.description").getResultList();
	}

	@SuppressWarnings("unchecked")
	public List<User> getAllSupervisors() {
		return this.entityManager
				.createQuery(
						"select u from User u, Role r where r member of u.roles and (r.description=:supervisor or r.description=:admin)")
				.setParameter("supervisor", Role.CRAWLERSUPERVISOR)
				.setParameter("admin", Role.ADMIN).getResultList();
	}

	public User getCurrentItem() {
		return this.currentItem;
	}

	public SortOrder getOrder() {
		return this.order;
	}

	public int getRows() {
		return this.rows;
	}

	public int getScrollerPage() {
		return this.scrollerPage;
	}

	public SimpleSelection getSelection() {
		return this.selection;
	}

	@SuppressWarnings("unchecked")
	public List<User> getUsersWithoutSupervisor() {
		if (this.allUsers == null) {
			this.allUsers = this.entityManager.createQuery(
					"from User u where u.supervisor is null").getResultList();
		}
		return this.allUsers;
	}

	@End(beforeRedirect = true)
	public void save() {
		if (this.currentItem.getSupervisor() == null) {
			this.allUsers.add(this.currentItem);
		}
		this.store();
	}

	public void setCurrentItem(User currentItem) {
		this.currentItem = currentItem;
	}

	public void setOrder(SortOrder order) {
		this.order = order;
	}

	public void setRows(int rows) {
		this.rows = rows;
	}

	public void setScrollerPage(int scrollerPage) {
		this.scrollerPage = scrollerPage;
	}

	public void setSelection(SimpleSelection selection) {
		this.selection = selection;
	}

	@End(beforeRedirect = true)
	public void store() {

		if(this.currentItem.hasRole("admin") || this.getCurrentItem().hasRole("Crawler Supervisor"))
			this.getCurrentItem().setSupervisor(null);
		User superv = this.currentItem.getSupervisor();
			
		this.entityManager.persist(this.currentItem);
		if (superv != null) {
			superv.getAssignedUsers().add(this.currentItem);
			this.entityManager.merge(superv);
		}
		// this.currentItem.getSupervisor().getAssignedUsers().add(
		// this.currentItem);
		// this.entityManager.merge(this.currentItem.getSupervisor());
		this.currentItem = new User();
	}
}
