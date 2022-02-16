/*
 * PluginUpdateComplete.java
 *
 * Created on 05. September 2007, 12:20
 *
 * complete, testing
 */

import java.util.ArrayList;
import java.util.HashMap;
import atlantis.tools.*;
import celsius.tools.ToolBox;
import celsius.data.*;


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
            put("type"              ,"manual-items");
            put("defaultParameters" ,"http://inspirehep.net/");
            put("parameter-help"    ,"Link to the Inspire mirror to be used.");
            put("finalize"          ,"yes");
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
        //System.out.println("Starting plugin");
        String finalize=communication.get("$$finalize");
        //System.out.println("Finalize: "+finalize);
        if ((finalize!=null) && (finalize.equals("yes"))) {
            String out="\n Total citations: "+communication.get("$$keep-total");
            if (communication.containsKey("$$keep-output")) out=communication.get("$$keep-output")+out;
            communication.put("showOutput", out.substring(1));
            try {
                TextFile outf = new TextFile("watchCitationCount.txt",false);
                outf.putString(out.substring(1));
                outf.close();
            } catch (Exception e) {
                System.out.println(e.toString());
            }
        } else {
            celsius.components.bibliography.BibTeXRecord BTR2=new celsius.components.bibliography.BibTeXRecord(item.get("bibtex"));
            if (item.get("inspirekey") != null) {
                //String citationtag = BTR2.getTag();
                try {
                String cit=webToolsHEP.getInspireNoOfCitations2(item.get("inspirekey"));
                if (cit.length() > 0) {
                    if (!BTR2.getS("citations").equals(cit)) {
                        String ret=communication.get("$$keep-output");
                        if (ret==null) ret="";
                        ret+="\n New citations for paper \""+item.get("title")+"\" : "+BTR2.getS("citations")+" → "+cit;
                        communication.put("$$keep-output", ret);
                        BTR2.put("citations", cit);
                    }
                    item.put("bibtex", BTR2.toString());
                    item.save();
                    
                } else {
                    //System.out.println("No citations found for paper : " + citationtag);
                }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                int tot = 0;
                String totalSoFar=communication.get("$$keep-total");
                if (totalSoFar!=null) {
                    tot = Integer.valueOf(totalSoFar);
                }
                tot += Integer.valueOf(BTR2.getS("citations"));
                communication.put("$$keep-total",String.valueOf(tot));
            }
        }
    }
}
