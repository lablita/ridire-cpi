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

import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.LocalResource;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.entity.User;
import it.drwolf.ridire.entity.comparators.LocalResourceFilenameComparator;
import it.drwolf.ridire.util.MD5DigestCreator;
import it.drwolf.ridire.util.exceptions.FileHandlingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.faces.model.SelectItem;
import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.HttpHeaders;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Factory;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.datamodel.DataModel;
import org.jboss.seam.faces.FacesMessages;
import org.jboss.seam.international.StatusMessage.Severity;
import org.jboss.seam.security.Identity;
import org.richfaces.event.UploadEvent;
import org.richfaces.model.UploadItem;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

@Name("localResourcesManager")
@Scope(ScopeType.CONVERSATION)
public class LocalResourcesManager {
	@In
	private Identity identity;
	@In
	private EntityManager entityManager;
	@In(create = true)
	private Map<String, String> messages;
	@In(create = true)
	private FacesMessages facesMessages;

	private Integer functionalMetadatum;
	private Integer semanticMetadatum;

	private String filterFileNameValue;
	private String filterUserValue;
	private String filterMimeTypeValue;
	private Integer semanticMetadatumValue;
	private Integer functionalMetadatumValue;
	@DataModel(scope = ScopeType.UNSPECIFIED)
	List<LocalResource> lrList;

	private List<SelectItem> allMetadata = new ArrayList<SelectItem>();

	public void assignMetadata() {
		for (LocalResource lr : this.lrList) {
			if (lr.isChecked()) {
				FunctionalMetadatum fm = this.entityManager.find(
						FunctionalMetadatum.class, this.functionalMetadatum);
				lr.setFunctionalMetadatum(fm);
				SemanticMetadatum sm = this.entityManager.find(
						SemanticMetadatum.class, this.semanticMetadatum);
				lr.setSemanticMetadatum(sm);
				lr.setChecked(false);
				this.entityManager.merge(lr);
			}
		}
	}

	public void delete(LocalResource localResource) {
		String localResourceDir = this.entityManager.find(Parameter.class,
				Parameter.LOCAL_RESOURCES_DIR.getKey()).getValue();
		File toBeDeleted = new File(new File(localResourceDir),
				localResource.getUniqueFileName());
		toBeDeleted.delete();
		User owner = localResource.getCrawlerUser();
		owner.getLocalResources().remove(localResource);
		this.entityManager.merge(owner);
		this.entityManager.remove(localResource);
		this.lrList.remove(localResource);
	}

