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
import it.drwolf.ridire.index.pattern.PatternData;
import it.drwolf.ridire.index.pattern.PatternQueryData;
import it.drwolf.ridire.index.results.CWBResult;
import it.drwolf.ridire.util.async.ExcelDataGenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.environment.EnvironmentUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.text.StrTokenizer;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Create;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

@Name("cwbPatternSearcher")
@Scope(ScopeType.CONVERSATION)
public class CWBPatternSearcher {

	@In(required = false)
	private CWBSketchExtractor cwbSketchExtractor;

	@In(create = true)
	private PatternQueryData patternQueryData;

	private List<String> functionalMetadatum;
	private List<String> semanticMetadatum;

	private Integer distanceLeft;
	private Integer distanceRight;
	private String filterOnPos;
	private static final int DEFAULT_CONTEXT_SIZE = 10;
	public static final String PIVOT_LEMMA = "pivot-lemma";
	public static final String PIVOT_POS = "pivot-pos";
	public static final String PIVOT_FORMA = "pivot-forma";
	private static final String NESSUN_ORDINE = "Nessun ordine";
	private static final String TUTTO = "Tutto";
	private static final String FORMA = "Forma";
	private static final String ASCENDENTE = "Ascendente";
	private static final String DISCENDENTE = "Discendente";
	private static final String CONTESTO_DESTRO = "Contesto destro";
	private static final String CONTESTO_SINISTRO = "Contesto sinistro";
	private static final String TESTO_CERCATO = "Testo cercato";
	private String forma;
	private String lemma;
	private String pos;
	private String phrase;
	private Integer contextLength = 5;
	private Integer contextGroupingLength = 0;
	private Integer pageSize = 20;
	private String toBeVisualized = CWBPatternSearcher.FORMA;
	private final List<String> availableToBeVisualized = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 6777784048431897496L;

