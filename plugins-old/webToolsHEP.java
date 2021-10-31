/*
 * PluginArXiv.java
 *
 * Created on 14. Juli 2007, 12:20
 *
 * complete, testing
 */

import java.util.*;
import java.io.*;
import celsius.tools.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class webToolsHEP {
    
    public final static String linesep = System.getProperty("line.separator");  // EndOfLine signal
    public final static String filesep = System.getProperty("file.separator");  // EndOfLine signal
    public final static String EOP=String.valueOf((char)12);   // EndOfPage signal
    
    /**
     * Download inspire record
     * @param inspirebase
     * @param inspirekey
     * @return 
     */
    public static String getInspireRecord(String inspirebase, String inspirekey) {
        String srchstring = "https://inspirehep.net/api/literature/" + inspirekey;
        return(TextFile.ReadOutURL(srchstring));
    }
    
    public static String getInspireDetails(String inspirebase, String inspirekey) {
        String srchstring = inspirebase + "record/" + inspirekey + "?of=xm&ot=100,700,84,37,245,260,300,520,773";
        String tmp=TextFile.ReadOutURL(srchstring);
        if (!tmp.startsWith("<?xml version")) return(null);
        HashMap data=new HashMap<String,String>();
        return(Parser.CutTill(Parser.CutFrom(tmp,"<controlfield tag=\"001\">"),"</controlfield>"));
    }
    
    public static String getInspireJson(String inspirebase, String inspirekey) {
        String srchstring = inspirebase + "record/" + inspirekey + "?of=recjson&ot=comment,title,system_number,abstract,authors,doi,primary_report_number,publication_info,physical_description,number_of_citations,thesaurus_terms";
        String tmp=TextFile.ReadOutURL(srchstring);
        if (!tmp.startsWith("[{")) return(null);
        return(tmp);
    }
        
    // Version with plain html, faster
    public static String getInspireNoOfCitations2(String inspirekey) {
        String tmp=jsonFromInspire("literature",inspirekey);
        System.out.println("Return:\n"+tmp);
        String out=Parser.CutTill(Parser.CutFrom(tmp,"\"citation_count\":"),",");
        System.out.println("Citations:"+out);
        return(out);
    }
    
    /**
     * Downloads abstract from the ADSABS-link given in the inspires record
     * @param tmp : The inspire record page
     * @return 
     */
    public static String abstractFromInspire(String tmp) {
        String abs=Parser.CutFrom(tmp,"\"abstracts\":");
        abs=Parser.CutFrom(abs,"\"value\":\"").trim();
        abs=Parser.CutTill(abs,"\"}");
        abs=Parser.CutTill(abs,"\",");
        if (abs.length()>1) return(abs);
        return(null);
    }
    
    public static String jsonFromInspire(String type, String query) {
        String srchstring = "https://inspirehep.net/api/"+type+"/"+query;
        StringBuffer rtn=new StringBuffer(4000);
        try {    
            URL url = new URL(srchstring);
            HttpURLConnection urlConnection = (HttpURLConnection)  url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/json");
            InputStream in=urlConnection.getInputStream();
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                rtn.append(new String(buffer,0,bytes_read));
            }
            in.close();
            return(rtn.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(null);
    }    
    
    public static String bibTeXFromInspire(String key) {
        String srchstring = "https://inspirehep.net/api/literature/" + key;
        StringBuffer rtn=new StringBuffer(4000);
        try {    
            URL url = new URL(srchstring);
            HttpURLConnection urlConnection = (HttpURLConnection)  url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/x-bibtex");
            InputStream in=urlConnection.getInputStream();
            byte[] buffer=new byte[4096];
            int bytes_read;
            while ((bytes_read=in.read(buffer))!=-1) {
                rtn.append(new String(buffer,0,bytes_read));
            }
            in.close();
            return(rtn.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return(null);
    }
    
    public static String keywordsFromInspire(String tmp) {
        String keywords = Parser.CutFrom(tmp, "<strong>Keyword(s):");
        keywords = Parser.CutTill(keywords, "<br").trim();
        keywords = Parser.CutTill(keywords, "<div").trim();
        keywords = Parser.CutFrom(keywords, "<a").trim();
        return(keywords);
    }
    
    public static String extractCitations(String tmp) {
        return(Parser.CutTill(Parser.CutFrom(tmp, "Citations ("), ")"));
    }
    
    public static String linksFromInspire(String inspirebase, String lnk) {
        String srchstring = inspirebase + "record/" + lnk + "/references";
        String tmp2 = TextFile.ReadOutURL(srchstring);
        String links = new String("");
        tmp2 = Parser.CutFrom(tmp2, "<table><tr><td valign=\"top\">");
        while (tmp2.indexOf("</tr><tr>") > -1) {
            String link = Parser.CutTill(tmp2, "</tr><tr>").trim();
            if (link.indexOf("/record/") > -1) {
                links += "|refers to:inspirekey:" + Parser.CutTill(Parser.CutFrom(link, "/record/"), "\n");
            } else {
                link = Parser.CutTill(Parser.CutFrom(Parser.CutFrom(Parser.CutFrom(link, "<small>"),"<small>"), "<small>"), "</small>").trim();
                links += "|refers to:identifier:" + link;
            }
            tmp2 = Parser.CutFrom(tmp2, "</tr><tr>");
        }
        srchstring = inspirebase + "search?ln=en&p=refersto%3Arecid%3A" + lnk;
        tmp2 = TextFile.ReadOutURL(srchstring);
        tmp2 = Parser.CutFrom(tmp2, "<!C-START");
        while (tmp2.indexOf("<div class=\"record_body\">") > -1) {
            tmp2 = Parser.CutFrom(tmp2, "<div class=\"record_body\">");
            String link = Parser.CutTill(tmp2, "</div>").trim();
            links += "|citation:inspirekey:" + Parser.CutTill(Parser.CutFrom(link, "/record/"), "\"");
            tmp2 = Parser.CutFrom(tmp2, "</div>");
        }
        return(links.substring(1));
    }
    
    public static String arXivRefFromInspire(String inspirebase, String lnk) {
        String links = new String("");
        String srchstring = inspirebase + "search?ln=en&p=refersto%3Arecid%3A" + lnk + "&rg=100";
        String tmp2 = TextFile.ReadOutURL(srchstring);
        tmp2 = Parser.CutFrom(tmp2, "<!C-START");
        while (tmp2.indexOf("<div class=\"record_body\">") > -1) {
            tmp2 = Parser.CutFrom(tmp2, "<div class=\"record_body\">");
            String link = Parser.CutTill(tmp2, "</div>").trim();
            links += "|" + Parser.CutTill(Parser.CutFrom(link, "<br/>e-Print: <b>"), "</b>");
            tmp2 = Parser.CutFrom(tmp2, "</div>");
        }
        return(links.substring(1));
    }
    
    public static String inspireRecordJSON(String rec) {
      String tmp2 = TextFile.ReadOutURL("https://inspirehep.net/api/literature/"+rec);
      return tmp2;
    }
    
    
}
