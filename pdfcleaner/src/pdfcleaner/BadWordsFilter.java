package pdfcleaner;

public class BadWordsFilter extends Filter
{
    public BadWordsFilter(String txt)
    {
        super(txt);
    }

    public void apply()
    {
        int i = 0;
        String s = readLine(i);
        while (s != null)
        {
            if (s.matches(".*[\\.|_]{5,}.*"))
            {
                lines[i] = "";
            }
            i++;
            s = readLine(i);
        }
    }

}