		{
			this.add(CWBPatternSearcher.FORMA);
			this.add(CWBPatternSearcher.TUTTO);
		}
	};
	private String corporaNamesString;
	private List<String> corporaNames = new ArrayList<String>();

	private Integer firstResult;

	private List<CWBResult> resultsSimple;

	private String sortBy = CWBPatternSearcher.NESSUN_ORDINE;
	private String sortOrder = CWBPatternSearcher.ASCENDENTE;

	private List<String> availableSortings = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 5450551734244115854L;

		{
			this.add(CWBPatternSearcher.NESSUN_ORDINE);
			this.add(CWBPatternSearcher.TESTO_CERCATO);
			this.add(CWBPatternSearcher.CONTESTO_SINISTRO);
			this.add(CWBPatternSearcher.CONTESTO_DESTRO);
		}
	};
	private List<String> sortOrders = new ArrayList<String>() {

		/**
		 * 
		 */
		private static final long serialVersionUID = 8415513115711427348L;

		{
			this.add(CWBPatternSearcher.ASCENDENTE);
			this.add(CWBPatternSearcher.DISCENDENTE);
		}
	};

	@In
	private EntityManager entityManager;
	private StrTokenizer strTokenizer = new StrTokenizer();
	private String longContextText;

	private static final int LONGCONTEXTSIZE = 200;

	private static final long TIMEOUT = 1000 * 60 * 10; // 10 mins
	private boolean resultsGrouping = false;
	@In(create = true)
	private ExcelDataGenerator excelDataGenerator;

	private String cqpExecutable;
	private boolean allDomains = true;
	private String cqpCorpusName;
	private String cqpRegistry;
	private int resultsSize;
	private String query = null;
	private boolean fromCollocation = false;

	private boolean fromSketch = false;
	private String collocateTerm = "";

	private boolean resultsFromCollocationExtracted = false;

	private boolean resultsFromSketchExtracted = false;
	private String sketchTerm = null;

	private String collocationBasedOn = "forma";

	private final Pattern pFirstToken = Pattern.compile("\\s*(\\d+)\\s+(.+)");

	private int groupResultsSize;
	private String groupBy = "lemma";

	private String cwbdecodeExecutable;

	public static final String SEPARATOR = "_____";

	public String addPatternData() {
		this.patternQueryData.getPatternDatas().add(new PatternData());
		return "OK";
	}

	public String collapseGroup(CWBResult group) {
		group.setGroupExpanded(false);
		return "OK";
	}

	private String createQueryForCQP(File resTblFile, boolean sizeOnly,
			boolean groupExpand, CWBResult cwbResult) {
		String realQuery = "C=";
		if (!this.isFromCollocation() && !this.isFromSketch()) {
			if (groupExpand) {
				List<String> groupQuery = cwbResult.getGroupQuery();
				String q = StringUtils.join(groupQuery, " ");
				String[] tokens = q.split("\\s");
				if (this.getGroupBy().equals("forma")) {
					realQuery = "C=[word=\"";
					realQuery += StringUtils.join(tokens, "\"] [word=\"");
					realQuery += "\"]";
				} else {
					realQuery = "C=[lemma=\"";
					realQuery += StringUtils.join(tokens, "\"] [lemma=\"");
					realQuery += "\"]";
				}
			} else {
				int countPds = 1;
				for (PatternData pd : this.getPatternDatas()) {
					String text = pd.getText();
					if (pd.getTextType().equals("FORMA")) {
						realQuery += "[word=\"";
					} else if (pd.getTextType().equals("POS ESTESI")) {
						realQuery += "[pos=\"";
						text = text.replaceAll(":", "");
					} else if (pd.getTextType().equals("POS")) {
						realQuery += "[easypos=\"";
					} else {
						realQuery += "[lemma=\"";
					}
					realQuery += text + "\"] ";
					if (countPds < this.getPatternDatas().size()) {
						if (pd.isExactDistance()) {
							realQuery += "[]{" + pd.getDistance() + "} ";
						} else {
							realQuery += "[]{0," + pd.getDistance() + "} ";
						}
					}
					countPds++;
					realQuery += " ";
				}
			}
			List<String> descsF = new ArrayList<String>();
			if (!this.isAllDomains() && this.getFunctionalMetadatum() != null
					&& this.getFunctionalMetadatum().size() > 0) {
				for (String fid : this.getFunctionalMetadatum()) {
					if (fid.equals("-1")) {
						continue;
					}
					FunctionalMetadatum fm = this.entityManager.find(
							FunctionalMetadatum.class, Integer.parseInt(fid));
					if (fm != null) {
						descsF.add(fm.getDescription().replaceAll("\\s", "_")
								.trim());
					}
				}
			}
			List<String> descsS = new ArrayList<String>();
			if (!this.isAllDomains() && this.getSemanticMetadatum() != null
					&& this.getSemanticMetadatum().size() > 0) {
				for (String sid : this.getSemanticMetadatum()) {
					if (sid.equals("-1")) {
						continue;
					}
					SemanticMetadatum sm = this.entityManager.find(
							SemanticMetadatum.class, Integer.parseInt(sid));
					if (sm != null) {
						descsS.add(sm.getDescription().replaceAll("\\s", "_")
								.trim());
					}
				}
			}
			if (descsS.size() > 0 || descsF.size() > 0) {
				realQuery += " :: ";
				if (descsF.size() > 0 && descsS.size() > 0) {
					realQuery += " match.text_functional=\""
							+ StringUtils.join(descsF, "|") + "\"";
					realQuery += " & ";
					realQuery += " match.text_semantic=\""
							+ StringUtils.join(descsS, "|") + "\"";
				} else if (descsF.size() > 0 && descsS.size() < 1) {
					realQuery += " match.text_functional=\""
							+ StringUtils.join(descsF, "|") + "\"";
				} else {
					realQuery += " match.text_semantic=\""
							+ StringUtils.join(descsS, "|") + "\"";
				}
			}
			if (!groupExpand) {
				this.setQuery(realQuery);
			}
		} else if (this.isFromCollocation()) {
			String queryFromCollocation = this.getQuery();
			int metadataIndex = queryFromCollocation.indexOf("::");
			String queryWithoutMetadata = queryFromCollocation.substring(2);
			String metadataCondition = "";
			if (metadataIndex != -1) {
				queryWithoutMetadata = queryFromCollocation.substring(2,
						metadataIndex);
				metadataCondition = queryFromCollocation
						.substring(metadataIndex);
			}
			String searchFor = "word";
			if (this.getCollocationBasedOn().equals("PoS")) {
				searchFor = "easypos";
			} else if (this.getCollocationBasedOn().equals("lemma")) {
				searchFor = "lemma";
			}
			String queryA = "A=" + queryWithoutMetadata + " []{0,"
					+ this.getDistanceRight() + "} [" + searchFor + "=\""
					+ this.getCollocateTerm() + "\"]" + metadataCondition;
			String queryB = "B=[" + searchFor + "=\"" + this.getCollocateTerm()
					+ "\"]" + " []{0," + this.getDistanceLeft() + "} "
					+ queryWithoutMetadata + " " + metadataCondition;
			realQuery = queryA + ";\n" + queryB + ";\n" + "C=union A B;\n";
		} else if (this.isFromSketch()) {
			if (this.cwbSketchExtractor.getSketchQueries().size() > 1) {
				for (int i = 1; i < this.cwbSketchExtractor.getSketchQueries()
						.size(); i++) {
					realQuery += this.cwbSketchExtractor
							.getSketchQueries()
							.get(i)
							.replaceAll("A1=", "A" + i + "=")
							.replaceAll(
									"@\\[",
									"@[lemma=\"" + this.getSketchTerm()
											+ "\" & ")
							+ ";\n";
				}
				for (int i = 1; i < this.cwbSketchExtractor.getSketchQueries()
						.size(); i++) {
					realQuery += "C=union A" + i + " A" + (i + 1) + ";\n";
				}
			} else {
				realQuery += this.cwbSketchExtractor
						.getSketchQueries()
						.get(0)
						.replaceAll("A1=", "C=")
						.replaceAll("@\\[",
								"@[lemma=\"" + this.getSketchTerm() + "\" & ")
						+ ";\n";
			}
		}
		realQuery += ";\n";
		String q = "set AutoShow off;\n"
				+ "set ProgressBar off;\n"
				+ "set PrettyPrint off;\n"
				+ "set Context 10 words;\n"
				+ "set LeftKWICDelim '--%%%--';\n"
				+ "set RightKWICDelim '--%%%--';\n"
				+ "show -cpos;\n"
				+ "set PrintStructures \"text_url, text_semantic, text_functional\";\n"
				+ realQuery;
		if (this.isResultsGrouping() && !groupExpand) {
			String groupWhat = "lemma";
			if (this.getGroupBy().equals("forma")) {
				groupWhat = "word";
			}
			if (sizeOnly) {
				if (this.getContextGroupingLength() == 0) {
					q += "tabulate C match .. matchend " + groupWhat
							+ " > \"| sort | uniq | wc -l\";";
				} else if (this.getContextGroupingLength() == 1) {
					q += "tabulate C match .. matchend word, matchend[1] word, match[-10] .. match[-1] word,"
							+ "matchend[2] .. matchend[11] word, match .. matchend "
							+ groupWhat
							+ ", matchend[1] "
							+ groupWhat
							+ " "
							+ "> \"| sed -e 's/[ ]/@@@@@@/g' |sed -e 's/@@##/\\t/g' | sort -k 7 | sed -e 's/\\t/@@##/g' | sed -e 's/\\(.*\\)@@##/\\1\\t/' | sed -e 's/\\(.*\\)@@##/\\1\\t/' |uniq -f 1 | wc -l\";";
				} else {
					q += "tabulate C match .. matchend word, matchend[1] .. matchend["
							+ this.getContextGroupingLength()
							+ "] word, match[-10] .. match[-1] word,"
							+ "matchend[1] .. matchend["
							+ (10 + this.getContextGroupingLength())
							+ "] word, match .. matchend "
							+ groupWhat
							+ ", matchend[1] .. matchend["
							+ this.getContextGroupingLength()
							+ "] "
							+ groupWhat
							+ " "
							+ "> \"| sed -e 's/[ ]/@@@@@@/g' |sed -e 's/@@##/\\t/g' | sort -k 7 | sed -e 's/\\t/@@##/g' | sed -e 's/\\(.*\\)@@##/\\1\\t/' | sed -e 's/\\(.*\\)@@##/\\1\\t/' |uniq -f 1 | wc -l\";";
				}
			} else {
				if (this.getContextGroupingLength() > 0) {
					int pageSize = this.getPageSize();
					if (this.getFirstResult() + this.getPageSize() > this.resultsSize) {
						pageSize = this.getPageSize()
								- (this.getFirstResult() + this.getPageSize())
								% this.resultsSize;
					}
					if (this.getContextGroupingLength() == 1) {
						q += "tabulate C match .. matchend word, matchend[1] word, match[-10] .. match[-1] word,"
								+ "matchend[2] .. matchend[11] word, match text_url, match text_semantic, match text_functional, match, matchend, match .. matchend "
								+ groupWhat
								+ ", matchend[1] "
								+ groupWhat
								+ " "
								+ "> \"| sed -e 's/[ ]/@@@@@@/g' |sed -e 's/@@##/\\t/g' | sort -k 9 | sed -e 's/\\t/@@##/g' | sed -e 's/\\(.*\\)@@##/\\1\\t/' | sed -e 's/\\(.*\\)@@##/\\1\\t/' |uniq -f 1 -c | sed -e 's/\\(.*\\)\\t/\\1@@##/' | sed -e 's/\\(.*\\)\\t/\\1@@##/' | sort -nr | sed -e 's/@@@@@@/ /g' | head -"
								+ (this.getFirstResult() + this.getPageSize())
								+ " | tail -" + pageSize;
					} else {
						q += "tabulate C match .. matchend word, matchend[1] .. matchend["
								+ this.getContextGroupingLength()
								+ "] word, match[-10] .. match[-1] word,"
								+ "matchend[1] .. matchend["
								+ (10 + this.getContextGroupingLength())
								+ "] word, match text_url, match text_semantic, match text_functional, match, matchend, match .. matchend "
								+ groupWhat
								+ ", matchend[1] .. matchend["
								+ this.getContextGroupingLength()
								+ "] "
								+ groupWhat
								+ " "
								+ "> \"| sed -e 's/[ ]/@@@@@@/g' |sed -e 's/@@##/\\t/g' | sort -k 9 | sed -e 's/\\t/@@##/g' | sed -e 's/\\(.*\\)@@##/\\1\\t/' | sed -e 's/\\(.*\\)@@##/\\1\\t/' |uniq -f 1 -c | sed -e 's/\\(.*\\)\\t/\\1@@##/' | sed -e 's/\\(.*\\)\\t/\\1@@##/' | sort -nr | sed -e 's/@@@@@@/ /g' | head -"
								+ (this.getFirstResult() + this.getPageSize())
								+ " | tail -" + pageSize;
					}
				} else {
					q += "tabulate C match .. matchend word, match[-10] .. match[-1] word, "
							+ "matchend[1] .. matchend[10] word, match text_url, match text_semantic, match text_functional, match, matchend, match .. matchend "
							+ groupWhat
							+ " "
							+ "> \"| sed -e 's/[ ]/@@@@@@/g' | sed -e 's/\\(.*\\)@@##/\\1\\t/' | sort -k 2 | uniq -f 1 -c | sed -e 's/\\(.*\\)\\t/\\1@@##/' | sort -nr | sed -e 's/@@@@@@/ /g' | head -"
							+ (this.getFirstResult() + this.getPageSize())
							+ " | tail -" + this.pageSize;
				}
				q += " > " + resTblFile.getAbsolutePath() + "\";";
			}
		} else {
			if (sizeOnly) {
				q += "size C;";
			} else {
				boolean sorted = false;
				if (this.getSortBy().equals(CWBPatternSearcher.TESTO_CERCATO)) {
					q += "sort C by word";
					sorted = true;
				} else if (this.getSortBy().equals(
						CWBPatternSearcher.CONTESTO_DESTRO)) {
					q += "sort C by word on matchend[1]";
					if (this.getContextLength() > 1) {
						q += " .. matchend[" + (1 + this.getContextLength())
								+ "]";
					}
					sorted = true;
				} else if (this.getSortBy().equals(
						CWBPatternSearcher.CONTESTO_SINISTRO)) {
					q += "sort C by word on ";
					if (this.getContextLength() > 1) {
						q += " match[" + (-1 - this.getContextLength())
								+ "] .. ";
					}
					q += " match[-1]";
					sorted = true;
				}
				if (sorted
						&& this.getSortOrder().equals(
								CWBPatternSearcher.DISCENDENTE)) {
					q += " descending ";
				}
				q += ";\n";
				if (!groupExpand) {
					q += "tabulate C match[-10] .. match[-1] word, match .. matchend word, matchend[1] .. matchend[10] word, "
							+ "match text_url, match text_semantic, match text_functional, match, matchend > \"| head -"
							+ (this.getFirstResult() + this.getPageSize())
							+ " | tail -"
							+ this.pageSize
							+ " > "
							+ resTblFile.getAbsolutePath() + "\";";
				} else {
					q += "tabulate C match[-10] .. match[-1] word, match .. matchend word, matchend[1] .. matchend[10] word, match text_url, match text_semantic, match text_functional, match, matchend > \""
							+ resTblFile.getAbsolutePath() + "\";";
				}
			}
		}
		return q;
	}

	public void dummySearch() {
		this.excelDataGenerator.setFileReady(false);
		this.setFromCollocation(false);
		this.setFromSketch(false);
		this.setFirstResult(0);
		this.resetResults();
		this.initResults();
	}

	private void executeCQPQuery(File queryFile) throws ExecuteException,
			IOException {
		Executor executor = new DefaultExecutor();
		File tempSh = File.createTempFile("ridireSH", ".sh");
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("export LC_ALL=C\n");
		stringBuffer.append(this.cqpExecutable + " -f "
				+ queryFile.getAbsolutePath() + " -D " + this.cqpCorpusName
				+ " -r " + this.cqpRegistry + "\n");
		FileUtils.writeStringToFile(tempSh, stringBuffer.toString());
		tempSh.setExecutable(true);
		CommandLine commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tempSh);
	}

	private void executeQueryForContext(CWBResult item, File contextFile,
			boolean left) throws ExecuteException, IOException {
		Executor executor = new DefaultExecutor();
		File tempSh = File.createTempFile("ridireCTX", ".sh");
		StringBuffer stringBuffer = new StringBuffer();
		stringBuffer.append("LC_ALL=C && ");
		if (left) {
			stringBuffer
					.append(this.cwbdecodeExecutable + " -r "
							+ this.cqpRegistry + " -C -s "
							+ Math.max(0, item.getStartPosition() - 101)
							+ " -e " + (item.getStartPosition() - 1) + " "
							+ this.cqpCorpusName + " -P word" + " > "
							+ contextFile.getAbsolutePath() + "\n");
		} else {
			stringBuffer
					.append(this.cwbdecodeExecutable + " -r "
							+ this.cqpRegistry + " -C -s "
							+ (item.getEndPosition() + 1) + " -e "
							+ (item.getEndPosition() + 101) + " "
							+ this.cqpCorpusName + " -P word" + " > "
							+ contextFile.getAbsolutePath() + "\n");
		}
		FileUtils.writeStringToFile(tempSh, stringBuffer.toString());
		tempSh.setExecutable(true);
		CommandLine commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		FileUtils.deleteQuietly(tempSh);
	}

	public String expandGroup(CWBResult group) {
		if (group.getMembers().size() < 1) {
			File resTblFile = null;
			try {
				resTblFile = File.createTempFile("ridireTBL", ".tbl");
				String queryString = this.createQueryForCQP(resTblFile, false,
						true, group);
				File queryFile = File.createTempFile("ridireQ", ".query");
				FileUtils.writeStringToFile(queryFile, queryString);
				this.executeCQPQuery(queryFile);
				List<String> lines = FileUtils.readLines(resTblFile);
				this.strTokenizer.setDelimiterString("@@##");
				boolean resultsInGroupSkipped = false;
				for (String l : lines) {
					String[] tokens = this.strTokenizer.reset(l.trim())
							.getTokenArray();
					if (tokens.length != 8) {
						continue;
					}
					String leftContext = tokens[0].trim();
					String searchedText2 = tokens[1].trim();
					String rightContext = tokens[2].trim();
					String url = tokens[3].trim();
					String semantic = tokens[4].trim();
					String functional = tokens[5].trim();
					if (leftContext.equals(group.getLeftContext())
							&& searchedText2.equals(group.getSearchedText())
							&& rightContext.equals(group.getRightContext())
							&& url.equals(group.getUrl())
							&& !resultsInGroupSkipped) {
						resultsInGroupSkipped = true;
						continue;
					}
					CWBResult item = new CWBResult(leftContext, searchedText2,
							rightContext, url, semantic, functional);
					item.setStartPosition(Integer.parseInt(tokens[6]));
					item.setEndPosition(Integer.parseInt(tokens[7]));
					group.getMembers().add(item);
				}

				FileUtils.deleteQuietly(resTblFile);
				FileUtils.deleteQuietly(queryFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		group.setGroupExpanded(true);
		return "OK";
	}

	public List<String> getAvailableSortings() {
		return this.availableSortings;
	}

	public List<String> getAvailableToBeVisualized() {
		return this.availableToBeVisualized;
	}

	public String getCollocateTerm() {
		return this.collocateTerm;
	}

	public String getCollocationBasedOn() {
		return this.collocationBasedOn;
	}

	public Integer getContextGroupingLength() {
		return this.contextGroupingLength;
	}

	public Integer getContextLength() {
		return this.contextLength;
	}

	public List<String> getCorporaNames() {
		return this.corporaNames;
	}

	public String getCorporaNamesString() {
		return this.corporaNamesString;
	}

	private Integer getCQPQueryResultsSize(File queryFile, String cqpSizeQuery)
			throws ExecuteException, IOException {
		EnvironmentUtils.addVariableToEnvironment(
				EnvironmentUtils.getProcEnvironment(), "LC_ALL=C");
		Executor executor = new DefaultExecutor();
		File tempSize = File.createTempFile("ridireSZ", ".size");
		File tempSh = File.createTempFile("ridireSH", ".sh");
		CommandLine commandLine = new CommandLine(this.cqpExecutable);
		commandLine.addArgument("-f").addArgument(queryFile.getAbsolutePath())
				.addArgument("-D").addArgument(this.cqpCorpusName)
				.addArgument("-r").addArgument(this.cqpRegistry);
		String commLineString = commandLine.toString() + " > "
				+ tempSize.getAbsolutePath();
		FileUtils.writeStringToFile(tempSh, commLineString);
		tempSh.setExecutable(true);
		executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(
				CWBPatternSearcher.TIMEOUT);
		executor.setWatchdog(watchdog);
		commandLine = new CommandLine(tempSh.getAbsolutePath());
		executor.execute(commandLine);
		Integer size = 0;
		List<String> lines = FileUtils.readLines(tempSize);
		if (lines.size() > 0) {
			size = Integer.parseInt(lines.get(0).trim());
		}
		FileUtils.deleteQuietly(tempSh);
		FileUtils.deleteQuietly(tempSize);
		return size;
	}

	public CWBSketchExtractor getCwbSketchExtractor() {
		return this.cwbSketchExtractor;
	}

	public Integer getDistanceLeft() {
		return this.distanceLeft;
	}

	public Integer getDistanceRight() {
		return this.distanceRight;
	}

	public String getFilterOnPos() {
		return this.filterOnPos;
	}

	public Integer getFirstResult() {
		if (this.firstResult == null) {
			return 0;
		}
		return this.firstResult;
	}

	public String getForma() {
		return this.forma;
	}

	public List<String> getFunctionalMetadatum() {
		return this.functionalMetadatum;
	}

	public String getGroupBy() {
		return this.groupBy;
	}

	public int getGroupResultsSize() {
		return this.groupResultsSize;
	}

	public int getLastFirstResult() {
		return this.resultsSize - 1;
	}

	public String getLemma() {
		return this.lemma;
	}

	public String getLongContextText() {
		return this.longContextText;
	}

	public int getNextFirstResult() {
		return (this.getFirstResult() == null ? 0 : this.getFirstResult())
				+ this.pageSize;
	}

	public Integer getPageSize() {
		return this.pageSize;
	}

	public List<PatternData> getPatternDatas() {
		return this.patternQueryData.getPatternDatas();
	}

	public String getPhrase() {
		return this.phrase;
	}

	public String getPos() {
		return this.pos;
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

	public List<CWBResult> getResults4Download(int start, int pageSize) {
		int tmpFirstResult = this.getFirstResult();
		int tmpPageSize = this.getPageSize();
		this.setFirstResult(start);
		this.setPageSize(pageSize);
		try {
			this.search(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.setFirstResult(tmpFirstResult);
		this.setPageSize(tmpPageSize);
		return this.resultsSimple;
	}

	public int getResults4DownloadSize() {
		int tmpFirstResult = this.getFirstResult();
		int tmpPageSize = this.getPageSize();
		this.setFirstResult(0);
		this.setPageSize(Integer.MAX_VALUE);
		try {
			this.search(true);
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.setFirstResult(tmpFirstResult);
		this.setPageSize(tmpPageSize);
		return this.resultsSize;
	}

	public List<CWBResult> getResultsSimple() {
		return this.resultsSimple;
	}

	public int getResultsSize() {
		return this.resultsSize;
	}

	public List<String> getSemanticMetadatum() {
		return this.semanticMetadatum;
	}

	public String getSketchTerm() {
		return this.sketchTerm;
	}

	public String getSortBy() {
		return this.sortBy;
	}

	public String getSortOrder() {
		return this.sortOrder;
	}

	public List<String> getSortOrders() {
		return this.sortOrders;
	}

	private String getTermVisualization(String term, boolean leftContext) {
		StrTokenizer s1 = new StrTokenizer();
		List<String> t1 = s1.reset(term).getTokenList();
		new ArrayList<String>();
		List<String> total = new ArrayList<String>();
		for (String t : t1) {
			List<String> ret = new ArrayList<String>();
			String[] tokens = new StrTokenizer(t, CWBPatternSearcher.SEPARATOR)
					.getTokenArray();
			if (this.getToBeVisualized().contains(CWBPatternSearcher.TUTTO)) {
				if (tokens.length > 0) {
					ret.add(tokens[0]);
				}
				if (tokens.length > 1) {
					ret.add(tokens[1]);
				}
				if (tokens.length > 2) {
					ret.add(tokens[2]);
				}
			} else {
				if (this.getToBeVisualized().contains(CWBPatternSearcher.FORMA)
						&& tokens.length > 0) {
					ret.add(tokens[0]);
				}
				if (this.getToBeVisualized().contains("PoS")
						&& tokens.length > 1) {
					ret.add(tokens[1]);
				}
				if (this.getToBeVisualized().contains("Lemma")
						&& tokens.length > 2) {
					ret.add(tokens[2]);
				}
			}
			if (!leftContext) {
				total.add(StringUtils.join(ret, " / "));
			} else {
				total.add(StringUtils.join(ret, "_/_"));
			}
		}
		return StringUtils.join(total, " ");
	}

	public String getToBeVisualized() {
		return this.toBeVisualized;
	}

	@Create
	public void initParams() {
		this.cqpExecutable = this.entityManager.find(Parameter.class,
				Parameter.CQP_EXECUTABLE.getKey()).getValue();
		this.cqpCorpusName = this.entityManager.find(Parameter.class,
				Parameter.CQP_CORPUSNAME.getKey()).getValue();
		this.cqpRegistry = this.entityManager.find(Parameter.class,
				Parameter.CQP_REGISTRY.getKey()).getValue();
		this.cwbdecodeExecutable = this.entityManager.find(Parameter.class,
				Parameter.CWBDECODE_EXECUTABLE.getKey()).getValue();
	}

	public void initResults() {
		// if (this.idsToSearch != null && this.idsToSearch.size() > 0) {
		try {
			this.search(false);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void initResultsFromCollocation() {
		if (this.isFromCollocation()
				&& !this.isResultsFromCollocationExtracted()) {
			this.initResults();
			this.setResultsFromCollocationExtracted(true);
		}
	}

	public void initResultsFromSketch() {
		if (this.isFromSketch() && !this.isResultsFromSketchExtracted()) {
			this.initResults();
			this.setResultsFromSketchExtracted(true);
		}
	}

	public boolean isAllDomains() {
		return this.allDomains;
	}

	public boolean isFromCollocation() {
		return this.fromCollocation;
	}

	public boolean isFromSketch() {
		return this.fromSketch;
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

	public boolean isResultsFromCollocationExtracted() {
		return this.resultsFromCollocationExtracted;
	}

	public boolean isResultsFromSketchExtracted() {
		return this.resultsFromSketchExtracted;
	}

	public boolean isResultsGrouping() {
		return this.resultsGrouping;
	}

	public String removePatternData() {
		if (this.getPatternDatas().size() > 0) {
			this.getPatternDatas().remove(this.getPatternDatas().size() - 1);
		}
		return "OK";
	}

	public void reset() {
		this.excelDataGenerator.setFileReady(false);
		this.setForma(null);
		this.setPos(null);
		this.setLemma(null);
		this.setPhrase(null);
		this.setSemanticMetadatum(null);
		this.setFunctionalMetadatum(null);
		this.setSortBy(CWBPatternSearcher.NESSUN_ORDINE);
		this.setResultsGrouping(false);
		this.setFromCollocation(false);
		this.setResultsFromCollocationExtracted(false);
	}

	public void resetDomains() {
		if (this.isAllDomains()) {
			this.getSemanticMetadatum().clear();
			this.getFunctionalMetadatum().clear();
		}
	}

	private void resetResults() {
		this.resultsSimple = null;
	}

	public void retrieveLongContext(CWBResult item) {
		try {
			File leftCtx = File.createTempFile("ridireLCTX", ".tab");
			this.executeQueryForContext(item, leftCtx, true);
			StringBuffer longContextBuf = new StringBuffer();
			longContextBuf.append(StringEscapeUtils.escapeHtml(StringUtils
					.join(FileUtils.readLines(leftCtx), " ")));
			longContextBuf
					.append(" <span class=\"searchedText\" id=\"searchedText\">"
							+ item.getSearchedText() + "</span> ");
			File rightCtx = File.createTempFile("ridireRCTX", ".tab");
			this.executeQueryForContext(item, rightCtx, false);
			longContextBuf.append(StringEscapeUtils.escapeHtml(StringUtils
					.join(FileUtils.readLines(rightCtx), " ")));
			this.setLongContextText(longContextBuf.toString());
			FileUtils.deleteQuietly(leftCtx);
			FileUtils.deleteQuietly(rightCtx);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private String search(boolean onlyResultsSize) throws IOException {
		this.strTokenizer.setDelimiterString("@@##");
		// first calculate results size
		File resTblFile = File.createTempFile("ridireTBL", ".tbl");
		String queryString = this.createQueryForCQP(resTblFile, true, false,
				null);
		if (queryString.trim().length() < 1) {
			return "";
		}
		File queryFile = File.createTempFile("ridireQ", ".query");
		FileUtils.writeStringToFile(queryFile, queryString);
		this.resultsSize = this.getCQPQueryResultsSize(queryFile, queryString);
		FileUtils.deleteQuietly(resTblFile);
		FileUtils.deleteQuietly(queryFile);
		// get sum of all results to calculate percentage
		if (this.isResultsGrouping()) {
			this.setResultsGrouping(false);
			resTblFile = File.createTempFile("ridireTBL", ".tbl");
			queryString = this.createQueryForCQP(resTblFile, true, false, null);
			if (queryString.trim().length() < 1) {
				return "";
			}
			queryFile = File.createTempFile("ridireQ", ".query");
			FileUtils.writeStringToFile(queryFile, queryString);
			this.setGroupResultsSize(this.getCQPQueryResultsSize(queryFile,
					queryString));
			FileUtils.deleteQuietly(resTblFile);
			FileUtils.deleteQuietly(queryFile);
			this.setResultsGrouping(true);
		}
		// now get paginated results
		resTblFile = File.createTempFile("ridireTBL", ".tbl");
		queryString = this.createQueryForCQP(resTblFile, false, false, null);
		queryFile = File.createTempFile("ridireQ", ".query");
		FileUtils.writeStringToFile(queryFile, queryString);
		this.executeCQPQuery(queryFile);
		this.resultsSimple = new ArrayList<CWBResult>();
		this.strTokenizer.setIgnoreEmptyTokens(false);
		if (this.isResultsGrouping()) {
			List<String> lines = FileUtils.readLines(resTblFile);
			for (String l : lines) {
				String[] tokens = this.strTokenizer.reset(l).getTokenArray();
				if (this.getContextGroupingLength() > 0 && tokens.length != 11
						|| this.getContextGroupingLength() == 0
						&& tokens.length != 9) {
					continue;
				}
				Matcher m = this.pFirstToken.matcher(tokens[0]);
				if (!m.find()) {
					continue;
				}
				int groupSize = Integer.parseInt(m.group(1));
				if (this.getContextGroupingLength() > 0) {
					CWBResult item = new CWBResult(tokens[2], m.group(2).trim()
							+ " " + tokens[1], tokens[3], tokens[4], tokens[5],
							tokens[6]);
					item.setStartPosition(Integer.parseInt(tokens[7]));
					item.setEndPosition(Integer.parseInt(tokens[8]));
					item.getGroupQuery().add(tokens[9]);
					item.getGroupQuery().add(tokens[10]);
					item.setGroupSize(groupSize);
					item.setGrouped(true);
					this.resultsSimple.add(item);
				} else {
					CWBResult item = new CWBResult(tokens[1],
							m.group(2).trim(), tokens[2], tokens[3], tokens[4],
							tokens[5]);
					item.setStartPosition(Integer.parseInt(tokens[6]));
					item.setEndPosition(Integer.parseInt(tokens[7]));
					item.getGroupQuery().add(tokens[8]);
					item.setGroupSize(groupSize);
					item.setGrouped(true);
					this.resultsSimple.add(item);
				}
			}
		} else {
			List<String> lines = FileUtils.readLines(resTblFile);
			for (String l : lines) {
				String[] tokens = this.strTokenizer.reset(l).getTokenArray();
				if (tokens.length != 8) {
					continue;
				}
				CWBResult item = new CWBResult(tokens[0], tokens[1], tokens[2],
						tokens[3], tokens[4], tokens[5]);
				item.setStartPosition(Integer.parseInt(tokens[6]));
				item.setEndPosition(Integer.parseInt(tokens[7]));
				this.resultsSimple.add(item);
			}
		}
		FileUtils.deleteQuietly(resTblFile);
		FileUtils.deleteQuietly(queryFile);
		return "OK";
	}

	public void setAllDomains(boolean allDomains) {
		this.allDomains = allDomains;
	}

	public void setAvailableSortings(List<String> availableSortings) {
		this.availableSortings = availableSortings;
	}

	public void setCollocateTerm(String collocateTerm) {
		this.collocateTerm = collocateTerm;
	}

	public void setCollocationBasedOn(String collocationBasedOn) {
		this.collocationBasedOn = collocationBasedOn;
	}

	public void setContextGroupingLength(Integer contextGroupingLength) {
		this.contextGroupingLength = contextGroupingLength;

	}

	public void setContextLength(Integer contextLength) {
		this.contextLength = contextLength;

	}

	public void setCorporaNames(List<String> corporaNames) {
		this.corporaNames = corporaNames;
		this.corporaNamesString = StringUtils.join(corporaNames, "####");

	}

	public void setCorporaNamesString(String corporaNamesString) {
		this.corporaNamesString = corporaNamesString;
		this.corporaNames = Arrays.asList(StringUtils.split(corporaNamesString,
				"####"));

	}

	public void setCwbSketchExtractor(CWBSketchExtractor cwbSketchExtractor) {
		this.cwbSketchExtractor = cwbSketchExtractor;
	}

	public void setDistanceLeft(Integer distanceLeft) {
		this.distanceLeft = distanceLeft;
	}

	public void setDistanceRight(Integer distanceRight) {
		this.distanceRight = distanceRight;
	}

	public void setFilterOnPos(String filterOnPos) {
		this.filterOnPos = filterOnPos;
	}

	public void setFirstResult(Integer firstResult) {
		this.firstResult = firstResult;
	}

	public void setForma(String forma) {
		this.forma = forma;

	}

	public void setFromCollocation(boolean fromCollocation) {
		this.fromCollocation = fromCollocation;
	}

	public void setFromSketch(boolean fromSketch) {
		this.fromSketch = fromSketch;
	}

	public void setFunctionalMetadatum(List<String> functionalMetadatum) {
		this.functionalMetadatum = functionalMetadatum;

	}

	public void setGroupBy(String groupBy) {
		this.groupBy = groupBy;
	}

	public void setGroupResultsSize(int groupResultsSize) {
		this.groupResultsSize = groupResultsSize;
	}

	public void setLemma(String lemma) {
		this.lemma = lemma;

	}

	public void setLongContextText(String longContextText) {
		this.longContextText = longContextText;
	}

	public void setPageSize(Integer pageSize) {
		this.pageSize = pageSize;
	}

	public void setPatternDatas(List<PatternData> patternDatas) {
		this.patternQueryData.setPatternDatas(patternDatas);
	}

	public void setPhrase(String phrase) {
		this.phrase = phrase;

	}

	public void setPos(String pos) {
		this.pos = pos;

	}

	public void setQuery(String query) {
		this.query = query;
	}

	public void setResultsFromCollocationExtracted(
			boolean resultsFromCollocationExtracted) {
		this.resultsFromCollocationExtracted = resultsFromCollocationExtracted;
	}

	public void setResultsFromSketchExtracted(boolean resultsFromSketchExtracted) {
		this.resultsFromSketchExtracted = resultsFromSketchExtracted;
	}

	public void setResultsGrouping(boolean resultsGrouping) {
		this.resultsGrouping = resultsGrouping;

	}

	public void setResultsSimple(List<CWBResult> resultsSimple) {
		this.resultsSimple = resultsSimple;
	}

	public void setResultsSize(int resultsSize) {
		this.resultsSize = resultsSize;
	}

	public void setSemanticMetadatum(List<String> semanticMetadatum) {
		this.semanticMetadatum = semanticMetadatum;
	}

	public void setSketchTerm(String sketchTerm) {
		this.sketchTerm = sketchTerm;
	}

	public void setSortBy(String sortBy) {
		this.sortBy = sortBy;

	}

	public void setSortOrder(String sortOrder) {
		this.sortOrder = sortOrder;

	}

	public void setSortOrders(List<String> sortOrders) {
		this.sortOrders = sortOrders;
	}

	public void setToBeVisualized(String toBeVisualized) {
		this.toBeVisualized = toBeVisualized;

	}

	@SuppressWarnings("unchecked")
	public void test() {
		String query = "set AutoShow off;\nset ProgressBar off;\nset PrettyPrint off;\nset Context 5 words;\nset LeftKWICDelim '--%%%--';\nset RightKWICDelim '--%%%--';\nshow -cpos;\nA=\"e\";\ncat A 0 9 > \"/home/drwolf/ridirecleaner_tmp/res.tbl\";";
		File temp = null;
		try {
			temp = File.createTempFile("ridireQ", ".query");
			FileUtils.writeStringToFile(temp, query);
			Executor executor = new DefaultExecutor();
			CommandLine commandLine = new CommandLine(
					"/usr/local/cwb-3.4.3/bin/cqp");
			commandLine.addArgument("-f").addArgument(temp.getAbsolutePath())
					.addArgument("-D").addArgument("RIDIRE2").addArgument("-r")
					.addArgument("/usr/local/share/cwb/registry/");
			executor.execute(commandLine);
			File resFile = new File("/home/drwolf/ridirecleaner_tmp/res.tbl");
			List<String> lines = FileUtils.readLines(resFile);
			this.resultsSimple = new ArrayList<CWBResult>();
			for (String l : lines) {
				String[] res = l.split("--%%%--");
				CWBResult item = new CWBResult(res[0], res[1], res[2], "",
						null, null);
				this.resultsSimple.add(item);
			}
			this.entityManager.createNativeQuery("drop table if exists pippo")
					.executeUpdate();
			this.entityManager
					.createNativeQuery(
							"CREATE TABLE `pippo` (`text_id` varchar(40) DEFAULT NULL,`beginPosition` int(11) DEFAULT NULL,`endPosition` int(11) DEFAULT NULL,`refnumber` bigint(20) NOT NULL,`dist` smallint(6) NOT NULL,`word` varchar(40) NOT NULL,`lemma` varchar(40) NOT NULL, `pos` varchar(40) NOT NULL) ENGINE=MyISAM DEFAULT CHARSET=utf8")
					.executeUpdate();
			File tmpAwk = File.createTempFile("ridireAWK", ".awk");
			String awk = "BEGIN{ OFS = FS = \"\t\" } { print $3, $1, $2, NR-1, -5, $4 } { print $3, $1, $2, NR-1, -4, $5 } { print $3, $1, $2, NR-1, -3, $6 } { print $3, $1, $2, NR-1, -2, $7 } { print $3, $1, $2, NR-1, -1, $8 } { print $3, $1, $2, NR-1, 1, $9 } { print $3, $1, $2, NR-1, 2, $10 } { print $3, $1, $2, NR-1, 3, $11 } { print $3, $1, $2, NR-1, 4, $12 } { print $3, $1, $2, NR-1, 5, $13 } ";
			FileUtils.writeStringToFile(tmpAwk, awk);
			File tmpTabulate = File.createTempFile("ridireTAB", ".tab");
			String tabulate = "set AutoShow off;\nset ProgressBar off;\nset PrettyPrint off;\nset Context 5 words;\nset LeftKWICDelim '--%%%--';\nset RightKWICDelim '--%%%--';\nshow -cpos;\nA=\"e\";\ntabulate A match, matchend, match text_id, match[-5] word, match[-4] word, match[-3] word, match[-2] word, match[-1] word, matchend[1] word, matchend[2] word, matchend[3] word, matchend[4] word, matchend[5] word "
					+ ">  \"| awk -f '"
					+ tmpAwk.getAbsolutePath()
					+ "' > '"
					+ tmpTabulate.getAbsolutePath() + "'\";";
			File tempSh = File.createTempFile("ridireSH", ".sh");
			FileUtils.writeStringToFile(tempSh, tabulate);
			tempSh.setExecutable(true);
			executor = new DefaultExecutor();
			executor.setExitValue(0);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(
					CWBPatternSearcher.TIMEOUT);
			executor.setWatchdog(watchdog);
			commandLine = new CommandLine("/usr/local/cwb-3.4.3/bin/cqp");
			commandLine.addArgument("-f").addArgument(tempSh.getAbsolutePath())
					.addArgument("-D").addArgument("RIDIRE2").addArgument("-r")
					.addArgument("/usr/local/share/cwb/registry/");
			executor.execute(commandLine);
			FileUtils.deleteQuietly(tempSh);
			this.entityManager.createNativeQuery(
					"LOAD DATA LOCAL INFILE '" + tmpTabulate.getAbsolutePath()
							+ "' INTO TABLE " + "pippo").executeUpdate();
			long n = ((Number) this.entityManager.createNativeQuery(
					"select sum(freq) as somma from freq_forma_all")
					.getSingleResult()).longValue();
			long r1 = ((Number) this.entityManager.createNativeQuery(
					"select count(*) from pippo where dist between -3 and 3")
					.getSingleResult()).longValue();
			String nativeQuery = "select pippo.word, count(pippo.word) as observed,"
					+ " ("
					+ r1
					+ " * (freq_forma_all.freq) / "
					+ n
					+ ") as expected, sign(COUNT(pippo.word) - ("
					+ r1
					+ " * (freq_forma_all.freq) / "
					+ n
					+ ")) * 2 * ( IF(COUNT(pippo.word) > 0, COUNT(pippo.word) * log(COUNT(pippo.word) / ("
					+ r1
					+ " * (freq_forma_all.freq) / "
					+ n
					+ ")), 0) + IF(("
					+ r1
					+ " - COUNT(pippo.word)) > 0, ("
					+ r1
					+ " - COUNT(pippo.word)) * log(("
					+ r1
					+ " - COUNT(pippo.word)) / ("
					+ r1
					+ " * ("
					+ n
					+ " - (freq_forma_all.freq)) / "
					+ n
					+ ")), 0) + IF(((freq_forma_all.freq) - COUNT(pippo.word)) > 0, ((freq_forma_all.freq) - COUNT(pippo.word)) * log(((freq_forma_all.freq) - COUNT(pippo.word)) / ("
					+ (n - r1)
					+ " * (freq_forma_all.freq) / "
					+ n
					+ ")), 0) + IF(("
					+ (n - r1)
					+ " - ((freq_forma_all.freq) - COUNT(pippo.word))) > 0, ("
					+ (n - r1)
					+ " - ((freq_forma_all.freq) - COUNT(pippo.word))) * log(("
					+ (n - r1)
					+ " - ((freq_forma_all.freq) - COUNT(pippo.word))) / ("
					+ (n - r1)
					+ " * ("
					+ n
					+ " - (freq_forma_all.freq)) / "
					+ n
					+ ")), 0) ) as significance, freq_forma_all.freq, count(distinct(text_id)) as text_id_count from pippo, freq_forma_all where pippo.word = freq_forma_all.item and dist between -3 and 3 and freq_forma_all.freq >= 1 group by pippo.word having observed >= 1 order by significance desc LIMIT 0, 50 ";
			List<Object[]> res = this.entityManager.createNativeQuery(
					nativeQuery).getResultList();
			for (Object[] r : res) {
				System.out.println(r[0] + "\t" + r[3]);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
