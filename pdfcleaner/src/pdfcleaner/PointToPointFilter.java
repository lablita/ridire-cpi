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

public class PointToPointFilter extends Filter
{
    public PointToPointFilter(String txt, int minWordAvg)
    {
        super(txt);
        minWord = minWordAvg;
    }

    int minWord;
    String[] term = {".","!","?"};

    @Override
    public void apply()
    {
        String f = "";
        String tmp;
        int i = 0;
        String s = readLine(i);
        int back = 0;
        float media;
        while (s != null)
        {
            if (isTerminated(s))
            {
                media = 0;
                for (int j = 0; j <= back; j++)
                {
                    media += lines[i-j].trim().split(" ").length;
                }
                media = media / (back+1);
                if (media > minWord)
                {
                    while (back >= 0)
                    {
                        f = f + readLine(i-back) + "\n";
                        back--;
                    }
                }
                back = 0;
            }
            else
            {
                back++;
            }
            i++;
            s = readLine(i);
        }
        lines = f.split("\n");
    }

    private boolean isTerminated(String x)
    {
        x = x.trim();
        for (int i = 0; i < term.length; i++)
        {
            if (x.endsWith(term[i]))
            {
                return true;
            }
        }
        return false;
    }
}
