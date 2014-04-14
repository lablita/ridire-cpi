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
package it.drwolf.ridire.filters;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.contexts.Lifecycle;
import org.jboss.seam.web.AbstractFilter;

@Filter(around = { "org.jboss.seam.web.ajax4jsfFilter" })
@BypassInterceptors
@Startup
@Scope(ScopeType.APPLICATION)
@Name("tmpResourcesFilter")
public class TmpResourcesFilter extends AbstractFilter {

	private static final String LF_DIR = "/home/drwolf/lf/";

	@SuppressWarnings("unchecked")
	public void doFilter(ServletRequest req, ServletResponse resp,
			FilterChain arg2) throws IOException, ServletException {
		String filename = req.getParameter("filename");
		String encoding = req.getParameter("encoding");
		String tempDir = System.getProperty("java.io.tmpdir");
		String freqList = req.getParameter("freqList");
		String tsvList = req.getParameter("tsvList");
		if (filename != null && encoding != null
				&& filename.indexOf(tempDir) != -1) {
			try {
				Lifecycle.beginCall();
				if (resp instanceof HttpServletResponse) {
					HttpServletResponse response = (HttpServletResponse) resp;
					response.setContentType("text/html");
					response.setCharacterEncoding(encoding);
					response.setHeader("Expires", "0");
					response.setHeader("Date", new Date().toString());
					response.setHeader("Cache-Control",
							"must-revalidate, post-check=0, pre-check=0");
					response.setHeader("Pragma", "public");
					File file = new File(filename);
					if (file.exists() && file.canRead()) {
						String ret = FileUtils.readFileToString(file, encoding);
						response.getWriter().write(ret);
						response.getWriter().flush();
						response.getWriter().close();
					}
					// FacesContext.getCurrentInstance().responseComplete();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Lifecycle.endCall();
			}
		} else if (freqList != null && freqList.trim().length() > 0
				|| tsvList != null && tsvList.trim().length() > 0) {
			try {
				Lifecycle.beginCall();
				String contentType = "application/vnd.oasis.opendocument.spreadsheet";
				File file = null;
				if (freqList != null) {
					file = new File(TmpResourcesFilter.LF_DIR + freqList
							+ ".ods");
					if (freqList.equals("RIDIRE_LF")) {
						file = new File(TmpResourcesFilter.LF_DIR + freqList
								+ ".zip");
						contentType = "application/zip";
					}
				} else {
					String zipFileName = tsvList.replaceAll("\\s\\(TSV\\)", "");
					file = new File(TmpResourcesFilter.LF_DIR + zipFileName
							+ ".zip");
					contentType = "application/zip";
				}
				if (file.exists() && file.canRead()) {
					if (resp instanceof HttpServletResponse) {
						HttpServletResponse response = (HttpServletResponse) resp;
						response.setContentType(contentType);
						response.setHeader("Expires", "0");
						if (freqList != null) {
							if (freqList.equals("RIDIRE_LF")) {
								response.addHeader("Content-disposition",
										"attachment; filename=\"" + freqList
												+ ".zip\"");
							} else {
								response.addHeader("Content-disposition",
										"attachment; filename=\"" + freqList
												+ ".ods\"");
							}
						} else if (tsvList != null) {
							response.addHeader("Content-disposition",
									"attachment; filename=\"" + tsvList
											+ ".zip\"");
						}
						response.setHeader("Date", new Date().toString());
						response.setHeader("Cache-Control",
								"must-revalidate, post-check=0, pre-check=0");
						response.setHeader("Pragma", "public");
						byte[] lf = FileUtils.readFileToByteArray(file);
						response.getOutputStream().write(lf);
						response.getOutputStream().flush();
						response.getOutputStream().close();
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				Lifecycle.endCall();
			}
		} else {
			Map<String, String[]> pMap = req.getParameterMap();
			final Map<String, String[]> additionalParams = new HashMap<String, String[]>();
			for (String key : pMap.keySet()) {
				// if (key.equals("forma") || key.equals("lemma")
				// || key.equals("pos") || key.equals("phrase")
				// || key.startsWith("pattern")) {
				String[] values = pMap.get(key);
				if (values != null && values.length == 1) {
					byte[] bytes = values[0].getBytes("ISO-8859-1");
					additionalParams.put(key, new String[] { new String(bytes,
							"UTF-8") });
				}
				// }
			}
			HttpServletRequest httpServletRequest = new PrettyFacesWrappedRequest(
					(HttpServletRequest) req, additionalParams);
			arg2.doFilter(httpServletRequest, resp);
		}
	}
}
