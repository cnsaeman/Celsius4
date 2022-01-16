import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import celsius.data.*;
import atlantis.tools.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;


/**
 * @author cnsaeman
 * 
 * To include:
 * http://api.semanticscholar.org/v1/paper/6162a894c2481599a92e4adaa06d08c200d8c31e
 * 
 */
public class PluginUniversalPeople extends Thread {
    
    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Universal People Plugin");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin tries to retrieve all possible information from offline and online sources for a given person.");
            put("needsFirstPage"    ,"no");
            put("wouldLikeFirstPage","yes");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"");
            put("type"              ,"manual-people");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };

    // to do:
    // arXiv: check that any new information can be gained from obtaining arXiv info
    
    private final String TI="P:Un>";
    
    private String oai;
    
    private boolean debug;

    public Person person;
    public Library library;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        person=(Person)tr;
        library=person.library;
        communication=com;
        Msgs = m;
    }
        
    public void run() {
        getFromInspire();
    }
    
    public void getFromInspire() {
        try {
            if (person.isEmpty("inspirekey")) {
                // inspirekey missing, obtain from inspire via links of document
                ResultSet rs=library.executeResEX("SELECT item_person_links.ord, items.inspirekey FROM item_person_links JOIN items ON item_person_links.item_id=items.id WHERE item_person_links.person_id="+person.id+" AND items.inspirekey NOT NULL LIMIT 1;");
                if (rs.next()) {
                    System.out.println("Database result 1");
                    String inspireRecord=webToolsHEP.jsonFromInspire("literature",rs.getString(2));
                    if (inspireRecord.length()>2) {
                        System.out.println("Length of returned data:");
                        System.out.println(inspireRecord.length());
                        JSONParser jp = new JSONParser(inspireRecord);
                        jp.moveToNextTag("authors");
                        for (int i = 0; i <= rs.getInt(1); i++) {
                            System.out.println("mm");
                            jp.moveToNextTag("ids");
                        }
                        String inspirekey = Parser.cutFrom(jp.extractStringFromNextTag("$ref"), "https://inspirehep.net/api/authors/");
                        System.out.println(inspirekey);
                        person.put("inspirekey", inspirekey);
                    }
                }
            }
            System.out.println("PluginUniversalPeople " + person.get("inspirekey"));
            if (!person.isEmpty("inspirekey")) {
                String inspireRecord = webToolsHEP.jsonFromInspire("authors", person.getS("inspirekey"));
                System.out.println(inspireRecord.substring(0,10));
                if (inspireRecord.length() > 2) {
                    JSONParser jp = new JSONParser(inspireRecord);
                    jp.moveToNextTag("email_addresses");
                    jp.restrictLevel();
                    String email = jp.extractStringFromNextTag("value");
                    while (email != null) {
                        Boolean current = jp.extractBooleanFromNextTag("current");
                        if (current) {
                            person.put("email", email);
                            System.out.println("Email found " + email);
                        }
                        email = jp.extractStringFromNextTag("value");
                    }
                    jp.releaseLevel();
                    jp.moveToFirstTag("advisors");
                    jp.restrictLevel();
                    String advisor = jp.extractStringFromNextTag("name");
                    String advisors = "";
                    while (advisor != null) {
                        System.out.println("Advisor: " + advisor);
                        advisors += ", " + advisor;
                        advisor = jp.extractStringFromNextTag("name");
                    }
                    if (advisors.length() > 2) {
                        person.put("advisors", advisors.substring(2));
                    }
                    jp.releaseLevel();
                    jp.moveToFirstTag("positions");
                    String rank = jp.extractStringFromNextTag("rank");
                    String institution = jp.extractStringFromNextTag("institution");
                    person.put("last_rank", rank);
                    person.put("last_institution", institution);
                    System.out.println("Last rank: " + rank);
                    System.out.println("Last institution: " + institution);
                    jp.moveToNextTag("ids");
                    String value = jp.extractStringFromNextTag("value");
                    jp.restrictLevel();
                    while (value != null) {
                        String schema = jp.extractStringFromNextTag("schema");
                        if (schema.toLowerCase().equals("orcid")) {
                            person.put("orcid", value);
                        }
                        if (schema.toLowerCase().equals("INSPIRE BAI")) {
                            person.put("inspirebai", value);
                        }
                        System.out.println("Schema: " + schema + " :: " + value);
                        value = jp.extractStringFromNextTag("value");
                    }
                    jp.releaseLevel();
                    if (jp.moveToFirstTag("urls")) {
                        jp.restrictLevel();
                        String homepage = jp.extractStringFromNextTag("value");
                        if (homepage != null) {
                            person.put("homepage", homepage);
                            System.out.println("Homepage: " + homepage);
                        }
                        jp.releaseLevel();
                    }
                    String categories = jp.extractStringFromNextTag("arxiv_categories");
                    if (categories != null) {
                        person.put("categories", categories);
                        System.out.println("Categories: " + categories);
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
}

 
