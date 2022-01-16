/*
 * PluginArXiv.java
 *
 * Created on 14. Juli 2007, 12:20
 *
 * complete, testing
 */

import java.util.*;
import java.io.*;
import atlantis.tools.*;

public class arxivTools {
    
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String EOP=String.valueOf((char)12);   // EndOfPage signal
    
    public final static String arxives=new String(" hep-th cond-mat gr-qc q-alg astro-ph quant-ph math-ph hep-lat hep-ph dg-ga funct-an alg-geom nlin.SI physics ");
    // best list
    private final static String arxivesAtSpires=new String(" acc-phys astro-ph gr-qc hep-ex hep-lat hep-ph hep-th nucl-ex nucl-th math-ph ");
    
    /**
     * Returns true, if the arxiv is at Spires
     */
    public static boolean arxivAtSpires(String tmp) {
        return(arxivesAtSpires.indexOf(" "+tmp+" ")>-1);
    }
    
    /**
     * Returns true, if the string is an arxiv
     */
    public static boolean arxiv(String tmp) {
        return(arxives.indexOf(" "+tmp+" ")>-1);
    }
    
    /**
     *  Returns current timestamp
     */
    public static String ActDatum() {
        Date ActD=new Date();
        return(new String(ActD.toString()));
    }
    
    /**
     *  Returns www address for googleing for key
     */
    public static String googleFor(String key) {
        return("http://www.google.com/search?hl=en&q="+key+"&btnG=Google+Search&meta=");
    }
    
    /**
     *  Returns www address of Latex at Spires
     */
    public static String amazonFor(String key) {
        return("http://www.amazon.com/gp/search/ref=br_ss_hs/002-7808632-1948867?platform=gurupa&url=index%3Dblended&keywords="+key+"&Go.x=0&Go.y=0&Go=Go");
    }
    
    /**
     *  Returns www address of BibTeX at Spires
     */
    public static String BibTeX(String key) {
        return("http://www-spires.slac.stanford.edu/spires/find/hep/www?key="+key+"&FORMAT=WWWBRIEFBIBTEX");
    }
    
    /**
     * Returns www address of SLACEntry key
     */
    public static String SLACEntry(String key) {
        return("http://www.slac.stanford.edu/spires/find/hep/www?rawcmd=FIND+key+"+key);
    }
    
    /**
     * Returns www address of SLACEntry key
     */
    public static String SLACEntry(String arx,String nmb) {
        if (nmb.indexOf('.')>0)
            return("http://www.slac.stanford.edu/spires/find/hep/www?rawcmd=find+eprint+"+nmb);
        return("http://www.slac.stanford.edu/spires/find/hep/www?rawcmd=find+eprint+"+arx+"%2F"+nmb);
    }
    
    /**
     * Returns www address of arXiv entry key
     */
    public static String arXivEntry(String arx,String nmb) {
        if (nmb.indexOf('.')>0)
            return("http://www.arxiv.org/abs/"+nmb);
        return("http://www.arxiv.org/abs/"+arx+"/"+nmb);
    }
    
    /**
     * Returns www address of eprintWeb entry key
     */
    public static String eprintWebEntry(String arx,String nmb) {
        return("http://eprintweb.org/S/article/"+arx+"/"+nmb);
    }
    
    
    /**
     * Returns www address of eprints entry key
     */
    public static String eprintsEntry(String arx,String nmb) {
        if (arx.indexOf(".")>-1) arx=Parser.cutUntil(arx,".");
        return("http://citebase.eprints.org/cgi-bin/citations?id=oai:arXiv.org:"+arx+"/"+nmb);
    }
    
    /**
     * Returns www address of eprints entry key
     */
    public static String eprintsBibTeXEntry(String arx,String nmb) {
        if (arx.indexOf(".")>-1) arx=Parser.cutUntil(arx,".");
        return("http://www.citebase.org/cgi-bin/openURL?url_ver=Z39.88-2004&svc_id=bibtex&rft_id=oai:arXiv.org:"+arx+"/"+nmb);
    }
    
    private static boolean forbiddenChars(String t) {
        boolean forb=false;
        forb=forb || (t.indexOf("-")>-1);
        return(forb);
    }
    
