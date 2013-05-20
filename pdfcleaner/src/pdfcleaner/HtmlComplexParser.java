package pdfcleaner;

import java.io.File;


public class HtmlComplexParser
{
    public HtmlComplexParser(String fname, String dir, int minLen, int maxLines)
    {
        cl = new CssClassList();
        wl = new WordList();
        mwl = minLen;
        Msl = maxLines;
        parseFiles(fname, dir);
    }
    public HtmlComplexParser(String fname, String dir)
    {
        cl = new CssClassList();
        wl = new WordList();
        mwl = 0;
        Msl = 0;
        parseFiles(fname, dir);
    }
    public HtmlComplexParser(String fpath)
    {
        cl = new CssClassList();
        wl = new WordList();
        mwl = 0;
        Msl = 0;
        parseFile(fpath);
    }
    public HtmlComplexParser(String fpath, int minLen, int maxLines)
    {
        cl = new CssClassList();
        wl = new WordList();
        mwl = minLen;
        Msl = maxLines;
        parseFile(fpath);
    }

    CssClassList cl;
    WordList wl;
    String text = "";
    int mwl, Msl;

    private void parseFile(String fname)
    {
        //System.out.println(fname);
        File f = new File(fname);
        HtmlParser h = new HtmlParser(f.getAbsolutePath(), cl, wl, mwl, Msl);
        //wl.print();
    }
    private void parseFiles(String fname, String dir)
    {
        int i = 1;
        String fi = fname.substring(0, fname.length()-5) + "-" + i + ".html";
        //System.out.println(fi);
        File f = new File(dir + File.separator + fi);

        while (f.exists())
        {
            //System.out.println(f.getAbsolutePath());
            
            HtmlParser h = new HtmlParser(f.getAbsolutePath(), cl, wl, mwl, Msl);
            //h.setLimits(mwl, Msl);
            i++;
            fi = fname.substring(0, fname.length()-5) + "-" + i + ".html";
            f = new File(dir + File.separator + fi);
        }
        //wl.print();
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
    }
}
