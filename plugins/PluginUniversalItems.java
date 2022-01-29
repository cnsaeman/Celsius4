import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.io.*;
import celsius.data.*;
import atlantis.tools.*;
import celsius.tools.ToolBox;

/**
 * @author cnsaeman
 * 
 * To include:
 * http://api.semanticscholar.org/v1/paper/6162a894c2481599a92e4adaa06d08c200d8c31e
 * 
 */
public class PluginUniversalItems extends Thread {
    
    public static final HashMap<String,String> metaData=new HashMap<String,String>() {
        {
            put("title"             ,"Universal item Plugin");
            put("author"            ,"Christian Saemann");
            put("version"           ,"4.0");
            put("help"              ,"This plugin tries to retrieve all possible information from offline and online sources.");
            put("needsFirstPage"    ,"no");
            put("wouldLikeFirstPage","yes");
            put("longRunTime"       ,"no");
            put("requiredFields"    ,"");
            put("type"              ,"auto-items|manual-items");
            put("defaultParameters" ,"");
            put("parameter-help"    ,"none.");
        }
    };

    private static final Pattern JSTORPattern = Pattern.compile(":\\/\\/www.jstor.org\\/stable\\/(\\d+)");
    
    // to do:
    // arXiv: check that any new information can be gained from obtaining arXiv info
    
    private final String TI="P:Un>";
    
    private String oai;
    
    private boolean debug;
    private BibTeXRecord BTR;
    private boolean BTRjustCreated;

    public Item item;
    private Attachment attachment;
    public HashMap<String,String> communication;
    public ArrayList<String> Msgs;

    public void Initialize(celsius.data.TableRow tr, HashMap<String,String> com, ArrayList<String> m) {
        item = (Item)tr;
        attachment=null;
        if (item.linkedAttachments.size()>0) attachment=item.linkedAttachments.get(0);
        communication=com;
        Msgs = m;
    }
        
    public void run() {
        System.out.println("Entering 2");
        debug=false;
        Msgs.add("Init");
        initializeThings();
        Msgs.add("FN");
        if (attachment!=null) {
            getFromFileName();
            Msgs.add("FP");
            if (!attachment.isEmpty("$plaintext")) getFromFirstPage();
        }
        Msgs.add("PE");
        if (containsKey("projecteuclid")) getFromProjectEuclid();
        Msgs.add("ArX");
        if (containsKey("arxiv-ref")) getFromArXiv();
        Msgs.add("INSP");
        if (containsKey("inspirekey") || containsKey("arxiv-ref") || containsKey("doi")) getFromInspire();
        Msgs.add("DOI");
        getFromDoiNew();
        completeBibTeX();
        completeIdentifier();
    }
    
    public void initializeThings() {
        if ((getS("authors").indexOf(" ")==0) || (getS("authors").length()<5)) putS("authors","<unknown>");
        if ((getS("title").indexOf(" ")==0) && (getS("title").length()<5)) putS("title","<unknown>");
        if (getS("bibtex").length()>5) {
            BTR=new BibTeXRecord(getS("bibtex"));
            BTRjustCreated=false;
        } else {
            BTR=new BibTeXRecord();
            BTRjustCreated=true;
        }
    }
    
