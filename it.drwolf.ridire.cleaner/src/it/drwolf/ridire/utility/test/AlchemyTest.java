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
