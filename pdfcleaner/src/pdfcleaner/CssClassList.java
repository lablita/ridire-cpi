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

import java.util.ArrayList;
import java.util.List;

public class CssClassList
{
    List<CssClass> classes;

    public CssClassList()
    {
        classes = new ArrayList();
    }

    public CssClass add(String name, String font_s, String font_f)
    {
        CssClass c = search(font_s, font_f);
        CssClass x;
        if (c == null)
        {
            x = new CssClass(name,font_s,font_f);
            classes.add(x);
        }
        else
        {
            c.addName(name);
            x = c;
        }
        return x;
    }

    public CssClass search(String name)
    {
        for (int i = 0; i < classes.size(); i++)
        {
            if (classes.get(i).class_names.contains(name))
            {
                return classes.get(i);
            }
        }
        return null;
    }

    public CssClass search(String font_s, String font_f)
    {
        for (int i = 0; i < classes.size(); i++)
        {
            if (classes.get(i).font_family.equals(font_f) && classes.get(i).font_size.equals(font_s))
            {
                return classes.get(i);
            }
        }
        return null;
    }
    public CssClass search(CssClass c)
    {
        for (int i = 0; i < classes.size(); i++)
        {
            if (classes.get(i).equals(c))
            {
                return classes.get(i);
            }
        }
        return null;
    }
    public void print()
    {
        for (int i = 0; i < classes.size(); i++)
        {
            System.out.println(" - - - Classe - - -");
            System.out.println("family: " + classes.get(i).font_family);
            System.out.println("size:   " + classes.get(i).font_size);
            for (int j = 0; j < classes.get(i).class_names.size(); j++)
            {
                System.out.print(classes.get(i).class_names.get(j) + ", ");
            }
            System.out.println("\n");
        }
    }
}