    public void getFromFileName() {
        String path=attachment.get("path");
        // extract potential arXiv infomation from file name, do not overwrite.
        if (item.isEmpty("arxiv-ref") && !item.properties.containsKey("arxiv-ref")) {
            String fn=Parser.cutUntilLast(Parser.cutFromLast(path,"/"),".");
            if (fn.matches("\\d{4}\\.\\d{4,5}")) {
                Msgs.add("arxiv-ref::"+fn);
                item.put("arxiv-ref",fn);
            }                 
        }
        if (getS("authors").equals("<unknown>") || getS("title").equals("<unknown>")) {
            String FileName = Parser.cutFromLast(path, arxivTools.filesep);
            if (FileName.startsWith("euclid.")) {
                putS("projecteuclid",Parser.replace(Parser.cutFrom(Parser.cutUntilLast(FileName,"."),"."),".","/"));
            } else if (FileName.startsWith("[")) {
                String authors=Parser.replace(Parser.cutUntil(FileName.substring(1),"]"),"_"," ").trim();
                authors=Parser.replace(authors,","," and ");
                authors=BibTeXRecord.convertBibTeXAuthorsToCelsius(authors);
                String title=Parser.replace(Parser.cutFrom(FileName,"]"),"_"," ").trim();
                title=Parser.replace(title,"(1)","");
                title=Parser.cutUntil(title,"(");
                if ((title.length() > 3) && (authors.length() > 1)) {
                    putS("type", "Book");
                    putS("title", title);
                    putS("authors", authors);
                    putS("recognition", "50");
                }
            } else if ((FileName.indexOf("(") > 0) || (FileName.indexOf(" - ")>0)) {
                if (FileName.indexOf(")(") > 0) {
                    // System.out.println("Type 0::"+FileName);
                    String title = Parser.cutUntil(FileName, "(").trim();
                    String authors = Parser.cutUntilLast(title, ". ") + ".";
                    title = Parser.cutFromLast(title, ". ");
                    StringBuffer sb = new StringBuffer(authors);
                    while (sb.indexOf(".") > 2) {
                        sb.delete(sb.indexOf(".") - 1, sb.indexOf(".") + 1);
                    }
                    authors = sb.toString().replace(" ,", ",").trim();
                    // Entries correct -> make the necessary entries
                    if ((title.length() > 3) && (authors.length() > 1)) {
                        putS("title", title);
                        putS("authors", authors);
                        putS("recognition", "50");
                    }
                } else {
                    // Filename is of the form "authorname, authorfirstname - title.filetype"
                    // System.out.println(FileName+"::"+String.valueOf(FileName.indexOf(" - "))+"::"+String.valueOf(FileName.indexOf(" - ")));
                    if ((FileName.indexOf(" - ")>FileName.indexOf(", "))) {
                        String title = Parser.cutUntilLast(Parser.cutFrom(FileName, " - "),".").trim();
                        String authors = Parser.cutUntil(FileName, " - ").trim();
                        if ((title.length() > 3) && (authors.length() > 1)) {
                            item.put("title", title);
                            item.put("authors", authors);
                            item.put("recognition", "50");
                        }
                    } else {
                        // System.out.println("Type 3::"+FileName);
                        // FileName is of the form "title (authors)"
                        String title = Parser.cutUntilLast(FileName, "(").trim().replace("_", " ");
                        String authors = Parser.cutUntil(Parser.cutFromLast(FileName, "("), ")").replace("_", " ");
                        authors = authors.replace(',', '|');
                        authors = authors.replace("| ", "|");
                        // Entries correct -> make the necessary entries
                        if ((title.length() > 3) && (authors.length() > 1)) {
                            item.put("title", title);
                            item.put("authors", authors);
                            item.put("recognition", "50");
                        }
                    }
                }
            }
        }
    }
    
    public void getFromFirstPage() {
        System.out.println("Getting first page");
        String plaintextfile=attachment.get("$plaintext");
        System.out.println(plaintextfile+"\n---\n");
        if (plaintextfile.equals("")) return;
        String FirstPage=ToolBox.getFirstPage(item.library.completeDir(plaintextfile));
        //System.out.println(FirstPage);
        // extract potential arXiv infomation from first page, do not overwrite.
        if ((!containsKey("arxiv-ref") || item.getS("arxiv-ref").equals("")) && (FirstPage.indexOf("arXiv:")>-1)) {
            ArrayList<String> T=arxivTools.GetArXivTags(FirstPage);
            String arx=T.get(0);
            String nmb=T.get(1);
            item.put("arxiv-name",arx);
            item.put("arxiv-number",nmb);
            String tmp=nmb;
            // Math arxives correction
            if (arx.indexOf(".")>-1) arx=Parser.cutUntil(arx,".");
            if (nmb.indexOf(".")==-1) tmp=arx+"/"+nmb;
            item.put("arxiv-ref",tmp);
        } else if (FirstPage.indexOf("www.jstor.org/stable/")>-1) {
            getFromJSTOR(FirstPage);
        } else {
            Pattern pattern=Pattern.compile("[Dd][Oo][Ii][: ]+(\\S+\\/\\S+)");
            Matcher matcher=pattern.matcher(FirstPage);
            if (matcher.find()) {
                item.put("doi",matcher.group(1));
            }
        }
    }
    
