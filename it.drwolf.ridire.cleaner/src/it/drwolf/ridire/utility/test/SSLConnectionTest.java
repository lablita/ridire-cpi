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
