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

import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.util.exceptions.HeritrixException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;
import javax.persistence.Transient;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.security.Identity;

@Scope(ScopeType.CONVERSATION)
@Name("profile")
public class Profile {

	public static final Map<String, List<String>> SUPPORTED_MIME_TYPES = new HashMap<String, List<String>>();
	static {
		List<String> htmlMT = new ArrayList<String>();
		htmlMT.add("text/html");
		htmlMT.add("application/xhtml");
		htmlMT.add("application/xhtml+xml");
		SUPPORTED_MIME_TYPES.put("HTML", htmlMT);
		List<String> pdfMT = new ArrayList<String>();
		pdfMT.add("application/pdf");
		SUPPORTED_MIME_TYPES.put("PDF", pdfMT);
		List<String> textMT = new ArrayList<String>();
		textMT.add("text/plain");
		SUPPORTED_MIME_TYPES.put("TXT", textMT);
		List<String> docMT = new ArrayList<String>();
		docMT.add("application/msword");
		docMT
				.add("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
		SUPPORTED_MIME_TYPES.put("DOC", docMT);
		List<String> rtfMT = new ArrayList<String>();
		rtfMT.add("application/rtf");
		SUPPORTED_MIME_TYPES.put("RTF", rtfMT);
	}

	@Transient
	public static String escRegEx(String inStr) {
		return inStr.replaceAll("([\\\\*+\\[\\]<>(){}\\$.?\\^|])", "\\\\$1");
	}

	@In(create = true)
	private CrawlerManager crawlerManager;

	@In
	private Identity identity;
	@In
	private EntityManager entityManager;
	private String profileName;
	private String jobName;
	private String jobSeeds;
	private String description;
	private List<String> chosenMimeTypes = new ArrayList<String>();
	private String goodURLs;
	private String writtenURLs;
	private String writtenResourceURLPattern;
	private boolean writtenResourceURLPatternDenied;
	private String followedURLPattern;

	private boolean followedURLPatternDenied;

	private Integer createdJobId;

	public String createJob() throws HeritrixException {
		String ret = this.crawlerManager.createJob(this, this.getCurrentUser(),
				false);
		this.setCreatedJobId(this.retieveJobId());
		return ret;
	}

	public List<SelectItem> getAllMimeTypes() {
		List<SelectItem> resultList = new ArrayList<SelectItem>();
		List<String> mimeTypesList = new ArrayList<String>();
		mimeTypesList.addAll(SUPPORTED_MIME_TYPES.keySet());
		Collections.sort(mimeTypesList);
		for (String c : mimeTypesList) {
			resultList.add(new SelectItem(c));
		}
		return resultList;
	}

	public List<String> getChosenMimeTypes() {
		return this.chosenMimeTypes;
	}

	public Integer getCreatedJobId() {
		return this.createdJobId;
	}

	private User getCurrentUser() {
		List<User> users = this.entityManager.createQuery(
				"from User u where u.username=:username").setParameter(
				"username", this.identity.getCredentials().getUsername())
				.getResultList();
		if (users.size() == 1) {
			return users.get(0);
		}
		return null;
	}

	public String getDescription() {
		if (this.profileName == null) {
			return "Nessuna descrizione disponibile";
		}
		if (this.profileName.equals("basic_seed_sites")) {
			return "Profilo base di Heritrix - Non usare";
		} else if (this.profileName.equals("broad_but_shallow")) {
			return "Profilo base di Heritrix - Non usare";
		} else if (this.profileName.equals("profilo_prova")) {
			return "Profilo di test";
		} else if (this.profileName.equals("profilo_1")) {
			return "Profilo che permette di recuperare risorse di vari formati a scelta, specificando anche un pattern delle URL da elaborare e un pattern per le URL da immagazzinare (entrambi opzionali)";
		} else if (this.profileName.equals("profilo_2")) {
			return "Profilo che permette di effettuare scraping, permette cioè di indicare al crawler un elenco di URL da scaricare, trascurando eventuali risorse collegate.";
		}
		return "Nessuna descrizione disponibile";
	}

	public String getFollowedURLPattern() {
		return this.followedURLPattern;
	}

	public String getGoodURLs() {
		return this.goodURLs;
	}

	public String getJobName() {
		return this.jobName;
	}

	public String getJobSeeds() throws HeritrixException {
		if (this.jobSeeds == null || this.jobSeeds.trim().length() < 1) {
			List<String> seedsList = this.crawlerManager.getJobSeeds(
					this.profileName, this.jobName, this.getCurrentUser());
			StringBuffer b = new StringBuffer();
			for (String s : seedsList) {
				b.append(s).append("\n");
			}
			return b.toString();
		}
		return this.jobSeeds;
	}

	public String getProfileName() {
		return this.profileName;
	}

	public String getWrittenResourceURLPattern() {
		return this.writtenResourceURLPattern;
	}

	public String getWrittenURLs() {
		return this.writtenURLs;
	}

	public boolean isFollowedURLPatternDenied() {
		return this.followedURLPatternDenied;
	}

	public boolean isWrittenResourceURLPatternDenied() {
		return this.writtenResourceURLPatternDenied;
	}

	@SuppressWarnings("unchecked")
	private Integer retieveJobId() {
		if (this.jobName != null) {
			List<Job> jobs = this.entityManager.createQuery(
					"from Job j where j.name=:jobName").setParameter("jobName",
					this.jobName).getResultList();
			if (jobs.size() == 1) {
				return jobs.get(0).getId();
			}
		}
		return null;
	}

	public void setChosenMimeTypes(List<String> chosenMimeTypes) {
		this.chosenMimeTypes = new ArrayList<String>(chosenMimeTypes);
	}

	public void setCreatedJobId(Integer createdJobId) {
		this.createdJobId = createdJobId;
	}

	public void setDescription(String description) {
		this.description = description.trim();
	}

	public void setFollowedURLPattern(String followedURLPattern) {
		this.followedURLPattern = followedURLPattern.trim();
	}

	public void setFollowedURLPatternDenied(boolean followedURLPatternDenied) {
		this.followedURLPatternDenied = followedURLPatternDenied;
	}

	public void setGoodURLs(String goodURLs) {

		this.goodURLs = escRegEx(goodURLs.trim());
	}

	public void setJobName(String jobName) {
		this.jobName = jobName.trim();
	}

	public void setJobSeeds(String jobSeeds) {
		this.jobSeeds = jobSeeds.trim();
	}

	public void setProfileName(String profileName) {
		this.profileName = profileName;
	}

	public void setWrittenResourceURLPattern(String writtenResourceURLPattern) {
		this.writtenResourceURLPattern = writtenResourceURLPattern.trim();
	}

	public void setWrittenResourceURLPatternDenied(
			boolean writtenResourceURLPatternDenied) {
		this.writtenResourceURLPatternDenied = writtenResourceURLPatternDenied;
	}

	public void setWrittenURLs(String writtenURLs) {
		this.writtenURLs = escRegEx(writtenURLs.trim());
	}
}