    // todo: extract primary arXiv, in particular for maths
    public void getFromArXiv() {
        String ref=getS("arxiv-ref");
        if (ref.isEmpty()) return;
        String arx="";
        String nmb="";
        String tmp;
        // set arxiv-name and number correctly
        if(ref.indexOf('/')>-1) {
            // old format
            arx=Parser.cutUntil(ref,"/");
            nmb=Parser.cutFrom(ref,"/");
            tmp=arx+"/"+nmb;
        } else {
            // new format
            arx="unknown";
            nmb=ref;
            tmp=nmb;
        }
        if (blank(getS("arXiv-name"))) item.put("arxiv-name",arx);
        if (blank(getS("arXiv-number"))) item.put("arxiv-number",nmb);
        String tag=arx+nmb;   // tag for naming temp files
        Msgs.add("arXiv-ref: "+item.get("arxiv-ref")+" :: TAG: "+tag+" :: ident: "+tmp);
        try {
            Msgs.add("Getting data from arXiv :: "+tmp);
            
            oai=TextFile.ReadOutURL("http://export.arxiv.org/oai2?verb=GetRecord&identifier=oai:arXiv.org:"+tmp+"&metadataPrefix=arXiv");
            //System.out.println(oai);
            
            if (!oai.startsWith("##??")) {
                SPut("title");
                //System.out.println("TITLE: "+item.get("title"));
                SPut("abstract");
                SPut("created");
                SPut("updated");
                SPut("categories");
                SPut("journal-ref");
                tmp=arXivExtract("doi");
                SPut("doi");
                if (!blank(tmp)) putS("doi",tmp);
                tmp=arXivExtract("comments");
                if (!blank(tmp)) putS("remarks","Comments: "+tmp);
                tmp=arXivExtract("categories");
                if (!blank(tmp)) putS("arxiv-name",Parser.cutUntil(tmp," "));
                String authors=arXivExtract("authors");
                tmp=new String();
                //System.out.println("A");
                authors=Parser.cutFrom(authors,"<author><keyname>");
                while (authors.length()>0) {
                    tmp+="|"+Parser.cutUntil(authors,"</keyname>")+", ";
                    authors=Parser.cutFrom(authors,"<forenames>");
                    tmp+=Parser.cutUntil(authors,"</forenames>");
                    authors=Parser.cutFrom(authors,"<author><keyname>");
                }
                putS("authors",Parser.decodeHTML(tmp.substring(1)));
                putS("type","Preprint");
                if (getS("remarks").toLowerCase().indexOf("talk")>-1) putS("type","Talk");
                if (getS("abstract").toLowerCase().indexOf("talk")>-1) putS("type","Talk");
                if (getS("remarks").toLowerCase().indexOf(" thesis")>-1) putS("type","Thesis");
                if (getS("abstract").toLowerCase().indexOf(" thesis")>-1) putS("type","Thesis");
                if (getS("remarks").toLowerCase().indexOf("lecture")>-1) putS("type","Lecture Notes");
                if (getS("abstract").toLowerCase().indexOf("lecture")>-1) putS("type","Lecture Notes");
                if (blank(BTR.getTag())) BTR.setTag(Parser.cutUntil(getS("authors"),",")+":"+getS("arxiv-number"));
                if (blank(BTR.get("title"))) BTR.put("title",getS("title"));
                if (blank(BTR.get("author"))) BTR.put("author",Parser.replace(getS("authors"),"|"," and "));
                if (blank(BTR.get("archiveprefix"))) BTR.put("archiveprefix","arXiv");
                if (blank(BTR.get("eprint"))) BTR.put("eprint",getS("arxiv-ref"));
                if (blank(BTR.get("primaryclass"))) BTR.put("primaryclass",getS("arxiv-name"));
                if (item.get("journal-ref")!=null) {
                    BTR.put("doi",getS("doi"));
                    String jref=getS("journal-ref");
                    if (jref.length()>4) {
                        putS("type","Paper");
                    }
                    
                    // Extract bibtex info from arXiv records 
                    Matcher myear = Pattern.compile("\\((\\d\\d\\d\\d)\\)").matcher(jref);
                    // Find year of the form (yyyy)
                    if (myear.find()) {
                        // Match following type: J. Topol. 8 (2015), no. 4, 1045-1084
                        String beforeYear=jref.substring(0,myear.start()).trim();
                        String afterYear=jref.substring(myear.end()).trim();
                        BTR.put("year",myear.group(1));
                        Matcher mvol = Pattern.compile("\\s([A-Z]?\\d+)").matcher(beforeYear);
                        if (mvol.find()) {
                            if (blank(BTR.get("volume"))) BTR.put("volume",mvol.group(1).trim());
                            if (blank(BTR.get("journal"))) BTR.put("journal",beforeYear.substring(0,mvol.start()).trim());
                        }
                        Matcher mpages = Pattern.compile("[,\\s](\\d+-\\d+)").matcher(afterYear);
                        if (mpages.find()) {
                            if (blank(BTR.get("pages"))) BTR.put("pages",mpages.group(1).trim());
                        }
                    } else {
                        // Match following types: JHEP 0403:048,2004   J.Math.Phys.54:013507,2013    Nucl.Phys.B826:456-489,2010
                        Matcher mjhep = Pattern.compile("([A-Z]?\\d+):([\\d-]+)\\s?,\\s?(\\d\\d\\d\\d)").matcher(jref);
                        if (mjhep.find()) {
                            if (blank(BTR.get("journal"))) BTR.put("journal",jref.substring(0,mjhep.start()).trim());
                            if (blank(BTR.get("volume"))) BTR.put("volume",mjhep.group(1).trim());
                            if (blank(BTR.get("pages"))) BTR.put("pages",mjhep.group(2).trim());
                            if (blank(BTR.get("year"))) BTR.put("year",mjhep.group(3).trim());
                        }
                    }
                }
            } else {
                Msgs.add(TI+"Retrieving data from arXiv failed :: "+oai);
                Msgs.add(TI);
            }                       
            
        } catch (Exception e) {
            Msgs.add(e.toString());
        }
        putS("recognition","50");
    }
    
