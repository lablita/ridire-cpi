package it.drwolf.ridire.utility;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.LogFactory;
import org.w3c.css.sac.CSSException;
import org.w3c.css.sac.CSSParseException;
import org.w3c.css.sac.ErrorHandler;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.IncorrectnessListener;
import com.gargoylesoftware.htmlunit.NicelyResynchronizingAjaxController;
import com.gargoylesoftware.htmlunit.ProxyConfig;
import com.gargoylesoftware.htmlunit.ThreadedRefreshHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.orchestr8.api.AlchemyAPI;

public class RIDIRECleaner_old {
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

	private static final String ALCHEMY = "alchemy";

	private static final String READABILITY = "readability";

	public static void main(String args[]) {
		try {
			new RIDIRECleaner_old(args);
		} catch (FailingHttpStatusCodeException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Options options;
	private String fileName;
	private String bookmark;
	private String host;
	private String encoding;
	private String alchemyKey;

	@SuppressWarnings("unchecked")
	public RIDIRECleaner_old(String[] args) throws FailingHttpStatusCodeException,
			MalformedURLException, IOException {
		this.createOptions();
		this.parseOptions(args);
		// first, use Readability
		boolean textExtracted = this.getTextWithReadability();
		if (!textExtracted) {
			this.getTextWithAlchemy();
		}
	}

	private void createOptions() {
		this.options = new Options();
		Option file = new Option("f", "file", true, "input file");
		this.options.addOption(file);
		Option bookmark = new Option("b", "bookmark", true,
				"bookmarks location");
		this.options.addOption(bookmark);
		Option host = new Option("h", "host", true, "host");
		this.options.addOption(host);
		Option encoding = new Option("e", "encoding", true, "encoding");
		this.options.addOption(encoding);
		Option alchemyKey = new Option("k", "key", true, "alchemy key");
		this.options.addOption(alchemyKey);
	}

	private void getTextWithAlchemy() {
		AlchemyAPI alchemyAPI = AlchemyAPI
				.GetInstanceFromString(this.alchemyKey);
		StringBuffer buffer = new StringBuffer();
		try {
			Document d = alchemyAPI.HTMLGetText(
					FileUtils.readFileToString(new File(this.fileName)),
					"http://dummy.it/");
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
		System.out.println(buffer.toString());
		System.err.println(RIDIRECleaner_old.ALCHEMY);
	}

	private boolean getTextWithReadability() throws IOException,
			MalformedURLException {
		LogFactory.getFactory().setAttribute("org.apache.commons.logging.Log",
				"org.apache.commons.logging.impl.NoOpLog");
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
		webClient.setThrowExceptionOnFailingStatusCode(false);
		webClient.setRefreshHandler(new ThreadedRefreshHandler());
		webClient.setThrowExceptionOnScriptError(false);
		ProxyConfig proxyConfig = new ProxyConfig("", 8056);
		proxyConfig.addHostsToProxyBypass("localhost.*");
		webClient.setProxyConfig(proxyConfig);
		HtmlPage htmlPage = webClient.getPage(this.host
				+ System.getProperty("file.separator") + "?filename="
				+ this.fileName + "&encoding=" + this.encoding);
		// System.out.println(htmlPage.asXml());
		// List<HtmlElement> scripts = htmlPage.getElementsByTagName("script");
		// for (HtmlElement script : scripts) {
		// script.setAttribute("src", "");
		// }
		String jsSrc = FileUtils.readFileToString(new File(this.bookmark));
		jsSrc = jsSrc.replaceAll("@@@HOST@@@", this.host);
		htmlPage.executeJavaScript(jsSrc);
		// System.out.println(htmlPage.asXml());
		List elements = htmlPage
				.getByXPath("//div[@id='readability-content']/div");
		HtmlElement element = null;
		if (elements != null && elements.size() > 0) {
			element = (HtmlElement) elements.get(0);
		}
		String ret = new String();
		boolean textExtracted = false;
		if (element != null) {
			ret = element.asText();
			if (ret != null && ret.trim().length() > 50) {
				textExtracted = true;
				System.out.println(ret);
				System.err.println(RIDIRECleaner_old.READABILITY);
			}
		}
		webClient.closeAllWindows();
		return textExtracted;
	}

	private void parseOptions(String[] args) {
		HelpFormatter formatter = new HelpFormatter();
		CommandLineParser parser = new GnuParser();
		CommandLine cmdline = null;
		try {
			// parse the command line arguments
			cmdline = parser.parse(this.options, args);
		} catch (ParseException exp) {
			// oops, something went wrong
			System.err.println("Parsing failed.  Reason: " + exp.getMessage());
			formatter.printHelp("RIDIRECleaner", this.options);
			System.exit(-1);
		}
		if (cmdline != null) {
			this.fileName = cmdline.getOptionValue("f");
			if (this.fileName == null) {
				System.err.println("No file provided.");
				formatter.printHelp("RIDIRECleaner", this.options);
				System.exit(-1);
			}
			this.bookmark = cmdline.getOptionValue("b");
			if (this.bookmark == null) {
				System.err.println("No bookmark script.");
				formatter.printHelp("RIDIRECleaner", this.options);
				System.exit(-1);
			}
			this.host = cmdline.getOptionValue("h");
			if (this.host == null) {
				System.err.println("No host.");
				formatter.printHelp("RIDIRECleaner", this.options);
				System.exit(-1);
			}
			this.encoding = cmdline.getOptionValue("e");
			if (this.encoding == null) {
				System.err.println("No encoding.");
				formatter.printHelp("RIDIRECleaner", this.options);
				System.exit(-1);
			}
			this.alchemyKey = cmdline.getOptionValue("k");
			if (this.alchemyKey == null) {
				System.err.println("No alchemyKey.");
				formatter.printHelp("RIDIRECleaner", this.options);
				System.exit(-1);
			}
		}
	}
}
