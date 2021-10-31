import celsius.tools.Parser;
import celsius.tools.TextFile;
import celsius.tools.toolbox;
import java.util.ArrayList;
import java.util.HashMap;
import celsius.tools.*;

public class PluginNormalizeBibTeX extends Thread {
 
    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Normalize BibTeX");
            put("author"            ,"Christian Saemann");
            put("version"           ,"1.01");
            put("help"              ,"This plugin substitutes long journal names in BibTeX by their abbreviation."
                    + "\n A list of journal names is found in the configuration file \"journal substitutions.txt\".");
            put("needsFirstPage"    ,"no");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"bibtex");
            put("type"              ,"auto|manual");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };
    
    public celsius.MProperties Information;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.MProperties i, ArrayList<String> m){
        Information=i; Msgs=m;
    }
    
    public static String normalizeJournal(String j) {
        j=j.replaceAll("\\.",". ");
        j=j.replaceAll("\\.  ",". ");
        return(j);
    }    
    
    public void replaceB(String s1,String s2) {
        String t=BTR.get("title");
        System.out.println("Testing :: "+t+" for "+s1+" to "+s2);
        t=t.replaceAll(s1,s2);
        if (!t.equals(BTR.get("title"))) {
            System.out.println("Replacing... : "+t.replaceAll(s1,s2));
            BTR.put("title",t);
            replaced=true;
        }
    }
    
    celsius.BibTeXRecord BTR;
    boolean replaced;

    public void run() {
    System.out.println("Running...");
	if (!Information.isEmpty("bibtex")) {
            try {
                String bib=Information.get("bibtex");
                BTR=new celsius.BibTeXRecord(bib);
                replaced=false;
                replaceB("BCJ","{B}{C}{J}");
                replaceB("BRST","{B}{R}{S}{T}");
                replaceB("BV","{B}{V}");
                replaceB("Chern","{C}hern");
                replaceB("\\{C\\}hern\\-\\{S\\}imons","{C}hern--{S}imons");
                replaceB("Drinfeld","{D}rinfeld");
                replaceB("Einstein","{E}instein");
                replaceB("Hilbert","{H}ilbert");
                replaceB("Jacobi","{J}acobi");
                replaceB("KK","{K}{K}");
                replaceB("Lie","{L}ie");
                replaceB("MHV","{M}{H}{V}");
                replaceB("Mills","{M}ills");
                replaceB("QCD","{Q}{C}{D}");
                replaceB("SYM","{S}{Y}{M}");
                replaceB("S\\-matrix","{S}-matrix");
                replaceB("Simons","{S}imons");
                replaceB("Ward","{W}ard");
                replaceB("Witten","{W}itten");
                replaceB("Yang","{Y}ang");
                replaceB("\\{Y\\}ang\\-\\{M\\}ills","{Y}ang--{M}ills");
                if (replaced) Information.put("bibtex", BTR.toString());
                if (!BTR.isEmpty("journal")) {
                    String journal=normalizeJournal(BTR.get("journal"));
                    journal=journal.replaceAll("\\. \\.",".");
                    if (journal.equals("JHEP")) {
                        if (BTR.get("volume").length()==2) {
                            BTR.put("volume",BTR.get("year").substring(2,4)+BTR.get("volume"));
                            Information.put("bibtex", BTR.toString());
                        }
                    } else {
                        journal=journal.trim();
                        char lastletter=journal.charAt(journal.length()-1);
                        if ((lastletter!='.') && (lastletter==java.lang.Character.toLowerCase(lastletter))) {
                            journal=journal+".";                            
                        }
                    }
                    if (!journal.equals(BTR.get("journal"))) {
                        BTR.put("journal",journal);
                        Information.put("bibtex", BTR.toString());
                        String ident = toolbox.Identifier(Information);
                        if ((ident == null) || (ident.length() < 3)) {
                            Information.remove("identifier");
                        } else {
                            Information.put("identifier", ident.trim());
                        }
                    }
                }
                if ((!BTR.isEmpty("eprint")) && (BTR.get("eprint").startsWith("math/")) && (Information.get("categories")!=null)) {
                    BTR.put("eprint",Parser.CutTill(Information.get("categories")," ")+"/"+Parser.CutFrom(BTR.get("eprint"),"/"));
                    Information.put("bibtex", BTR.toString());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
  
                      
}
