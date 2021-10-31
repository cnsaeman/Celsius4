/*
 * PluginCreateBibTeX.java
 *
 * v4.0
 *
 */

import java.util.ArrayList;
import java.util.HashMap;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import celsius.tools.ToolBox;
import celsius.data.*;


/**
 * @author cnsaeman
 */
public class PluginCreateBibTeX extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Create BibTeX");
            put("author"            ,"Christian Saemann");
            put("version"           ,"1.0");
            put("help"              ,"This plugin automatically creates rudimentary BibTeX entries for preprints that are not found on inSPIRE.");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"authors");
            put("type"              ,"manual");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"");
        }
    };

    public celsius.data.Item item;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        item = (Item)tr;
        communication=com;
        Msgs = m;
    }
        
    public void run() {
        celsius.data.BibTeXRecord BTR;
        String currentBibTeX=item.getS("bibtex");
        if ((currentBibTeX!=null) && (currentBibTeX.length()>0)) {
            BTR=new celsius.data.BibTeXRecord(currentBibTeX);
        } else {
            BTR=new celsius.data.BibTeXRecord();
        }
        // type
        if ((BTR.type==null) || (BTR.type.length()==0)) {
            String type="Article";
            if (item.getS("type").equals("book") || item.getS("type").equals("ebook")) type="Book";
            if (item.getS("type").equals("thesis")) type="Phdthesis";
            BTR.type=type;
        }
        
        // tag
        if ((BTR.getTag()==null) || (BTR.getTag().length()==0)) {
            String tag=item.linkedPersons.get("authors").get(1).getS("last_name").replaceAll("[^a-zA-Z]", "");
            if (!item.isEmpty("arxiv-number")) {
                tag+=":"+item.getS("arxiv-number");
            } else {
                tag+=":xxxx";
            }
            BTR.setTag(tag);
        }
        
        // title
        if (BTR.isEmpty("title")) {
            BTR.put("title",item.getS("title"));
        }
        
        // authors
        if (BTR.isEmpty("author")) {
            StringBuilder authors=new StringBuilder();
            for (celsius.data.Person person : item.linkedPersons.get("authors")) {
                authors.append(" and ");
                authors.append(person.getS("last_name"));
                authors.append(", ");
                authors.append(person.getS("first_name"));
            }
            if (authors.length()>0) BTR.put("author",authors.substring(5));
        }
        
        // eprints
        if (!item.getS("arxiv-name").equals("")) {
            BTR.put("eprint",item.getS("arxiv-ref"));
            BTR.put("archiveprefix","arXiv");
            if ((item.getS("arxiv-ref").length()==9) || (item.getS("arxiv-ref").length()==10)) {
                BTR.put("primaryclass",item.getS("arxiv-name"));
            }
        }
        
        item.put("bibtex", BTR.toString());
        item.put("citation-tag",BTR.getTag());
    }
  

}
