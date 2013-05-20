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
package it.drwolf.heritrix.addons;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.archive.modules.CrawlURI;
import org.archive.modules.writer.ARCWriterProcessor;

public class DWARCWriterProcessor extends ARCWriterProcessor {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7999111846442784481L;
	boolean contentTypeNotMatchesPatternEnabled = false;
	String contentTypeNotMatchesPattern = "^.*$";
	boolean notMatchesFilePatternEnabled = true;
	String notMatchesFilePattern = "^.*$";
	boolean matchesFilePatternEnabled = false;
	String matchesFilePattern = "^.*$";

	public String getContentTypeNotMatchesPattern() {
		return this.contentTypeNotMatchesPattern;
	}

	public boolean getContentTypeNotMatchesPatternEnabled() {
		return this.contentTypeNotMatchesPatternEnabled;
	}

	public String getMatchesFilePattern() {
		return this.matchesFilePattern;
	}

	public boolean getMatchesFilePatternEnabled() {
		return this.matchesFilePatternEnabled;
	}

	public String getNotMatchesFilePattern() {
		return this.notMatchesFilePattern;
	}

	public boolean getNotMatchesFilePatternEnabled() {
		return this.notMatchesFilePatternEnabled;
	}

	public void setContentTypeNotMatchesPattern(
			String contentTypeNotMatchesPattern) {
		this.contentTypeNotMatchesPattern = contentTypeNotMatchesPattern;
	}

	public void setContentTypeNotMatchesPatternEnabled(
			boolean contentTypeNotMatchesPatternEnabled) {
		this.contentTypeNotMatchesPatternEnabled = contentTypeNotMatchesPatternEnabled;
	}

	public void setMatchesFilePattern(String matchesFilePattern) {
		this.matchesFilePattern = matchesFilePattern;
	}

	public void setMatchesFilePatternEnabled(boolean matchesFilePatternEnabled) {
		this.matchesFilePatternEnabled = matchesFilePatternEnabled;
	}

	public void setNotMatchesFilePattern(String notMatchesFilePattern) {
		this.notMatchesFilePattern = notMatchesFilePattern;
	}

	public void setNotMatchesFilePatternEnabled(
			boolean notMatchesFilePatternEnabled) {
		this.notMatchesFilePatternEnabled = notMatchesFilePatternEnabled;
	}

	@Override
	protected boolean shouldWrite(CrawlURI curi) {
		if (!super.shouldWrite(curi)) {
			return false;
		}
		String uri = curi.getURI();
		if (uri == null || uri.trim().length() < 1) {
			return false;
		}
		if (this.getNotMatchesFilePatternEnabled()
				&& this.getNotMatchesFilePattern() != null) {
			Pattern p1 = Pattern.compile(this.getNotMatchesFilePattern());
			Matcher m1 = p1.matcher(uri);
			if (!m1.matches()) {
				return false;
			}
		}
		if (this.getMatchesFilePatternEnabled()
				&& this.getMatchesFilePattern() != null) {
			Pattern p2 = Pattern.compile(this.getMatchesFilePattern());
			Matcher m2 = p2.matcher(uri);
			if (m2.matches()) {
				return false;
			}
		}
		String contentType = curi.getContentType();
		if (contentType != null
				&& this.getContentTypeNotMatchesPatternEnabled()
				&& this.getContentTypeNotMatchesPattern() != null) {
			Pattern p3 = Pattern
					.compile(this.getContentTypeNotMatchesPattern());
			Matcher m3 = p3.matcher(contentType);
			if (!m3.matches()) {
				System.out
						.println("NOK: " + curi.getURI() + "\t" + contentType);
				return false;
			}
		}
		System.out.println("OK: " + curi.getURI() + "\t" + contentType);
		return true;
	}
}
