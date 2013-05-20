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

public class HtmlParser
{
    public HtmlParser(String fname, CssClassList cli, WordList wli, int _mwl, int _Msl)
    {
        cl = cli;
        wl = wli;
        mwl = _mwl;
        Msl = _Msl;
        try
        {
            f = new File(fname);
            fis = new FileInputStream(f);
            isr = new InputStreamReader(fis,"8859_1");
            br = new BufferedReader(isr);
        }
        catch (Exception e){}
        readAll();
    }

    CssClassList cl;
    WordList wl;
    String text = "";
    File f;
    FileInputStream fis;
    InputStreamReader isr;
    BufferedReader br;
    int mwl, Msl;

    public void setLimits(int minWordLength, int maxShortLines)
    {
        mwl = minWordLength;
        Msl = maxShortLines;
    }

    public CssClass getBestClass()
    {
        CssClass best = cl.classes.get(0);
        for (int i = 0; i < cl.classes.size(); i++)
        {
            int tmp = wl.getNumWordsOfClass(cl.classes.get(i));
            if (tmp > wl.getNumWordsOfClass(best))
            {
                best = cl.classes.get(i);
            }
        }
        return best;
    }

    private void readText()
    {
        String testo, prima, classe;
        int ind = 0;
        int x = 0, y = 0;
        int block = 0;
        while (ind < text.length()-15)
        {
            x = text.indexOf(">", ind);
            y = text.indexOf("<",ind);
            //System.out.println(x + " - " + y);
            if (x < 0 || y < 0)
                return;
            if ((y-x) == 1 || text.substring(x, y).trim().length() == 0)
            {
            }
            else
            {
                if (mwl != 0 || Msl != 0) //filtro sulla lunghezza
                {
                    if (text.substring(x, y).trim().length() < mwl)
                    {
                        block++;
                    }
                    else
                    {
                        block = 0;
                    }
                    if (block == Msl)
                    {
                        for (int j = 1; j < Msl; j++)
                        {
                            //System.out.println("REMOVING.............." + wl.words.get(wl.words.size()-j-1).w);
                            if (wl.words.size()-j-1 >= 0)
                                wl.words.remove(wl.words.size()-j-1);
                        }
                    }
                    if (block >= Msl)
                    {
                        ind = y+1;
                        continue;
                    }
                }
                testo = text.substring(x+1,y);
                prima = text.substring(0, x);
                int cli = prima.lastIndexOf("class=\"ft");
                prima = prima.substring(cli+9);
                prima = prima.substring(0,prima.indexOf("\""));
                classe = "ft" + prima;
                //System.out.println(classe);
                CssClass cla = cl.search(classe);
                String[] spl = testo.split(" ");
                for (int i = 0; i < spl.length-1; i++)
                {
                    wl.addWord(cla, spl[i].trim());
                }
                if (spl.length > 0)
                    wl.addWord(cla, spl[spl.length-1].trim() + "\n");
            }
            ind = y+1;
        }
        //wl.print();
    }

    private void readAll()
    {
        int i = 0;
        String x = "ciao";
        while (x != null)
        {
            
            while (x != null && !x.matches(".*\\.ft[0-9]+\\{.*\\}"))
            {
                //System.out.println("1 --> " + x);
                x = readLine();
            }
            if (x == null)
            {
                break;
            }
            while (x.matches(".*\\.ft[0-9]+\\{.*\\}"))
            {
                //System.out.println("2 --> " + x);
                addClass(x);
                x = readLine();
            }
            while (x != null && !x.matches(".*\\.ft[0-9]+\\{.*\\}"))
            {
                //System.out.println("3 --> " + x);
                text = text + x;
                x = readLine();
            }
            //x = readLine();
            
        }
        //cl.print();
        readText();
    }
    private void addClass(String x)
    {
        String size, family, name;
        int i = x.indexOf("font-size");
        i += 10;
        int j = i+1;
        while (x.charAt(j) != ';')
        {
            j++;
        }
        size = x.substring(i,j);
        i = x.indexOf("font-family");
        i += 12;
        j = i+1;
        while (x.charAt(j) != ';')
        {
            j++;
        }
        family = x.substring(i,j);
        i = x.indexOf(".");
        i += 1;
        j = i+1;
        while (x.charAt(j) != '{')
        {
            j++;
        }
        name = x.substring(i,j);
        cl.add(name, size, family);
        //System.out.println("Class... " + name + " - " + size + " - " + family);
    }

    public String readLine()
    {
        try
        {
            String x = br.readLine();
            if (x != null)
            {
                /*String asString = new String(x.getBytes(), "ISO8859_1");
                byte[] newBytes = asString.getBytes("UTF8");
                return new String(newBytes);*/
                return x;
            }
            else
            {
                br.close();
                return null;
            }
        }
        catch (Exception e){}
        return null;
    }

}
