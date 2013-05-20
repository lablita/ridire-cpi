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
package it.drwolf.ridire.servlet;

import java.io.File;
import java.io.IOException;

import javax.faces.context.ExternalContext;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;

@Name("tmpResource")
public class TmpResourcesServlet {
	@In(value = "#{facesContext.externalContext}")
	public ExternalContext extCtx;

	public void getTmpFile(String filename) {
		HttpServletResponse response = (HttpServletResponse) this.extCtx
				.getResponse();
		response.setContentType("text/html");
		response.setCharacterEncoding("UTF-8");
		response.setHeader("Expires", "0");
		response.setHeader("Cache-Control",
				"must-revalidate, post-check=0, pre-check=0");
		response.setHeader("Pragma", "public");
		try {
			String ret = FileUtils.readFileToString(
					new File(System.getProperty("java.io.tmpdir")
							+ System.getProperty("file.separator") + filename),
					"UTF-8");
			response.getOutputStream().print(ret);
			response.getOutputStream().flush();
			// FacesContext.getCurrentInstance().responseComplete();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
