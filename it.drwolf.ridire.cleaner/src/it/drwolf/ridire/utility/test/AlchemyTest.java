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

import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.orchestr8.api.AlchemyAPI;

public class AlchemyTest {
	private static final String apiKey = "1bf8db7d2a5b5bdc8920d796d2bc7944a0c62a06";
	private static final String url = "http://www.corriere.it/politica/10_marzo_19/berlusconi-santoro-crisi-balducci_71ea344a-3348-11df-82b0-00144f02aabe.shtml";

	public static void main(String[] args) {
		new AlchemyTest();
	}

	public AlchemyTest() {
		AlchemyAPI alchemyAPI = AlchemyAPI.GetInstanceFromString(apiKey);
		try {
			Document d = alchemyAPI.URLGetText(url);
			NodeList list = d.getElementsByTagName("text");
			for (int i = 0; i < list.getLength(); i++) {
				System.out.println(list.item(i).getTextContent());
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
	}
}
