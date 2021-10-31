/*
 * PluginUpdateComplete.java
 *
 * Created on 05. September 2007, 12:20
 *
 * complete, testing
 */

import java.util.ArrayList;
import java.util.HashMap;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import celsius.tools.toolbox;


/**
 * @author cnsaeman
 */
public class PluginWatchCitationCount extends Thread {

    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Watch Citation Count");
            put("author"            ,"Christian Saemann");
            put("version"           ,"1.0");
            put("help"              ,"This plugin updates and compares citation counts from Inspire. ");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex|inspirekey");
            put("type"              ,"manual");
            put("defaultParameters" ,"http://inspirehep.net/");
            put("parameter-help"    ,"Link to the Inspire mirror to be used.");
            put("finalize"          ,"yes");
        }
    };


    public celsius.MProperties Information;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.MProperties i, ArrayList<String> m) {
        Information = i;
        Msgs = m;
    }

    public void run() {
        String inspirebase=Information.get("$$params");

        celsius.BibTeXRecord BTR2=new celsius.BibTeXRecord(Information.get("bibtex"));
        if (Information.getS("$$finalize").equals("yes")) {
            String out="\n Total citations: "+Information.get("$$keep-total");
            if (!Information.isEmpty("$$keep-output")) out=Information.getS("$$keep-output")+out;
            Information.put("$$output", out.substring(1));
            try {
                TextFile outf = new TextFile("watchCitationCount.tex",false);
                outf.putString(out.substring(1));
                outf.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        } else {
            if (Information.get("inspirekey") != null) {
                String citationtag = BTR2.tag;
                try {
                String cit=webToolsHEP.getInspireNoOfCitations2(Information.get("inspirekey"));
                if (cit.length() > 0) {
                    if (!BTR2.getS("citations").equals(cit)) {
                        String ret=Information.getS("$$keep-output");
                        ret+="\n New citations for paper \""+Information.get("title")+"\" : "+BTR2.getS("citations")+" → "+cit;
                        Information.put("$$keep-output", ret);
                        BTR2.put("citations", cit);
                    }
                    //System.out.println("Citations found: " + cit);
                    //System.out.println(BTR2.toString());
                    Information.put("bibtex", BTR2.toString());
                    //System.out.println(Information.changed);
                    
                } else {
                    //System.out.println("No citations found for paper : " + citationtag);
                }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int tot = 0;
                if (Information.getS("$$keep-total").length() > 0) {
                    tot = Integer.valueOf(Information.getS("$$keep-total"));
                }
                tot += Integer.valueOf(BTR2.getS("citations"));
                Information.put("$$keep-total",String.valueOf(tot));
            }
        }
    }
}
