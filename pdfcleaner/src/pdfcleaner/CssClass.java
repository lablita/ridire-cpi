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
package pdfcleaner;

import java.util.LinkedList;
import java.util.List;

public class CssClass
{
    String font_size;
    String font_family;
    int num_words = 0;
    List<String> class_names;

    public CssClass(String name, String size, String family)
    {
        class_names = new LinkedList<String>();
        font_size = size;
        font_family = family;
        class_names.add(name);
    }

    public boolean equals(CssClass c)
    {
        if (font_size.equals(c.font_size) && font_family.equals(c.font_family))
            return true;
        return false;
    }

    public void addName(String n)
    {
        class_names.add(n);
    }

    public void addNumWords(int n)
    {
        num_words += n;
    }
}
