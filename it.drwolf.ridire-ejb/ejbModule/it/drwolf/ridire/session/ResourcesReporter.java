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

import it.drwolf.ridire.util.data.CorpusDataReport;
import it.drwolf.ridire.util.data.UserDataReport;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.Identity;

@Name("resourcesReporter")
@Scope(ScopeType.CONVERSATION)
public class ResourcesReporter {

	@In
	EntityManager entityManager;
	@In
	Identity identity;
	private Integer allTotal = 0;
	private Integer functionalTotal = 0;
	private Integer semanticTotal = 0;
	private Integer allTotalValid = 0;
	private Integer functionalTotalValid = 0;
	private Integer semanticTotalValid = 0;

	@Factory("resourcesReporter_all")
	public List<CorpusDataReport> getAll() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (int i = 0; i < ret1.size(); i++) {
			Object[] r = ret1.get(i);
			if (r[0] != null && r[1] == null) {
				continue;
			}
			if (r[0] == null) {
				this.setAllTotal(((Number) r[2]).intValue());
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
				this.setAllTotalValid(((Number) r2[2]).intValue());
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
		return report;
	}

	public Integer getAllTotal() {
		return this.allTotal;
	}

	public Integer getAllTotalValid() {
		return this.allTotalValid;
	}

	@Factory("resourcesReporter_perFunctional")
	public List<CorpusDataReport> getFunctional() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (Object[] r : ret) {
			if (r[0] == null) {
				this.setFunctionalTotal(((Number) r[2]).intValue());
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
				this.setFunctionalTotalValid(((Number) r2[2]).intValue());
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
		return report;
	}

	public Integer getFunctionalTotal() {
		return this.functionalTotal;
	}

	public Integer getFunctionalTotalValid() {
		return this.functionalTotalValid;
	}

	@Factory("resourcesReporter_mappedJobs")
	public List<UserDataReport> getMappedJobs() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		return userDataReports;
	}

	@Factory("resourcesReporter_notMappedJobs")
	public List<UserDataReport> getNotMappedJobs() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		return userDataReports;
	}

	@Factory("resourcesReporter_notStartedJobs")
	public List<UserDataReport> getNotStartedJobs() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		return userDataReports;
	}

	@Factory("resourcesReporter_perSemantic")
	public List<CorpusDataReport> getSemantic() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<CorpusDataReport> report = new ArrayList<CorpusDataReport>();
		for (Object[] r : ret) {
			if (r[1] == null) {
				this.setSemanticTotal(((Number) r[2]).intValue());
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
				this.setSemanticTotalValid(((Number) r2[2]).intValue());
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
		return report;
	}

	public Integer getSemanticTotal() {
		return this.semanticTotal;
	}

	public Integer getSemanticTotalValid() {
		return this.semanticTotalValid;
	}

	@Factory("resourcesReporter_totalJobs")
	public List<UserDataReport> getTotalJobs() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		return userDataReports;
	}

	private List<Integer> getUsersIds(String username) {
		List<Integer> users = this.entityManager
				.createQuery(
						"select u.id from User u left join u.supervisor as supervisor where u.username=:username or supervisor.username=:supervisorname")
				.setParameter("username", username)
				.setParameter("supervisorname", username).getResultList();
		return users;
	}

	@Factory("resourcesReporter_wordsNumber")
	public List<UserDataReport> getWordsNumber() {
		String username = null;
		boolean admin = true;
		if (this.identity != null) {
			username = this.identity.getCredentials().getUsername();
			admin = this.identity.hasRole("Admin");
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
		List<UserDataReport> userDataReports = new ArrayList<UserDataReport>();
		for (Object[] r : ret) {
			List<Long> values = new ArrayList<Long>();
			values.add((Long) r[0]);
			userDataReports.add(new UserDataReport(values, (String) r[1],
					(String) r[2], (String) r[3]));
		}
		return userDataReports;
	}

	public void setAllTotal(Integer allTotal) {
		this.allTotal = allTotal;
	}

	public void setAllTotalValid(Integer allTotalValid) {
		this.allTotalValid = allTotalValid;
	}

	public void setFunctionalTotal(Integer functionalTotal) {
		this.functionalTotal = functionalTotal;
	}

	public void setFunctionalTotalValid(Integer functionalTotalValid) {
		this.functionalTotalValid = functionalTotalValid;
	}

	public void setSemanticTotal(Integer semanticTotal) {
		this.semanticTotal = semanticTotal;
	}

	public void setSemanticTotalValid(Integer semanticTotalValid) {
		this.semanticTotalValid = semanticTotalValid;
	}
}
