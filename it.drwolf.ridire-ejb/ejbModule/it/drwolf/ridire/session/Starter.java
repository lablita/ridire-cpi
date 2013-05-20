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

import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.Role;
import it.drwolf.ridire.entity.ScheduledJobHandle;
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.index.cwb.CorpusSizeParams;
import it.drwolf.ridire.session.async.AsyncResourcesReporter;
import it.drwolf.ridire.session.async.JobDBDataUpdater;
import it.drwolf.ridire.session.async.JobMapperMonitor;
import it.drwolf.ridire.session.async.ResourcesReporterData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.ejb.Remove;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.jboss.seam.Component;
import org.jboss.seam.annotations.Destroy;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Observer;
import org.jboss.seam.async.QuartzTriggerHandle;
import org.jboss.seam.transaction.UserTransaction;

@Name("starter")
public class Starter {

	EntityManager entityManager;

	private JobMapperMonitor jobMapperMonitor;

	private JobDBDataUpdater jobDBDataUpdater;

	private CorpusSizeParams corpusSizeParams;

	private UserTransaction userTx;

	private ResourcesReporterData resourcesReporterData;

	private AsyncResourcesReporter asyncResourcesReporter;

	private void checkAdmin() {
		Role adminRole = (Role) this.entityManager
				.createQuery("from Role where description = :admin")
				.setParameter("admin", Role.ADMIN).getSingleResult();
		if (adminRole.getUsers().size() == 0) {
			User admin = new User();
			admin.setUsername("admin");
			admin.setPassword("changeme");
			admin.getRoles().add(adminRole);
			adminRole.getUsers().add(admin);
			this.entityManager.persist(admin);
		}
	}

	private void checkCrawlerCommandsParams() {
		for (CommandParameter cc : CommandParameter.defaults) {
			if (this.entityManager.find(CommandParameter.class,
					cc.getCommandName()) == null) {
				this.entityManager.persist(cc);
			}
		}
	}

	private void checkMetadata() {
		for (String fm : FunctionalMetadatum.DEFAULT) {
			if (this.entityManager
					.createQuery(
							"from FunctionalMetadatum m where m.description=:description")
					.setParameter("description", fm).getResultList().size() < 1) {
				FunctionalMetadatum m = new FunctionalMetadatum();
				m.setDescription(fm);
				this.entityManager.persist(m);
			}
		}
		for (String sm : SemanticMetadatum.DEFAULT) {
			if (this.entityManager
					.createQuery(
							"from SemanticMetadatum m where m.description=:description")
					.setParameter("description", sm).getResultList().size() < 1) {
				SemanticMetadatum m = new SemanticMetadatum();
				m.setDescription(sm);
				this.entityManager.persist(m);
			}
		}
	}

	private void checkParams() {
		for (Parameter ap : Parameter.defaults) {
			if (this.entityManager.find(Parameter.class, ap.getKey()) == null) {
				this.entityManager.persist(ap);
			}
		}
		this.entityManager.flush();
	}

	@SuppressWarnings("unchecked")
	private void checkRoles() {
		List<String> roles = new ArrayList<String>();
		for (Role rc : (List<Role>) this.entityManager.createQuery("from Role")
				.getResultList()) {
			roles.add(rc.getDescription());
		}
		for (String role : Role.DEFAULT_ROLES) {
			if (!roles.contains(role)) {
				Role rc = new Role();
				rc.setDescription(role);
				this.entityManager.persist(rc);
			}
		}
		this.checkRolesHierarchy();
	}

