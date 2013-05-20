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
