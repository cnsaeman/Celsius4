import java.util.ArrayList;
import java.util.HashMap;
import celsius.data.*;

public class PluginBibTeXPath extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"BibTeXPath");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin is an export plugin and dumps the BibTeX field of a document plus path");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex|attachments");
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
    
    public String getPath(Item item) {
        String path=item.linkedAttachments.get(0).get("path");
        path=path.replace("LD::","/home/cnsaeman/Celsius4/Libraries/MathsPhys/");
        return(path);
    }


    public void run() {
        String bib = item.get("bibtex");
        if (bib == null) {
            communication.put("output", "");
            return;
        }
        celsius.data.BibTeXRecord BTR=new celsius.data.BibTeXRecord(bib);
        if (item.linkedAttachments.size()>0) BTR.put("fullpath", getPath(item));
        communication.put("output", BTR.toString()+"\n\n");
    }
}
