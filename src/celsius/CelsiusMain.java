/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius;

import celsius.data.Attachment;
import celsius.data.Item;
import celsius.gui.MainFrame;
import celsius.data.Library;
import celsius.data.TableRow;
import celsius.tools.FileTools;
import celsius.tools.JSONParser;
import celsius3.Library3;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author cnsaeman
 */
public class CelsiusMain {

    public static MainFrame MF;
    public static Resources RSC;

    private static SplashScreen StartUp;
    
    public static void doWork() {
        String mainFile="/home/cnsaeman/Celsius4/Celsius4/test.txt";
        String contents=TextFile.ReadOutFile(mainFile);
        JSONParser jp=new JSONParser(contents);
        jp.moveToNextTag("authors");
        ArrayList<JSONParser> authorsArray=jp.extractArray();
        String authors = "";
        for (JSONParser author : authorsArray) {
            String bai = author.extractStringFromNextTag("value");
            // ref for author could be empty
            String ref=author.extractStringFromNextTag("$ref");
            String inspirekey=null;
            if (ref!=null) {
                inspirekey=Parser.cutFrom(ref, "https://inspirehep.net/api/authors/");
            }
            String fullname = author.extractStringFromNextTag("full_name");
            authors += "|" + fullname;
            if (bai!=null) {
                authors += "#inspirebai::" + bai;
            }
            if (inspirekey!=null) {
                authors += "#inspirekey::" + inspirekey;
            }
        }
        System.out.println(authors);
        /*String url="jdbc:sqlite:"+mainLibraryFile;
        try {
            Connection dbConnection = DriverManager.getConnection(url);
            boolean locked=false;
            try {
                dbConnection.prepareStatement("BEGIN EXCLUSIVE").execute();
                dbConnection.prepareStatement("COMMIT").execute();
            } catch (Exception e) {
                locked=true;
            }
            System.out.println("Locked status: "+locked);
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        /*RSC.guiNotify=false;

        try {
            Library library = new Library("/home/cnsaeman/Celsius4/Libraries/MathsPhys", RSC);
            library.dbConnection.setAutoCommit(false);
            ResultSet rs = library.executeResEX("SELECT id,search,attributes FROM items;");
            while (rs.next()) {
                Item item = new Item(library, rs);
                if (item.properties.containsKey("bibex")) {
                    System.out.println("BIBEX:"+item.get("search"));
                    item.put("bibex", null);
                }
                if (item.properties.containsKey("attachment-source-0")) {
                    System.out.println("ATTACHMENTS:"+item.get("search"));
                    for (String key : item.properties.keySet()) {
                        if (key.startsWith("attachment-")) item.put(key,null);
                    }
                }
                if (item.needsSaving()) {
                    System.out.println("SAVING");
                    item.save();
                }
            }
            library.dbConnection.commit();
            library.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }*/
        
        /*try {
            Library library=new Library("/home/cnsaeman/Celsius4/Libraries/MathsPhys",RSC);
            library.dbConnection.setAutoCommit(false);
            ResultSet rs=library.executeResEX("SELECT id,path FROM attachments;");
            while (rs.next()) {
                String fn=Parser.replace(rs.getString(2),"LD::","/home/cnsaeman/Celsius4/Libraries/MathsPhys/");
                try {
                    String md5=TextFile.md5checksum(fn);
                    System.out.println(md5);
                    if (md5!=null) {
                        library.executeEX("UPDATE attachments SET md5=? WHERE id=?;",new String[]{md5,rs.getString(1)});
                    }
                } catch (Exception ex) {
                    RSC.outEx(ex);
                }
            }
            library.dbConnection.commit();
            library.dbConnection.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }*/
        System.exit(0);
    }


    public static void main(String args[]) throws Exception {
        RSC=new Resources();
        double gSF;
        if ((args.length>0) && (args[0].startsWith("scale="))) {
            gSF=Double.valueOf(Parser.cutFrom(args[0],"scale="));
        } else {
            gSF=1;
        }
        gSF=1.3;        
        System.out.println("Celsius "+RSC.VersionNumber);
        RSC.guiScaleFactor=gSF;
        RSC.initResources();
        RSC.logLevel=99;
        
        if (3>4) {
            doWork();
        } else {
            RSC.setLookAndFeel();
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    StartUp = new SplashScreen(RSC.VersionNumber, true, RSC);
                    StartUp.setStatus("Initializing Resources...");

                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            StartUp.setStatus("Creating Main Frame...");
                        }
                    });
                    MF = new MainFrame();
                    MF.StartUp = StartUp;
                    MF.RSC = RSC;

                    RSC.setMainFrame(MF);
                    StartUp.setStatus("Loading Plugins...");
                    RSC.loadPlugins();
                    StartUp.setStatus("Laying out GUI...");
                    MF.gui1();
                    RSC.guiInformationPanel = MF.guiInfoPanel;
                    StartUp.setStatus("Setting Shortcuts...");
                    RSC.loadShortCuts();
                    MF.setShortCuts();
                    StartUp.setStatus("Loading Libraries...");
                    RSC.loadLibraries();
                    StartUp.setStatus("Final gui...");
                    MF.gui2();
                }
            });
        }
     
    }


}


