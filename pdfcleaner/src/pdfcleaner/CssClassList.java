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
