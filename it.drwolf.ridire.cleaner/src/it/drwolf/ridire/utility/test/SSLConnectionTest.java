package it.drwolf.ridire.utility.test;

import it.drwolf.ridire.session.ssl.EasySSLProtocolSocketFactory;

import java.io.IOException;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.NameValuePair;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.protocol.Protocol;

public class SSLConnectionTest {
	public static void main(String[] args) {
		new SSLConnectionTest();
	}

	private HttpClient httpClient;

	public SSLConnectionTest() {
		Protocol.registerProtocol("https", new Protocol("https",
				new EasySSLProtocolSocketFactory(), 8443));
		this.httpClient = new HttpClient();
		// this.httpClient.getParams().setAuthenticationPreemptive(true);
		Credentials defaultcreds = new UsernamePasswordCredentials("admin",
				"admin");
		this.httpClient.getState().setCredentials(
				new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT,
						AuthScope.ANY_REALM), defaultcreds);
		PostMethod method = new PostMethod("https://localhost:8443/engine");
		method.addParameter(new NameValuePair("action", "rescan"));
		try {
			int status = this.httpClient.executeMethod(method);
			Header redirectLocation = method.getResponseHeader("location");
			String loc = redirectLocation.getValue();
			GetMethod getmethod = new GetMethod("https://localhost:8443/engine");
			status = this.httpClient.executeMethod(getmethod);
			System.out.println(status);
		} catch (HttpException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			method.releaseConnection();
		}
	}
}
