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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.persistence.EntityManager;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.core.Conversation;
import org.jboss.seam.transaction.UserTransaction;

@Name("cwbCollocatesExtractor")
@Scope(ScopeType.CONVERSATION)
public class CWBCollocatesExtractor {
	public static final String LOG_LIKELIHOOD = "Log Likelihood";
	public static final String MI_SCORE = "MI Score";
	public static final String T_SCORE = "T Score";
	public static final String LOGDICE_SCORE = "logDice";
	private String score = CWBCollocatesExtractor.LOGDICE_SCORE;

	private static final String COLLOCATE_DB_PREFIX = "collocate_db_";
	private static final int CWB_COLLOCATES_EXTRACTOR_TIMEOUT = 1000 * 60 * 10; // 10
	private static final String BASED_ON_FORMA = "forma";
	private static final String BASED_ON_POS = "PoS";
	private static final String BASED_ON_LEMMA = "lemma";
	// mins
	private EntityManager entityManager;

	@In
	private CorpusSizeParams corpusSizeParams;

	private String conversationId;
	private String query;
	private Integer distanceLeft = 3;
	private Integer distanceRight = 3;
	private Integer freqMinNC = 5;
	private Integer freqMinColl = 5;
	private Integer resultsSize = 0;

	private UserTransaction userTx;

	private String cqpExecutable;

	private String cqpCorpusName;

	private String cqpRegistry;

	private boolean includePos = true;

	private boolean includeLemmas = true;

	private final List<String> availableScores = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6716237565009505120L;

