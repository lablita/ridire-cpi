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
package it.drwolf.ridire.index.cwb;

import it.drwolf.ridire.entity.FunctionalMetadatum;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.entity.SemanticMetadatum;
import it.drwolf.ridire.index.results.FrequencyItem;
import it.drwolf.ridire.util.async.ExcelDataGenerator;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.async.Asynchronous;
import org.jboss.seam.transaction.UserTransaction;

@Name("cwbFrequencyList")
@Scope(ScopeType.CONVERSATION)
public class CWBFrequencyList {

	@In
	private EntityManager entityManager;

	@In
	private CorpusSizeParams corpusSizeParams;
	private static final long TIMEOUT = 3000000; // 10 mins
	private Integer quantity = 100;
	private Integer threshold = 0;
	private String frequencyBy;
	private List<FrequencyItem> frequencyList;

	private List<String> corporaNames = new ArrayList<String>();
	private List<String> frequencyByValues = new ArrayList<String>() {
		private static final long serialVersionUID = 7353762879575717983L;
		{
			this.add("forma");
			this.add("PoS");
			this.add("lemma");
			this.add("PoS-forma");
			this.add("PoS-lemma");
		}
	};
	private List<String> functionalMetadatum;
	private List<String> semanticMetadatum;

	private UserTransaction userTx;

	private String cqpExecutable;

	private String cqpCorpusName;

	private String cqpRegistry;

	private String cwbscanExecutable;
	@In(create = true)
	private ExcelDataGenerator excelDataGenerator;

	public void calculateFrequencyList() {
		this.excelDataGenerator.setFileReady(false);
		List<String> semDescription = new ArrayList<String>();
		for (String sm : this.getSemanticMetadatum()) {
			if (!sm.equals("-1")) {
				semDescription.add(this.entityManager.find(
						SemanticMetadatum.class, Integer.parseInt(sm))
						.getDescription().trim().replaceAll("\\s", "_"));
			}
		}
		List<String> funDescription = new ArrayList<String>();
		for (String fm : this.getFunctionalMetadatum()) {
			if (!fm.equals("-1")) {
				funDescription.add(this.entityManager.find(
						FunctionalMetadatum.class, Integer.parseInt(fm))
						.getDescription().trim().replaceAll("\\s", "_"));
			}
		}
		this.getFrequencyList(true, semDescription, funDescription, this
				.getQuantity(), this.getFrequencyBy(), this.getThreshold(),
				true);
	}

