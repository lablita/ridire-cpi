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

/*
 * PrettyFaces is an OpenSource JSF library to create bookmarkable URLs.
 * Copyright (C) 2010 - Lincoln Baxter, III <lincoln@ocpsoft.com> This program
 * is free software: you can redistribute it and/or modify it under the terms of
 * the GNU Lesser General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version. This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details. You should have received a copy of the GNU Lesser General
 * Public License along with this program. If not, see the file COPYING.LESSER
 * or visit the GNU website at <http://www.gnu.org/licenses/>.
 */
import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

public class PrettyFacesWrappedRequest extends HttpServletRequestWrapper {
	private final Map<String, String[]> modifiableParameters;
	private Map<String, String[]> allParameters = null;

	/**
	 * Create a new request wrapper that will merge additional parameters into
	 * the request object without prematurely reading parameters from the
	 * original request.
	 * 
	 * @param request
	 * @param additionalParams
	 */
	public PrettyFacesWrappedRequest(final HttpServletRequest request,
			final Map<String, String[]> additionalParams) {
		super(request);
		this.modifiableParameters = new TreeMap<String, String[]>();
		this.modifiableParameters.putAll(additionalParams);
	}

	@Override
	public String getParameter(final String name) {
		String[] strings = this.getParameterMap().get(name);
		if (strings != null) {
			return strings[0];
		}
		return null;
	}

	@Override
	public Map<String, String[]> getParameterMap() {
		if (this.allParameters == null) {
			this.allParameters = new TreeMap<String, String[]>();
			this.allParameters.putAll(super.getParameterMap());
			this.allParameters.putAll(this.modifiableParameters);
		}
		// Return an unmodifiable collection because we need to uphold the
		// interface contract.
		return Collections.unmodifiableMap(this.allParameters);
	}

	@Override
	public Enumeration<String> getParameterNames() {
		return Collections.enumeration(this.getParameterMap().keySet());
	}

	@Override
	public String[] getParameterValues(final String name) {
		return this.getParameterMap().get(name);
	}
}