    /**
     * get citation tag from a bibtex string
     */
    public static String getCitationFromBibTeX(String in) {
        in=Parser.cutFrom(Parser.cutUntil(in,","),"{");
        return(in);
    }
    
    
    /**
     * Returns the http-address for a search at spires for a paper named [title]
     * and having the authors [authors]
     */
    public static String searchSpires(String authors, String title) {
        String tmp="http://www.slac.stanford.edu/spires/find/hep/www?rawcmd=FIND";
        String authorpart="";
        while (authors.length()>0) {
            authorpart+="+A+%22"+Parser.cutUntil(authors,",")+"%22+AND";
            authors=Parser.cutFrom(authors,", ");
        }
        tmp+=authorpart;
        String keyword="";
        while ((title.length()>0) && (keyword.length()<4)) {
            keyword=Parser.cutUntil(title," ");
            if (forbiddenChars(keyword)) keyword="";
            title=Parser.cutFrom(title," ");
        }
        if (keyword.length()>3) tmp+="+T+"+keyword;
        else authorpart=Parser.cutUntilLast(authorpart,"+AND");
        tmp+="&FORMAT=www&SEQUENCE=";
        return(tmp);
    }
    
    private static boolean completeTag(String s) {
        if (s.equals("")) return(false);
        if ((s.indexOf("/")==-1) && (s.indexOf("[")==-1)) return(false);
        return(true);
    }
    
    private static boolean greater(String a,String b) {
        if (b.equals("")) return(true);
        if (a.startsWith("9") && !b.startsWith("9")) return(false);
        if ((a.indexOf(".")>-1) && (b.indexOf(".")==-1)) return(true);
        if ((a.indexOf(".")==-1) && (b.indexOf(".")>-1)) return(false);
        return(a.compareTo(b)>0);
    }
    
    /**
     * Get ArXivTags from a given file name, returns ArrayList with two empty elements
     * if not successful.
     */
    public static ArrayList<String> GetArXivTags(String FP) {
        // Try to parse like old arXiv Identifiers
        String tmptag=new String("");
        // go to last complete arXiv:...
        String ken=FP.substring(0,40);
        String arx=new String("");
        String nmb=new String("");
        String arxb=new String("");
        String nmbb=new String("");
        int posa=FP.indexOf("arXiv:");
        int posb=FP.indexOf("\n",posa);
        while ((posa>-1) && (posb>-1)) {
            while (!(completeTag(tmptag)) && (posa>-1) && (posb>-1)) {
                tmptag=FP.substring(posa,posb);
                posa=FP.indexOf("arXiv:",posb);
                posb=FP.indexOf("\n",posa);
            }
            if (posb==-1) posa=-1;
            if (completeTag(tmptag)) {
                tmptag=Parser.cutFrom(tmptag,"arXiv:");
                if (tmptag.indexOf("/")>0) {
                    tmptag=Parser.cutUntil(tmptag," ");
                    arxb=Parser.cutUntil(tmptag,"/");
                    nmbb=Parser.cutFrom(tmptag,"/");
                } else {
                    nmbb=Parser.cutUntil(tmptag," ");
                    arxb=Parser.cutUntil(Parser.cutFrom(tmptag,"["),"]");
                }
            }
            if (greater(nmbb,nmb)) {
                arx=arxb; nmb=nmbb;
            }
        }
        if (nmb.indexOf("v")>0) nmb=Parser.cutUntil(nmb,"v");
        ArrayList<String> Results=new ArrayList<String>();
        Results.add(arx);
        Results.add(nmb);
        return(Results);
    }
    
    /**
     * Correct auto-lower end of words
     */
    public static String Correct(String t) {
        t=Parser.replace(t,"Qcd","QCD");
        t=Parser.replace(t,"Sym","SYM");
        t=Parser.replace(t,"SYMm","Symm");
        t=Parser.replace(t,"SYMpl","Sympl");
        t=Parser.replace(t,"Sdym","SDYM");
        t=Parser.replace(t,"Bps","BPS");
        t=Parser.replace(t,"Cft","CFT");
        t=Parser.replace(t,"Ads","AdS");
        t=Parser.replace(t,"Scft","SCFT");
        t=Parser.replace(t,"Mhv","MHV");
        t=Parser.replace(t,"Bcft","BCFT");
        t=Parser.replace(t,"qft","QFT");
        t=Parser.replace(t,"Nc","NC");
        t=Parser.replace(t,"Su(","SU(");
        t=Parser.replace(t,"Iii","III");
        t=Parser.replace(t,"Ii","II");
        return(t);
    }
    
