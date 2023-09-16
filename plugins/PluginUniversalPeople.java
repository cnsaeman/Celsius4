import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.*;
import java.io.*;
import celsius.data.*;
import atlantis.tools.*;
import atlantis.JSON.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import celsius.components.library.Library;

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
                    TextFile.writeStringToFile(inspireRecord,"inspire-PUP-paper.json");
                    if (inspireRecord.length()>2) {
                        System.out.println("Length of returned data:");
                        System.out.println(inspireRecord.length());
                        JSONParser jp = new JSONParser(inspireRecord);
                        jp.parse();
                        JSONObject jt=jp.root;
                        String ref=jt.get("metadata").get("authors").get(rs.getInt(1)).get("record").get("$ref").toString();
                        String inspirekey = Parser.cutFrom(ref, "https://inspirehep.net/api/authors/");
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
                    TextFile.writeStringToFile(inspireRecord,"inspire-PUP-author.json");
                    JSONParser jp = new JSONParser(inspireRecord);
                    jp.parse();
                    JSONObject jt=jp.root;
                    String email = jt.get("metadata").get("email_addresses").get(0).get("value").toString();
                    person.put("email", email);
                    
                    JSONObject advisorsList=jt.get("metadata").get("advisors");
                    String advisors = "";
                    for (JSONObject entry : advisorsList) {
                        String advisor=entry.get("name").toString();
                        advisors += "|" + advisor;
                    }
                    if (advisors.length() > 1) {
                        person.put("advisors", advisors.substring(1));
                    }
                    
                    JSONObject positionsList=jt.get("metadata").get("positions");
                    for (int i=0;i<positionsList.size();i++) {
                        String rank = positionsList.get(i).get("rank").toString();
                        String institution = positionsList.get(i).get("institution").toString();
                        if (i==0) {
                            person.put("last_rank", rank);
                            person.put("last_institution", institution);
                        }
                        // do something with rest?
                    }

                    JSONObject idsList=jt.get("metadata").get("ids");
                    for (JSONObject entry : idsList) {
                        String value = entry.get("value").toString();
                        String schema = entry.get("schema").toString();
                        if (schema.toLowerCase().equals("orcid")) {
                            person.put("orcid", value);
                        }
                        if (schema.toLowerCase().equals("INSPIRE BAI")) {
                            person.put("inspirebai", value);
                        }
                        System.out.println("Schema: " + schema + " :: " + value);
                    }

                    String homepage=jt.get("metadata").get("urls").get(0).get("value").toString();
                    if (homepage != null) {
                        person.put("homepage", homepage);
                        System.out.println("Homepage: " + homepage);
                    }

                    JSONObject arxivCategoryList=jt.get("metadata").get("arxiv_categories");
                    String categories="";
                    for (JSONObject entry : arxivCategoryList) {
                        categories+="|"+entry.toString();
                    }
                    if (categories.length()>1) {
                        person.put("categories", categories.substring(1));
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    
}

 
