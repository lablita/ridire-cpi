package pdfcleaner;

public class StyledWord
{
    public StyledWord(CssClass css, String word)
    {
        w = word;
        c = css;
    }
    CssClass c;
    String w;
}
