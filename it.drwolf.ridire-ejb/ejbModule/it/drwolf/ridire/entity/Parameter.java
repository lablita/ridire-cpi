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
package it.drwolf.ridire.entity;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;

@Entity
public class Parameter implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 576435105987796072L;
	private String key;
	private String value;
	public static final Parameter INDEXING_ENABLED = new Parameter(
			"indexing.enabled", "false");
	public static final Parameter RIDIRETESTCorpus = new Parameter(
			"test.corpus", "CorpusTest");
	public static final Parameter HOSTNAME = new Parameter("app.hostname",
			"www.ridire.it");
	public static final Parameter ENGINE_PROTOCOL = new Parameter(
			"engine.protocol", "https");
	public static final Parameter ENGINE_HOST = new Parameter("engine.host",
			"localhost");
	public static final Parameter ENGINE_PORT = new Parameter("engine.port",
			"8443");
	public static final Parameter ENGINE_URI = new Parameter("engine.uri",
			Parameter.ENGINE_PROTOCOL.getValue() + "://"
					+ Parameter.ENGINE_HOST.getValue() + ":"
					+ Parameter.ENGINE_PORT.getValue() + "/engine/");
	public static final Parameter JOBS_DIR = new Parameter("jobs.dir",
			"/home/drwolf/heritrix-3.1.1-SNAPSHOT/jobs");
	// public static final Parameter APP_NAME = new Parameter("app.name",
	// "it.drwolf.ridire.crawlerwrap");
	public static final Parameter JOBMAPPER_CRON = new Parameter(
			"jobMapper.cron", "0 */5 * * * ?");
	public static final Parameter JOBUPDATER_CRON = new Parameter(
			"jobUpdater.cron", "0 */5 * * * ?");
	public static final Parameter RESOURCESREPORT_CRON = new Parameter(
			"resources.report.cron", "0 0 */4 * * ?");
	public static final Parameter LOCAL_RESOURCES_DIR = new Parameter(
			"localresources.dir", "/home/drwolf/heritrix-3.1.1-SNAPSHOT/local");
	public static final Parameter ALCHEMY_KEY = new Parameter("alchemy.key",
			"1bf8db7d2a5b5bdc8920d796d2bc7944aaaaaaaa");
	public static final Parameter READABILITY_KEY = new Parameter(
			"readability.key", "b08fc64ae2b94e165b298508aab00df69d9aaaaa");
	public static final Parameter READABILITY_HOSTAPP = new Parameter(
			"readability.host", "http://localhost:8180/it.drwolf.ridire/");
	public static final Parameter POS_ENABLED = new Parameter("pos.enabled",
			"false");
	public static final Parameter INDEX_DIR = new Parameter("index.location",
			"/home/drwolf/tmp/index");
	public static final Parameter TEST_INDEX_DIR = new Parameter(
			"index.location.test", "/home/drwolf/tmp/index_test");
	public static final Parameter POS_FILES_LOCATION = new Parameter(
			"pos.location", "/home/drwolf/tmp/total_tgd");
	public static final Parameter PERL_CLEANER_USER = new Parameter(
			"perl.cleaner.user", "perlcleaner");
	public static final Parameter PERL_CLEANER_PW = new Parameter(
			"perl.cleaner.pw", "ridireperl");
	public static final Parameter PERL_CLEANER_PATH = new Parameter(
			"perl.cleaner.path", "/home/ridireperl/cleanerroom");
	public static final Parameter WWW1_USERNAME = new Parameter(
			"www1.username", "username");
	public static final Parameter WWW1_PW = new Parameter("www1.pw", "secret");
	public static final Parameter WWW1_HOST = new Parameter("www1.host",
			"www.ridire.it");
	public static final Parameter WWW1_LOCAL_STORE = new Parameter(
			"www1.local.store", "/home/drwolf/www1");
	public static final Parameter PERL_CLEANER_TEMPLATE = new Parameter(
			"perl.cleaner.template",
			"/home/drwolf/jboss-5.1.0.GA/server/default/deploy/it.drwolf.ridire-ear.ear/it.drwolf.ridire.war/perl/template.pl");
	public static final Parameter TEMP_DIR = new Parameter("temp.dir",
			"/home/drwolf/ridirecleaner_tmp");

	public static final Parameter CQP_EXECUTABLE = new Parameter(
			"cqp.executable", "/usr/local/cwb-3.4.3/bin/cqp");
	public static final Parameter CWBDECODE_EXECUTABLE = new Parameter(
			"cwb.decode.executable", "/usr/local/cwb-3.4.3/bin/cwb-decode");
	public static final Parameter CWBSCAN_EXECUTABLE = new Parameter(
			"cwbscan.executable", "/usr/local/cwb-3.4.3/bin/cwb-scan-corpus");
	public static final Parameter CQP_CORPUSNAME = new Parameter(
			"cqp.corpusname", "RIDIRE2");
	public static final Parameter CQP_REGISTRY = new Parameter("cqp.registry",
			"/usr/local/share/cwb/registry/");
	public static final Parameter SKETCH_INDEX_LOCATION = new Parameter(
			"sketch.index.location", "/home/drwolf/index/");
	public static final Parameter SKETCH_INDEX_LOCATION2 = new Parameter(
			"sketch.index.location2", "/home/drwolf/index2/");
	public static final Parameter JOBS_TO_BE_PROCESSED = new Parameter(
			"jobs.tobeprocessed", "4");
	public static final Parameter[] defaults = new Parameter[] {
			Parameter.HOSTNAME, Parameter.ENGINE_PROTOCOL,
			Parameter.ENGINE_HOST, Parameter.ENGINE_PORT, Parameter.ENGINE_URI,
			Parameter.JOBMAPPER_CRON, Parameter.LOCAL_RESOURCES_DIR,
			Parameter.JOBS_DIR, Parameter.ALCHEMY_KEY,
			Parameter.READABILITY_HOSTAPP, Parameter.READABILITY_KEY,
			Parameter.POS_ENABLED, Parameter.INDEX_DIR,
			Parameter.TEST_INDEX_DIR, Parameter.POS_FILES_LOCATION,
			Parameter.JOBUPDATER_CRON, Parameter.RESOURCESREPORT_CRON,
			Parameter.PERL_CLEANER_USER, Parameter.PERL_CLEANER_PW,
			Parameter.PERL_CLEANER_TEMPLATE, Parameter.PERL_CLEANER_PATH,
			Parameter.WWW1_USERNAME, Parameter.WWW1_PW, Parameter.WWW1_HOST,
			Parameter.WWW1_LOCAL_STORE, Parameter.RIDIRETESTCorpus,
			Parameter.TEMP_DIR, Parameter.INDEXING_ENABLED,
			Parameter.CQP_EXECUTABLE, Parameter.CQP_CORPUSNAME,
			Parameter.CQP_REGISTRY, Parameter.CWBSCAN_EXECUTABLE,
			Parameter.CWBDECODE_EXECUTABLE, Parameter.JOBS_TO_BE_PROCESSED,
			Parameter.SKETCH_INDEX_LOCATION, Parameter.SKETCH_INDEX_LOCATION2 };

	public static final Integer FINISHED = 10;
	public static final Integer NOT_PROCESSED = 0;
	public static final Integer INDEXING_PHASE = 80;
	public static final Integer INDEXED = 100;
	public static final int PROCESSING_ERROR = 1000;

	public Parameter() {
	}

	public Parameter(String key, String value) {
		super();
		this.key = key;
		this.value = value;
	}

	@Id
	@Column(name = "key_column")
	public String getKey() {
		return this.key;
	}

	@Lob
	@Column(columnDefinition = "TEXT")
	public String getValue() {
		return this.value;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
