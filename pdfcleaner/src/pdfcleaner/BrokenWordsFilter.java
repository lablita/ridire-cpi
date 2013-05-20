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
