import java.util.ArrayList;
import java.util.HashMap;

public class PluginBibTeXPath extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"BibTeXPath");
            put("author"            ,"Christian Saemann");
            put("version"           ,"1.0");
            put("help"              ,"This plugin is an export plugin and dumps the BibTeX field of a document plus path");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex");
            put("type"              ,"export");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };

    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public celsius.MProperties Information;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.MProperties i, ArrayList<String> m) {
        Information = i;
        Msgs = m;
    }


    public void run() {
        String bib = Information.get("bibtex");
        if (bib == null) {
            Information.put("output", "");
            return;
        }
        celsius.BibTeXRecord BTR=new celsius.BibTeXRecord(bib);
        BTR.put("fullpath", Information.getS("fullpath"));
        Information.put("output", BTR.toString()+"\n\n");
    }
}
