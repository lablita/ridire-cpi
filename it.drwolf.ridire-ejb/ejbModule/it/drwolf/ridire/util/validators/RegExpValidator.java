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
package it.drwolf.ridire.util.validators;


import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.ValidatorException;

import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.faces.Validator;
import org.jboss.seam.annotations.intercept.BypassInterceptors;

@Name("regExpValidator")
@BypassInterceptors
@Validator
public class RegExpValidator implements javax.faces.validator.Validator {


        public void validate(FacesContext context, UIComponent component,
                        Object value) throws ValidatorException {

                if (null == value || 0 == value.toString().length()) {
                        return;
                }
                
                String regExpr = new String(value.toString());
				try {
					Pattern.compile(regExpr);
				} catch (PatternSyntaxException pse) {
                    FacesMessage message = new FacesMessage();
                    message.setDetail(regExpr);
                    message.setSummary(regExpr);
                    message.setSeverity(FacesMessage.SEVERITY_ERROR);						
//					pse.printStackTrace();
					throw new ValidatorException(message);
				}

        }
}
