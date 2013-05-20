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
