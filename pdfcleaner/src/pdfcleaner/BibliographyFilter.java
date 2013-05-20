package pdfcleaner;

public class BibliographyFilter extends Filter
{
    public BibliographyFilter(String txt)
    {
        super(txt);
    }

    int numWords, numLett, numPunct, numCapsLett, numCapsWord;
    double val;
    boolean[] bib = new boolean[lines.length];

    public void apply()
    {
        int i = 0;
        String s = readLine(i);
        while (s != null)
        {
            String[] arr = s.split(" ");
            numWords = arr.length;
            numPunct = count(s,"[\\.|\\,|\\;|\\(|\\)]");
            numCapsLett = count(s,"[A-Z]");
            numLett = s.length();
            numCapsWord = 0;
            for (int j = 0; j < arr.length; j++)
            {
                if (arr[j].matches("^[A-Z].*"))
                {
                    numCapsWord++;
                }
            }
            double d1 = ((double) numCapsWord) / numWords;
            double d2 = ((double) numPunct) / numWords;
            val = d1 + d2;
            if (val > 0.8)
            {
                bib[i] = true;
            }
            else
            {
                bib[i] = false;
            }
            //System.out.println(bib[i] + "\t" + s);
            i++;
            s = readLine(i);
        }
        normal();
        for (i = 0; i < lines.length; i++)
        {
            if (bib[i])
            {
                lines[i] = null;
            }
            //System.out.println(bib[i] + "\t" + lines[i]);
        }
    }

    private void normal()
    {
        if (lines.length < 2)
        {
            return;
        }
        bib[0] = false;
        bib[1] = false;
        int start = 0, end = 0;
        float dens = 0;
        for (int i = 0; i < bib.length; i++)
        {
            if (bib[i])
            {
                float x = calcDensity(i);
                if (x > dens)
                {
                    dens = x;
                    start = i;
                }
            }
        }
        if (dens > 0.65)
        {
            int i = start;
            while ((i-3) > 0 && (bib[i-1] || bib[i-2] || bib[i-3]))
            {
                i--;
            }
            while (!bib[i])
            {
                i++;
            }
            end = start;
            start = i;
            while ((i+3) < bib.length && (bib[i+1] || bib[i+2] || bib[i+3]))
            {
                i++;
            }
            while (!bib[i])
            {
                i--;
            }
            end = i;
            if ((bib.length - i) < 12)
            {
                end = bib.length - 1;
            }
        }
        else
        {
            start = bib.length;
        }
        for (int j = 0; j < bib.length; j++)
        {
            if (j < start)
            {
                bib[j] = false;
            }
            else if (j > end)
            {
                bib[j] = false;
            }
            else
            {
                bib[j] = true;
            }
        }
    }

    private float calcDensity(int x)
    {
        int[] val = new int[12];
        for (int i = 0; i < 6; i++)
        {
            if ((x+i) >= bib.length || (x-i) <= 0)
            {
                val[i] = 0;
                continue;
            }
            if (bib[x+i])
            {
                val[i] = 1;
            }
            else
            {
                val[i] = 0;
            }
            if (bib[x-i])
            {
                val[val.length-i-1] = 1;
            }
            else
            {
                val[val.length-i-1] = 0;
            }
        }
        float sum = 0;
        for (int i = 0; i < val.length; i++)
        {
            sum += val[i];
        }
        return (sum / val.length);
    }

    private int count(String x, String re)
    {
        int c = 0;
        for (int i = 1; i <= x.length(); i++)
        {
            if (x.substring(i-1,i).matches(re))
            {
                c++;
            }
        }
        return c;
    }

}
