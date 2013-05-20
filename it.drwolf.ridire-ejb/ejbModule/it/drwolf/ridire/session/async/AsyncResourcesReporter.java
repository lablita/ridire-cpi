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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.util.data.CorpusDataReport;
import it.drwolf.ridire.util.data.UserDataReport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.annotations.async.Expiration;
import org.jboss.seam.annotations.async.FinalExpiration;
import org.jboss.seam.annotations.async.IntervalCron;
import org.jboss.seam.async.QuartzTriggerHandle;

@Name("asyncResourcesReporter")
@Scope(ScopeType.APPLICATION)
public class AsyncResourcesReporter {

	@In
	EntityManager entityManager;

	@In(required = true, create = true)
	private FlagBearer flagBearer;

	private ResourcesReporterData resourcesReporterData;

	private void getAll(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret1 = null;
		List<Object[]> ret2 = null;
		if (admin) {
			ret1 = this.entityManager
					.createNativeQuery(
							"SELECT fun, sem, sum(word) from (select coalesce(F.description,'fun') as fun, "
									+ "coalesce(S.description,'sem') as sem, C.wordsNumber as word "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on S.id=C.semanticMetadatum_id "
									+ "LEFT JOIN FunctionalMetadatum F ON F.id=C.functionalMetadatum_id where C.deleted is false) as subs "
									+ " group by fun,sem with rollup")
					.getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"SELECT fun, sem, sum(word) from (select coalesce(F.description,'fun') as fun, "
									+ "coalesce(S.description,'sem') as sem, C.wordsNumber as word "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on S.id=C.semanticMetadatum_id "
									+ "LEFT JOIN FunctionalMetadatum F ON F.id=C.functionalMetadatum_id "
									+ "LEFT JOIN Job J ON J.id=C.job_id where C.deleted is false and J.validationStatus=2) as subs "
									+ " group by fun,sem with rollup")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret1 = this.entityManager
					.createNativeQuery(
							"SELECT fun, sem, sum(word) from (select coalesce(F.description,'fun') as fun, "
									+ "coalesce(S.description,'sem') as sem, C.wordsNumber as word "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on S.id=C.semanticMetadatum_id "
									+ "LEFT JOIN FunctionalMetadatum F ON F.id=C.functionalMetadatum_id, Job J "
									+ "where C.job_id=J.id and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false) as subs "
									+ "group by fun,sem with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"SELECT fun, sem, sum(word) from (select coalesce(F.description,'fun') as fun, "
									+ "coalesce(S.description,'sem') as sem, C.wordsNumber as word "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on S.id=C.semanticMetadatum_id "
									+ "LEFT JOIN FunctionalMetadatum F ON F.id=C.functionalMetadatum_id, Job J "
									+ "where C.job_id=J.id and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false and J.validationStatus=2) as subs "
									+ "group by fun,sem with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (int i = 0; i < ret1.size(); i++) {
			Object[] r = ret1.get(i);
			if (r[0] != null && r[1] == null) {
				continue;
			}
			if (r[0] == null) {
				this.resourcesReporterData.getAllTotal().put(username,
						((Number) r[2]).intValue());
				continue;
			}
			if (r[0].equals("fun")) {
				r[0] = "N/A";
			}
			if (r[1].equals("sem")) {
				r[1] = "N/A";
			}
			CorpusDataReport cdr = new CorpusDataReport((String) r[0],
					(String) r[1], ((Number) r[2]).intValue());
			report.add(cdr);
		}
		for (int i = 0; i < ret2.size(); i++) {
			Object[] r2 = ret2.get(i);
			if (r2[0] != null && r2[1] == null) {
				continue;
			}
			if (r2[0] == null) {
				this.resourcesReporterData.getAllTotalValid().put(username,
						((Number) r2[2]).intValue());
				continue;
			}
			String fun = ((String) r2[0]).equals("fun") ? "N/A"
					: (String) r2[0];
			String sem = ((String) r2[1]).equals("sem") ? "N/A"
					: (String) r2[1];
			for (CorpusDataReport cdr : report) {
				if (cdr.getDomFunzionale().equals(fun)
						&& cdr.getDomSemantico().equals(sem)) {
					cdr.setWordsValid(((Number) r2[2]).intValue());
					break;
				}
			}
		}
		this.resourcesReporterData.getAll().put(username, report);
	}

	private void getFunctional(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		List<Object[]> ret2 = null;
		if (admin) {
			ret = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on C.semanticMetadatum_id=S.id, FunctionalMetadatum F "
									+ "where F.id=C.functionalMetadatum_id and C.deleted is false "
									+ "group By F.description with rollup")
					.getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on C.semanticMetadatum_id=S.id, FunctionalMetadatum F, Job J "
									+ "where F.id=C.functionalMetadatum_id and C.deleted is false and C.job_id=J.id and J.validationStatus=2 "
									+ "group By F.description with rollup")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on C.semanticMetadatum_id=S.id, FunctionalMetadatum F, Job J "
									+ "where F.id=C.functionalMetadatum_id and C.job_id=J.id and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false "
									+ "group By F.description with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN SemanticMetadatum S on C.semanticMetadatum_id=S.id, FunctionalMetadatum F, Job J "
									+ "where F.id=C.functionalMetadatum_id and C.job_id=J.id and J.validationStatus=2 and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false "
									+ "group By F.description with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (Object[] r : ret) {
			if (r[0] == null) {
				this.resourcesReporterData.getFunctionalTotal().put(username,
						((Number) r[2]).intValue());
				continue;
			}
			report.add(new CorpusDataReport((String) r[0], (String) r[1],
					((Number) r[2]).intValue()));
		}
		for (int i = 0; i < ret2.size(); i++) {
			Object[] r2 = ret2.get(i);
			if (r2[0] != null && r2[1] == null) {
				continue;
			}
			if (r2[0] == null) {
				this.resourcesReporterData.getFunctionalTotalValid().put(
						username, ((Number) r2[2]).intValue());
				continue;
			}
			String fun = ((String) r2[0]).equals("fun") ? "N/A"
					: (String) r2[0];
			String sem = ((String) r2[1]).equals("sem") ? "N/A"
					: (String) r2[1];
			for (CorpusDataReport cdr : report) {
				if (cdr.getDomFunzionale().equals(fun)
						&& cdr.getDomSemantico().equals(sem)) {
					cdr.setWordsValid(((Number) r2[2]).intValue());
					break;
				}
			}
		}
		this.resourcesReporterData.getFunctional().put(username, report);
	}

	private void getMappedJobs(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		if (admin) {
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.mappedResources=true group by u.username order by count(j.id) desc")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.mappedResources=true and u.id in (:users) group by u.username order by count(j.id) desc")
					.setParameter("users", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		this.resourcesReporterData.getMappedJobs().put(username,
				userDataReports);
	}

	private void getNotMappedJobs(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		if (admin) {
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.mappedResources=false and j.jobStage='FINISHED'  group by u.username order by count(j.id) desc")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.mappedResources=false and j.jobStage='FINISHED'  and u.id in (:users) group by u.username order by count(j.id) desc")
					.setParameter("users", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		this.resourcesReporterData.getNotMappedJobs().put(username,
				userDataReports);
	}

	private void getNotStartedJobs(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		if (admin) {
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.jobStage='CREATED' group by u.username order by count(j.id) desc")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and j.jobStage='CREATED' and u.id in (:users) group by u.username order by count(j.id) desc")
					.setParameter("users", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		this.resourcesReporterData.getNotStartedJobs().put(username,
				userDataReports);
	}

	private void getSemantic(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		List<Object[]> ret2 = null;
		if (admin) {
			ret = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN FunctionalMetadatum F on C.functionalMetadatum_id=F.id, SemanticMetadatum S "
									+ "where S.id=C.semanticMetadatum_id and C.deleted is false "
									+ "group By S.description with rollup")
					.getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN FunctionalMetadatum F on C.functionalMetadatum_id=F.id, SemanticMetadatum S, Job J "
									+ "where S.id=C.semanticMetadatum_id and C.deleted is false and J.validationStatus=2 "
									+ "group By S.description with rollup")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN FunctionalMetadatum F on C.functionalMetadatum_id=F.id, SemanticMetadatum S, Job J "
									+ "where S.id=C.semanticMetadatum_id and C.job_id=J.id and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false "
									+ "group By S.description with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
			ret2 = this.entityManager
					.createNativeQuery(
							"select coalesce(F.description,NULL), coalesce(S.description,NULL), SUM(C.wordsNumber) "
									+ "from CrawledResource C LEFT JOIN FunctionalMetadatum F on C.functionalMetadatum_id=F.id, SemanticMetadatum S, Job J "
									+ "where S.id=C.semanticMetadatum_id and C.job_id=J.id and J.crawlerUser_id IN (:crawlerUserIds) and C.deleted is false and J.validationStatus=2 "
									+ "group By S.description with rollup")
					.setParameter("crawlerUserIds", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (Object[] r : ret) {
			if (r[1] == null) {
				this.resourcesReporterData.getSemanticTotal().put(username,
						((Number) r[2]).intValue());
				continue;
			}
			report.add(new CorpusDataReport((String) r[0], (String) r[1],
					((Number) r[2]).intValue()));
		}
		for (int i = 0; i < ret2.size(); i++) {
			Object[] r2 = ret2.get(i);
			if (r2[0] != null && r2[1] == null) {
				continue;
			}
			if (r2[0] == null) {
				this.resourcesReporterData.getSemanticTotalValid().put(
						username, ((Number) r2[2]).intValue());
				continue;
			}
			String fun = ((String) r2[0]).equals("fun") ? "N/A"
					: (String) r2[0];
			String sem = ((String) r2[1]).equals("sem") ? "N/A"
					: (String) r2[1];
			for (CorpusDataReport cdr : report) {
				if (cdr.getDomFunzionale().equals(fun)
						&& cdr.getDomSemantico().equals(sem)) {
					cdr.setWordsValid(((Number) r2[2]).intValue());
					break;
				}
			}
		}
		this.resourcesReporterData.getSemantic().put(username, report);
	}

	private void getTotalJobs(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		if (admin) {
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u group by u.username order by count(j.id) desc")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createQuery(
							"SELECT count(j.id), u.username, u.name, u.surname from Job j, User u where j.crawlerUser=u and u.id in (:users) group by u.username order by count(j.id) desc")
					.setParameter("users", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (int i = 0; i < ret.size(); i++) {
			Object[] r = ret.get(i);
			String userString = (String) r[1];
			List<Object[]> c = this.entityManager
					.createQuery(
							"SELECT j.validationStatus, count(j.id) from Job j where j.crawlerUser.username=:userString group by j.validationStatus")
					.setParameter("userString", userString).getResultList();
			List<Long> values = new ArrayList<Long>();
			for (int j = 0; j < 6; j++) {
				values.add(0L);
			}
			for (Object[] o : c) {
				Object object = o[0];
				if (object == null) {
					object = 0;
				}
				values.set((Integer) object + 1, (Long) o[1]);
			}
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		this.resourcesReporterData.getTotalJobs()
				.put(username, userDataReports);
	}

	private List<Integer> getUsersIds(String username) {
		List<Integer> users = this.entityManager
				.createQuery(
						"select u.id from User u left join u.supervisor as supervisor where u.username=:username or supervisor.username=:supervisorname")
				.setParameter("username", username)
				.setParameter("supervisorname", username).getResultList();
		return users;
	}

	private void getWordsNumber(String username) {
		boolean admin = true;
		if (username != null) {
			admin = false;
		}
		List<Object[]> ret = null;
		if (admin) {
			ret = this.entityManager
					.createQuery(
							"SELECT sum(c.wordsNumber), u.username, u.name, u.surname "
									+ "from CrawledResource c, Job j, User u "
									+ "where j.crawlerUser=u and j.mappedResources=true and c.job=j and c.deleted is false "
									+ "group by u.username order by sum(c.wordsNumber) desc")
					.getResultList();
		} else {
			List<Integer> users = this.getUsersIds(username);
			ret = this.entityManager
					.createQuery(
							"SELECT sum(c.wordsNumber), u.username, u.name, u.surname "
									+ "from CrawledResource c, Job j, User u "
									+ "where j.crawlerUser=u and j.mappedResources=true and c.job=j and u.id in (:users) and c.deleted is false "
									+ "group by u.username order by sum(c.wordsNumber) desc")
					.setParameter("users", users).getResultList();
		}
		if (username == null) {
			username = "all";
		}
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		this.resourcesReporterData.getWordsNumber().put(username,
				userDataReports);
	}

	@Asynchronous
	public QuartzTriggerHandle updateReports(@Expiration Date expirationDate,
			@IntervalCron String cronData, @FinalExpiration Date endDate,
			ResourcesReporterData resourcesReporterData) {
		this.resourcesReporterData = resourcesReporterData;
		QuartzTriggerHandle handle = new QuartzTriggerHandle(
				"RIDIRE resources reporter");
		if (!this.flagBearer.isResourcesUpdaterRunning()) {
			System.out.println("RIDIRE resources reporter started");
			this.flagBearer.setResourcesUpdaterRunning(true);
			this.resourcesReporterData.clearAll();
			List<String> usernames = this.entityManager.createQuery(
					"select u.username from User u").getResultList();
			usernames.add(null);
			for (String username : usernames) {
				this.getAll(username);
				this.getFunctional(username);
				this.getMappedJobs(username);
				this.getNotMappedJobs(username);
				this.getNotStartedJobs(username);
				this.getSemantic(username);
				this.getTotalJobs(username);
				this.getWordsNumber(username);
			}
			System.out.println("RIDIRE resources reporter ended");
			this.flagBearer.setResourcesUpdaterRunning(false);
		}
		return handle;
	}
}