    /**
     * Get the corresponding SLAC-Key from the internet, using HTML page slac.
     * TI and Msg1: data for protocol
     */
    public static String GetKeyFromInternet(String slac, ArrayList<String> Msg) throws IOException {
        // 2.1: get SPIRES-key
        Msg.add("Reading Key from SPIRES:");
        Msg.add(slac);
        String gotit=TextFile.ReadOutURL(slac);
        if (!gotit.startsWith("##??")) {
            Msg.add("got it:"+GetSpiresKey(gotit));
            return(GetSpiresKey(gotit));
        } else {
            Msg.add("Retrieving key from SPIRES failed.");
            return("?");
        }
    }

    /**
     * Get the corresponding SLAC-Key from the internet, using HTML page slac.
     * TI and Msg1: data for protocol
     */
    public static String GetKeyFromInternet(String arx, String nmb, String spiresbase, ArrayList<String> Msg) throws IOException {
        String searchstr;
        if (nmb.indexOf('.')>0) {
            searchstr=spiresbase+"find/hep/www?rawcmd=find+eprint+"+nmb;
        } else {
            searchstr=spiresbase+"find/hep/www?rawcmd=find+eprint+"+arx+"%2F"+nmb;
        }
        return(GetKeyFromInternet(searchstr,Msg));
    }

    /**
     * Complete SLAC-Latex and Bibtex info in data vector Data.
     * TI and Msg1: data for protocol
     */
    public static String GetRefFromInternet(String key, String spiresbase, ArrayList<String> Msg) throws IOException {
        // get Bibtex reference
        Msg.add("Reading reference from SPIRES (BibTeX)");
        String URL=spiresbase+"/find/hep/www?key="+key+"&FORMAT=WWWBRIEFBIBTEX";
        String gotit2=TextFile.ReadOutURL(URL);
        if (!gotit2.startsWith("##??")) {
            Msg.add("got it:"+extractBibTeX(gotit2));
            return(extractBibTeX(gotit2));
        } else {
            Msg.add("Retrieving data from SPIRES failed (bibtex)");
        }
        return(new String(""));
    }
    
    /**
     * Extracts Spires Key from file entry at SLAC
     */
    public static String GetSpiresKey(String page) throws IOException {
        String tmp=Parser.cutFrom(page,"key");
        tmp=Parser.cutFrom(tmp,"key=");
        tmp=Parser.cutUntil(tmp,">");
        if (tmp.indexOf("&")>1) tmp=Parser.cutUntil(tmp,"&");
        return(tmp);
    }
    
    /**
     * Returns Latex String from file source
     */
    public static String ReadLatex(String tmp) {
        tmp=Parser.cutFrom(tmp,"<pre>");
        tmp=Parser.cutUntil(tmp,"</pre>").trim();
        tmp=Parser.replace(tmp,"arXiv:","");
        tmp=Parser.cutUntil(tmp,"%%Cited").trim();
        tmp=Parser.replace(tmp,"%%","*#*");
        tmp=Parser.replace(tmp,"%\\cite","*#*#*");
        tmp=Parser.replace(tmp,"%","");
        tmp=Parser.replace(tmp,"*#*#*","%\\cite");
        tmp=Parser.replace(tmp,"*#*","%%");
        return(tmp.trim());
    }
    
    /**
     * Returns BibTeX String from file source
     */
    public static String extractBibTeX(String tmp) {
        //System.out.println(tmp);
        tmp=Parser.cutFrom(tmp,"<pre>");
        tmp=Parser.cutUntil(tmp,"</pre>").trim();
        tmp=Parser.replace(tmp,"arXiv:","");
        tmp=Parser.replace(tmp,"%``","``");
        tmp=Parser.cutUntil(tmp,"%%Cited").trim();
        //System.out.println(tmp);
        return(tmp.trim());
    }
        
    /**
     * get the appropriate tag from bibtex string in
     */
    public static String getFromBibTeX(String tag,String in) {
        in=Parser.cutUntil(Parser.cutFrom(Parser.cutFrom(in,tag),"\""),"\",");
        in=Parser.replace(in,linesep," ").trim();
        while (in.indexOf("  ")>-1) in=Parser.replace(in,"  "," ").trim();
        if (in.startsWith("{")) in=Parser.cutUntilLast(in.substring(1),"}");
        return(in);
    }
    
    
    
    
}
