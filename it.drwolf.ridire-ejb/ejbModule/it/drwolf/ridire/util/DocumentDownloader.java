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
package it.drwolf.ridire.util;

import it.drwolf.ridire.entity.LocalResource;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.index.cwb.CWBConcordancer;
import it.drwolf.ridire.index.cwb.CWBFrequencyList;
import it.drwolf.ridire.util.async.ExcelDataGenerator;
import it.drwolf.ridire.util.async.ExcelGenerator;
import it.drwolf.ridire.util.exceptions.FileHandlingException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.persistence.EntityManager;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.faces.FacesMessages;

@Name("documentDownloader")
@Scope(ScopeType.SESSION)
public class DocumentDownloader {
	public static final String CONCORDANZE = "Concordanze";

	private static final String EXCELMIMETYPE = "application/vnd.ms-excel";
	@In(create = true)
	private Map<String, String> messages;

	@In
	public EntityManager entityManager;

	@In
	private CWBConcordancer cwbConcordancer;

	@In(required = false)
	private CWBFrequencyList cwbFrequencyList;
	@In(value = "#{facesContext.externalContext}")
	public ExternalContext extCtx;

	@In(create = true)
	private ExcelDataGenerator excelDataGenerator;

	@In(create = true)
	private ExcelGenerator excelGenerator;

	public String download(LocalResource localResource)
			throws FileHandlingException {
		if (localResource == null) {
			FacesMessages.instance().add(
					"Impossibile recuperare il documento.", null);
			return "Dowload_ko";
		}
		HttpServletResponse response = (HttpServletResponse) this.extCtx
				.getResponse();
		String mimeType = localResource.getContentType();
		if (mimeType == null) {
			mimeType = "application/octet-stream";
		}
		response.setContentType(mimeType);
		response.addHeader("Content-disposition", "attachment; filename=\""
				+ localResource.getOrigFileName() + "\"");
		String localResourceDir = this.entityManager.find(Parameter.class,
				Parameter.LOCAL_RESOURCES_DIR.getKey()).getValue();
		File file = new File(new File(localResourceDir),
				localResource.getUniqueFileName());
		InputStream fis;
		try {
			fis = new FileInputStream(file);
			byte[] buf = new byte[(int) file.length()];
			// Read in the bytes
			int offset = 0;
			int numRead = 0;
			while (offset < buf.length
					&& (numRead = fis.read(buf, offset, buf.length - offset)) >= 0) {
				offset += numRead;
			}

			// Ensure all the bytes have been read in
			if (offset < buf.length) {
				throw new IOException("Could not completely read file "
						+ file.getName());
			}
			// Close the input stream and return bytes
			fis.close();
			ServletOutputStream os = response.getOutputStream();
			os.write(buf);
			os.flush();
			os.close();
			FacesContext.getCurrentInstance().responseComplete();
		} catch (FileNotFoundException e1) {
			throw new FileHandlingException(
					this.messages.get("FileRetrievingError"));
		} catch (IOException e) {
			throw new FileHandlingException(
					this.messages.get("FileRetrievingError"));
		}
		return "Download ok";
	}

	public String exportCSVTable() {
		this.excelDataGenerator.setProgress(0);
		HttpServletResponse response = (HttpServletResponse) this.extCtx
				.getResponse();
		response.setContentType(DocumentDownloader.EXCELMIMETYPE);
		response.addHeader("Content-disposition",
				"attachment; filename=\"concordances.xls\"");
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control",
				"must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		try {
			ServletOutputStream os = response.getOutputStream();
			int size = this.excelDataGenerator.getBaos().size();
			// System.out.println(size);
			response.setContentLength(size);
			this.excelDataGenerator.getBaos().writeTo(os);
			os.flush();
			os.close();
			FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Download ok";
	}

	public String exportFLTable() {
		this.excelDataGenerator.setProgress(0);
		HttpServletResponse response = (HttpServletResponse) this.extCtx
				.getResponse();
		response.setContentType(DocumentDownloader.EXCELMIMETYPE);
		response.addHeader("Content-disposition",
				"attachment; filename=\"frequencylist.xls\"");
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control",
				"must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		try {
			ServletOutputStream os = response.getOutputStream();
			int size = this.excelDataGenerator.getBaos().size();
			// System.out.println(size);
			response.setContentLength(size);
			this.excelDataGenerator.getBaos().writeTo(os);
			os.flush();
			os.close();
			FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "Download ok";
	}

	public void getExcelTable() {
		this.excelDataGenerator.setInProgress(true);
		this.excelDataGenerator.setForma(this.cwbConcordancer.getForma());
		this.excelDataGenerator.setContextGroupingLength(this.cwbConcordancer
				.getContextGroupingLength());
		this.excelDataGenerator.setContextLength(this.cwbConcordancer
				.getContextLength());
		this.excelDataGenerator.setLemma(this.cwbConcordancer.getLemma());
		this.excelDataGenerator.setPhrase(this.cwbConcordancer.getPhrase());
		this.excelDataGenerator.setEasypos(this.cwbConcordancer.getEasypos());
		this.excelDataGenerator.setPos(this.cwbConcordancer.getPos());
		this.excelDataGenerator.setSortBy(this.cwbConcordancer.getSortBy());
		this.excelDataGenerator.setSortOrder(this.cwbConcordancer
				.getSortOrder());
		this.excelDataGenerator.setToBeVisualized(this.cwbConcordancer
				.getToBeVisualized());
		this.excelDataGenerator.setCorporaNames(this.cwbConcordancer
				.getCorporaNames());
		this.excelDataGenerator.setFunctionalMetadatum(this.cwbConcordancer
				.getFunctionalMetadatum());
		this.excelDataGenerator.setSemanticMetadatum(this.cwbConcordancer
				.getSemanticMetadatum());
		this.excelGenerator.generateExcelTable(this.excelDataGenerator);
		// System.out.println("Done");
	}

	public void getFLTable() {
		this.excelDataGenerator.setInProgress(true);
		this.excelDataGenerator.setFileReady(false);
		this.excelDataGenerator.setFrequencyBy(this.cwbFrequencyList
				.getFrequencyBy());

		this.excelDataGenerator
				.setQuantity(this.cwbFrequencyList.getQuantity());
		this.excelDataGenerator.setThreshold(this.cwbFrequencyList
				.getThreshold());
		this.excelDataGenerator.setFunctionalMetadatum(this.cwbFrequencyList
				.getFunctionalMetadatum());
		this.excelDataGenerator.setSemanticMetadatum(this.cwbFrequencyList
				.getSemanticMetadatum());
		this.excelDataGenerator.setFrequencyBy(this.cwbFrequencyList
				.getFrequencyBy());
		this.excelGenerator.generateFLTable(this.excelDataGenerator);
	}
}
