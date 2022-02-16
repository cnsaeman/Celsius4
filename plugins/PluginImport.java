import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import celsius.data.*;
import celsius.components.bibliography.BibTeXRecord;
import atlantis.tools.*;

/**
 * @author cnsaeman
 * 
 * To include:
 * http://api.semanticscholar.org/v1/paper/6162a894c2481599a92e4adaa06d08c200d8c31e
 * 
 */
public class PluginImport extends Thread {
    
    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Basic Import");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin is applied to items imported from files as, e.g., BibTeX files.");
            put("needsFirstPage"    ,"no");
            put("wouldLikeFirstPage","yes");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"");
            put("type"              ,"import");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };
        
    private final String TI="P:Imp>";
    
    public Item item;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        item = (Item)tr;
        communication=com;
        Msgs = m;
    }
        
    public void run() {
        BibTeXRecord BTR=new BibTeXRecord(item.get("bibtex"));
        item.put("citation-tag",BTR.getTag());
        
        String title=BTR.get("title");
        if (title.startsWith("{")) title=title.substring(1);
        if (title.endsWith("}")) title=title.substring(0,title.length()-1);
        item.put("title",title);
        
        item.put("authors",BibTeXRecord.convertBibTeXAuthorsToCelsius(BTR.get("author")));
        String type=BTR.type.toLowerCase();
        if (BTR.type.equals("phdthesis") || BTR.type.equals("mastersthesis")) {
            item.put("type","Thesis");
        } else if (BTR.type.equals("book")) {
            item.put("type","Book");
        } else if (BTR.type.equals("article")) {
            if (BTR.isEmpty("journal")) {
                item.put("type","Preprint");
            } else {
                item.put("type","Paper");
            }
        } else {
            item.put("type","Other");
        }
        if (!BTR.isEmpty("doi")) {
            item.put("doi",BTR.get("doi"));
        }
        if (!BTR.isEmpty("eprint")) {
            item.put("arxiv-ref",BTR.get("eprint"));
            if (!BTR.isEmpty("primaryclass")) {
                item.put("arxiv-name",BTR.get("primaryclass"));
            } else {
                item.put("arxiv-name",Parser.cutUntil(BTR.get("eprint"),"/"));
            }
        }
        
        // identifier
        String tmp="";
        
        String arxref=item.get("arxiv-ref");
        String arxname=item.get("arxiv-name");
        if ((arxref!=null) && (arxname!=null)) {
            if (arxref.indexOf(arxname)>-1) {
                tmp=arxref;
            } else {
                tmp=arxref+" ["+arxname+"]";
            }
        }
        if (!BTR.isEmpty("journal")) {
            String identifier=new String("");
            identifier=BTR.get("journal");
            if (!BTR.isEmpty("volume")) identifier+=" "+BTR.get("volume");
            if (!BTR.isEmpty("year")) identifier+=" ("+BTR.get("year")+")";
            if (!BTR.isEmpty("pages")) identifier+=" "+BTR.get("pages");
            tmp+=" "+identifier.trim();
        }
        
        if (BTR.get("year")!=null) tmp=BTR.get("year")+" "+tmp;
        item.put("identifier", tmp.trim());
    }
    
}

