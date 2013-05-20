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

import java.io.*;

public class HtmlColor
{
    public HtmlColor(String fileHtml, String fileTxt)
    {
        try
        {
            f = new File(fileHtml);
            fis = new FileInputStream(f);
            isr = new InputStreamReader(fis,"8859_1");
            br = new BufferedReader(isr);
            String x = readLine(br);
            while (x != null)
            {
                html = html + x + " ";
                //System.out.println(x);
                x = readLine(br);
            }

            f2 = new File(fileTxt);
            fis2 = new FileInputStream(f2);
            isr2 = new InputStreamReader(fis2);
            br2 = new BufferedReader(isr2);
            x = readLine(br2);
            while (x != null)
            {
                txt = txt + x + " ";
                //System.out.println(x);
                x = readLine(br2);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        html = html.replaceAll("\\s+", " ").trim();
        txt = txt.replaceAll("\\s+", " ").trim();
        parseText();
        //System.out.println(html);
    }

    String html = "";
    String txt = "";
    String html2 = "";
    File f;
    FileInputStream fis;
    InputStreamReader isr;
    BufferedReader br;
    File f2;
    FileInputStream fis2;
    InputStreamReader isr2;
    BufferedReader br2;
    int first = -1, last = -1;

    public void parseText()
    {
        String[] arr = txt.split(" ");
        for (int i = 0; i < arr.length; i += 3)
        {
            if (i+2 >= arr.length)
                return;

            if (bestMatching(arr[i],arr[i+1],arr[i+2]))
            {
                htmlTag();
            }
        }
    }

    private void htmlTag()
    {
        html = html.substring(0,first) + "<font color=\"red\">" + html.substring(first,last) + "</font>" + html.substring(last);
    }

    private boolean bestMatching(String a1, String a2, String a3)
    {
        int i = html.indexOf(a1 + " " + a2 + " " + a3);
        if (i != -1)
        {
            while (i < last)
            {
                int j = html.indexOf(a1 + " " + a2 + " " + a3, i+1);
                if (j == -1)
                {
                    break;
                }
                i = j;
            }

            first = i;
            last = i + a1.length() + a2.length() + a3.length() + 2;
            return true;
        }
        else
        {
            i = html.indexOf(a1 + " " + a2);
            if (i != -1)
            {
                while (i < last)
                {
                    int j = html.indexOf(a1 + " " + a2, i+1);
                    if (j == -1)
                    {
                        break;
                    }
                    i = j;
                }
                first = i;
                last = i + a1.length() + a2.length() + 1;
                return true;
            }
            else
            {
                i = html.indexOf(a2 + " " + a3);
                if (i != -1)
                {
                    while (i < last)
                    {
                        int j = html.indexOf(a2 + " " + a3, i+1);
                        if (j == -1)
                        {
                            break;
                        }
                        i = j;
                    }
                    first = i;
                    last = i + a2.length() + a3.length() + 1;
                    return true;
                }
            }
        }
        return false;
    }

    private void tagBest2(String x)
    {
        x = x.trim();
        String[] arr = x.split(" ");
        if (arr.length < 2)
        {
            return;
        }
        for (int i = 0; i < arr.length; i+=3)
        {

        }


        if (arr.length > 4)
        {
            String ts = x.substring(0,x.indexOf(arr[3])).trim();
            int p = html.indexOf(ts);
            if (p > -1)
            {
                html = html.substring(0,p) + "<font color=\"red\">" + x + "</font>" + html.substring(p+x.length());
                
            }
            tagBest2(x.substring(4));
        }
        else
        {
            int p = html.indexOf(x);
            if (p > -1)
            {
                html = html.substring(0,p) + "<font color=\"red\">" + x + "</font>" + html.substring(p+x.length());
            }
        }

    }

    private boolean isRed(String x)
    {
        int i = html.indexOf(x);
        String g = html.substring(0,i);
        int j = g.lastIndexOf("<font color");
        if (j == -1)
            return false;
        int k = g.lastIndexOf("</font>");
        if (k > j)
        {
            return false;
        }
        return true;
    }

    private void tagBest(String x)
    {
        
        if (x == null)
            return;
        int inc = 1;
        int i = 0;
        String left = "", right = "";

        while (html.indexOf(left) == -1 && html.indexOf(right) == -1)
        {
            if (i > 0)
            {
                left = getSubstr(x,i);
                 i = i-inc;
            }
            else
            {
                right = getSubstr(x,i);
                i = i+inc;
            }
            inc++;
        }

        
        String g = getSubstr(x,i);

        if (g.split(" ").length > 2)
        {
            i = html.indexOf(g);
            if (i != -1)
                html = html.substring(0,i) + "<font color=\"red\">" + x + "</font>" + html.substring(i+x.length());
        }
        g = getSubstr(x,-i);
        if (i > 2 && i < x.length() && g.length() > 2)
        {
            tagBest(x.substring(0,x.indexOf(g)));
        }
        else if (i < -2 && g.length() > 2)
        {
            tagBest(x.substring(x.indexOf(g) + g.length()));
        }
    }

    private String getSubstr(String s, int x)
    {
        if (x > 0)
        {
            return s.substring(x);
        }
        return s.substring(0, s.length() + x - 1);
    }


    public String readLine(BufferedReader re)
    {
        try
        {
            String x = re.readLine();
            if (x != null)
            {
                return x;
            }
            else
            {
                re.close();
                return null;
            }
        }
        catch (Exception e){}
        return null;
    }
}