/* Old script code

        try {
            Library library=new Library("/home/cnsaeman/Celsius4/Libraries/MathsPhys",RSC);
            library.dbConnection.setAutoCommit(false);
            ResultSet rs=library.executeResEX("SELECT items.id, items.created, attachments.path FROM item_attachment_links JOIN items on items.id=item_id JOIN attachments on attachments.id=attachment_id;");
            while (rs.next()) {
                String fn=Parser.replace(rs.getString(3),"LD::","/home/cnsaeman/Celsius4/Libraries/MathsPhys/");
                try {
                    Path file = Paths.get(fn);
                    BasicFileAttributes attr = Files.readAttributes(file, BasicFileAttributes.class);
                    long ts = attr.lastModifiedTime().toMillis() / 1000;
                    long cts = rs.getLong(2);
                    if (ts < cts) {
                        System.out.println("Changing date from " + RSC.SDF.format(new Date(cts * 1000)) + " to " + attr.lastModifiedTime());
                        library.executeEX("UPDATE items SET created=" + String.valueOf(ts) + " WHERE id=" + rs.getString(1) + ";");
                    } else {
                        System.out.println("Time ok");
                    }
                } catch (Exception ex) {
                    RSC.outEx(ex);
                }
            }
            library.dbConnection.commit();
            library.dbConnection.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }


        Library3.convertLib(MF, "/home/cnsaeman/Celsius4/Libraries/MathsPhys.xml", RSC);
        
        Library3.convertLib(MF, "/home/cnsaeman/Celsius4/Libraries/SmallTester.xml", RSC);
        
        System.out.println("Parsing");
        String in="{\"updated\":\"2021-06-25T18:31:45.428979+00:00\",\"metadata\":{\"citation_count_without_self_citations\":0,\"citation_count\":1,\"publication_info\":[{\"year\":2020,\"artid\":\"425201\",\"material\":\"publication\",\"journal_issue\":\"42\",\"journal_title\":\"J.Phys.A\",\"journal_volume\":\"53\",\"pubinfo_freetext\":\"J. Phys. A: Math. Theor. 53 425201 (2020)\"}],\"core\":true,\"dois\":[{\"value\":\"10.1088/1751-8121/abb6b0\",\"source\":\"arXiv\",\"material\":\"publication\"}],\"titles\":[{\"title\":\"Complex (super)-matrix models with external sources and $q$-ensembles of Chern–Simons and ABJ(M) type\",\"source\":\"IOP\"},{\"title\":\"A Chern-Simons theory view of noncommutative scalar field theory\",\"source\":\"arXiv\"},{\"title\":\"Complex (super)-matrix models with external sources and $q$-ensembles of Chern-Simons and ABJ(M) type\",\"source\":\"arXiv\"}],\"$schema\":\"https://inspirehep.net/schemas/records/hep.json\",\"authors\":[{\"ids\":[{\"value\":\"0000-0002-2497-4868\",\"schema\":\"ORCID\"},{\"value\":\"L.Santilli.1\",\"schema\":\"INSPIRE BAI\"}],\"uuid\":\"ab4ceabc-fe8c-469a-8031-d8da3a67dda1\",\"emails\":[\"lsantilli@fc.ul.pt\"],\"record\":{\"$ref\":\"https://inspirehep.net/api/authors/1778005\"},\"full_name\":\"Santilli, Leonardo\",\"affiliations\":[{\"value\":\"Lisbon U.\",\"record\":{\"$ref\":\"https://inspirehep.net/api/institutions/905609\"}}],\"signature_block\":\"SANTALl\",\"curated_relation\":true,\"raw_affiliations\":[{\"value\":\"Grupo de Física Matemática, Departamento de Matemática, Faculdade de Ciências, Universidade de Lisboa, Campo Grande, Edifício C6, 1749-016 Lisboa, Portugal\"}]},{\"ids\":[{\"value\":\"0000-0002-4579-4364\",\"schema\":\"ORCID\"},{\"value\":\"M.Tierz.1\",\"schema\":\"INSPIRE BAI\"}],\"uuid\":\"11314a4f-396d-43d2-93fc-3c845e154e9f\",\"emails\":[\"tierz@fc.ul.pt\"],\"record\":{\"$ref\":\"https://inspirehep.net/api/authors/1045400\"},\"full_name\":\"Tierz, Miguel\",\"affiliations\":[{\"value\":\"Lisbon U.\",\"record\":{\"$ref\":\"https://inspirehep.net/api/institutions/905609\"}}],\"signature_block\":\"TARm\",\"curated_relation\":true,\"raw_affiliations\":[{\"value\":\"Departamento de Matemática, Faculdade de Ciências, ISCTE—Instituto Universitário de Lisboa, Avenida das Forças Armadas, 1649-026 Lisboa, Portugal\"},{\"value\":\"Grupo de Física Matemática, Departamento de Matemática, Faculdade de Ciências, Universidade de Lisboa, Campo Grande, Edifício C6, 1749-016 Lisboa, Portugal\"}]}],\"curated\":true,\"figures\":[{\"key\":\"759eb07502df8dd7fcc94afda3fce809\",\"url\":\"https://inspirehep.net/files/759eb07502df8dd7fcc94afda3fce809\",\"source\":\"arxiv\",\"caption\":\"$-\\\\frac{1}{N^3} \\\\ln \\\\frac{ \\\\mz_{\\\\mathrm{LSZ}}  ( \\\\emptyset, \\\\emptyset ) }{ \\\\mathcal{B}_N \\\\mz_{\\\\mathrm{SW}} }$ as a function of the coupling $g_s$ at $m^2=0$. The curves correspond to $N$ from $2$ to $7$.\",\"filename\":\"LSZvsSWm0.png\"},{\"key\":\"07fbe3c754bc73d5350b80bc085b96f4\",\"url\":\"https://inspirehep.net/files/07fbe3c754bc73d5350b80bc085b96f4\",\"source\":\"arxiv\",\"caption\":\"$-\\\\frac{1}{N^3} \\\\ln \\\\frac{ \\\\mz_{\\\\mathrm{LSZ}}  ( \\\\emptyset, \\\\emptyset ) }{ \\\\mathcal{B}_N \\\\mz_{\\\\mathrm{SW}} }$ as a function of the coupling $g_s$ at $m^2=2$ (left) and $m^2 = \\\\frac{N}{2} +1 $ (right). The curves correspond to $N$ from $2$ to $7$.\",\"filename\":\"LSZvsSWm2.png\"},{\"key\":\"b204292bf3b48d2f92de4c45207ddd50\",\"url\":\"https://inspirehep.net/files/b204292bf3b48d2f92de4c45207ddd50\",\"source\":\"arxiv\",\"caption\":\"$-\\\\frac{1}{N^3} \\\\ln \\\\frac{ \\\\mz_{\\\\mathrm{LSZ}}  ( \\\\emptyset, \\\\emptyset ) }{ \\\\mathcal{B}_N \\\\mz_{\\\\mathrm{SW}} }$ as a function of the coupling $g_s$ at $m^2=2$ (left) and $m^2 = \\\\frac{N}{2} +1 $ (right). The curves correspond to $N$ from $2$ to $7$.\",\"filename\":\"LSZvsSWmNhalf.png\"}],\"license\":[{\"url\":\"http://arxiv.org/licenses/nonexclusive-distrib/1.0/\",\"license\":\"arXiv nonexclusive-distrib 1.0\",\"material\":\"preprint\"}],\"texkeys\":[\"Santilli:2018ilo\"],\"citeable\":true,\"imprints\":[{\"date\":\"2020-10-02\"}],\"keywords\":[{\"value\":\"field theory: scalar\",\"schema\":\"INSPIRE\"},{\"value\":\"gauge field theory: abelian\",\"schema\":\"INSPIRE\"},{\"value\":\"dimension: quantum\",\"schema\":\"INSPIRE\"},{\"value\":\"sphere: fuzzy\",\"schema\":\"INSPIRE\"},{\"value\":\"Chern-Simons term\",\"schema\":\"INSPIRE\"},{\"value\":\"matrix model\",\"schema\":\"INSPIRE\"},{\"value\":\"Wess-Zumino-Witten model\",\"schema\":\"INSPIRE\"},{\"value\":\"partition function\",\"schema\":\"INSPIRE\"},{\"value\":\"noncommutative\",\"schema\":\"INSPIRE\"},{\"value\":\"modular\",\"schema\":\"INSPIRE\"},{\"value\":\"U(N)\",\"schema\":\"INSPIRE\"},{\"value\":\"Hopf\",\"schema\":\"INSPIRE\"}],\"refereed\":true";
        JSONParser jp=new JSONParser(in);
        System.out.println(jp.moveToFirstTag("keywords"));
        jp.restrictLevel();
        System.out.println(in.substring(jp.upperBound-10,jp.upperBound+10));
        String keys = "";
        while (jp.moveToNextTag("value")) {
            String key = jp.extractStringFromNextTag("value");
            keys += "|" + key;
        }
        if (keys.length()>0) keys = keys.substring(1);
        System.out.println(keys);*/

        //Library library=new Library("/home/cnsaeman/Celsius3/Celsius4/Libraries/SmallTester.clf",RSC);
        
        /*Item item=new Item(library);
        item.put("title","testing 1500");
        item.put("authors","Schambes, TheYounger #inspirebai:Schambers.T.Y|Schambes, TheOlder#orcid:0032-1232-4432-1123|AnotherSchambes, TheMiddleOne#orcid:0032-1232-4432-1125");
        item.put("keywords","string model 2|crazy model|crazy model 3");
        item.save();*/
        
        /*Item item=new Item(library,1022);
        item.put("authors","Saemann, Christian");
        item.save();*/
        
        /*Item item=new Item(library,1022);
        item.loadFullData();
        item.put("categories","id:1|label:stuss");
        item.save();
        
        library.close();


*/