	@Asynchronous
	public void calculateFrequencyListForEachCorpora() {
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		try {
			this.userTx.setTransactionTimeout(10 * 10 * 60);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<SemanticMetadatum> resultList = this.entityManager
					.createQuery("from SemanticMetadatum sm").getResultList();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			for (SemanticMetadatum sm : resultList) {
				this.createFreqTable("lemma", sm.getDescription().replaceAll(
						"\\s", "_"), null, sm.getId() + "", "-1");
				this.createFreqTable("forma", sm.getDescription().replaceAll(
						"\\s", "_"), null, sm.getId() + "", "-1");
				this.createFreqTable("PoS", sm.getDescription().replaceAll(
						"\\s", "_"), null, sm.getId() + "", "-1");
				this.createFreqTable("easypos", sm.getDescription().replaceAll(
						"\\s", "_"), null, sm.getId() + "", "-1");
			}
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<FunctionalMetadatum> resultList2 = this.entityManager
					.createQuery("from FunctionalMetadatum fm").getResultList();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			for (FunctionalMetadatum fm : resultList2) {
				this.createFreqTable("lemma", null, fm.getDescription()
						.replaceAll("\\s", "_"), "-1", fm.getId() + "");
				this.createFreqTable("forma", null, fm.getDescription()
						.replaceAll("\\s", "_"), "-1", fm.getId() + "");
				this.createFreqTable("PoS", null, fm.getDescription()
						.replaceAll("\\s", "_"), "-1", fm.getId() + "");
				this.createFreqTable("easypos", null, fm.getDescription()
						.replaceAll("\\s", "_"), "-1", fm.getId() + "");
			}
			this.createFreqTable("lemma", null, null, "-1", "-1");
			this.createFreqTable("forma", null, null, "-1", "-1");
			this.createFreqTable("PoS", null, null, "-1", "-1");
			this.createFreqTable("easypos", null, null, "-1", "-1");
			this.corpusSizeParams.init();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
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

	private void createFreqTable(String type, String smDescription,
			String fmDescription, String smId, String fmId)
			throws SystemException, NotSupportedException, SecurityException,
			IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		String tableName = "freq_" + type + "_";
		if (smDescription != null) {
			tableName += smDescription.replaceAll("\\s", "_");
		} else {
			if (fmDescription != null) {
				tableName += fmDescription.replaceAll("\\s", "_");
			} else {
				tableName += "all";
			}
		}
		System.out.print("Creating freq table: " + tableName + " ... ");
		List<String> smIds = new ArrayList<String>();
		smIds.add(smId);
		this.setSemanticMetadatum(smIds);
		List<String> fmIds = new ArrayList<String>();
		fmIds.add(fmId);
		this.setFunctionalMetadatum(fmIds);
		if (!this.userTx.isActive()) {
			this.userTx.begin();
		}
		this.entityManager.joinTransaction();
		this.entityManager.createNativeQuery(
				"drop table if exists " + tableName).executeUpdate();
		this.entityManager
				.createNativeQuery(
						"CREATE TABLE `"
								+ tableName
								+ "` (`freq` int(11) unsigned DEFAULT NULL, `item` varchar(210) NOT NULL,  PRIMARY KEY (`item`)) ENGINE=MyISAM DEFAULT CHARSET=utf8")
				.executeUpdate();
		this.entityManager.flush();
		this.entityManager.clear();
		this.userTx.commit();
		List<String> smDescriptions = new ArrayList<String>();
		smDescriptions.add(smDescription);
		List<String> fmDescriptions = new ArrayList<String>();
		fmDescriptions.add(fmDescription);
		String filename = this.getFrequencyList(false, smDescriptions,
				fmDescriptions, Integer.MAX_VALUE, type, 0, false);
		System.out.print(" Done. File upload... ");
		File f = new File(filename);
		if (f.canRead() && FileUtils.sizeOf(f) > 0) {
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.entityManager.createNativeQuery(
					"LOAD DATA LOCAL INFILE '" + filename + "' INTO TABLE "
							+ tableName + " CHARACTER SET utf8")
					.executeUpdate();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
		}
		FileUtils.deleteQuietly(new File(filename));
		System.out.println("Done.");
	}

	public List<String> getCorporaNames() {
		return this.corporaNames;
	}

	public String getFrequencyBy() {
		return this.frequencyBy;
	}

	public List<String> getFrequencyByValues() {
		return this.frequencyByValues;
	}

	public List<FrequencyItem> getFrequencyList() {
		return this.frequencyList;
	}

	private String getFrequencyList(boolean deleteFLFile,
			List<String> semDescription, List<String> funDescription,
			int quantityP, String type, Integer threshold, boolean sorted) {
		CommandLine commandLine = CommandLine.parse(this.cwbscanExecutable);
		commandLine.addArgument("-q");
		if (threshold != null && threshold > 0) {
			commandLine.addArgument("-f");
			commandLine.addArgument(threshold + "");
		}
		commandLine.addArgument("-r").addArgument(this.cqpRegistry);
		commandLine.addArgument("-C");
		commandLine.addArgument(this.cqpCorpusName);
		if (type.equals("forma")) {
			commandLine.addArgument("word+0");
		} else if (type.equals("PoS")) {
			commandLine.addArgument("pos+0");
		} else if (type.equals("easypos")) {
			commandLine.addArgument("easypos+0");
		} else if (type.equals("lemma")) {
			commandLine.addArgument("lemma+0");
		} else if (type.equals("PoS-forma")) {
			commandLine.addArgument("pos+0");
			commandLine.addArgument("word+0");
		} else if (type.equals("PoS-lemma")) {
			commandLine.addArgument("pos+0");
			commandLine.addArgument("lemma+0");
		}
		String semFuncParam = "";
		if (funDescription != null && funDescription.size() > 0
				&& funDescription.get(0) != null
				&& funDescription.get(0).trim().length() > 0
				|| semDescription != null && semDescription.size() > 0
				&& semDescription.get(0) != null
				&& semDescription.get(0).trim().length() > 0) {
			semFuncParam = "?";
			if (funDescription != null && funDescription.size() > 0
					&& funDescription.get(0) != null
					&& funDescription.get(0).trim().length() > 0) {
				String fd = StringUtils.join(funDescription, "\\|");
				semFuncParam += "text_functional=/\\(" + fd + "\\)/ ";
			}
			if (semDescription != null && semDescription.size() > 0
					&& semDescription.get(0) != null
					&& semDescription.get(0).trim().length() > 0) {
				String sd = StringUtils.join(semDescription, "\\|");
				semFuncParam += "text_semantic=/\\(" + sd + "\\)/ ";

			}
			commandLine.addArgument(semFuncParam);
		}
		if (sorted) {
			commandLine.addArgument("|");
			commandLine.addArgument("sort");
			commandLine.addArgument("-nr");
			commandLine.addArgument("-k");
			commandLine.addArgument("1");
		}
		if (quantityP > 0) {
			commandLine.addArgument("|");
			commandLine.addArgument("head");
			commandLine.addArgument("-" + quantityP);
		}
		File flTempFile = null;
		try {
			flTempFile = File.createTempFile("ridireFL", null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		commandLine.addArgument(" > ");
		commandLine.addArgument(flTempFile.getAbsolutePath());
		String c = commandLine.toString();
		try {
			File tempSh = File.createTempFile("ridireSH", ".sh");
			FileUtils.writeStringToFile(tempSh, c);
			tempSh.setExecutable(true);
			commandLine = CommandLine.parse(tempSh.getAbsolutePath());
			DefaultExecutor executor = new DefaultExecutor();
			executor.setExitValue(0);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(
					CWBFrequencyList.TIMEOUT);
			executor.setWatchdog(watchdog);
			ByteArrayOutputStream baosStdOut = new ByteArrayOutputStream(1024);
			ByteArrayOutputStream baosStdErr = new ByteArrayOutputStream(1024);
			ExecuteStreamHandler executeStreamHandler = new PumpStreamHandler(
					baosStdOut, baosStdErr, null);
			executor.setStreamHandler(executeStreamHandler);
			int exitValue = 0;
			exitValue = executor.execute(commandLine);
			FileUtils.deleteQuietly(tempSh);
			if (exitValue == 0) {
				StrTokenizer strTokenizer = new StrTokenizer();
				this.frequencyList = new ArrayList<FrequencyItem>();
				List<String> lines = FileUtils.readLines(flTempFile);
				for (String line : lines) {
					strTokenizer.reset(line);
					String[] tokens = strTokenizer.getTokenArray();
					if (tokens.length == 2) {
						FrequencyItem frequencyItem = new FrequencyItem(
								tokens[1], Integer.parseInt(tokens[0].trim()));
						this.frequencyList.add(frequencyItem);
					} else if (tokens.length == 3) {
						FrequencyItem frequencyItem = new FrequencyItem(
								tokens[2], tokens[1], Integer
										.parseInt(tokens[0].trim()));
						this.frequencyList.add(frequencyItem);
					}
				}
				if (deleteFLFile) {
					FileUtils.deleteQuietly(flTempFile);
				}
			}
		} catch (ExecuteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return flTempFile.getAbsolutePath();
	}

	public List<String> getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public Integer getQuantity() {
		return this.quantity;
	}

	public List<String> getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public Integer getThreshold() {
		return this.threshold;
	}

	@Create
	public void initParams() {
		this.cqpExecutable = this.entityManager.find(Parameter.class,
				Parameter.CQP_EXECUTABLE.getKey()).getValue();
		this.cwbscanExecutable = this.entityManager.find(Parameter.class,
				Parameter.CWBSCAN_EXECUTABLE.getKey()).getValue();
		this.cqpCorpusName = this.entityManager.find(Parameter.class,
				Parameter.CQP_CORPUSNAME.getKey()).getValue();
		this.cqpRegistry = this.entityManager.find(Parameter.class,
				Parameter.CQP_REGISTRY.getKey()).getValue();
	}

	public void setCorporaNames(List<String> corporaNames) {
		this.corporaNames = corporaNames;
	}

	public void setFrequencyBy(String frequencyBy) {
		this.frequencyBy = frequencyBy;
	}

	public void setFrequencyByValues(List<String> frequencyByValues) {
		this.frequencyByValues = frequencyByValues;
	}

	public void setFrequencyList(List<FrequencyItem> frequencyList) {
		this.frequencyList = frequencyList;
	}

	public void setFunctionalMetadatum(List<String> functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setQuantity(Integer quantity) {
		this.quantity = quantity;
	}

	public void setSemanticMetadatum(List<String> semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setThreshold(Integer threshold) {
		this.threshold = threshold;
	}
}
