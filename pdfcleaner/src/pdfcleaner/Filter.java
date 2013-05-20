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

public abstract class Filter
{
    public Filter(String x)
    {
        lines = x.split("\n");
    }

    String[] lines;

    public String readLine(int i)
    {
        if (i < 0 || i >= lines.length)
        {
            return null;
        }
        return lines[i];
    }

    public String toString()
    {
        String x = "";
        for (int i = 0; i < lines.length; i++)
        {
            if (lines[i] != null && lines[i].length() > 0)
            {
                x = x + lines[i] + "\n";
            }
        }
        return x;
    }

    protected abstract void apply();
}
