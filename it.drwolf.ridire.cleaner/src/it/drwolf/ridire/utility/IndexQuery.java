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
package it.drwolf.ridire.utility;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.MMapDirectory;

public class IndexQuery {

	public static void main(String[] args) {
		new IndexQuery(args);
	}

	private Options options;
	private String dirName;

	private String term;

	public IndexQuery(String[] args) {
		this.createOptions();
		this.parseOptions(args);
		try {
			IndexReader indexReader = IndexReader.open(new MMapDirectory(
					new File(this.dirName)));
			IndexSearcher indexSearcher = new IndexSearcher(indexReader);
			TermQuery tqLemma = new TermQuery(new Term("lemma", this.term));
			TopDocs results = indexSearcher.search(tqLemma, Integer.MAX_VALUE);
			System.out.println("Total results: " + results.totalHits);
			for (int i = 0; i < results.totalHits; i++) {
				Document d = indexReader.document(results.scoreDocs[i].doc);
				String sketch = d.get("sketch");
				System.out.println(sketch);
			}
		} catch (CorruptIndexException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void createOptions() {
		this.options = new Options();
		Option dir = new Option("i", "indexDir", true, "index dir");
		this.options.addOption(dir);
		Option t = new Option("t", "term", true, "term");
		this.options.addOption(t);
	}

	private void parseOptions(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new GnuParser();
		org.apache.commons.cli.CommandLine cmdline = null;
		try {
			// parse the command line arguments
			cmdline = parser.parse(this.options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Index query.  Reason: " + exp.getMessage());
			formatter.printHelp("Index query.", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.dirName = cmdline.getOptionValue("i");
			if (this.dirName == null) {
				System.err.println("No directory provided.");
				formatter.printHelp("Index query.", this.options);
				System.exit(-1);
			}
			this.term = cmdline.getOptionValue("t");
			if (this.term == null) {
				System.err.println("No term provided.");
				formatter.printHelp("Index query.", this.options);
				System.exit(-1);
			}
		}
	}
}
