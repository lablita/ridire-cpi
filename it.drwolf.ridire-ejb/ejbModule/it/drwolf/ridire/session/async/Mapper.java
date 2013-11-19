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
package it.drwolf.ridire.session.async;

import it.drwolf.ridire.cleaners.utils.StringWithEncoding;
import it.drwolf.ridire.entity.CommandParameter;
import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Job;
import it.drwolf.ridire.entity.Parameter;
import it.drwolf.ridire.util.MD5DigestCreator;
import it.drwolf.ridire.utility.RIDIREReTagger;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import javax.persistence.EntityManager;
import javax.persistence.RollbackException;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteStreamHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.PrefixFileFilter;
import org.apache.commons.lang.text.StrTokenizer;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaMetadataKeys;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.txt.CharsetDetector;
import org.apache.tika.parser.txt.CharsetMatch;
import org.archive.io.ArchiveReader;
import org.archive.io.ArchiveReaderFactory;
import org.archive.io.ArchiveRecord;
import org.archive.io.arc.ARCRecord;
import org.archive.io.arc.ARCRecordMetaData;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.faces.Renderer;
import org.jboss.seam.transaction.UserTransaction;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import de.spieleck.app.cngram.NGramProfiles;

public class Mapper implements Runnable {

	private interface OutputType {
		ContentHandler getContentHandler(String encoding, Writer writer)
				throws TransformerConfigurationException;
	}

	private static final int PDFCLEANER_TIMEOUT = 300000;

	private Integer jobId;

	private EntityManager em = null;

	private boolean running = false;
	private Job job;

	private UserTransaction mapperUserTx;

	private static final String ATTICDIR = "attic/";
	private static final int BUFLENGTH = 8192;

	private static final String ITALIAN = "it";

	private static final long TREETAGGER_TIMEOUT = 240000; // 4 mins

	private static final long READABILITY_TIMEOUT = 360000; // 6 mins

	private List<String> notWordPoSs = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = -3892423757401579609L;

