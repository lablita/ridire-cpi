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
// Copyright © - ANR Textométrie - http://textometrie.ens-lsh.fr
//
// This file is part of the TXM platform.
//
// The TXM platform is free software: you can redistribute it and/or modif y
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// The TXM platform is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with the TXM platform.  If not, see <http://www.gnu.org/licenses/>.
// 
// 
// 
// $LastChangedDate: 2011-11-23 10:53:49 +0100 (mer, 23 nov 2011) $
// $LastChangedRevision: 2063 $
// $LastChangedBy: mdecorde $ 
//
package it.drwolf.ridire.index.cwb.exception;

// TODO: Auto-generated Javadoc
/**
 * Base class for the Exception raised by the CQi client itself (i.e. not
 * recieved from the CQi server)
 * 
 * @author Jean-Philippe Magué
 */
public class CqiClientException extends Exception {

	/** The Constant serialVersionUID. */
	private static final long serialVersionUID = 1L;

	/**
	 * Instantiates a new cqi client exception.
	 */
	public CqiClientException() {
		super();
	}

	/**
	 * Instantiates a new cqi client exception.
	 * 
	 * @param message
	 *            the message
	 */
	public CqiClientException(String message) {
		super(message);
	}

	/**
	 * Instantiates a new cqi client exception.
	 * 
	 * @param message
	 *            the message
	 * @param cause
	 *            the cause
	 */
	public CqiClientException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * Instantiates a new cqi client exception.
	 * 
	 * @param cause
	 *            the cause
	 */
	public CqiClientException(Throwable cause) {
		super(cause);
	}

}
