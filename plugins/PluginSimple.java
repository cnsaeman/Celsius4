import java.util.ArrayList;
import java.util.HashMap;
import atlantis.tools.*;
import celsius.data.*;


public class PluginSimple extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Simple reference");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin creates a simple literature reference");
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
    
    /**
     * Returns Authors Latex format from BibTex string
     */
    public String LaTeXAuthorsFromBibTex(String authors) {
        String author;
        String out=new String("");
        while(authors.length()>0) {
            author=Parser.cutUntil(authors," and ");
            if (author.indexOf(",")>-1)
                author=Parser.cutFrom(author,",").trim()+" "+Parser.cutUntil(author,",").trim();
            if (author.indexOf(".")==-1) {
                String prenomes=Parser.cutUntilLast(author," ").trim();
                author=Parser.cutFromLast(author," ").trim();
                int i=prenomes.lastIndexOf(" ");
                while (i>-1) {
                    author=prenomes.substring(i+1,i+2)+". "+author;
                    prenomes=prenomes.substring(0,i).trim();
                    i=prenomes.lastIndexOf(" ");
                }
                if (prenomes.length()>0) author = prenomes.substring(0, 1) + ". " + author;
            }
            out+=author+", ";
            authors=Parser.cutFrom(authors," and ");
        }
        out=Parser.cutUntilLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.cutUntilLast(out,", ")+" and "+Parser.cutFromLast(out,", ");
        return(out);
    }
    
    public String clean(String s) {
      if (s.startsWith("{"))
        s=s.substring(1);
      if (s.endsWith("}"))
        s=s.substring(0,s.length()-1);
      return(s);  
    }
    
    
    /**
     * Turn BibTeX-information string into a Latex string
     */
    public String LatexFromBibTeX(String t1) {
        celsius.data.BibTeXRecord BTR=new celsius.data.BibTeXRecord(t1);
        if (BTR.parseError!=0) return("parse Error");
        String out=new String("");
        out+=LaTeXAuthorsFromBibTex(BTR.get("author"))+", \""+clean(BTR.get("title"))+"\",";
        boolean journal=false;
        if (BTR.get("journal")!=null) {
            out+="\n"+BTR.get("journal");
            String tmp=BTR.getS("volume");
            if (!tmp.equals("")) {
                if (Character.isLetter(tmp.charAt(0))) {
                    out+=" "+tmp.substring(0,1);
                    tmp=tmp.substring(1);
                }
                out+=" "+tmp+" ("+BTR.getS("year")+") ";
            } else {
                out+=" ("+BTR.getS("year")+") ";
            }
            out+=Parser.cutUntil(BTR.getS("pages"),"-");
            journal=true;
        } else if (BTR.type.equals("book")) {
            out+=" "+BTR.get("publisher");
            if (BTR.get("location")!=null) out+=", "+BTR.get("location");
            out+=", "+BTR.get("year");            
        }
        if (BTR.get("note")!=null)
            out+="\n"+BTR.get("note");
        if (BTR.get("eprint")!=null) {
            if (!out.endsWith("\n")) out+=" ";
            if (journal) out+="[";
            out+="arXiv:"+BTR.get("eprint");
            if (journal) out+="]";
        }
        out+=".";
        while (out.indexOf("  ")>-1) out=out.replace("  "," ");
        return(out);
    }
    
    
    
    public void run() {
        String bib=item.get("bibtex");
        if (bib==null) {
            communication.put("output","");
            return;
        }
        communication.put("output",LatexFromBibTeX(bib));
    }
    
    
}