		{
			this.add("PON");
			this.add("SENT");
			this.add("SYM");
		}
	};

	@In(create = true)
	private Renderer renderer;
	private String tempDir;
	private final OutputType HTML = new OutputType() {
		public ContentHandler getContentHandler(String encoding, Writer writer)
				throws TransformerConfigurationException {
			return Mapper.this.getTransformerHandler("html", encoding, writer);
		}
	};

	private FlagBearer flagBearer;

	private RIDIREReTagger ridireReTagger;

	private final List<String> allowedCharsets = new ArrayList<String>() {
		/**
		 * 
		 */
		private static final long serialVersionUID = 2950026736924724677L;

		{
			this.add("UTF-8");
			this.add("ISO-8859-1");
			this.add("UTF-16BE");
			this.add("UTF-16LE");
		}
	};

	private static final String ALCHEMY = "alchemy";

	private static final String READABILITY = "readability";

	public Mapper(Job job, FlagBearer flagBearer) {
		this.job = job;
		this.jobId = job.getId();
		this.flagBearer = flagBearer;
		this.ridireReTagger = new RIDIREReTagger(null);
	}

	@SuppressWarnings("unchecked")
	private Integer countWordsFromPoSTagResource(String posTagResourceFileName)
			throws IOException {
		List<String> lines = FileUtils.readLines(new File(
				posTagResourceFileName));
		Integer count = 0;
		StrTokenizer tokenizer = StrTokenizer.getTSVInstance();
		for (String l : lines) {
			tokenizer.reset(l);
			String[] tokens = tokenizer.getTokenArray();
			if (tokens.length == 3) {
				if (this.isValidPos(tokens[1].trim())) {
					++count;
				}
			}
		}
		return count;
	}

	private void createArchivedResource(File f, CrawledResource cr,
			EntityManager entityManager) {
		// System.out.println(System.getProperty("java.io.tmpdir"));
		String posEnabled = this.em.find(Parameter.class,
				Parameter.POS_ENABLED.getKey()).getValue();
		File resourceDir;
		int status = Parameter.FINISHED;
		try {
			resourceDir = new File(FilenameUtils.getFullPath(f
					.getCanonicalPath().replaceAll("__\\d+", ""))
					+ JobMapperMonitor.RESOURCESDIR);
			if (!resourceDir.exists()) {
				FileUtils.forceMkdir(resourceDir);
			}
			ArchiveReader reader = ArchiveReaderFactory.get(f);
			ARCRecord record = (ARCRecord) reader.get(cr.getOffset());
			record.skipHttpHeader();
			byte[] buf = new byte[Mapper.BUFLENGTH];
			int count = 0;
			String resourceFile = cr.getDigest() + ".gz";
			GZIPOutputStream baos = new GZIPOutputStream(new FileOutputStream(
					new File(resourceDir, resourceFile)));
			while ((count = record.read(buf)) != -1) {
				baos.write(buf, 0, count);
			}
			baos.finish();
			baos.close();
			reader.close();
			// long t1 = System.currentTimeMillis();
			StringWithEncoding cleanText = this.createPlainTextResource(f, cr,
					entityManager);
			this.removeGZippedResource(resourceDir, resourceFile);
			// long t2 = System.currentTimeMillis();
			// System.out.println("Creazione plain text: " + (t2 - t1));
			String plainTextFileName = cr.getDigest() + ".txt";
			if (cleanText != null
					&& cleanText.getString() != null
					&& cleanText.getString().trim().length() > 0
					&& cleanText.getCleaner() != null
					&& (cleanText.getCleaner().equals(Mapper.ALCHEMY) || cleanText
							.getCleaner().equals(Mapper.READABILITY))) {
				cr.setCleaner(cleanText.getCleaner());
				File plainTextFile = new File(resourceDir, plainTextFileName);
				FileUtils.writeStringToFile(plainTextFile,
						cleanText.getString(), cleanText.getEncoding());
				cr.setExtractedTextHash(MD5DigestCreator
						.getMD5Digest(plainTextFile));
				// language detection
				// t1 = System.currentTimeMillis();
				String language = this.detectLanguage(cleanText.getString());
				// t2 = System.currentTimeMillis();
				// System.out.println("Language detection: " + (t2 - t1));
				cr.setLanguage(language);
				if (language != null
						&& language.equalsIgnoreCase(Mapper.ITALIAN)
						&& posEnabled != null
						&& posEnabled.equalsIgnoreCase("true")) {
					// PoS tag if it's an italian text
					// t1 = System.currentTimeMillis();
					String posTagResourceFileName = this.createPoSTagResource(
							plainTextFile, entityManager,
							cleanText.getEncoding());
					// t2 = System.currentTimeMillis();
					// System.out.println("PoS tagging: " + (t2 - t1));
					if (posTagResourceFileName != null) {
						Integer wordsNumber = this
								.countWordsFromPoSTagResource(posTagResourceFileName);
						cr.setWordsNumber(wordsNumber);
					}
				}
			}
		} catch (Exception e) {
			status = Parameter.PROCESSING_ERROR;
			e.printStackTrace();
		}
		cr.setProcessed(status);
	}

	private void createArchivedResourceAndDeleteFromAttic(File f,
			String oldDigest, CrawledResource cr, EntityManager entityManager)
			throws SAXException, TikaException, IOException {
		this.createArchivedResource(f, cr, entityManager);
		this.removeResourceInAttic(f, oldDigest, entityManager);
	}

	private StringWithEncoding createPlainTextResource(File f,
			CrawledResource cr, EntityManager entityManager)
			throws SAXException, TikaException, IOException,
			TransformerConfigurationException, InterruptedException {
		File resourceDir = new File(FilenameUtils.getFullPath(f
				.getCanonicalPath().replaceAll("__\\d+", ""))
				+ JobMapperMonitor.RESOURCESDIR);
		String alchemyKey = entityManager.find(Parameter.class,
				Parameter.ALCHEMY_KEY.getKey()).getValue();
		String readabilityKey = entityManager.find(Parameter.class,
				Parameter.READABILITY_KEY.getKey()).getValue();
		String resourceFileName = cr.getDigest() + ".gz";
		File resourceFile = new File(resourceDir, resourceFileName);
		StringWithEncoding rawContentAndEncoding = null;
		String contentType = cr.getContentType();
		// long t1 = System.currentTimeMillis();
		if (contentType != null && contentType.contains("application/msword")) {
			rawContentAndEncoding = this.transformDOC2HTML(resourceFile,
					entityManager);
		}
		if (contentType != null && contentType.contains("application/rtf")) {
			rawContentAndEncoding = this.transformRTF2HTML(resourceFile,
					entityManager);
		}
		if (contentType != null && contentType.contains("text/plain")) {
			// txt -> html -> txt is for txt cleaning
			rawContentAndEncoding = this.transformTXT2HTML(resourceFile,
					entityManager);
		}
		if (contentType != null && contentType.contains("pdf")) {
			rawContentAndEncoding = this.transformPDF2HTML(resourceFile,
					entityManager);
		}
		if (contentType != null && contentType.contains("html")) {
			rawContentAndEncoding = this
					.getGuessedEncodingAndSetRawContentFromGZFile(resourceFile);
		}
		// long t2 = System.currentTimeMillis();
		// System.out.println("Transformation: " + (t2 - t1));
		if (rawContentAndEncoding != null) {
			if (rawContentAndEncoding.getEncoding() == null) {
				rawContentAndEncoding = new StringWithEncoding(
						rawContentAndEncoding.getString(), "UTF8");
			}
			// t1 = System.currentTimeMillis();
			String cleanText = this
					.replaceUnsupportedChars(rawContentAndEncoding.getString());
			rawContentAndEncoding = new StringWithEncoding(cleanText,
					rawContentAndEncoding.getEncoding());
			File tmpFile = File.createTempFile("ridire", null);
			FileUtils.writeStringToFile(tmpFile,
					rawContentAndEncoding.getString(), "UTF-8");
			String ridireCleanerJar = entityManager.find(
					CommandParameter.class,
					CommandParameter.RIDIRE_CLEANER_EXECUTABLE_KEY)
					.getCommandValue();
			String host = entityManager.find(Parameter.class,
					Parameter.READABILITY_HOSTAPP.getKey()).getValue();
			CommandLine commandLine = CommandLine
					.parse("java -Xmx128m -Djava.io.tmpdir=" + this.tempDir
							+ " -jar " + ridireCleanerJar);
			commandLine.addArgument("-f");
			commandLine.addArgument(tmpFile.getPath());
			commandLine.addArgument("-e");
			commandLine.addArgument("UTF-8");
			commandLine.addArgument("-h");
			commandLine.addArgument(host);
			commandLine.addArgument("-k");
			commandLine.addArgument(alchemyKey);
			commandLine.addArgument("-r");
			commandLine.addArgument(readabilityKey);
			DefaultExecutor executor = new DefaultExecutor();
			executor.setExitValue(0);
			ExecuteWatchdog watchdog = new ExecuteWatchdog(
					Mapper.READABILITY_TIMEOUT);
			executor.setWatchdog(watchdog);
			ByteArrayOutputStream baosStdOut = new ByteArrayOutputStream(1024);
			ByteArrayOutputStream baosStdErr = new ByteArrayOutputStream(1024);
			ExecuteStreamHandler executeStreamHandler = new PumpStreamHandler(
					baosStdOut, baosStdErr, null);
			executor.setStreamHandler(executeStreamHandler);
			int exitValue = executor.execute(commandLine);
			if (exitValue == 0) {
				rawContentAndEncoding = new StringWithEncoding(
						baosStdOut.toString(), "UTF-8");
				// TODO filter real errors
				rawContentAndEncoding.setCleaner(baosStdErr.toString().trim());
			}
			FileUtils.deleteQuietly(tmpFile);
		}
		return rawContentAndEncoding;
	}

	private String createPoSTagResource(File plainTextFile,
			EntityManager entityManager, String encoding)
			throws InterruptedException, IOException {
		// this is needed because TreeTagger doesn't handle spaces inside
		// filenames correctly
		File tmpFile = File.createTempFile("treetagger", null);
		FileUtils.copyFile(plainTextFile, tmpFile);
		String treeTaggerBin = entityManager.find(CommandParameter.class,
				CommandParameter.TREETAGGER_EXECUTABLE_KEY).getCommandValue();
		// if (encoding.equalsIgnoreCase("UTF-8")
		// || encoding.equalsIgnoreCase("UTF8")) {
		// treeTaggerBin = entityManager.find(CommandParameter.class,
		// CommandParameter.TREETAGGER_EXECUTABLE_UTF8_KEY)
		// .getCommandValue();
		// }
		this.ridireReTagger.setTreetaggerBin(treeTaggerBin);
		String tmpPoSFile = this.ridireReTagger.retagFile(tmpFile);
		File newPosFile = new File(plainTextFile.getAbsolutePath() + ".pos");
		if (tmpPoSFile != null) {
			if (newPosFile.exists()) {
				FileUtils.deleteQuietly(newPosFile);
			}
			FileUtils.moveFile(new File(tmpPoSFile), newPosFile);
			return newPosFile.getAbsolutePath();
		}
		return null;

		// CommandLine commandLine = CommandLine.parse(treeTaggerBin);
		// commandLine.addArgument(tmpFile.getPath());
		// DefaultExecutor executor = new DefaultExecutor();
		// executor.setExitValue(0);
		// ExecuteWatchdog watchdog = new ExecuteWatchdog(
		// Mapper.TREETAGGER_TIMEOUT);
		// executor.setWatchdog(watchdog);
		// ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
		// ExecuteStreamHandler executeStreamHandler = new
		// PumpStreamHandler(baos,
		// null, null);
		// executor.setStreamHandler(executeStreamHandler);
		// int exitValue = executor.execute(commandLine);
		// FileUtils.deleteQuietly(tmpFile);
		// if (exitValue == 0) {
		// File posTagFile = new File(plainTextFile.getPath() + ".pos");
		// FileUtils.writeByteArrayToFile(posTagFile, baos.toByteArray());
		// return posTagFile.getCanonicalPath();
		// }
		// return null;
	}

	private String detectLanguage(String cleanText) throws IOException {
		NGramProfiles nps = new NGramProfiles();
		NGramProfiles.Ranker ranker = nps.getRanker();
		ranker.account(cleanText);
		NGramProfiles.RankResult res = ranker.getRankResult();
		String language = null;
		if (res != null) {
			language = res.getName(0);
		}
		return language;
	}

	private boolean existsResourceWithSameExtractedText(CrawledResource cr) {
		Long count = (Long) this.em
				.createQuery(
						"select count(cr) from CrawledResource cr where cr.extractedTextHash=:md5 and cr.id<>:id")
				.setParameter("md5", cr.getExtractedTextHash())
				.setParameter("id", cr.getId()).getSingleResult();
		if (count > 0) {
			return true;
		}
		return false;
	}

	private StringWithEncoding getGuessedEncodingAndSetRawContentFromGZFile(
			File resourceFile) throws IOException {
		byte[] buf = new byte[Mapper.BUFLENGTH];
		int count = 0;
		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(
				resourceFile));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((count = gzis.read(buf)) != -1) {
			baos.write(buf, 0, count);
		}
		gzis.close();
		baos.close();
		CharsetDetector charsetDetector = new CharsetDetector();
		byte[] byteArray = baos.toByteArray();
		charsetDetector.setText(byteArray);
		CharsetMatch[] matches = charsetDetector.detectAll();
		String encoding = this.allowedCharsets.get(1);
		for (CharsetMatch cm : matches) {
			if (this.allowedCharsets.contains(cm.getName())) {
				encoding = cm.getName();
				// System.out.println(encoding);
				break;
			}
		}
		String rawContent = new String(byteArray, encoding);
		return new StringWithEncoding(rawContent, encoding);
	}

	public Integer getJobId() {
		return this.jobId;
	}

	public String getJobName() {
		if (this.job != null) {
			return this.job.getName();
		}
		return "" + this.jobId;
	}

	/**
	 * Returns a transformer handler that serializes incoming SAX events to
	 * XHTML or HTML (depending the given method) using the given output
	 * encoding.
	 * 
	 * @see <a
	 *      href="https://issues.apache.org/jira/browse/TIKA-277">TIKA-277</a>
	 * @param method
	 *            "xml" or "html"
	 * @param encoding
	 *            output encoding, or <code>null</code> for the platform default
	 * @param writer
	 * @return {@link System#out} transformer handler
	 * @throws TransformerConfigurationException
	 *             if the transformer can not be created
	 */
	private TransformerHandler getTransformerHandler(String method,
			String encoding, Writer writer)
			throws TransformerConfigurationException {
		SAXTransformerFactory factory = (SAXTransformerFactory) TransformerFactory
				.newInstance();
		TransformerHandler handler = factory.newTransformerHandler();
		handler.getTransformer().setOutputProperty(OutputKeys.METHOD, method);
		handler.getTransformer().setOutputProperty(OutputKeys.INDENT, "yes");
		if (encoding != null) {
			handler.getTransformer().setOutputProperty(OutputKeys.ENCODING,
					encoding);
		}
		handler.setResult(new StreamResult(writer));
		return handler;
	}

	private boolean isResourceAlreadyMapped(String digest, String url,
			EntityManager entityManager) {
		boolean ret = entityManager
				.createQuery(
						"from CrawledResource cr where cr.digest=:digest or cr.url=:url")
				.setParameter("digest", digest).setParameter("url", url)
				.getResultList().size() > 0 ? true : false;
		return ret;
	}

	public boolean isRunning() {
		return this.running;
	}

	private boolean isValidPos(String pos) {
		if (this.notWordPoSs.contains(pos)) {
			return false;
		}
		return true;
	}

	private void lookForNoMoreAvailableResources(Job persistedJob,
			Set<CrawledResource> childResources, File f,
			EntityManager entityManager) throws IOException,
			NotSupportedException, SystemException, SecurityException,
			IllegalStateException, RollbackException, HeuristicMixedException,
			HeuristicRollbackException {
		for (CrawledResource cr : persistedJob.getCrawledResources()) {
			if (!childResources.contains(cr)) {
				cr.setNoMoreAvailable(true);
				entityManager.persist(cr);
				this.moveResourceInAttic(cr, f);
			}
		}
	}

	private void moveResourceInAttic(CrawledResource cr, File f)
			throws IOException {
		File atticDir = new File(FilenameUtils.getFullPath(f.getCanonicalPath()
				.replaceAll("__\\d+", "")) + Mapper.ATTICDIR);
		File resourceDir = new File(FilenameUtils.getFullPath(f
				.getCanonicalPath().replaceAll("__\\d+", ""))
				+ JobMapperMonitor.RESOURCESDIR);
		File toBeMoved = new File(resourceDir, cr.getDigest() + ".gz");
		FileUtils.moveFileToDirectory(toBeMoved, atticDir, true);
		toBeMoved = new File(resourceDir, cr.getDigest() + ".txt");
		if (toBeMoved != null) {
			FileUtils.moveFileToDirectory(toBeMoved, atticDir, true);
		}
		// System.out.println("Resource moved in attic: " + cr.getUrl());
	}

	private void removeGZippedResource(File resourceDir, String resourceFile) {
		FileUtils.deleteQuietly(new File(resourceDir.getAbsolutePath()
				+ System.getProperty("file.separator") + resourceFile));
	}

	private void removeModifiedArchivedResource(File f, String oldDigest,
			EntityManager entityManager) throws IOException {
		this.removeResource(f, oldDigest, false, entityManager);
	}

	private void removeResource(File f, String oldDigest, boolean attic,
			EntityManager entityManager) throws IOException {
		File resourceDir = null;
		if (attic) {
			resourceDir = new File(FilenameUtils.getFullPath(f
					.getCanonicalPath().replaceAll("__\\d+", ""))
					+ Mapper.ATTICDIR);
		} else {
			resourceDir = new File(FilenameUtils.getFullPath(f
					.getCanonicalPath().replaceAll("__\\d+", ""))
					+ JobMapperMonitor.RESOURCESDIR);
		}
		String resourceFile = oldDigest + ".gz";
		File toBeDeleted = new File(resourceDir, resourceFile);
		if (toBeDeleted.exists() && toBeDeleted.canWrite()) {
			toBeDeleted.delete();
		}
		String txtResourceFile = oldDigest + ".txt";
		toBeDeleted = new File(resourceDir, txtResourceFile);
		if (toBeDeleted.exists() && toBeDeleted.canWrite()) {
			toBeDeleted.delete();
		}
	}

	private void removeResourceInAttic(File f, String oldDigest,
			EntityManager entityManager) throws IOException {
		this.removeResource(f, oldDigest, true, entityManager);
	}

	/**
	 * This method is to deal with MS special and non standard chars
	 * 
	 * @param cleanText
	 * @return
	 */
	private String replaceUnsupportedChars(String cleanText) {
		cleanText = cleanText.replaceAll("’", "'");
		cleanText = cleanText.replaceAll("‘", "'");
		cleanText = cleanText.replaceAll("”", "\"");
		cleanText = cleanText.replaceAll("“", "\"");
		cleanText = cleanText.replaceAll("–", "-");
		cleanText = cleanText.replaceAll("…", "...");
		return cleanText;
	}

	public void run() {
		Random random = new Random();
		int waitSec = random.nextInt(100);
		this.setRunning(true);
		Lifecycle.beginCall();
		this.mapperUserTx = (UserTransaction) org.jboss.seam.Component
				.getInstance("org.jboss.seam.transaction.transaction",
						ScopeType.CONVERSATION);
		this.em = (EntityManager) Component.getInstance("eventEntityManager");
		this.tempDir = this.em.find(Parameter.class,
				Parameter.TEMP_DIR.getKey()).getValue();
		try {
			this.mapperUserTx.setTransactionTimeout(60 * 20);
			Thread.sleep(waitSec);
			if (this.mapperUserTx != null && !this.mapperUserTx.isActive()) {
				if (this.mapperUserTx.getStatus() != javax.transaction.Status.STATUS_ACTIVE) {
					this.mapperUserTx.begin();
				}
			}
			this.em.joinTransaction();
			String dir = this.em.find(Parameter.class,
					Parameter.JOBS_DIR.getKey()).getValue();
			File filename = null;
			boolean childJobMapping = false;
			Job persistedJob = this.em.find(Job.class, this.jobId);
			if (persistedJob.getChildJobName() != null
					&& !persistedJob.isMappedResources()) {
				childJobMapping = true;
				filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
						+ persistedJob.getChildJobName()
						+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
						+ JobMapperMonitor.FILE_SEPARATOR);
			} else {
				filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
						+ persistedJob.getName()
						+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
						+ JobMapperMonitor.FILE_SEPARATOR);
			}
			this.em.flush();
			this.mapperUserTx.commit();
			File[] arcFiles = filename.listFiles();
			if (arcFiles == null) {
				// try also 'completed-' for back compatibility
				filename = new File(dir + JobMapperMonitor.FILE_SEPARATOR
						+ "completed-" + persistedJob.getName()
						+ JobMapperMonitor.FILE_SEPARATOR + "arcs"
						+ JobMapperMonitor.FILE_SEPARATOR);
				arcFiles = filename.listFiles();
			} else {
				for (File f : arcFiles) {
					if (f.getName().equals("resources")) {
						continue;
					}
					boolean uncompressed = false;
					if (f.getName().endsWith(".gz")) {
						f = this.uncompressGzippedArcFile(f);
						uncompressed = true;
					}
					ArchiveReader archiveReader;
					try {
						archiveReader = ArchiveReaderFactory.get(f);
					} catch (IOException e) {
						System.err.println("Errore nella lettura del file: "
								+ f.getAbsolutePath());
						e.printStackTrace();
						continue;
					}
					Iterator<ArchiveRecord> itOnArchiveRecord = archiveReader
							.iterator();
					Set<CrawledResource> childResources = new HashSet<CrawledResource>();
					while (itOnArchiveRecord.hasNext()) {
						ARCRecord archiveRecord = (ARCRecord) itOnArchiveRecord
								.next();
						ARCRecordMetaData metadata = archiveRecord
								.getMetaData();
						Date now = new Date();
						// store only succeeded requests
						if (metadata.getStatusCode() != null
								&& metadata.getStatusCode().equals("200")) {
							String url = metadata.getUrl();
							long length = metadata.getLength();
							System.out.println("Mapping URL: " + url + "\n"
									+ "Size: " + length + "\t"
									+ this.mapperUserTx);
							try {
								archiveRecord.skipHttpHeader();
								archiveRecord.close();
								String digest = archiveRecord.getDigestStr();
								if (!childJobMapping) {
									// do not store equal resources
									if (this.mapperUserTx != null
											&& !this.mapperUserTx.isActive()) {
										if (this.mapperUserTx.getStatus() != javax.transaction.Status.STATUS_ACTIVE) {
											this.mapperUserTx.begin();
										}
									}
									this.em.joinTransaction();
									if (!this.isResourceAlreadyMapped(digest,
											url, this.em)) {
										CrawledResource cr = new CrawledResource();
										cr.setDigest(digest);
										cr.setOffset(metadata.getOffset());
										cr.setArcFile(f.getCanonicalPath());
										cr.setArchiveDate(now);
										cr.setLastModified(now);
										cr.setLength(length);
										cr.setContentType(metadata
												.getMimetype());
										cr.setIp(metadata.getIp());
										cr.setUrl(url);
										cr.setJob(persistedJob);
										persistedJob = this.em.find(Job.class,
												this.jobId);
										persistedJob.getCrawledResources().add(
												cr);
										this.em.persist(cr);
										this.em.persist(persistedJob);
										this.createArchivedResource(f, cr,
												this.em);
										this.em.persist(cr);
										if (this.existsResourceWithSameExtractedText(cr)) {
											persistedJob.getCrawledResources()
													.remove(cr);
											this.em.persist(persistedJob);
											this.em.remove(cr);
										}
									}
									this.em.flush();
									this.mapperUserTx.commit();
								} else {
									CrawledResource cr = new CrawledResource();
									cr.setUrl(url);
									if (this.mapperUserTx != null
											&& !this.mapperUserTx.isActive()) {
										if (this.mapperUserTx.getStatus() != javax.transaction.Status.STATUS_ACTIVE) {
											this.mapperUserTx.begin();
										}
									}
									this.em.joinTransaction();
									// do not store equal resources
									if (!this.isResourceAlreadyMapped(digest,
											url, this.em)) {
										// check urls
										CrawledResource sameUrlResource = this
												.sameURLExists(url,
														persistedJob, this.em);
										boolean modifiedResource = false;
										String oldDigest = null;
										if (sameUrlResource != null) {
											// modified resource
											modifiedResource = true;
											oldDigest = sameUrlResource
													.getDigest();
											cr = sameUrlResource;
										} else {
											// new resource
											cr.setArchiveDate(now);
										}
										cr.setDigest(digest);
										cr.setNoMoreAvailable(false);
										cr.setOffset(metadata.getOffset());
										cr.setLastModified(now);
										cr.setArcFile(f.getCanonicalPath());
										cr.setLength(length);
										cr.setContentType(metadata
												.getMimetype());
										cr.setIp(metadata.getIp());
										cr.setJob(persistedJob);
										persistedJob = this.em.find(Job.class,
												this.jobId);
										persistedJob.getCrawledResources().add(
												cr);
										this.em.persist(cr);
										this.em.persist(persistedJob);
										if (modifiedResource) {
											this.updateArchivedResource(f, cr,
													oldDigest, this.em);
											this.em.persist(cr);
										} else {
											this.createArchivedResource(f, cr,
													this.em);
											if (this.existsResourceWithSameExtractedText(cr)) {
												persistedJob
														.getCrawledResources()
														.remove(cr);
												this.em.persist(persistedJob);
												this.em.remove(cr);
											}
										}
									} else {
										// this is an url that was
										// marked as
										// noMoreAvailable and now it is
										// available
										// again
									}
									CrawledResource sameUrlResource = this
											.sameURLExists(url, persistedJob,
													this.em);
									if (sameUrlResource != null
											&& sameUrlResource
													.isNoMoreAvailable()) {
										String oldDigest = sameUrlResource
												.getDigest();
										cr = sameUrlResource;
										cr.setDigest(digest);
										cr.setNoMoreAvailable(false);
										cr.setOffset(metadata.getOffset());
										cr.setLastModified(now);
										cr.setArcFile(f.getCanonicalPath());
										cr.setLength(length);
										cr.setContentType(metadata
												.getMimetype());
										cr.setIp(metadata.getIp());
										cr.setJob(persistedJob);
										persistedJob = this.em.find(Job.class,
												this.jobId);
										persistedJob.getCrawledResources().add(
												cr);
										this.em.persist(cr);
										this.em.persist(persistedJob);
										this.createArchivedResourceAndDeleteFromAttic(
												f, oldDigest, cr, this.em);
										this.em.persist(cr);
									}
									childResources.add(cr);
									this.em.flush();
									this.mapperUserTx.commit();
								}
							} catch (Exception e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							} finally {
								try {
									if (this.mapperUserTx != null
											&& this.mapperUserTx.isActive()) {
										this.mapperUserTx.rollback();
									}
								} catch (IllegalStateException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (SecurityException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								} catch (SystemException e1) {
									// TODO Auto-generated catch block
									e1.printStackTrace();
								}
								this.setRunning(false);
							}
						}
					}
					archiveReader.close();
					if (uncompressed) {
						FileUtils.deleteQuietly(f);
					}
					if (childJobMapping) {
						if (this.mapperUserTx != null
								&& !this.mapperUserTx.isActive()) {
							if (this.mapperUserTx.getStatus() != javax.transaction.Status.STATUS_ACTIVE) {
								this.mapperUserTx.begin();
							}
						}

						this.em.joinTransaction();
						this.lookForNoMoreAvailableResources(persistedJob,
								childResources, f, this.em);
						this.em.flush();
						this.mapperUserTx.commit();
					}
				}
			}
			if (this.mapperUserTx != null && !this.mapperUserTx.isActive()) {
				if (this.mapperUserTx.getStatus() != javax.transaction.Status.STATUS_ACTIVE) {
					this.mapperUserTx.begin();
				}
			}
			this.em.joinTransaction();
			this.em.refresh(persistedJob);
			persistedJob.setMappedResources(true);
			this.em.persist(persistedJob);
			this.em.flush();
			this.mapperUserTx.commit();
			this.flagBearer.setOwnerName(persistedJob.getCrawlerUser()
					.getName());
			this.flagBearer.setOwnerSurname(persistedJob.getCrawlerUser()
					.getSurname());
			this.flagBearer.setJobName(persistedJob.getName());
			this.flagBearer.setEmailAddress(persistedJob.getCrawlerUser()
					.getEmail());
			if (this.flagBearer.getEmailAddress() != null
					&& this.flagBearer.getEmailAddress().trim().length() > 0) {
				this.renderer.render("/mail/mappedJob.xhtml");
			}

			this.setRunning(false);
			System.out.println("Job " + this.getJobName()
					+ " mapping terminated.");
		} catch (Exception e) {
			if (this.mapperUserTx != null) {
				try {
					System.err.println("STATUS: "
							+ this.mapperUserTx.getStatus());
				} catch (SystemException e1) {
					System.err.println("SSTACKTRACE mapperUserTx");
					e1.printStackTrace();
				}
			}
			e.printStackTrace();
		} finally {
			try {
				if (this.mapperUserTx != null && this.mapperUserTx.isActive()) {
					this.mapperUserTx.rollback();
				}
			} catch (Exception e) {
				System.err.println("FINALLY");
				e.printStackTrace();
			}
			Lifecycle.endCall();
			this.setRunning(false);
		}
	}

	@SuppressWarnings("unchecked")
	private CrawledResource sameURLExists(String url, Job persistedJob,
			EntityManager entityManager) {
		List<CrawledResource> listCR = entityManager
				.createQuery("from CrawledResource cr where cr.url=:url")
				.setParameter("url", url).getResultList();
		if (listCR.size() == 1) {
			return listCR.get(0);
		}
		return null;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	private StringWithEncoding transformDOC2HTML(File resourceFile,
			EntityManager entityManager) throws IOException, SAXException,
			TikaException, TransformerConfigurationException {
		ParseContext context = new ParseContext();
		Parser parser = new AutoDetectParser();
		context.set(Parser.class, parser);
		Metadata metadata = new Metadata();
		Writer writer = null;
		if (resourceFile.isFile()) {
			metadata.set(TikaMetadataKeys.RESOURCE_NAME_KEY,
					resourceFile.getName());
			InputStream input = new FileInputStream(resourceFile);
			try {
				writer = new StringWriter();
				parser.parse(input, this.HTML.getContentHandler(null, writer),
						metadata, context);
			} finally {
				input.close();
				if (writer != null) {
					writer.close();
				}
			}
			CharsetDetector charsetDetector = new CharsetDetector();
			charsetDetector.setText(writer.toString().getBytes());
			String encoding = charsetDetector.detect().getName();
			StringWithEncoding stringWithEncoding = new StringWithEncoding(
					writer.toString(), encoding);
			return stringWithEncoding;
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private StringWithEncoding transformPDF2HTML(File resourceFile,
			EntityManager entityManager) throws IOException,
			InterruptedException {
		String workingDirName = System.getProperty("java.io.tmpdir");
		String userDir = System.getProperty("user.dir");
		byte[] buf = new byte[Mapper.BUFLENGTH];
		int count = 0;
		GZIPInputStream gzis = new GZIPInputStream(new FileInputStream(
				resourceFile));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		while ((count = gzis.read(buf)) != -1) {
			baos.write(buf, 0, count);
		}
		gzis.close();
		baos.close();
		byte[] byteArray = baos.toByteArray();
		String uuid = UUID.randomUUID().toString();
		String pdfFileName = uuid + ".pdf";
		String htmlFileName = uuid + ".html";
		File tmpDir = new File(workingDirName);
		String htmlFileNameCompletePath = workingDirName
				+ JobMapperMonitor.FILE_SEPARATOR + htmlFileName;
		File fileToConvert = new File(tmpDir, pdfFileName);
		FileUtils.writeByteArrayToFile(fileToConvert, byteArray);
		DefaultExecutor executor = new DefaultExecutor();
		executor.setExitValue(0);
		CommandParameter cp = entityManager.find(CommandParameter.class,
				CommandParameter.PDFTOHTML_EXECUTABLE_KEY);
		CommandLine commandLine = CommandLine.parse(cp.getCommandValue());
		commandLine.addArgument("-c");
		commandLine.addArgument("-i");
		commandLine.addArgument(fileToConvert.getAbsolutePath());
		commandLine.addArgument(htmlFileNameCompletePath);
		executor.setExitValue(0);
		executor.execute(commandLine);
		try {
			FileUtils.moveFileToDirectory(
					new File(userDir + JobMapperMonitor.FILE_SEPARATOR + uuid
							+ "-outline.html"), tmpDir, false);
		} catch (IOException e) {
		}
		cp = entityManager.find(CommandParameter.class,
				CommandParameter.PDFCLEANER_EXECUTABLE_KEY);
		commandLine = CommandLine.parse("java -Xmx128m -jar -Djava.io.tmpdir="
				+ this.tempDir + " " + cp.getCommandValue());
		commandLine.addArgument(htmlFileNameCompletePath);
		commandLine.addArgument("39");
		commandLine.addArgument("6");
		commandLine.addArgument("5");
		executor = new DefaultExecutor();
		executor.setExitValue(0);
		ExecuteWatchdog watchdog = new ExecuteWatchdog(
				Mapper.PDFCLEANER_TIMEOUT);
		executor.setWatchdog(watchdog);
		ByteArrayOutputStream baosStdOut = new ByteArrayOutputStream(1024);
		ExecuteStreamHandler executeStreamHandler = new PumpStreamHandler(
				baosStdOut, null, null);
		executor.setStreamHandler(executeStreamHandler);
		int exitValue = executor.execute(commandLine);
		String htmlString = null;
		if (exitValue == 0) {
			htmlString = baosStdOut.toString();
		}
		FileUtils.deleteQuietly(new File(htmlFileNameCompletePath));
		PrefixFileFilter pff = new PrefixFileFilter(uuid);
		for (File f : FileUtils.listFiles(tmpDir, pff, null)) {
			FileUtils.deleteQuietly(f);
		}
		if (htmlString != null) {
			htmlString = htmlString.replaceAll("&nbsp;", " ");
			htmlString = htmlString.replaceAll("<br.*?>", " ");
			CharsetDetector charsetDetector = new CharsetDetector();
			charsetDetector.setText(htmlString.getBytes());
			String encoding = charsetDetector.detect().getName();
			return new StringWithEncoding(htmlString, encoding);
		}
		return null;
	}

	private StringWithEncoding transformRTF2HTML(File resourceFile,
			EntityManager entityManager) throws IOException, SAXException,
			TikaException, TransformerConfigurationException {
		// AutoDetectParser works for rtf as for doc
		return this.transformDOC2HTML(resourceFile, entityManager);
	}

	private StringWithEncoding transformTXT2HTML(File resourceFile,
			EntityManager entityManager) throws IOException, SAXException,
			TikaException, TransformerConfigurationException {
		// AutoDetectParser works for txt as for doc
		return this.transformDOC2HTML(resourceFile, entityManager);
	}

	private File uncompressGzippedArcFile(File f) throws IOException {
		FileInputStream fin = new FileInputStream(f.getAbsolutePath());
		BufferedInputStream in = new BufferedInputStream(fin);
		File uncompressedFile = new File(FilenameUtils.removeExtension(f
				.getAbsolutePath()));
		FileOutputStream out = new FileOutputStream(uncompressedFile);
		GzipCompressorInputStream gzIn = new GzipCompressorInputStream(in, true);
		final byte[] buffer = new byte[1024];
		int n = 0;
		while (-1 != (n = gzIn.read(buffer))) {
			out.write(buffer, 0, n);
		}
		out.close();
		gzIn.close();
		return uncompressedFile;
	}

	private void updateArchivedResource(File f, CrawledResource cr,
			String oldDigest, EntityManager entityManager) throws SAXException,
			TikaException, IOException {
		this.createArchivedResource(f, cr, entityManager);
		this.removeModifiedArchivedResource(f, oldDigest, entityManager);
	}
}
