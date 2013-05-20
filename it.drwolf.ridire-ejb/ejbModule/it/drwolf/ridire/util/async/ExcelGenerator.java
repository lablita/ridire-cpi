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
package it.drwolf.ridire.util.async;

import it.drwolf.ridire.index.cwb.CWBConcordancer;
import it.drwolf.ridire.index.cwb.CWBFrequencyList;
import it.drwolf.ridire.index.results.CWBResult;
import it.drwolf.ridire.index.results.FrequencyItem;
import it.drwolf.ridire.util.DocumentDownloader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.async.Asynchronous;

@Name("excelGenerator")
public class ExcelGenerator {

	private static final int PAGE_SIZE = 100;
	@In(create = true)
	private CWBConcordancer cwbConcordancer;

	@In(create = true)
	private CWBFrequencyList cwbFrequencyList;

	@Asynchronous
	public void generateExcelTable(ExcelDataGenerator excelDataGenerator) {
		excelDataGenerator.setProgress(0);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.cwbConcordancer.setForma(excelDataGenerator.getForma());
		this.cwbConcordancer.setContextGroupingLength(excelDataGenerator
				.getContextGroupingLength());
		this.cwbConcordancer.setContextLength(excelDataGenerator
				.getContextLength());
		this.cwbConcordancer.setLemma(excelDataGenerator.getLemma());
		this.cwbConcordancer.setPhrase(excelDataGenerator.getPhrase());
		this.cwbConcordancer.setPos(excelDataGenerator.getPos());
		this.cwbConcordancer.setSortBy(excelDataGenerator.getSortBy());
		this.cwbConcordancer.setSortOrder(excelDataGenerator.getSortOrder());
		this.cwbConcordancer.setToBeVisualized(excelDataGenerator
				.getToBeVisualized());
		this.cwbConcordancer.setFunctionalMetadatum(excelDataGenerator
				.getFunctionalMetadatum());
		this.cwbConcordancer.setSemanticMetadatum(excelDataGenerator
				.getSemanticMetadatum());
		Workbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet(DocumentDownloader.CONCORDANZE);
		int rowNumber = 0;
		int results4DownloadSize = this.cwbConcordancer
				.getResults4DownloadSize();
		// System.out.println("Query size: " + results4DownloadSize);
		int i = 0;
		for (int start = 0; start < results4DownloadSize; start += ExcelGenerator.PAGE_SIZE) {
			List<CWBResult> results4Download = this.cwbConcordancer
					.getResults4Download(start, ExcelGenerator.PAGE_SIZE);
			for (CWBResult itemWithContext : results4Download) {
				Row row = sheet.createRow(rowNumber);
				++rowNumber;
				row.createCell(0).setCellValue(
						itemWithContext.getLeftContext().toString());
				row.createCell(1).setCellValue(
						itemWithContext.getSearchedText());
				row.createCell(2).setCellValue(
						itemWithContext.getRightContext().toString());
				excelDataGenerator.setProgress(Math.round(++i
						/ (results4DownloadSize * 1.0f) * 100));
			}
		}
		try {
			workbook.write(baos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		excelDataGenerator.setFileReady(true);
		excelDataGenerator.setBaos(baos);
		excelDataGenerator.setProgress(100);
		excelDataGenerator.setInProgress(false);
	}

	@Asynchronous
	public void generateFLTable(ExcelDataGenerator excelDataGenerator) {
		excelDataGenerator.setProgress(0);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		this.cwbFrequencyList.setFunctionalMetadatum(excelDataGenerator
				.getFunctionalMetadatum());
		this.cwbFrequencyList.setSemanticMetadatum(excelDataGenerator
				.getSemanticMetadatum());
		this.cwbFrequencyList.setFrequencyBy(excelDataGenerator
				.getFrequencyBy());
		this.cwbFrequencyList.setQuantity(excelDataGenerator.getQuantity());
		this.cwbFrequencyList.setThreshold(excelDataGenerator.getThreshold());
		Workbook workbook = new HSSFWorkbook();
		Sheet sheet = workbook.createSheet(DocumentDownloader.CONCORDANZE);
		int rowNumber = 0;
		this.cwbFrequencyList.calculateFrequencyList();
		int results4DownloadSize = this.cwbFrequencyList.getFrequencyList()
				.size();
		// System.out.println("Query size: " + results4DownloadSize);
		int i = 0;
		for (int start = 0; start < results4DownloadSize; start += ExcelGenerator.PAGE_SIZE) {
			List<FrequencyItem> results4Download = this.cwbFrequencyList
					.getFrequencyList().subList(
							start,
							Math.min(start + ExcelGenerator.PAGE_SIZE,
									results4DownloadSize));
			for (FrequencyItem frequencyItem : results4Download) {
				Row row = sheet.createRow(rowNumber);
				++rowNumber;
				row.createCell(0)
						.setCellValue(frequencyItem.getFormaPosLemma());
				if (excelDataGenerator.getFrequencyBy().equals("PoS-forma")
						|| excelDataGenerator.getFrequencyBy().equals(
								"PoS-lemma")) {
					row.createCell(1).setCellValue(frequencyItem.getPos());
					row.createCell(2)
							.setCellValue(frequencyItem.getFrequency());
				} else {
					row.createCell(1)
							.setCellValue(frequencyItem.getFrequency());
				}
				excelDataGenerator.setProgress(Math.round(++i
						/ (results4DownloadSize * 1.0f) * 100));
			}
		}
		try {
			workbook.write(baos);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		excelDataGenerator.setFileReady(true);
		excelDataGenerator.setBaos(baos);
		excelDataGenerator.setProgress(100);
		excelDataGenerator.setInProgress(false);
	}
}
