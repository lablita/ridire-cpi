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
package it.drwolf.ridire.cleaners;

import it.drwolf.ridire.cleaners.utils.StringWithEncoding;
import it.drwolf.ridire.entity.CrawledResource;
import it.drwolf.ridire.entity.Parameter;

import java.io.IOException;

import javax.persistence.EntityManager;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.jboss.seam.annotations.Name;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.orchestr8.api.AlchemyAPI;

@Name("alchemyCleaner")
public class AlchemyCleaner {

	public String getCleanedTextFromString(CrawledResource crawledResource,
			StringWithEncoding rawContentAndEncoding,
			EntityManager entityManager) {
		String apiKey = entityManager.find(Parameter.class,
				Parameter.ALCHEMY_KEY.getKey()).getValue();
		AlchemyAPI alchemyAPI = AlchemyAPI.GetInstanceFromString(apiKey);
		StringBuffer buffer = new StringBuffer();
		try {
			Document d = alchemyAPI.HTMLGetText(rawContentAndEncoding
					.getString(), crawledResource.getUrl());
			NodeList list = d.getElementsByTagName("text");
			for (int i = 0; i < list.getLength(); i++) {
				buffer.append(list.item(i).getTextContent() + " ");
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer.toString();
	}

	/**
	 * Do not use, because the same url could refer to different resources, so
	 * Alchemy could extract text from a different resource than the one stored
	 * in RIDIRE
	 * 
	 * @param url
	 * @param entityManager
	 * @return
	 */
	@Deprecated
	public String getCleanedTextFromURL(String url, EntityManager entityManager) {
		String apiKey = entityManager.find(Parameter.class,
				Parameter.ALCHEMY_KEY.getKey()).getValue();
		AlchemyAPI alchemyAPI = AlchemyAPI.GetInstanceFromString(apiKey);
		StringBuffer buffer = new StringBuffer();
		try {
			Document d = alchemyAPI.URLGetText(url);
			NodeList list = d.getElementsByTagName("text");
			for (int i = 0; i < list.getLength(); i++) {
				buffer.append(list.item(i).getTextContent() + " ");
			}
		} catch (XPathExpressionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buffer.toString();
	}
}
