import java.util.ArrayList;
import java.util.HashMap;
import celsius.data.*;

public class PluginBibTeX extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"BibTeX");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin is an export plugin and dumps the BibTeX field of a document");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex");
            put("type"              ,"export");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };

    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public celsius.data.Item item;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        item = (Item)tr;
        communication=com;
        Msgs = m;
    }


    public void run() {
        String bib = item.get("bibtex");
        if (bib == null) {
            communication.put("output", "");
            return;
        }
        communication.put("output", bib+"\n\n");
    }
}