		{
			this.add(CWBCollocatesExtractor.LOGDICE_SCORE);
			this.add(CWBCollocatesExtractor.MI_SCORE);
			this.add(CWBCollocatesExtractor.LOG_LIKELIHOOD);
			this.add(CWBCollocatesExtractor.T_SCORE);

		}
	};

	private List<String> availableBasedOn = new ArrayList<String>();
	private ArrayList<CWBCollocateResult> cwbCollocateResults;

	private int pageSize = 20;

	private Integer firstResult;

	private Integer concordancesResultsSize;

	private String collocationBasedOn = CWBCollocatesExtractor.BASED_ON_FORMA;

	private String filterOnPoS;

	private Integer functionalMetadatum = -1;

	private Integer semanticMetadatum = -1;

	private String createAWKString() {
		StringBuffer awkStringBuffer = new StringBuffer();
		awkStringBuffer.append("BEGIN{ OFS = FS = \"@@##\" } ");
		int counter = 4;
		for (int i = -this.getDistanceLeft(); i <= this.getDistanceRight(); i++) {
			if (i == 0) {
				continue;
			}
			awkStringBuffer.append("{ print $3, $1, $2, NR-1, " + i + ", $"
					+ counter + " ");
			++counter;
			if (this.isIncludePos() || this.isIncludeLemmas()) {
				awkStringBuffer.append(", $" + counter + " ");
				++counter;
			}
			if (this.isIncludePos() && this.isIncludeLemmas()) {
				awkStringBuffer.append(", $" + counter + " ");
				++counter;
			}
			awkStringBuffer.append("}");
		}
		return awkStringBuffer.toString();
	}

	private String createTabulateString(File tmpAwk, File tmpTabulate) {
		StringBuffer tabulateStringBuffer = new StringBuffer();
		tabulateStringBuffer
				.append("set AutoShow off;\nset ProgressBar off;\nset PrettyPrint off;\nset Context 5 words;\nset LeftKWICDelim '--%%%--';\nset RightKWICDelim '--%%%--';\nshow -cpos;\n"
						+ this.getQuery() + ";\n");
		tabulateStringBuffer
				.append("tabulate C match, matchend, match text_id");
		for (int i = -this.getDistanceLeft(); i <= this.getDistanceRight(); i++) {
			if (i == 0) {
				continue;
			}
			tabulateStringBuffer.append(", match" + (i > 0 ? "end" : "") + "["
					+ i + "] word");
			if (this.isIncludeLemmas()) {
				tabulateStringBuffer.append(", match" + (i > 0 ? "end" : "")
						+ "[" + i + "] lemma");
			}
			if (this.isIncludePos()) {
				tabulateStringBuffer.append(", match" + (i > 0 ? "end" : "")
						+ "[" + i + "] easypos");
			}
		}
		tabulateStringBuffer.append(" >  \"| awk -f '"
				+ tmpAwk.getAbsolutePath() + "' | sed -e 's/@@##/\\t/g' > '"
				+ tmpTabulate.getAbsolutePath() + "'\";");
		return tabulateStringBuffer.toString();
	}

	private void dropDB() throws SystemException, NotSupportedException,
			SecurityException, IllegalStateException, RollbackException,
			HeuristicMixedException, HeuristicRollbackException {
		if (!this.userTx.isActive()) {
			this.userTx.begin();
		}
		this.entityManager.joinTransaction();
		this.entityManager.createNativeQuery(
				"drop table if exists "
						+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
						+ this.conversationId).executeUpdate();
		this.entityManager.flush();
		this.entityManager.clear();
		this.userTx.commit();
	}

	public String extractCollocates(boolean calculateSize) {
		try {
			this.dropDB();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			String createDBQuery = "CREATE TABLE `"
					+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
					+ this.conversationId
					+ "` (`text_id` varchar(40) DEFAULT NULL,`beginPosition` int(11) DEFAULT NULL,`endPosition` int(11) DEFAULT NULL,`refnumber` bigint(20) NOT NULL,`dist` smallint(6) NOT NULL,`word` varchar(40) NOT NULL ";
			if (this.isIncludeLemmas()) {
				createDBQuery += ", `lemma` varchar(40) NOT NULL ";
			}
			if (this.isIncludePos()) {
				createDBQuery += ", `pos` varchar(40) NOT NULL ";
			}
			createDBQuery += ") ENGINE=MyISAM DEFAULT CHARSET=utf8";
			this.entityManager.createNativeQuery(createDBQuery).executeUpdate();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			File tmpTabulate = this.tabulate();
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.entityManager.createNativeQuery(
					"LOAD DATA LOCAL INFILE '" + tmpTabulate.getAbsolutePath()
							+ "' INTO TABLE "
							+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
							+ this.conversationId).executeUpdate();
			if (calculateSize) {
				if (this.getCollocationBasedOn().equals(
						CWBCollocatesExtractor.BASED_ON_LEMMA)) {
					this.resultsSize = ((Number) this.entityManager
							.createNativeQuery(
									"select count(distinct(lemma)) from "
											+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
											+ this.conversationId)
							.getSingleResult()).intValue();
				} else if (this.getCollocationBasedOn().equals(
						CWBCollocatesExtractor.BASED_ON_POS)) {
					this.resultsSize = ((Number) this.entityManager
							.createNativeQuery(
									"select count(distinct(pos)) from "
											+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
											+ this.conversationId)
							.getSingleResult()).intValue();
				} else {
					this.resultsSize = ((Number) this.entityManager
							.createNativeQuery(
									"select count(distinct(word)) from "
											+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
											+ this.conversationId)
							.getSingleResult()).intValue();
				}
			}
			long r1 = ((Number) this.entityManager.createNativeQuery(
					"select count(*) from "
							+ CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
							+ this.conversationId + " where dist between -"
							+ this.getDistanceLeft() + " and "
							+ this.getDistanceRight()).getSingleResult())
					.longValue();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			String collBasedOn = "word";
			String freqTable = "freq_";
			if (this.getCollocationBasedOn().equals(
					CWBCollocatesExtractor.BASED_ON_LEMMA)) {
				collBasedOn = "lemma";
				freqTable += "lemma_";
			} else if (this.getCollocationBasedOn().equals(
					CWBCollocatesExtractor.BASED_ON_POS)) {
				collBasedOn = "pos";
				freqTable += "easypos_";
			} else {
				freqTable += "forma_";
			}
			if (this.getFunctionalMetadatum() >= 0) {
				FunctionalMetadatum fm = this.entityManager.find(
						FunctionalMetadatum.class,
						this.getFunctionalMetadatum());
				freqTable += fm.getDescription().trim().replaceAll("\\s", "_");
			} else if (this.getSemanticMetadatum() >= 0) {
				SemanticMetadatum sm = this.entityManager.find(
						SemanticMetadatum.class, this.getSemanticMetadatum());
				freqTable += sm.getDescription().trim().replaceAll("\\s", "_");
			} else {
				freqTable += "all";
			}
			long n = this.corpusSizeParams
					.getCorpusSize(freqTable.substring(5)).longValue();
			String nativeQuery = "select %1$s.%2$s, count(%1$s.%2$s) as observed,("
					+ r1 + " * (%3$s.freq) / " + n + ") as expected,";
			String significance = " sign(COUNT(%1$s.%2$s) - ("
					+ r1
					+ " * (%3$s.freq) / "
					+ n
					+ ")) * 2 * ( IF(COUNT(%1$s.%2$s) > 0, COUNT(%1$s.%2$s) * log(COUNT(%1$s.%2$s) / ("
					+ r1
					+ " * (%3$s.freq) / "
					+ n
					+ ")), 0) + IF(("
					+ r1
					+ " - COUNT(%1$s.%2$s)) > 0, ("
					+ r1
					+ " - COUNT(%1$s.%2$s)) * log(("
					+ r1
					+ " - COUNT(%1$s.%2$s)) / ("
					+ r1
					+ " * ("
					+ n
					+ " - (%3$s.freq)) / "
					+ n
					+ ")), 0) + IF((CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s)) > 0, "
					+ "(CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s)) * log((CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s)) / ("
					+ (n - r1)
					+ " * (%3$s.freq) / "
					+ n
					+ ")), 0) + "
					+ "IF(("
					+ (n - r1)
					+ " - (CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s))) > 0, ("
					+ (n - r1)
					+ " - (CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s))) * log(("
					+ (n - r1)
					+ " - (CAST(%3$s.freq AS SIGNED) - COUNT(%1$s.%2$s))) / ("
					+ (n - r1) + " * (" + n + " - (%3$s.freq)) / " + n
					+ ")), 0) ) as significance, ";
			if (this.getScore().equals(CWBCollocatesExtractor.MI_SCORE)) {
				significance = "log2(COUNT(%1$s.%2$s)/(" + r1
						+ " * (%3$s.freq) / " + n + ")) as significance, ";
			} else if (this.getScore().equals(CWBCollocatesExtractor.T_SCORE)) {
				significance = "(COUNT(%1$s.%2$s) - (" + r1
						+ " * (%3$s.freq) / " + n
						+ "))/sqrt(COUNT(%1$s.%2$s)) as significance, ";
			} else if (this.getScore().equals(
					CWBCollocatesExtractor.LOGDICE_SCORE)) {
				String subquery = "SELECT COUNT(DISTINCT refnumber) from %1$s WHERE dist between -"
						+ this.getDistanceLeft()
						+ " and "
						+ this.getDistanceRight();
				if (!this.userTx.isActive()) {
					this.userTx.begin();
				}
				subquery = String.format(subquery,
						CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
								+ this.conversationId);
				this.entityManager.joinTransaction();
				Number diceNodeF = (Number) this.entityManager
						.createNativeQuery(subquery).getSingleResult();
				this.entityManager.flush();
				this.entityManager.clear();
				this.userTx.commit();
				String pCollNodeE = "(COUNT(DISTINCT refnumber) / "
						+ diceNodeF.longValue() + ")";
				String pNodeColl = "(COUNT(%1$s.%2$s) / (%3$s.freq))";
				significance = "2 / ((1 / " + pCollNodeE + ") + (1 / "
						+ pNodeColl + ")) as significance, ";

			}
			nativeQuery += significance;
			nativeQuery += " %3$s.freq, "
					+ "count(distinct(text_id)) as text_id_count "
					+ "from %1$s, %3$s where %1$s.%2$s = %3$s.item ";
			if (this.isIncludePos() && this.getFilterOnPoS() != null
					&& this.getFilterOnPoS().length() > 0
					&& !this.getFilterOnPoS().equals("Tutti")) {
				nativeQuery += " and %1$s.pos='" + this.getFilterOnPoS().trim()
						+ "'";
			}
			nativeQuery += " and dist between -" + this.getDistanceLeft()
					+ " and " + this.getDistanceRight() + " and %3$s.freq >= "
					+ this.getFreqMinColl()
					+ " group by %1$s.%2$s having observed >= "
					+ this.getFreqMinNC()
					+ " order by significance desc LIMIT "
					+ this.getFirstResult() + ", " + this.getPageSize();

			nativeQuery = String.format(nativeQuery,
					CWBCollocatesExtractor.COLLOCATE_DB_PREFIX
							+ this.conversationId, collBasedOn, freqTable);
			System.out.println(nativeQuery);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			List<Object[]> res = this.entityManager.createNativeQuery(
					nativeQuery).getResultList();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
			this.cwbCollocateResults = new ArrayList<CWBCollocateResult>();
			for (Object[] r : res) {
				CWBCollocateResult cwbCollocateResult = new CWBCollocateResult(
						r[0] + "", ((Number) r[4]).longValue(),
						((Number) r[2]).doubleValue(),
						((Number) r[1]).longValue(),
						((Number) r[5]).longValue(),
						((Number) r[3]).doubleValue(), this.getScore());
				this.cwbCollocateResults.add(cwbCollocateResult);
			}
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return "OK";
	}

	public List<String> getAvailableBasedOn() {
		this.availableBasedOn.clear();
		this.availableBasedOn.add(CWBCollocatesExtractor.BASED_ON_FORMA);
		if (this.isIncludePos()) {
			this.availableBasedOn.add(CWBCollocatesExtractor.BASED_ON_POS);
		}
		if (this.isIncludeLemmas()) {
			this.availableBasedOn.add(CWBCollocatesExtractor.BASED_ON_LEMMA);
		}
		return this.availableBasedOn;
	}

	public List<String> getAvailableScores() {
		return this.availableScores;
	}

	public String getCollocationBasedOn() {
		return this.collocationBasedOn;
	}

	public Integer getConcordancesResultsSize() {
		return this.concordancesResultsSize;
	}

	public ArrayList<CWBCollocateResult> getCwbCollocateResults() {
		return this.cwbCollocateResults;
	}

	public Integer getDistanceLeft() {
		return this.distanceLeft;
	}

	public Integer getDistanceRight() {
		return this.distanceRight;
	}

	public String getFilterOnPoS() {
		return this.filterOnPoS;
	}

	public Integer getFirstResult() {
		if (this.firstResult == null) {
			return 0;
		}
		return this.firstResult;
	}

	public Integer getFreqMinColl() {
		return this.freqMinColl;
	}

	public Integer getFreqMinNC() {
		return this.freqMinNC;
	}

	public Integer getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public int getLastFirstResult() {
		return this.resultsSize - 1;
	}

	public int getNextFirstResult() {
		return (this.getFirstResult() == null ? 0 : this.getFirstResult())
				+ this.pageSize;
	}

	public int getPageSize() {
		return this.pageSize;
	}

	public int getPreviousFirstResult() {
		if (this.pageSize > (this.getFirstResult() == null ? 0 : this
				.getFirstResult())) {
			return 0;
		} else {
			return this.getFirstResult() - this.pageSize;
		}
	}

	public String getQuery() {
		return this.query;
	}

	public Integer getResultsSize() {
		return this.resultsSize;
	}

	public String getScore() {
		return this.score;
	}

	public Integer getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	@Create
	public void init() {
		this.userTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction");
		this.entityManager = (EntityManager) Component
				.getInstance("entityManager");
		try {
			this.userTx
					.setTransactionTimeout(CWBCollocatesExtractor.CWB_COLLOCATES_EXTRACTOR_TIMEOUT);
			if (!this.userTx.isActive()) {
				this.userTx.begin();
			}
			this.entityManager.joinTransaction();
			this.cqpExecutable = this.entityManager.find(Parameter.class,
					Parameter.CQP_EXECUTABLE.getKey()).getValue();
			this.cqpCorpusName = this.entityManager.find(Parameter.class,
					Parameter.CQP_CORPUSNAME.getKey()).getValue();
			this.cqpRegistry = this.entityManager.find(Parameter.class,
					Parameter.CQP_REGISTRY.getKey()).getValue();
			this.entityManager.flush();
			this.entityManager.clear();
			this.userTx.commit();
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
		this.conversationId = Conversation.instance().getId();
	}

	public boolean isIncludeLemmas() {
		return this.includeLemmas;
	}

	public boolean isIncludePos() {
		return this.includePos;
	}

	public boolean isNextExists() {
		return this.resultsSize > 0 && this.resultsSize >= this.pageSize
				&& this.resultsSize > this.getFirstResult() + this.pageSize;
	}

	public boolean isPreviousExists() {
		if (this.resultsSize > 0) {
			return this.getFirstResult() != null && this.getFirstResult() != 0;
		}
		return false;
	}

	@PreDestroy
	public void preDestroy() {
		try {
			this.dropDB();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SystemException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotSupportedException e) {
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

	public void setCollocationBasedOn(String collocationBasedOn) {
		this.collocationBasedOn = collocationBasedOn;
	}

	public void setConcordancesResultsSize(Integer concordancesResultsSize) {
		this.concordancesResultsSize = concordancesResultsSize;
	}

	public void setCwbCollocateResults(
			ArrayList<CWBCollocateResult> cwbCollocateResults) {
		this.cwbCollocateResults = cwbCollocateResults;
	}

	public void setDistanceLeft(Integer distanceLeft) {
		this.distanceLeft = distanceLeft;
	}

	public void setDistanceRight(Integer distanceRight) {
		this.distanceRight = distanceRight;
	}

	public void setFilterOnPoS(String filterOnPoS) {
		this.filterOnPoS = filterOnPoS;
	}

	public void setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
	}

	public void setFreqMinColl(Integer freqMinColl) {
		this.freqMinColl = freqMinColl;
	}

	public void setFreqMinNC(Integer freqMinNC) {
		this.freqMinNC = freqMinNC;
	}

	public void setFunctionalMetadatum(Integer functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;
	}

	public void setIncludeLemmas(boolean includeLemmas) {
		this.includeLemmas = includeLemmas;
	}

	public void setIncludePos(boolean includePos) {
		this.includePos = includePos;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setResultsSize(Integer resultsSize) {
		this.resultsSize = resultsSize;
	}

	public void setScore(String score) {
		this.score = score;
	}

	public void setSemanticMetadatum(Integer semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	private File tabulate() throws IOException {
		EnvironmentUtils.addVariableToEnvironment(
				EnvironmentUtils.getProcEnvironment(), "LC_ALL=C");
		File tmpAwk = File.createTempFile("ridireAWK", ".awk");
		String awk = this.createAWKString();
		FileUtils.writeStringToFile(tmpAwk, awk);
		File tmpTabulate = File.createTempFile("ridireTAB", ".tab");
		String tabulate = this.createTabulateString(tmpAwk, tmpTabulate);
		File tempSh = File.createTempFile("ridireSH", ".sh");
		FileUtils.writeStringToFile(tempSh, tabulate);
		tempSh.setExecutable(true);
		Executor executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(
				CWBCollocatesExtractor.CWB_COLLOCATES_EXTRACTOR_TIMEOUT);
		executor.setWatchdog(watchdog);
		CommandLine commandLine = new CommandLine(this.cqpExecutable);
		commandLine.addArgument("-f").addArgument(tempSh.getAbsolutePath())
				.addArgument("-D").addArgument(this.cqpCorpusName)
				.addArgument("-r").addArgument(this.cqpRegistry);
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tmpAwk);
		FileUtils.deleteQuietly(tempSh);
		return tmpTabulate;
	}

}
