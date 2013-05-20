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
package it.drwolf.ridire.util.validators;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("slashValidator")
@BypassInterceptors
@Validator
public class SlashValidator implements javax.faces.validator.Validator {

	public void validate(FacesContext context, UIComponent component,
			Object value) throws ValidatorException {
		Pattern p = Pattern.compile("[^a-zA-Z0-9 ]+");

		if (context == null || component == null) {
			throw new NullPointerException();
		}
		if (null == value || 0 == value.toString().trim().length()) {
			return;
		}

		String str = value.toString().trim();

		/* Check for non letters or numbers chars */
		if (str.contains("/")) {
			FacesMessage message = new FacesMessage();
			message.setDetail("Non è possibile usare la slash ('/') per i nomi di job");
			message.setSummary("/ exception");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}
		Matcher m = p.matcher(str);
		if (m.find()) {
			FacesMessage message = new FacesMessage();
			message.setDetail("Per favore, usa soltanto lettere non accentate, numeri e spazi per i nomi di job.");
			message.setSummary("Invalid chars exception");
			message.setSeverity(FacesMessage.SEVERITY_ERROR);
			throw new ValidatorException(message);
		}
	}
}
