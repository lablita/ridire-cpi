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
package it.drwolf.ridire.utility.test;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AlchemyAPI {
	static public AlchemyAPI GetInstanceFromFile(String keyFilename)
			throws FileNotFoundException, IOException {
		AlchemyAPI api = new AlchemyAPI();
		api.LoadAPIKey(keyFilename);

		return api;
	}

	static public AlchemyAPI GetInstanceFromString(String apiKey) {
		AlchemyAPI api = new AlchemyAPI();
		api.SetAPIKey(apiKey);

		return api;
	}

	private String _apiKey;

	private String _hostPrefix;

	private AlchemyAPI() {
		this._hostPrefix = "access";
	}

	private void CheckHTML(String html, String url) {
		if (null == html || html.length() < 5) {
			throw new IllegalArgumentException(
					"Enter a HTML document to analyze.");
		}

		if (null == url || url.length() < 10) {
			throw new IllegalArgumentException("Enter an URL to analyze.");
		}
	}

	private void CheckText(String text) {
		if (null == text || text.length() < 5) {
			throw new IllegalArgumentException("Enter some text to analyze.");
		}
	}

	private void CheckURL(String url) {
		if (null == url || url.length() < 10) {
			throw new IllegalArgumentException("Enter an URL to analyze.");
		}
	}

	private String getNodeValue(XPathFactory factory, Document doc,
			String xpathStr) throws XPathExpressionException {
		XPath xpath = factory.newXPath();
		XPathExpression expr = xpath.compile(xpathStr);
		Object result = expr.evaluate(doc, XPathConstants.NODESET);
		NodeList results = (NodeList) result;

		if (results.getLength() > 0 && null != results.item(0)) {
			return results.item(0).getNodeValue();
		}

		return null;
	}

	public Document HTMLGetCategory(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetCategory", "html", "html", html, "url", url);
	}

	public Document HTMLGetConstraintQuery(String html, String url, String query)
			throws IOException, XPathExpressionException, SAXException,
			ParserConfigurationException {
		this.CheckHTML(html, url);
		if (null == query || query.length() < 2) {
			throw new IllegalArgumentException(
					"Invalid constraint query specified");
		}

		return this.POST("HTMLGetConstraintQuery", "html", "html", html, "url",
				url, "cquery", query);
	}

	public Document HTMLGetFeedLinks(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetFeedLinks", "html", "html", html, "url", url);
	}

	public Document HTMLGetKeywords(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetKeywords", "html", "html", html, "url", url);
	}

	public Document HTMLGetLanguage(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetLanguage", "html", "html", html, "url", url);
	}

	public Document HTMLGetMicroformats(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetMicroformatData", "html", "html", html, "url",
				url);
	}

	public Document HTMLGetNamedEntities(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetNamedEntities", "html", "html", html, "url",
				url);
	}

	public Document HTMLGetRankedNamedEntities(String html, String url)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetRankedNamedEntities", "html", "html", html,
				"url", url);
	}

	public Document HTMLGetRawText(String html, String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetRawText", "html", "html", html, "url", url);
	}

	public Document HTMLGetText(String html, String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetText", "html", "html", html, "url", url);
	}

	public Document HTMLGetTitle(String html, String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckHTML(html, url);

		return this.POST("HTMLGetTitle", "html", "html", html, "url", url);
	}

	public void LoadAPIKey(String filename) throws IOException,
			FileNotFoundException {
		if (null == filename || 0 == filename.length()) {
			throw new IllegalArgumentException("Empty API key file specified.");
		}

		File file = new File(filename);
		FileInputStream fis = new FileInputStream(file);

		BufferedReader breader = new BufferedReader(new InputStreamReader(fis));

		this._apiKey = breader.readLine().replaceAll("\\n", "").replaceAll(
				"\\r", "");

		fis.close();
		breader.close();

		if (null == this._apiKey || this._apiKey.length() < 5) {
			throw new IllegalArgumentException("Too short API key.");
		}
	}

	private Document POST(String callName, String callPrefix, String... param)
			throws IOException, SAXException, ParserConfigurationException,
			XPathExpressionException {
		URL url = new URL("http://" + this._hostPrefix
				+ ".alchemyapi.com/calls/" + callPrefix + "/" + callName);

		HttpURLConnection handle = (HttpURLConnection) url.openConnection();
		handle.setDoOutput(true);

		StringBuilder data = new StringBuilder();

		data.append("apikey=").append(this._apiKey).append("&outputMode=xml");
		for (int i = 0; i < param.length; ++i) {
			data.append('&').append(param[i]);
			if (++i < param.length) {
				data.append('=').append(URLEncoder.encode(param[i], "UTF8"));
			}
		}

		handle.addRequestProperty("Content-Length", Integer.toString(data
				.length()));

		DataOutputStream ostream = new DataOutputStream(handle
				.getOutputStream());
		ostream.write(data.toString().getBytes());
		ostream.close();

		DataInputStream istream = new DataInputStream(handle.getInputStream());
		Document doc = DocumentBuilderFactory.newInstance()
				.newDocumentBuilder().parse(istream);

		istream.close();
		handle.disconnect();

		XPathFactory factory = XPathFactory.newInstance();

		String statusStr = this.getNodeValue(factory, doc,
				"/results/status/text()");
		if (null == statusStr || !statusStr.equals("OK")) {
			String statusInfoStr = this.getNodeValue(factory, doc,
					"/results/statusInfo/text()");
			if (null != statusInfoStr && statusInfoStr.length() > 0) {
				throw new IOException("Error making API call: " + statusInfoStr
						+ '.');
			}

			throw new IOException("Error making API call: " + statusStr + '.');
		}

		return doc;
	}

	public void SetAPIHost(String apiHost) {
		this._hostPrefix = apiHost;

		if (null == this._hostPrefix || this._hostPrefix.length() < 2) {
			throw new IllegalArgumentException("Too short API host.");
		}
	}

	public void SetAPIKey(String apiKey) {
		this._apiKey = apiKey;

		if (null == this._apiKey || this._apiKey.length() < 5) {
			throw new IllegalArgumentException("Too short API key.");
		}
	}

	public Document TextGetKeywords(String text) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckText(text);

		return this.POST("TextGetKeywords", "text", "text", text);
	}

	public Document TextGetLanguage(String text) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckText(text);

		return this.POST("TextGetLanguage", "text", "text", text);
	}

	public Document TextGetNamedEntities(String text) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckText(text);

		return this.POST("TextGetNamedEntities", "text", "text", text);
	}

	public Document TextGetRankedNamedEntities(String text) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckText(text);

		return this.POST("TextGetRankedNamedEntities", "text", "text", text);
	}

	public Document URLGetCategory(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetCategory", "url", "url", url);
	}

	public Document URLGetConstraintQuery(String url, String query)
			throws IOException, XPathExpressionException, SAXException,
			ParserConfigurationException {
		this.CheckURL(url);
		if (null == query || query.length() < 2) {
			throw new IllegalArgumentException(
					"Invalid constraint query specified");
		}

		return this.POST("URLGetConstraintQuery", "url", "url", url, "cquery",
				query);
	}

	public Document URLGetFeedLinks(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetFeedLinks", "url", "url", url);
	}

	public Document URLGetKeywords(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetKeywords", "url", "url", url);
	}

	public Document URLGetLanguage(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetLanguage", "url", "url", url);
	}

	public Document URLGetMicroformats(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetMicroformatData", "url", "url", url);
	}

	public Document URLGetNamedEntities(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetNamedEntities", "url", "url", url);
	}

	public Document URLGetRankedNamedEntities(String url) throws IOException,
			SAXException, ParserConfigurationException,
			XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetRankedNamedEntities", "url", "url", url);
	}

	public Document URLGetRawText(String url) throws IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetRawText", "url", "url", url);
	}

	public Document URLGetText(String url) throws IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetText", "url", "url", url);
	}

	public Document URLGetTitle(String url) throws IOException, SAXException,
			ParserConfigurationException, XPathExpressionException {
		this.CheckURL(url);

		return this.POST("URLGetTitle", "url", "url", url);
	}
}