	@SuppressWarnings("unchecked")
	private void checkRolesHierarchy() {
		List<Role> adminRoles = this.entityManager
				.createQuery("from Role r where r.description=:admin")
				.setParameter("admin", Role.ADMIN).getResultList();
		if (adminRoles.size() == 1) {
			List<Role> crawlerSupervisorRoles = this.entityManager
					.createQuery("from Role r where r.description=:cs")
					.setParameter("cs", Role.CRAWLERSUPERVISOR).getResultList();
			if (crawlerSupervisorRoles.size() == 1) {
				Role adminRole = adminRoles.get(0);
				Role crawlerSupervisorRole = crawlerSupervisorRoles.get(0);
				adminRole.getChildrenRoles().add(crawlerSupervisorRole);
				this.entityManager.persist(adminRole);
				crawlerSupervisorRole.setSuperRole(adminRole);
				this.entityManager.persist(crawlerSupervisorRole);
				List<Role> crawlerRoles = this.entityManager
						.createQuery("from Role r where r.description=:cr")
						.setParameter("cr", Role.CRAWLERUSER).getResultList();
				if (crawlerRoles.size() == 1) {
					Role crawlerRole = crawlerRoles.get(0);
					crawlerSupervisorRole.getChildrenRoles().add(crawlerRole);
					this.entityManager.persist(crawlerSupervisorRole);
					crawlerRole.setSuperRole(crawlerSupervisorRole);
					this.entityManager.persist(crawlerRole);
				}
			}
			List<Role> indexerRoles = this.entityManager
					.createQuery("from Role r where r.description=:cs")
					.setParameter("cs", Role.INDEXER).getResultList();
			if (indexerRoles.size() == 1) {
				Role adminRole = adminRoles.get(0);
				Role indexerRole = indexerRoles.get(0);
				adminRole.getChildrenRoles().add(indexerRole);
				this.entityManager.persist(adminRole);
				indexerRole.setSuperRole(adminRole);
			}
		}

	}

	@Remove
	@Destroy
	public void destroy() {

	}

	@SuppressWarnings("unchecked")
	private void removeScheduledJob() {
		for (Job j : (List<Job>) this.entityManager.createQuery("from Job")
				.getResultList()) {
			ScheduledJobHandle sjh = j.getScheduledJobHandle();
			if (sjh != null) {
				this.entityManager.remove(sjh);
				j.setScheduledJobHandle(null);
				this.entityManager.persist(j);
			}
		}
	}

	private void startJobDBUpdater() {
		Calendar endDate = Calendar.getInstance();
		endDate.set(2100, 1, 1);
		this.jobDBDataUpdater.updater(
				new Date(),
				this.entityManager.find(Parameter.class,
						Parameter.JOBUPDATER_CRON.getKey()).getValue(),
				endDate.getTime());
	}

	private void startJobMapperMonitor() {
		Calendar endDate = Calendar.getInstance();
		endDate.set(2100, 1, 1);
		this.jobMapperMonitor.lookForNotMappedJob(
				new Date(),
				this.entityManager.find(Parameter.class,
						Parameter.JOBMAPPER_CRON.getKey()).getValue(),
				endDate.getTime());
	}

	private void startResourcesReporter() {
		Calendar endDate = Calendar.getInstance();
		endDate.set(2100, 1, 1);
		QuartzTriggerHandle handle = this.asyncResourcesReporter.updateReports(
				new Date(),
				this.entityManager.find(Parameter.class,
						Parameter.RESOURCESREPORT_CRON.getKey()).getValue(),
				endDate.getTime(), this.resourcesReporterData);
		System.out.println(handle);
	}

	@Observer("org.jboss.seam.postInitialization")
	public void startup() {
		this.userTx = null;
		this.entityManager = null;
		while (this.entityManager == null) {
			try {
				Thread.sleep(1000);
				this.userTx = (UserTransaction) Component
						.getInstance("org.jboss.seam.transaction.transaction");
				this.entityManager = (EntityManager) Component
						.getInstance("entityManager");
			} catch (Exception e) {
			}
		}
		try {
			this.userTx.begin();
			this.entityManager.joinTransaction();
			this.jobMapperMonitor = (JobMapperMonitor) Component
					.getInstance("jobMapperMonitor");
			this.jobDBDataUpdater = (JobDBDataUpdater) Component
					.getInstance("jobDBDataUpdater");
			this.corpusSizeParams = (CorpusSizeParams) Component
					.getInstance("corpusSizeParams");
			this.resourcesReporterData = (ResourcesReporterData) Component
					.getInstance("resourcesReporterData");
			this.asyncResourcesReporter = (AsyncResourcesReporter) Component
					.getInstance("asyncResourcesReporter");
			this.checkRoles();
			this.checkAdmin();
			this.checkParams();
			this.checkCrawlerCommandsParams();
			this.checkMetadata();
			this.removeScheduledJob();
			this.startJobDBUpdater();
			this.startJobMapperMonitor();
			this.startResourcesReporter();
			this.entityManager.flush();
			this.userTx.commit();
			if (this.userTx != null && !this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			if (this.entityManager
					.find(Parameter.class, Parameter.INDEXING_ENABLED.getKey())
					.getValue().equals("true")) {
				// this.flagBearer.getCorporaList();
				this.corpusSizeParams.init();
			}
			this.entityManager.flush();
			this.userTx.commit();
		} catch (NotSupportedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicMixedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (HeuristicRollbackException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
