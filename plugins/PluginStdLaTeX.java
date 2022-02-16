import java.util.ArrayList;
import java.util.HashMap;
import atlantis.tools.*;
import celsius.data.*;


public class PluginStdLaTeX extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Standard LaTeX");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin creates standard LaTeX bibliography output.");
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
        String out = new String("");
        while (authors.length() > 0) {
            author = Parser.cutUntil(authors, " and ");
            if (author.indexOf(",") > -1) {
                author = Parser.cutFrom(author, ",").trim() + " " + Parser.cutUntil(author, ",").trim();
            }
            if (author.indexOf(".") == -1) {
                String prenomes = Parser.cutUntilLast(author, " ").trim();
                author = Parser.cutFromLast(author, " ").trim();
                int i = prenomes.lastIndexOf(" ");
                while (i > -1) {
                    author = prenomes.substring(i + 1, i + 2) + ". " + author;
                    prenomes = prenomes.substring(0, i).trim();
                    i = prenomes.lastIndexOf(" ");
                }
                if (prenomes.length()>0) author = prenomes.substring(0, 1) + ". " + author;
            }
            out += author.replace(" ", "~") + ", ";
            authors = Parser.cutFrom(authors, " and ");
        }
        out = Parser.cutUntilLast(out, ", ");
        if (out.indexOf(", ") > -1) {
            out = Parser.cutUntilLast(out, ", ") + " and " + Parser.cutFromLast(out, ", ");
        }
        return (out);
    }

    /**
     * Turn BibTeX-information string into a Latex string
     */
    public String LatexFromBibTeX(String t1) {
        celsius.components.bibliography.BibTeXRecord BTR=new celsius.components.bibliography.BibTeXRecord(t1);
        if (BTR.parseError!=0) return("parse Error");
        String out = new String("%\\cite{" + BTR.getTag() + "}\n" +
                "\\bibitem{" + BTR.getTag() + "}\n");
        out += LaTeXAuthorsFromBibTex(BTR.get("author")) + ", ``" + BTR.get("title") + ",''";
        boolean journal = false;
        if (BTR.get("journal")!=null) {
            out += "\n" + BTR.get("journal").replace(".", ".\\");
            String tmp = BTR.getS("volume");
            if (!tmp.equals("")) {
                if (Character.isLetter(tmp.charAt(0))) {
                    out += " " + tmp.substring(0, 1);
                    tmp = tmp.substring(1);
                }
                out += " {\\bf " + tmp + "} (" + BTR.getS("year") + ") ";
            } else {
                out += " (" + BTR.getS("year") + ") ";
            }
            out += Parser.cutUntil(BTR.getS("pages"), "-");
            journal = true;
        }
        if (BTR.get("note")!=null) {
            out += "\n" + BTR.get("note");
        }
        if (BTR.get("eprint")!=null) {
            if (!out.endsWith("\n")) {
                out += " ";
            }
            if (journal) {
                out += "[";
            }
            out += BTR.get("eprint");
            if (journal) {
                out += "]";
            }
        }
        out += ".";
        if (BTR.get("slaccitation")!=null) {
            out += "\n" + BTR.get("slaccitation");
        }
        while (out.indexOf("  ") > -1) {
            out = out.replace("  ", " ");
        }
        return (out);
    }

    public void run() {
        String bib = item.get("bibtex");
        if (bib == null) {
            communication.put("output", "");
            return;
        }
        communication.put("output", LatexFromBibTeX(bib));
    }
}