    public void getFromInspire() {
        try {
            Msgs.add("Trying inspire...");
            String inspireRecord="";
            // read inspire record
            if (item.isEmpty("inspirekey") && !item.isEmpty("arxiv-ref")) {
                Msgs.add("Getting from arxiv-ref");
                inspireRecord=webToolsHEP.jsonFromInspire("arxiv",item.get("arxiv-ref"));
                // Uncomment for debugging...
                //Msgs.add("Response: "+inspireRecord);
            } else if (item.isEmpty("inspirekey") && !item.isEmpty("doi")) {
                Msgs.add("Getting from doi");
                inspireRecord=webToolsHEP.jsonFromInspire("doi",item.get("doi"));
                Msgs.add("Response: "+inspireRecord);
            } else if (!item.isEmpty("inspirekey")) {
                Msgs.add("Getting from inspirekey");
                inspireRecord=webToolsHEP.jsonFromInspire("literature",item.get("inspirekey"));
                Msgs.add("Response: "+inspireRecord);
            }
            if (inspireRecord.length()>2) {
                Msgs.add("XXI Inspire record found");
                if (item.isEmpty("inspirekey")) {
                    String inspirekey=Parser.cutUntil(Parser.cutFrom(inspireRecord,"\"control_number\":"),",");
                    Msgs.add("Inspire key missing, identified as "+inspirekey);
                    item.put("inspirekey",inspirekey);
                }
                JSONParser jp=new JSONParser(inspireRecord);                
                jp.moveToNextTag("authors");
                ArrayList<JSONParser> authorsArray=jp.extractArray();
                Msgs.add("XXF Found "+String.valueOf(authorsArray.size())+" authors");
                String authors = "";
                for (JSONParser author : authorsArray) {
                    String bai = author.extractStringFromNextTag("value");
                    // ref for author could be empty
                    String ref=author.extractStringFromNextTag("$ref");
                    String inspirekey=null;
                    if (ref!=null) {
                        inspirekey=Parser.cutFrom(ref, "https://inspirehep.net/api/authors/");
                    }
                    String fullname = author.extractStringFromTag("full_name");
                    authors += "|" + fullname;
                    if (bai!=null) {
                        authors += "#inspirebai::" + bai;
                    }
                    if (inspirekey!=null) {
                        authors += "#inspirekey::" + inspirekey;
                    }
                }
                authors=authors.substring(1);
                Msgs.add(authors);
                if (item.id==null || item.id.equals("")) {
                    item.putS("authors",authors);
                    Msgs.add("authors written: "+item.getS("authors"));
                }
                
                jp.moveToFirstTag("references");
                jp.restrictLevel();
                String refs="";
                while (jp.moveToNextTag("$ref")) {
                    String ref=jp.extractStringFromNextTag("$ref");
                    refs+="|inspirekey:"+ref.substring(38);
                }
                if (refs.length()>1) {
                    refs=refs.substring(1);
                    putS("references",refs);
                }
                jp.releaseLevel();

                jp.moveToFirstTag("keywords");
                jp.restrictLevel();
                String keys="";
                while (jp.moveToNextTag("value")) {
                    String key=jp.extractStringFromNextTag("value");
                    keys+="|"+key;
                }
                if (keys.length()>0) {
                    keys=keys.substring(1);
                    putS("keywords",keys);
                }
                jp.releaseLevel();
                
                jp.moveToFirstTag("abstracts");
                //Abstract
                if (item.isEmpty("abstract")) {
                    String abs=jp.extractStringFromNextTag("value");                
                    putS("abstract",abs);
                }
                
                //BibTeX
                Msgs.add("inspireBIB");
                String bt = webToolsHEP.bibTeXFromInspire(getS("inspirekey"));
                Msgs.add("Found:\n"+bt);
                bt = "@" + Parser.cutFrom(bt, "@");
                Msgs.add("before");
                BibTeXRecord BTRInsp = new BibTeXRecord(bt);
                Msgs.add("after");
                Msgs.add("BibTeX accepted");
                
                // overwrite doi results if empty
                if (blank(BTR.get("journal")) && !blank(BTRInsp.get("journal"))) {
                    BTR.put("journal", BTRInsp.get("journal"));
                    BTR.put("year", BTRInsp.get("year"));
                }
                
                String[] copies = {"title","author","booktitle","pages", "volume", "doi", "slaccitation", "school", "eprint", "arXiv"};
                for (String s : copies) {
                    if (blank(BTR.get(s)) && !blank(BTRInsp.get(s))) {
                        BTR.put(s, BTRInsp.get(s));
                    }
                }
                
                if ((blank(BTR.get("primaryclass")) || (BTR.get("primaryclass").equals("unknown"))) && !blank(BTRInsp.get("primaryclass"))) {
                    BTR.put("primaryclass", BTRInsp.get("primaryclass"));
                }
                
                // Item type
                if (!blank(BTR.get("journal"))) {
                    putS("type", "Paper");
                } if (BTRInsp.type.equals("phdthesis")) {
                    BTR.type="phdthesis";
                    putS("type", "Thesis");
                }
                else {
                    putS("type", "Preprint");
                }
                
                // Inspire citation tag over arxiv:
                BTR.setTag(BTRInsp.getTag());
                
                /* This is dangerous, just extracts arbitrary doi from references
                //doi, but not in BibTeX
                String doi = null;
                if (blank(BTR.getS("doi")) && (inspireRecord.indexOf("http://dx.doi.org/") > -1)) {
                    doi = Parser.cutFrom(inspireRecord, "http://dx.doi.org/");
                    doi = Parser.cutUntil(doi, "\">");
                    BTR.put("doi", doi);
                }*/
                
                // adjust title authors, if no other source
                if (invalid("title")) {
                    putS("title", BTR.get("title"));
                }
                if (item.get("title").startsWith("{")) {
                    putS("title", Parser.cutUntilLast(item.get("title").substring(1), "}"));
                }
                if (invalid("authors")) {
                    System.out.println("authors from bibtex");
                    putS("authors", BibTeXRecord.convertBibTeXAuthorsToCelsius(BTR.get("author")));
                }
                
                // adjust recognition level
                if (!item.isEmpty("title") && !item.isEmpty("authors") && !item.isEmpty("bibtex")) {
                    if ((!item.get("title").equals("<unknown>")) && (!item.get("authors").equals("<unknown>")) && (item.get("bibtex").length() > 2) && (item.getS("inspirekey").length() > 2)) {
                        putS("recognition", "100");
                    }
                }
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Msgs.add("Error in GetFromInspire : "+errors.toString());
        }
    }
    
    public void getFromDoiNew() {
        try {
            if (!item.isEmpty("doi") && invalid("BTR.pages")) {
                String doi = item.get("doi");
                BTR.put("doi",doi);
                StringBuffer rtn=new StringBuffer(4000);
                String dataPage=TextFile.ReadOutURL("https://api.crossref.org/works/"+doi);
                try {
                    TextFile tf=new TextFile("Plugin.Universal.ReturnedData.txt.tmp",false);
                    tf.putString(dataPage);
                    tf.close();
                } catch (Exception e) { 
                    StringWriter errors = new StringWriter();
                    e.printStackTrace(new PrintWriter(errors));
                    Msgs.add("Error writing: "+errors.toString());
                }
                String title=Parser.cutUntil(Parser.cutFrom(dataPage,"\"title\":[\""),"\"");
                String year=Parser.cutFrom(dataPage,"\"published-print\":{\"date-parts\":[[").substring(0,4);
                String volume=Parser.cutUntil(Parser.cutFrom(dataPage,"\"volume\":\""),"\"");
                String pages=Parser.cutUntil(Parser.cutFrom(dataPage,"\"page\":\""),"\"");
                String journal=Parser.cutUntil(Parser.cutFrom(dataPage,"\"container-title\":[\""), "\"");
                if (dataPage.indexOf("\"short-container-title\":[\"")>-1) {
                    journal=Parser.cutUntil(Parser.cutFrom(dataPage,"\"short-container-title\":[\""), "\"");
                }
                String authors="";
                String res3=Parser.cutFrom(dataPage,"\"author\":");
                while (res3.indexOf("\"given\"")>-1) {
                    int i=res3.indexOf("\"family\":\"");
                    int j=res3.indexOf("\"given\":\"");
                    if (i<j) i=j;
                    String author = Parser.cutUntil(Parser.cutFrom(res3,"\"family\":\""),"\"")+", "+Parser.cutUntil(Parser.cutFrom(res3,"\"given\":\""),"\"");
                    res3=res3.substring(i+1);
                    authors+="|"+author;
                }
                authors=authors.substring(1);
                if (invalid("title")) item.put("title",title);
                if (invalid("authors")) item.put("authors",authors);
                if (blank(BTR.get("journal"))) BTR.put("journal",journal);
                if (blank(BTR.get("volume"))) BTR.put("volume",volume);
                if (!year.equals(BTR.get("year"))) BTR.put("year",year);
                if (blank(BTR.get("pages"))) BTR.put("pages",pages);
                putS("type","Paper");
            }
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Msgs.add("Error in GetFromDoi: "+errors.toString());
        }
    }
        
    public void completeBibTeX() {
        try {
            Msgs.add("Completing BibTeX...");
            Msgs.add("OLD:"+BTR.toString());
            if (blank(BTR.get("author")) && !invalid("authors")) {
                Msgs.add("Adjusting author..."+BibTeXRecord.convertCelsiusAuthorsToBibTeX(getS("authors")));
                BTR.put("author",BibTeXRecord.convertCelsiusAuthorsToBibTeX(getS("authors")));
            }
            if (blank(BTR.get("title")) && !invalid("title")) {
                BTR.put("title",getS("title"));
            }
            String titleN=normalizeBTR(BTR.get("title"));
            if (!titleN.equals(BTR.get("title"))) BTR.put("title",titleN);
            if (blank(BTR.getTag()) && !invalid("authors")) {
                String tag=Parser.cutUntil(getS("authors"),",").replace(" ","");
                if (!blank(BTR.get("year"))) tag=tag+":"+BTR.get("year");
                if (!blank(BTR.get("pages"))) tag=tag+":"+BTR.get("pages"); else tag=tag+"aa";
                BTR.setTag(tag);
            }
            if (BTR.get("year")==null) {
                String eprint=BTR.get("eprint");
                if (eprint!=null) {
                    String tmp="";
                    int i=eprint.indexOf("/");
                    if (i>0) {
                        if (eprint.charAt(i+1)=='9') {
                            tmp="19"+eprint.substring(i+1,i+3);
                        } else {
                            tmp="20"+eprint.substring(i+1,i+3);
                        }
                    } else {
                        tmp="20"+eprint.substring(0,2);
                    }
                    BTR.put("year",tmp);
                }
            }
            if ((!blank(BTR.get("journal"))) || (!blank(getS("inspirekey")))) {
                putS("recognition", "100");
            }
            // Remove empty tags 
            BTR.entrySet().removeIf(e -> blank(e.getValue()));
            // Write, if minimum of information.
            normalizeBTRJournal();
            // normalize JHEP
            if ("JHEP".equals(BTR.get("journal"))) {
                if ((BTR.get("volume").length()==2) && (BTR.get("year").length()==4)) {
                    BTR.put("volume", BTR.get("year").substring(2,4)+BTR.get("volume"));
                }
            }
            Msgs.add("Thinking about writing BTR...");
            if (!blank(BTR.get("author")) && !blank(BTR.get("title"))) {
                Msgs.add("Entered...");
                BibTeXRecord BTRold=new BibTeXRecord(getS("bibtex"));
                Msgs.add("OLD::"+BTRold.toString());
                Msgs.add("NEW::"+BTR.toString());
                if (!BTRold.toString().equals(BTR.toString())) putS("bibtex",BTR.toString());
            }
            // adjust citation tag if necessary
            if (blank(item.getS("citation-tag")) || !item.getS("citation-tag").equals(BTR.getTag())) {
                putS("citation-tag", BTR.getTag());
            }
            Msgs.add("NEW: "+BTR.getTag());
        } catch (Exception e) {
            StringWriter errors = new StringWriter();
            e.printStackTrace(new PrintWriter(errors));
            Msgs.add("Error in completeBibTeX: "+errors.toString());
        }
    }
    
    public void completeIdentifier() {
        String tmp="";
        
        String arxref=item.get("arxiv-ref");
        String arxname=item.get("arxiv-name");
        if ((arxref!=null) && (arxname!=null)) {
            if (arxref.indexOf(arxname)>-1) {
                tmp=arxref;
            } else {
                tmp=arxref+" ["+arxname+"]";
            }
        }
        if (!blank(BTR.get("journal"))) {
            if (item.get("type").equals("Preprint")) putS("type", "Paper");
            String identifier=new String("");
            identifier=BTR.get("journal");
            if (!blank(BTR.get("volume"))) identifier+=" "+BTR.get("volume");
            if (!blank(BTR.get("year"))) identifier+=" ("+BTR.get("year")+")";
            if (!blank(BTR.get("pages"))) identifier+=" "+BTR.get("pages");
            tmp+=" "+identifier.trim();
        }
        if (blank(BTR.get("doi")) && !blank(getS("doi"))) BTR.put("doi",getS("doi"));
        
        if (BTR.get("year")!=null) tmp=BTR.get("year")+" "+tmp;
        putS("identifier", tmp.trim());
    }
    
    public void getFromProjectEuclid() {
        String url="https://projecteuclid.org/euclid."+getS("projecteuclid");
        Msgs.add("Reading out from Project Euclid: "+url);
        String info=TextFile.ReadOutURL(url);
        if (!info.startsWith("##??")) {
            if (invalid("title")) {
                putS("title",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_title\" content=\""),"\""));
                BTR.put("title",getS("title"));
            }
            if (invalid("authors")) {
                String authors="";
                String info2=new String(info);
                while(info2.indexOf("<meta name=\"citation_author\" content=\"")>-1) {
                    info2=Parser.cutFrom(info2,"<meta name=\"citation_author\" content=\"");
                    String aut=Parser.cutUntil(info2,"\"");
                    authors+="|"+Parser.cutFromLast(aut," ")+", "+Parser.cutUntilLast(aut," ");
                }
                Msgs.add("Authors: "+authors.substring(1));
                putS("authors",authors.substring(1));
            }
            putS("type", "Paper");
            if (blank(getS("abstract"))) putS("abstract",Parser.cutTags(Parser.cutFrom(Parser.cutUntil(Parser.cutFrom(info,"<div class=\"abstract-text\">"),"</p>"),"<p>")).trim());
            if (blank(BTR.get("url"))) BTR.put("url",url);
            if (blank(BTR.get("journal"))) BTR.put("journal",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_journal_title\" content=\""),"\""));
            if (blank(BTR.get("year"))) BTR.put("year",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_year\" content=\""),"\""));
            if (blank(BTR.get("volume"))) BTR.put("volume",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_volume\" content=\""),"\""));
            if (blank(BTR.get("issue"))) BTR.put("issue",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_issue\" content=\""),"\""));
            if (blank(BTR.get("pages"))) {
                BTR.put("pages",Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_firstpage\" content=\""),"\"")+"-"+Parser.cutUntil(Parser.cutFrom(info,"<meta name=\"citation_lastpage\" content=\""),"\""));
            }
        }
    }
    
    public void getFromJSTOR(String fp) {
        // find jstore number
        Matcher m = JSTORPattern.matcher(fp);
        if (m.find()) {
            String nmb = m.group(1);
            putS("JSTOR-ID", nmb);
            Parser.cutUntil(Parser.cutFrom(fp, "http://www.jstor.org/stable/"), " ");
            String title = Parser.cutFromLast(Parser.cutUntil(fp, "Author(s):").trim(), "\n").trim();
            String authors = Parser.cutUntil(Parser.cutFrom(fp, "Author(s):"), "\n").trim();
            fp = Parser.cutFrom(fp, "Source:");
            String journal = Parser.cutUntil(fp, ", ").trim();
            String vol;
            if (fp.indexOf("No.") > -1) {
                vol = Parser.cutUntil(Parser.cutFrom(fp, ", Vol."), ", No.").trim();
                fp = Parser.cutFrom(fp, ", No.");
            } else {
                vol = Parser.cutUntil(Parser.cutFrom(fp, ", Vol."), "(").trim();
            }
            String year = Parser.cutUntil(Parser.cutFrom(fp, ".,"), ")").trim();
            String pages = Parser.cutUntil(Parser.cutFrom(fp, "pp."), "Published").trim();
            if (blank(getS("title"))) putS("title", title);
            if (blank(getS("authors"))) putS("authors", BibTeXRecord.convertBibTeXAuthorsToCelsius(authors));
            if (blank(getS("type"))) putS("type", "Paper");
            if (blank(BTR.get("authors"))) {
                BTR.put("authors", authors);
            }
            if (blank(BTR.get("journal"))) {
                BTR.put("journal", journal);
            }
            if (blank(BTR.get("volume"))) {
                BTR.put("volume", vol);
            }
            if (blank(BTR.get("year"))) {
                BTR.put("volume", year);
            }
            if (blank(BTR.get("pages"))) {
                BTR.put("volume", pages);
            }
            if (blank(BTR.get("url"))) {
                BTR.put("url", "http://www.jstor.org/stable/" + nmb);
            }
            Msgs.add("Found JSTOR-ID:" + nmb);
        }
    }
    
    public String arXivExtract(String s) {
        int i=oai.indexOf("<"+s+">");
        int k=oai.indexOf("</"+s+">");
        if (i==-1) return(new String(""));
        String ret=oai.substring(i+s.length()+2,k).trim();
        ret=ret.replace("\n"," ");
        while (ret.indexOf("  ")>-1) ret=ret.replace("  "," ");
        return(ret);
    }
    
    public void SPut(String s) {
        String tmp=arXivExtract(s);
        if (!tmp.equals("")) item.put(s,tmp);
    }
    
    public void putS(String t,String v) {
        if (v!=null) item.put(t,v);
    }
    
    public String getS(String s) {
        String tmp=item.get(s);
        if (tmp==null) tmp=new String("");
        return(tmp);
    }
    
    public boolean containsKey(String s) {
        return(item.properties.containsKey(s));
    }
    
    public boolean blank(String s) {
        return(!(s != null && !s.isEmpty()));
    }
    
    public String normalizeBTR(String s) {
        // normalize things
        s=s.replaceAll("Chern","{C}hern");
        s=s.replaceAll("Simons","{S}imons");
        s=s.replaceAll("hern\\-\\{S\\}imons","hern--{S}imons");
        s=s.replaceAll("Yang","{Y}ang");
        s=s.replaceAll("Mills","{M}ills");
        s=s.replaceAll("ang\\-\\{M\\}ills","ang--{M}ills");
        s=s.replaceAll("Calabi","{C}alabi");
        s=s.replaceAll("Yau","{Y}au");
        s=s.replaceAll("alabi\\-\\{Y\\}au","alabi--{Y}au");
        s=s.replaceAll("Feynman","{F}eynman");
        s=s.replaceAll("D-brane","{D}-brane");
        s=s.replaceAll(" BV"," {BV}");
        return(s);
    }
    
    public void normalizeBTRJournal() {
        String journal=BTR.get("journal");
        if (!blank(journal)) {
            journal=journal.replaceAll("([a-zA-Z]\\.)([a-zA-Z])","$1 $2");
            journal=journal.replaceAll("Journal", "J.");
            journal=journal.replaceAll("Communications", "Commun.");
            journal=journal.replaceAll(" of ", " ");
            journal=journal.replaceAll(" in ", " ");
            journal=journal.replaceAll("Advances", "Adv.");
            journal=journal.replaceAll("Mathematics", "Math.");
            journal=journal.replaceAll("Mathematical", "Math.");
            journal=journal.replaceAll("Physics", "Phys.");
            journal=journal.replaceAll("Letters", "Lett.");
            if (!journal.equals(BTR.get("journal"))) BTR.put("journal",journal);
        }
    }
    
    public boolean invalid(String s) {
        String tmp;
        if (s.equals("title")) {
            tmp=getS(s);
            return(blank(tmp) || tmp.equals("<unknown>"));
        } 
        if (s.equals("authors")) {
            tmp=getS(s);
            return(blank(tmp) || tmp.equals("<unknown>") || (!tmp.contains(",")));
        }        
        if (s.equals("BTR.pages")) {
            return(blank(BTR.get("pages")));
        } else {
            tmp=getS(s);
            return(blank(tmp));
        }
    }
    
}