	public boolean filterFileNames(Object current) {
		if (this.filterFileNameValue == null
				|| this.filterFileNameValue.length() == 0) {
			return true;
		}
		LocalResource lr = (LocalResource) current;
		if (lr.getOrigFileName() != null
				&& lr.getOrigFileName().toLowerCase()
						.contains(this.filterFileNameValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean filterFunctionalMetadatum(Object current) {
		if (this.functionalMetadatumValue == null
				|| this.functionalMetadatumValue < 0) {
			return true;
		}
		LocalResource lr = (LocalResource) current;
		if (lr.getFunctionalMetadatum() != null
				&& lr.getFunctionalMetadatum().getId()
						.equals(this.functionalMetadatumValue)) {
			return true;
		}
		return false;
	}

	public boolean filterMimeTypes(Object current) {
		if (this.filterMimeTypeValue == null
				|| this.filterMimeTypeValue.length() == 0) {
			return true;
		}
		LocalResource lr = (LocalResource) current;
		if (lr.getContentType() != null
				&& lr.getContentType().toLowerCase()
						.contains(this.filterMimeTypeValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	public boolean filterSemanticMetadatum(Object current) {
		if (this.semanticMetadatumValue == null
				|| this.semanticMetadatumValue < 0) {
			return true;
		}
		LocalResource lr = (LocalResource) current;
		if (lr.getSemanticMetadatum() != null
				&& lr.getSemanticMetadatum().getId()
						.equals(this.semanticMetadatumValue)) {
			return true;
		}
		return false;
	}

	public boolean filterUsers(Object current) {
		if (this.filterUserValue == null || this.filterUserValue.length() == 0) {
			return true;
		}
		LocalResource lr = (LocalResource) current;
		if (lr.getCrawlerUser() != null
				&& lr.getCrawlerUser().getUsername() != null
				&& lr.getCrawlerUser().getUsername()
						.contains(this.filterUserValue.toLowerCase())) {
			return true;
		}
		return false;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Integer> getAllFunctionalMetadataMap() {
		List<Object[]> listFM = this.entityManager
				.createQuery(
						"select m.id, m.description from FunctionalMetadatum m order by m.description")
				.getResultList();
		Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
		// ret.put("Tutti", -1);
		for (Object[] o : listFM) {
			ret.put((String) o[1], (Integer) o[0]);
		}
		return ret;
	}

	public Map<String, Integer> getAllFunctionalMetadataMapPlusAll() {
		Map<String, Integer> ret = this.getAllFunctionalMetadataMap();
		ret.put("Tutti", -1);
		return ret;
	}

	public Map<String, Integer> getAllFunctionalMetadataMapPlusNull() {
		LinkedHashMap<String, Integer> ret1 = new LinkedHashMap<String, Integer>();
		ret1.put("", -1);
		ret1.putAll(this.getAllFunctionalMetadataMap());
		return ret1;
	}

	@SuppressWarnings("unchecked")
	public List<SelectItem> getAllMetadata() {
		this.allMetadata.clear();
		this.allMetadata.add(new SelectItem("Tutti", "Tutti"));
		List<Object[]> listFM = this.entityManager
				.createQuery(
						"select m.description, m.description from FunctionalMetadatum m order by m.description")
				.getResultList();
		List<Object[]> listSM = this.entityManager
				.createQuery(
						"select m.description,m.description from SemanticMetadatum m order by m.description")
				.getResultList();
		for (Object[] o : listSM) {
			this.allMetadata.add(new SelectItem((String) o[1], (String) o[0]));
		}
		for (Object[] o : listFM) {
			this.allMetadata.add(new SelectItem((String) o[1], (String) o[0]));
		}
		return this.allMetadata;
	}

	@SuppressWarnings("unchecked")
	public Map<String, Integer> getAllSemanticMetadataMap() {
		List<Object[]> listFM = this.entityManager
				.createQuery(
						"select m.id, m.description from SemanticMetadatum m order by m.description")
				.getResultList();
		Map<String, Integer> ret = new LinkedHashMap<String, Integer>();
		// ret.put("Tutti", -1);
		for (Object[] o : listFM) {
			ret.put((String) o[1], (Integer) o[0]);
		}
		return ret;
	}

	public Map<String, Integer> getAllSemanticMetadataMapPlusAll() {
		Map<String, Integer> ret = this.getAllSemanticMetadataMap();
		ret.put("Tutti", -1);
		return ret;
	}

	public Map<String, Integer> getAllSemanticMetadataMapPlusNull() {
		LinkedHashMap<String, Integer> ret1 = new LinkedHashMap<String, Integer>();
		ret1.put("", -1);
		ret1.putAll(this.getAllSemanticMetadataMap());
		return ret1;
	}

	@SuppressWarnings("unchecked")
	private User getCurrentUser() {
		List<User> users = this.entityManager
				.createQuery("from User u where u.username=:username")
				.setParameter("username",
						this.identity.getCredentials().getUsername())
				.getResultList();
		if (users.size() == 1) {
			return users.get(0);
		}
		return null;
	}

	public String getFilterFileNameValue() {
		return this.filterFileNameValue;
	}

	public String getFilterMimeTypeValue() {
		return this.filterMimeTypeValue;
	}

	public String getFilterUserValue() {
		return this.filterUserValue;
	}

	public Integer getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public Integer getFunctionalMetadatumValue() {
		return this.functionalMetadatumValue;
	}

	public Integer getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public Integer getSemanticMetadatumValue() {
		return this.semanticMetadatumValue;
	}

	@Factory("lrList")
	public void initLrList() {
		List<LocalResource> retList = new ArrayList<LocalResource>();
		User currentUser = this.getCurrentUser();
		retList.addAll(currentUser.getLocalResources());
		for (User assignedUser : currentUser.getAssignedUsers()) {
			retList.addAll(assignedUser.getLocalResources());
		}
		Collections.sort(retList, new LocalResourceFilenameComparator());
		this.lrList = retList;
	}

	public void listener(UploadEvent event) throws Exception {
		String localResourceDir = this.entityManager.find(Parameter.class,
				Parameter.LOCAL_RESOURCES_DIR.getKey()).getValue();
		UploadItem item = event.getUploadItem();
		String uuid = UUID.randomUUID().toString();
		File f = item.getFile();
		try {
			String digest = MD5DigestCreator.getMD5Digest(f);
			Long count = (Long) this.entityManager
					.createQuery(
							"select count(*) from LocalResource l where l.digest=:digest")
					.setParameter("digest", digest).getSingleResult();
			if (count > 0) {
				this.facesMessages.addFromResourceBundle(Severity.ERROR,
						"error message saying file already present");
				return;
			}
			User currentUser = this.getCurrentUser();
			FileUtils.copyFile(f, new File(new File(localResourceDir), uuid));
			LocalResource lr = new LocalResource();
			lr.setArchiveDate(new Date());
			Metadata md = new Metadata();
			ContentHandler contentHandler = new BodyContentHandler();
			md.set(TikaMetadataKeys.RESOURCE_NAME_KEY, f.getName());
			Parser parser = new AutoDetectParser();
			parser.parse(new FileInputStream(f), contentHandler, md);
			lr.setContentType(md.get(HttpHeaders.CONTENT_TYPE));
			lr.setCrawlerUser(currentUser);
			lr.setDigest(digest);
			lr.setLength(f.length());
			lr.setOrigFileName(item.getFileName());
			lr.setUniqueFileName(uuid);
			this.entityManager.persist(lr);
			currentUser.getLocalResources().add(lr);
			this.entityManager.merge(currentUser);
			this.lrList.add(lr);
		} catch (IOException e) {
			throw new FileHandlingException(
					this.messages.get("FileSavingError"));
		} catch (NoSuchAlgorithmException e) {
			throw new Exception();
		} catch (SAXException e) {
			throw new MimeTypeException();
		} catch (TikaException e) {
			throw new MimeTypeException();
		}
	}

	public void setAllMetadata(List<SelectItem> allMetadata) {
		this.allMetadata = allMetadata;
	}

	public void setFilterFileNameValue(String filterFileNameValue) {
		this.filterFileNameValue = filterFileNameValue;
	}

	public void setFilterMimeTypeValue(String filterMimeTypeValue) {
		this.filterMimeTypeValue = filterMimeTypeValue;
	}

	public void setFilterUserValue(String filterUserValue) {
		this.filterUserValue = filterUserValue;
	}

	public void setFunctionalMetadatum(Integer functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setFunctionalMetadatumValue(Integer functionalMetadatumValue) {
		this.functionalMetadatumValue = functionalMetadatumValue;
	}

	public void setSemanticMetadatum(Integer semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSemanticMetadatumValue(Integer semanticMetadatumValue) {
		this.semanticMetadatumValue = semanticMetadatumValue;
	}

	public void toggleDeleted(LocalResource localResource) {
		LocalResource lr = this.entityManager.find(LocalResource.class,
				localResource.getId());
		if (lr != null) {
			lr.setDeleted(!lr.isDeleted());
			this.entityManager.merge(lr);
		}
	}
}
