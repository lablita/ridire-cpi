package pdfcleaner;

public class SingleNumberFilter extends Filter
{
    public SingleNumberFilter(String txt)
    {
        super(txt);
    }

    public void apply()
    {
        int i = 0;
        String s = readLine(i);

        while (s != null)
        {
            s = s.trim();
            if (s.matches("^[0-9]+$"))
            {
                lines[i] = "";
            }
            i++;
            s = readLine(i);
        }
    }

}
