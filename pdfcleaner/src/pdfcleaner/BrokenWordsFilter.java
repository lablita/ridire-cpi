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

public class BrokenWordsFilter extends Filter
{
    public BrokenWordsFilter(String txt)
    {
        super(txt);
    }

    @Override
    public void apply()
    {
        int i = 0;
        String s = readLine(i);
        String t;
        while (s != null)
        {
            t = readLine(i+1);
            if (t != null)
            {
                s = s.trim();
                if (s.matches(".*[A-Za-z]-$"))
                {
                    s = s.substring(0,s.length()-1);
                    lines[i+1] = s + t.trim();
                    lines[i] = "";
                }
            }
            i++;
            s = readLine(i);
        }
    }

}
