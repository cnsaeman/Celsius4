import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import atlantis.tools.*;
import celsius.data.*;


public class PluginListOfPublications extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Create List of Publications");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin creates a list of publications, using the ListOfPublications.conf configuration file.");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex");
            put("type"              ,"export");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
            put("finalize"          ,"yes");
        }
    };

    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public HashMap<String,String> Strings;
    public ArrayList<String> PublishedPapers;
    public ArrayList<String> Preprints;
    public ArrayList<String> Reviews;
    public ArrayList<String> Proceedings;
    public ArrayList<String> BookChapters;
    public ArrayList<String> Other;

    public TextFile out;
    public celsius.data.Item item;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        item = (Item)tr;
        communication=com;
        Msgs = m;
        Strings=new HashMap<String,String>();
        PublishedPapers=new ArrayList<String>();
        Preprints=new ArrayList<String>();
        Reviews=new ArrayList<String>();
        Proceedings=new ArrayList<String>();
        BookChapters=new ArrayList<String>();
        Other=new ArrayList<String>();
    }
    
    public String getFromCom(String key) {
        if (communication.containsKey(key)) return(communication.get(key));
        return("");
    }

    public void run() {
        String output=new String("");
        if (communication.containsKey("$$finalize")) {
            try {
                ReadInStrings();
                SortPapers();
                WriteFile();
            } catch (Exception e) { e.printStackTrace(); }
            return;
        } else {
            if (!communication.containsKey("$$keep-output")) {
                communication.put("$$keep-output","");
            }
        }
        communication.put("$$keep-output", getFromCom("$$keep-output")+"---\n"+item.get("type")+"\n---\n"+item.get("bibtex"));
    }

    public void ReadInStrings() throws IOException {
         TextFile HAF = new TextFile("plugins/ListOfPublications.conf");
         String tmp;
         String cur=new String("");
         String curcont=new String("");
         while (HAF.ready()) {
            tmp=HAF.getString();
            if (tmp.startsWith("---")) {
                if (!cur.equals("")) Strings.put(cur,curcont.substring(1));
                cur=Parser.cutFrom(tmp,"---").trim().toLowerCase();
                curcont=new String("");
            } else {
                curcont+="\n"+tmp;
            }
         }
         if (!cur.equals("eof")) {
             Strings.put(cur,curcont.substring(1));
         }
         HAF.close();
    }

    public void SortPapers() {
         String big=getFromCom("$$keep-output");
         String type,bibtex;
         while (big.length()>0) {
             big=Parser.cutFrom(big,"---");
             type=Parser.cutUntil(big,"---").trim();
             big=Parser.cutFrom(big,"---");
             bibtex=Parser.cutUntil(big,"---").trim();
             if ((bibtex.trim().length()>0) && (bibtex.indexOf("year")>0)) {
                 //System.out.println(type);
                 //System.out.println(bibtex.substring(0,5));
                 if (type.equals("Paper")) {
                     PublishedPapers.add(bibtex);
                 } else if (type.equals("Preprint")) {
                     Preprints.add(bibtex);
                 } else if (type.equals("Review")) {
                     Reviews.add(bibtex);
                 } else if (type.equals("Talk")) {
                     Proceedings.add(bibtex);
                 } else if (type.equals("Book Chapter")) {
                     BookChapters.add(bibtex);
                 } else Other.add(bibtex);
             }
         }
    }

    public boolean compare(celsius.data.BibTeXRecord btr1,celsius.data.BibTeXRecord btr2) throws IOException  {
	if (btr1.getS("year").length()==0) out.putString("Error: no year in "+item.id);
	if (btr2.getS("year").length()==0) out.putString("Error: no year in "+item.id);
        String eprint1=btr1.getS("eprint");
        if (eprint1.indexOf('/')>0) eprint1=Parser.cutFrom(eprint1,"/");
        if (eprint1.length()==0) eprint1=btr1.getS("year").substring(2)+"00000";
        String eprint2=btr2.getS("eprint");
        if (eprint2.indexOf('/')>0) eprint2=Parser.cutFrom(eprint2,"/");
        if (eprint2.length()==0) eprint2=btr2.getS("year").substring(2)+"00000";
        return(eprint1.compareTo(eprint2)<0);
    }

    public String lowestOut(ArrayList<String> lop, String l) throws IOException  {
        int i=0;
        celsius.data.BibTeXRecord btr=new celsius.data.BibTeXRecord(lop.get(0));
        for (int j=0;j<lop.size();j++) {
            celsius.data.BibTeXRecord btr2=new celsius.data.BibTeXRecord(lop.get(j));
            if (compare(btr,btr2)) { btr=btr2; i=j; }
        }
        System.out.println(lop.get(i));
        lop.remove(i);
        return(LatexFromBibTeX(btr,l,lop.size()+1));
    }

    public void WriteFile() throws IOException {
         out = new TextFile("listofpublications.tex",false);
         out.putString(Strings.get("header"));
         out.putString(Strings.get("published"));
         while (PublishedPapers.size()>0) out.putString(lowestOut(PublishedPapers,"J"));
         out.putString(Strings.get("published end"));
         out.putString(Strings.get("preprints"));
         while (Preprints.size()>0) out.putString(lowestOut(Preprints,"P"));
         out.putString(Strings.get("preprints end"));
         out.putString(Strings.get("reviews"));
         while (Reviews.size()>0) out.putString(lowestOut(Reviews,"R"));
         out.putString(Strings.get("reviews end"));
         out.putString(Strings.get("proceedings"));
         while (Proceedings.size()>0) out.putString(lowestOut(Proceedings,"C"));
         out.putString(Strings.get("proceedings end"));
         out.putString(Strings.get("book chapters"));
         while (BookChapters.size()>0) out.putString(lowestOut(BookChapters,"B"));
         out.putString(Strings.get("book chapters end"));
         out.putString(Strings.get("others"));
         while (Other.size()>0) out.putString(lowestOut(Other,"F"));
         out.putString(Strings.get("others end"));
         out.putString(Strings.get("footer"));
         out.close();
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
            String prenomes = Parser.cutUntilLast(author, " ").trim();
            author = Parser.cutFromLast(author, " ").trim();
            int i = prenomes.lastIndexOf(" ");
            while (i > -1) {
                author = prenomes.substring(i + 1, i + 2) + ". " + author;
                prenomes = prenomes.substring(0, i).trim();
                i = prenomes.lastIndexOf(" ");
            }
            if (prenomes.length() > 0) {
                author = prenomes.substring(0, 1) + ". " + author;
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
    public String LatexFromBibTeX(celsius.data.BibTeXRecord BTR, String l, int i) {
        if (BTR.parseError!=0) return("parse Error");
        String out = new String("\\item[{["+l+Integer.toString(i)+"]}] ");
        out += LaTeXAuthorsFromBibTex(BTR.get("author")) + ", {\\em " + BTR.get("title") + ",}";
        boolean journal = false;
        if (BTR.get("note2")!=null) {
            out += "\n" + BTR.get("note2");
            journal=true;
        } else {
            if (BTR.get("journal") != null) {
                if (BTR.get("doi") != null) {
                    out += "\n\\href{http://dx.doi.org/" + BTR.get("doi") + "}{";
                } else {
                    out += "\n";
                }
                out += BTR.get("journal").replace(".", ".\\");
                String tmp = BTR.getS("volume");
                if (!tmp.equals("")) {
                    if ((Character.isLetter(tmp.charAt(0))) && (!Character.isLetter(tmp.charAt(1)))) {
                        out += " " + tmp.substring(0, 1);
                        tmp = tmp.substring(1);
                    }
                    out += " {\\bf " + tmp + "} (" + BTR.getS("year") + ") ";
                } else {
                    out += " (" + BTR.getS("year") + ") ";
                }
                out += Parser.cutUntil(BTR.getS("pages"), "-");
                journal = true;
                if (BTR.get("doi") != null) {
                    out += "}";
                }
            }
            if (BTR.get("note") != null) {
                out += "\n" + BTR.get("note");
                journal = true;
            }
        }
        if (BTR.get("eprint")!=null) {
            if (!out.endsWith("\n")) {
                out += " ";
            }
            if (journal) {
                out += "[";
            }
            out += "{\\tt \\href{http://www.arxiv.org/abs/"+BTR.get("eprint")+"}{"+eprint(BTR)+"}}";
            if (journal) {
                out += "]";
            }
        }
        if (BTR.get("citations")!=null) {
            if (BTR.get("citations").equals("1"))
                out+=", 1~citation";
            else out+=", "+BTR.get("citations")+"~citations";
        }
        out += ".";
        while (out.indexOf("  ") > -1) {
            out = out.replace("  ", " ");
        }
        return (out);
    }

    public String eprint(celsius.data.BibTeXRecord BTR) {
        String out=BTR.get("eprint");
        if (out.indexOf("/")==-1) out+=" ["+BTR.get("primaryclass")+"]";
        return(out);
    }

}
