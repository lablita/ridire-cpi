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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class WordList
{
    List<StyledWord> words;

    public WordList()
    {
        words = new LinkedList<StyledWord>();
    }

    public void addWord(CssClass c, String w)
    {
        words.add(new StyledWord(c,w));
    }

    public int getNumWordsOfClass(CssClass c)
    {
        int count = 0;
        Iterator it = words.iterator();
        while (it.hasNext())
        {
            StyledWord s = (StyledWord) it.next();
            if (s.c.equals(c))
            {
                count++;
            }
        }
        return count;
    }
    public void print()
    {
        Iterator it = words.iterator();
        while (it.hasNext())
        {
            StyledWord s = (StyledWord) it.next();
            System.out.println("word: " + s.w + " - " + s.c.font_size);
        }
    }
    public void printOnly(CssClass c)
    {
        Iterator it = words.iterator();
        while (it.hasNext())
        {
            StyledWord s = (StyledWord) it.next();
            if (s.c.equals(c))
            {
                System.out.print(s.w + " ");
            }
        }
    }
    public String toString()
    {
        String x = "";
        Iterator it = words.iterator();
        while (it.hasNext())
        {
            StyledWord s = (StyledWord) it.next();
            x = x + s.w + " ";
        }
        return x.trim();
    }
    public String toString(CssClass c)
    {
        String x = "";
        Iterator it = words.iterator();
        while (it.hasNext())
        {
            StyledWord s = (StyledWord) it.next();
            if (s.c.equals(c))
            {
                x = x + s.w + " ";
            }
        }
        return x.trim();
    }
}
