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
package it.drwolf.ridire.cleaners;

import it.drwolf.ridire.cleaners.utils.StringWithEncoding;
import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Parameter;

import java.io.File;
import java.io.IOException;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.annotations.Name;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ScriptResult;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;

@Name("readabilityCleaner")
public class ReadabilityCleaner {

	public class NoOpErrorHandler implements ErrorHandler {

		public void error(CSSParseException arg0) throws CSSException {
			// TODO Auto-generated method stub

		}

		public void fatalError(CSSParseException arg0) throws CSSException {
			// TODO Auto-generated method stub

		}

		public void warning(CSSParseException arg0) throws CSSException {
			// TODO Auto-generated method stub

		}

	}

	public class NoOpIncorrectnessListener implements IncorrectnessListener {

		public void notify(String arg0, Object arg1) {
			// TODO Auto-generated method stub

		}

	}

	public String getCleanedTextFromString(CrawledResource crawledResource,
			StringWithEncoding rawContentAndEncoding,
			EntityManager entityManager) {
		String bookmarkJS = entityManager.find(Parameter.class,
				Parameter.READABILITY_SOURCE.getKey()).getValue();
		String host = entityManager.find(Parameter.class,
				Parameter.READABILITY_HOSTAPP.getKey()).getValue();
		bookmarkJS = bookmarkJS.replaceAll("@@@HOST@@@", host.trim());
		WebClient webClient = new WebClient();
		webClient = new WebClient(BrowserVersion.FIREFOX_3);
		webClient.setCssEnabled(true);
		webClient.setJavaScriptEnabled(true);
		// vedi FAQ: http://htmlunit.sourceforge.net/faq.html#AJAXDoesNotWork
		webClient.setAjaxController(new NicelyResynchronizingAjaxController());
		webClient.waitForBackgroundJavaScript(5000);
		webClient.waitForBackgroundJavaScriptStartingBefore(5000);
		// i seguenti 4 set servono per limitare i log
		webClient.setHTMLParserListener(null);
		webClient.setIncorrectnessListener(new NoOpIncorrectnessListener());
		webClient.setCssErrorHandler(new NoOpErrorHandler());
		webClient.setPrintContentOnFailingStatusCode(false);
		// questo serve per avere il tipo di errore HTTP
		webClient.setThrowExceptionOnFailingStatusCode(true);
		webClient.setThrowExceptionOnScriptError(false);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		// webClient.setJavaScriptTimeout(4000);
		webClient.setTimeout(4000);
		File tmpFile = null;
		HtmlPage htmlPage = null;
		String ret = new String();
		try {
			tmpFile = File.createTempFile("readability", null);
			FileUtils.writeStringToFile(tmpFile, rawContentAndEncoding
					.getString());
			htmlPage = webClient.getPage(host.trim()
					+ "/tmp/resource.seam?filename=" + tmpFile.getName());
			FileUtils.deleteQuietly(tmpFile);
			if (htmlPage != null) {
				// webClient.setJavaScriptEnabled(false);
				ScriptResult scriptResult = htmlPage
						.executeJavaScript(bookmarkJS);
				// System.out.println(htmlPage.asXml());
				for (HtmlElement he : htmlPage.getElementsByName("div")) {
					// System.out.println("-->" + he.getAttribute("id") +
					// "<--");
					// System.out.println(he.asText());
				}
				HtmlElement element = htmlPage
						.getElementById("readability-content");
				if (element != null) {
					ret = element.asText();
				}
			}
			if (tmpFile != null) {
				FileUtils.deleteQuietly(tmpFile);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RuntimeException re) {
			// TODO: handle exception
			re.printStackTrace();
		} catch (Throwable t) {
			t.printStackTrace();
		} finally {
			webClient.closeAllWindows();
		}
		return ret.trim();
	}
}
