//
// Celsius Library3 System v2
// (w) by C. Saemann
//
// Library3.java
//
// This class combines all necessary data for a library
//
// typesafesh
//
// checked 16.09.2007
//

package celsius3;

import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import celsius3.LibraryIterator3;
import celsius.gui.CelsiusTable;
import celsius.gui.MainFrame;
import celsius.gui.SafeMessage;
import celsius.data.BibTeXRecord;
import celsius.Resources;
import celsius.data.BibTeXRecord;
import celsius3.Item3;
import celsius.data.Item;
import celsius.data.Item;
import celsius.data.Library;
import celsius.data.Person;
import celsius.gui.GUIToolBox;
import celsius.tools.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultTreeModel;
import java.sql.*;
import java.util.zip.GZIPInputStream;

public final class Library3 implements Iterable<Item3> {
    
    public final String[] LibraryFields={"name","index","standardfields","tablecolumns","columnheaders","columntypes","columnsizes","autosortcolumn","people","plugins-manual","plugins-auto","plugins-export","filetypes","searchtags","hide","essential-fields","differentiating-fields","item-representation","naming-convention","choice-fields","icon-fields","icon-dictionary","default-add-method", "item-folder"};
    
    // Status messages after adding a item
    public static final String[] status={
        "Everything OK",  // 0
        "No title or author information found in record.", // 1
        "File with the same name existed. Adding aborted.", // 2
        "Error deleting item.", // 3
        "Item not found in library.", //4
        "Additional information could not be loaded in going to item.", // 5
        "Document id not found.", // 6
        "IOError with Index/addinfo file while adding item reference.", // 7
        "Error deleting library.", //8
        "Couldn't move item file", //9
        "Library3 index and data files may be out of synch." //10
    };

    public Connection conn;
    public Connection searchConn;
    public final Resources RSC;
    public XMLHandler MainFile;
    public XMLTree Structure;
    public XMLTree Rules;
    public IndexedXMLHandler Index;
    public XMLHandler PeopleRemarks;
    public XMLHandler CatRemarks;
    public XMLHandler HTMLtemplates;
    public Item3 lastAddedItem;
    private boolean Changed;
    private boolean PeopleOrKeywordsChanged;
    private boolean modfile;
    private int totalpages; // total number of pages in current library
    private int totalduration; // total duration in seconds
    // Metainformation on the Library3
    public String name;
    public String basedir;
    public String celsiusbasedir;
    public ArrayList<String> IndexTags;
    private ArrayList<String> PeopleTags;
    public ArrayList<String> Hide;
    public ArrayList<String> TableTags;
    public ArrayList<String> TableHeaders;
    public ArrayList<String> StyleSheetRules;
    public ArrayList<String> IconFields;
    public LinkedHashMap<String,ArrayList<String>> usbdrives;
    public HashMap<String,ArrayList<String>> ChoiceFields;
    public HashMap<String,String> IconDictionary;
    public ArrayList<Integer> ColumnSizes;
    public ArrayList<String> ColumnTypes;

    // Buffered information on the Library3
    public ArrayList<String> PeopleList;
    public ArrayList<String> PeopleLongList;
    public ArrayList<String> KeywordList;
    public ArrayList<String> Positions;      // List with String IDs storing the position
    public ArrayList<Integer> PositionsI;    // List with String IDs storing the position
    public HashMap<String,ArrayList<String>> Links; // Links is set from createLinksTree
    public HashMap<String,ArrayList<String>> LinksRef; // Links is set from createLinksTree

    public String CurrentCategory;
    public boolean catExists;
    public String CurrentPerson;
    public boolean personExists;
    
    public String LastErrorMessage;

    public int currentStatus;
    
    public Item3 marker;
    
    public void getFieldsFromMainFile() {
        IconFields=new ArrayList<String>();
        if (MainFile.get("icon-fields")!=null) {
            String[] iconfields=MainFile.get("icon-fields").split("\\|");
            IconFields.addAll(Arrays.asList(iconfields));
        }
        usbdrives=new LinkedHashMap<String,ArrayList<String>>();
        if (MainFile.get("usbdrives")!=null) {
            String[] usbfields=MainFile.get("usbdrives").split("\\|");
            for (int i=0;i<usbfields.length;i++) {
                ArrayList<String> list=new ArrayList<String>();
                String[] lst=Parser.cutFrom(usbfields[i],":").split("\\:");
                list.addAll(Arrays.asList(lst));
                usbdrives.put(Parser.cutUntil(usbfields[i],":"),list);
            }
        }
        ChoiceFields=new HashMap<String,ArrayList<String>>();
        if (MainFile.get("choice-fields")!=null) {
            String[] choicefields=ToolBox.stringToArray(MainFile.get("choice-fields"));
            for (int i=0;i<choicefields.length;i++) {
                String field=Parser.cutUntil(choicefields[i],":");
                String[] possibilities=Parser.cutFrom(choicefields[i], ":").split(",");
                ArrayList<String> poss=new ArrayList<String>();
                poss.addAll(Arrays.asList(possibilities));
                ChoiceFields.put(field, poss);
            }
        }
        IconDictionary=new HashMap<String,String>();
        if (MainFile.get("icon-dictionary")!=null) {
            String[] icondict=ToolBox.stringToArray(MainFile.get("icon-dictionary"));
            for (int i=0;i<icondict.length;i++) {
                String field=Parser.cutUntil(icondict[i],":");
                String value=Parser.cutFrom(icondict[i],":");
                IconDictionary.put(field,value);
            }
        }
    }
    
    /** Loads an existing library */
    public Library3(String fn,Resources rsc) {
        currentStatus=0;
        lastAddedItem=null;
        PeopleTags=null;
        PeopleOrKeywordsChanged=true;
        RSC=rsc;
        try {
            String relbase=Parser.cutUntil(fn,(new File(fn)).getName());
            if (relbase.endsWith(ToolBox.filesep)) relbase=relbase.substring(0,relbase.length()-1);
            MainFile=new XMLHandler(fn);
            
            name=MainFile.get("name");
            celsiusbasedir=Parser.cutUntilLast((new File(".")).getAbsolutePath(),".");
            basedir=MainFile.get("directory");
            if (!celsiusbasedir.endsWith(ToolBox.filesep)) celsiusbasedir+=ToolBox.filesep;
            if (!basedir.endsWith(ToolBox.filesep)) basedir+=ToolBox.filesep;
            if ((name.length()==0) || (basedir.length()==0)) {
                (new SafeMessage("The library file seems to be corrupt. Cancelling...","Warning:",0)).showMsg();
                name="??##Library3 file corrupt.";
                return;
            }
            if (!(new File(basedir)).exists())
            basedir=relbase+"/"+basedir;
            if ((new File(basedir + "/lock")).exists()) {
                int i=RSC.askQuestionAB("The library "+name+" is locked. If no other instance of Celsius is accessing it," +
                        "\nyou can select \"Ignore Lock\" and open it anyway. This can happen " +
                        "\nwhen Celsius has not been shut down properly.", "Library3 locked", "Cancel", "Ignore Lock");
                if (i==0) {
                    name="??##cancelled";
                    return;
                }
            }
            if ((new File(basedir + "/modified")).exists()) {
                (new SafeMessage("This library ("+name+") has not been closed properly. Please run the\n\"Synchronized Library3\" command as soon as possible.", "Warning:", 0)).showMsg();
            }
            Changed=false;
            loadStyleSheetRules();
            if (Changed) {
                MainFile.writeBack();
                Changed=false;
            }

            initIndexTags();

            try {
                Index=new IndexedXMLHandler(basedir+"libraryindex.xml",IndexTags);
            } catch (Exception e) {
                e.printStackTrace();
                RSC.out(Index.lastError);
            }
            if (Index.lastError.length()>0) {
                RSC.showWarning(Index.lastError, "Error in library index");
                RSC.out(Index.lastError);
            }

            if ((new File(basedir+"authorremarks.xml")).exists()) {
                FileTools.moveFile(basedir+"authorremarks.xml", basedir+"peopleremarks.xml");
            }

            try {
                PeopleRemarks=new XMLHandler(basedir+"peopleremarks.xml");
            } catch(IOException e) {
                XMLHandler.Create("celsiusv3.0.peopleremarksfile",basedir+"peopleremarks.xml");
                PeopleRemarks=new XMLHandler(basedir+"peopleremarks.xml");
            }
            try {
                CatRemarks=new XMLHandler(basedir+"categoryremarks.xml");
            } catch(IOException e) {
                XMLHandler.Create("celsiusv3.0.categoryremarksfile",basedir+"categoryremarks.xml");
                CatRemarks=new XMLHandler(basedir+"categoryremarks.xml");
            }
            Rules=new XMLTree(basedir+"rules.xml","$full");
            Structure=new XMLTree(basedir+"librarystructure.xml","/title/");
            if ((new File(basedir+"htmltemplates.xml")).exists()) {
                HTMLtemplates=new XMLHandler(basedir+"htmltemplates.xml");
            }

            Positions=new ArrayList<String>();
            PositionsI=new ArrayList<Integer>();
            
            CurrentCategory="";

            TextFile TF=new TextFile(basedir+"lock",false);
            TF.close();
            getFieldsFromMainFile();
        } catch (Exception ex) {
            RSC.outEx(ex);
            name="??##Error"+ex.toString();
            CurrentCategory="";
        }
    }

    public boolean isPeopleOrKeywordTag(String t) {
        if (PeopleTags==null) {
            this.createPeopleTags();
        }
        if (PeopleTags.contains(t)) return(true);
        if (t.equals("keywords")) return(true);
        return(false);
    }

    public String[] listOf(String s) {
        String t=MainFile.get(s);
        if (t==null) return(new String[0]);
        return(t.split("\\|"));
    }

    private void initIndexTags() {
        Hide=new ArrayList<String>(Arrays.asList(listOf("hide")));
        IndexTags = new ArrayList<String>();
        if (MainFile.get("index") != null) {
            String[] list = listOf("index");
            IndexTags.addAll(Arrays.asList(list));
        } else {
            IndexTags.add("type");
            IndexTags.add("title");
            IndexTags.add("authors");
            IndexTags.add("identifier");
        }
        IndexTags.add("id");
        IndexTags.add("registered");
        IndexTags.add("autoregistered");
        IndexTags.add("location");
        IndexTags.add("addinfo");
        IndexTags.add("filetype");
        IndexTags.add("attributes");
    }

    public void setColumnSize(int c,int w) {
        ColumnSizes.set(c, Integer.valueOf(w));
        String s="";
        for (Integer i : ColumnSizes) {
            if (i==0) i=1;
            s+="|"+Integer.toString(i);
        }
        s=s.substring(1);
        MainFile.put("columnsizes", s);
        this.setChanged(true);
    }
    
    public void closeLibrary3() {
            FileTools.deleteIfExists(basedir + "/lock");
            FileTools.deleteIfExists(basedir + "/modified");
    }
    
    /**
     * Deletes all files associated with library
     */
    public int deleteLibrary3() {
        String TI = "LIB" + name + ">";
        try {
            RSC.out(TI + "Removing base folder " + basedir);
            if (!(FileTools.removeFolder(basedir))) {
                RSC.out(TI + "failed!");
                return (8);
            }
            RSC.out(TI + "Removing main file " + MainFile.source);
            if (!(FileTools.deleteIfExists(MainFile.source))) {
                RSC.out(TI + "failed!");
                return (8);
            }
        } catch (Exception e) {
            e.printStackTrace();
            RSC.out(TI + "failed!");
            return (8);
        }
        return (0);
    }

    public int getSize() {
        return(Index.getSize());
    }
    
    public String compressDir(String s) {
        if (s.startsWith(basedir)) return("LD::"+Parser.cutFrom(s,basedir));
        if (s.startsWith(celsiusbasedir)) return("BD::"+Parser.cutFrom(s,celsiusbasedir));
        return(s);
    }
    
    public void reloadDisplayString() {
        try {
            HTMLtemplates=new XMLHandler(basedir+"htmltemplates.xml");
        } catch(IOException e) { e.printStackTrace(); }
    }

    /**
     * Fills in an HTML template according to the properties given
     * @param s the template
     * @param properties the properties
     * @return the filled-out HTML string
     */
    private String replaceInTemplate(String s, MProperties properties) {
        String template=s;
        String line,tag,value;
        String out="";

        while (template.length() > 0) {
            line = Parser.cutUntil(template, "\n");
            template = Parser.cutFrom(template, "\n");
            if (line.startsWith("#if#")) {
                while (line.startsWith("#if#")) {
                    line=Parser.cutFrom(line,"#if#");
                    tag=Parser.cutUntil(line,"#");
                    if (tag.charAt(0)=='!') {
                        tag=tag.substring(1);
                        if ((!properties.containsKey(tag)) || (properties.get(tag).length()==0))
                            line=Parser.cutFrom(line,"#");
                        else line="";
                    } else {
                        if ((properties.containsKey(tag)) && (properties.get(tag).length()>0))
                            line=Parser.cutFrom(line,"#");
                        else line="";
                    }
                }
            } else {
                out+="\n";
            }
            if (line.trim().length() > 0) {
                for (String key : properties.keySet()) {
                    value = properties.get(key);
                    line=line.replace("#" + key + "#", value);
                    line=line.replace("#|" + key + "#", "<ul><li>"+Parser.replace(value,"|", "</li><li>")+"</li></ul>");
                    line=line.replace("#$" + key + "#", value);
                }
                out+=line;
            }
        }
        return(out.trim());
    }

    public String addLinks(String s) {
        String tmp = "";
        String a;
        try {
            while (s.indexOf(",") > 0) {
                a = Parser.cutUntil(s, ",");
                tmp += "<a href='http://$$author." + URLEncoder.encode(a, "UTF-8") + "'>" + a + "</a>, ";
                s = Parser.cutFrom(s, ",");
            }
            if (s.indexOf(" and ") == -1) {
                tmp += "<a href='http://$$author." + URLEncoder.encode(s.trim(), "UTF-8") + "'>" + s.trim() + "</a>";
            } else {
                while (s.length() > 0) {
                    a = Parser.cutUntil(s, " and ");
                    tmp += "<a href='http://$$author." + URLEncoder.encode(a, "UTF-8") + "'>" + a + "</a> and ";
                    s = Parser.cutFrom(s, " and ");
                }
            }
        } catch (UnsupportedEncodingException ex) {
        }
        return (Parser.cutUntilLast(tmp, " and "));
    }

    public String fileSize(String n) {
        long l=new File(n).length();
        if (l<1024) return(String.valueOf(l)+" Bytes");
        DecimalFormat df = new DecimalFormat("0.000") ;
        if (l<(1024*1024)) return(df.format(l/1024.0)+" KiB");
        return(df.format(l/1024.0/1024.0)+" MiB");
    }
    
    public boolean hasChanged() {
        return(Changed);
    }

    public void setChanged(boolean ch) {
        if (!Changed && ch)  {
            try {
                TextFile TF=new TextFile(basedir+"modified",false);
                TF.close();
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        if (Changed && !ch)  {
            try {
                FileTools.deleteIfExists(basedir+"modified");
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        Changed=ch;
    }


    public Iterator<Item3> iterator() {
        return(new LibraryIterator3(this));
    }

    public String completeDir(String s,String id) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.filesep)) s2=s2.substring(1);
        if (sig.equals("AI")) {
            if (s2.charAt(0)=='.') s2=s2.substring(1);
            return(basedir+"information"+ToolBox.filesep+id+"."+s2);
        }
        if (sig.equals("LD")) return(basedir+s2);
        if (sig.equals("BD")) return(celsiusbasedir+s2);
        return(s);
    }

    public void adjustStyleSheet(StyleSheet styleSheet) {
        for (String rule : StyleSheetRules) {
            styleSheet.addRule(rule);
        }
    }

    public void loadStyleSheetRules() throws IOException {
        StyleSheetRules = new ArrayList<String>();
        if (MainFile.get("style") != null) {
            TextFile style = new TextFile(completeDir(MainFile.get("style"),""));
            while (style.ready()) {
                StyleSheetRules.add(style.getString());
            }
            style.close();
        }
    }

    public DefaultComboBoxModel getTypesDCBM() {
        DefaultComboBoxModel DCBM=new DefaultComboBoxModel();
        DCBM.addElement("arbitrary");
        for (String ft : RSC.icons.Types) {
            DCBM.addElement(ft);
        }
        return(DCBM);
    }

    public Comparator getComparator(String t,boolean invertSort, int ty) {
        if (ChoiceFields.containsKey(t)) {
            return(new CompChoice(t,invertSort,ChoiceFields.get(t)));
        } 
        return (new CompTable(t, invertSort,ty));
    }

    public int getPosition(String id) {
        return(Index.getPosition(id));
    }

    public int getPosition(int id) {
        return(Index.getPosition(id));
    }

    private void createPeopleTags() {
        String[] lop=listOf("people");
        PeopleTags=new ArrayList<String>();
        PeopleTags.addAll(Arrays.asList(lop));
    }
    
    public static String formatInt3(int i) {
        String nmb="00"+String.valueOf(i);
        nmb=nmb.substring(nmb.length()-3);
        return(nmb);
    }
    
    public static String formatInt6(int i) {
        String nmb="00000"+String.valueOf(i);
        nmb=nmb.substring(nmb.length()-6);
        return(nmb);
    }
    
    // compress strings a bit
    public String preprocess(String tmp) {
        return(tmp.toLowerCase().trim().replaceAll(" +", " "));
    }
    
    public StringBuffer getPlainText(Item3 item, String tag) {
        StringBuffer buffer=new StringBuffer();
        String tmp;
        try {
            String path=completeDir(item.get(tag),item.get("id"));
            if (path==null) return(buffer);
            File f=new File(path);
            if (!f.exists()) return(buffer);
            GZIPInputStream fis = new GZIPInputStream(new FileInputStream(f));
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader br = new BufferedReader(isr);
            while ((tmp = br.readLine()) != null) {
                tmp=preprocess(tmp);
                if (!tmp.isBlank()) {
                    buffer.append(preprocess(tmp));
                    buffer.append(' ');
                }
            }
            br.close();
            isr.close();
            fis.close();
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return (buffer);
    }
    
    // todo : 
    // - implement registration in categories
    // - authors done properly
    // - pages to integers, SUM function in SQLite
    // - additional attributes in index, in particular secondary files
    // 
    
    public static void convertLib(String fn,Resources RSC) {
        try {
            Library3 Lib=new Library3(fn,RSC);
            int done=0;
            
            String folder=Parser.cutUntilLast(fn,ToolBox.filesep)+ToolBox.filesep+Lib.MainFile.get("directory");
            String dbfname=folder+ToolBox.filesep+"CelsiusLibrary.sql";
            String dbsname=folder+ToolBox.filesep+"CelsiusSearchIndex.sql";
            FileTools.deleteIfExists(dbfname);
            FileTools.deleteIfExists(dbsname);
            FileTools.makeDir(folder+ToolBox.filesep+"item-thumbnails");
            FileTools.makeDir(folder+ToolBox.filesep+"person-thumbnails");
            /*if ((new File(dbfname)).exists()) {
                toolbox.Warning(RSC.getMF(), "Library3 seems to be in SQLite format.", "Can't convert:");
                return;
            }*/
            String URL = "jdbc:sqlite:"+dbfname;
            String searchURL = "jdbc:sqlite:"+dbsname;
            Lib.conn = DriverManager.getConnection(URL);
            Lib.searchConn = DriverManager.getConnection(searchURL);
            RSC.out("Connection to SQLite has been established::"+dbfname);
            
            // bunch transactions
            Lib.conn.setAutoCommit(false);
            
            // get all the headers
            ArrayList<String> indexTags = ToolBox.stringToArrayList(Lib.MainFile.get("index"));
            boolean isArXivDB=indexTags.indexOf("inspirekey")>-1;
            if (isArXivDB) {
                indexTags.add("doi");
                indexTags.add("arxiv-ref");
                indexTags.add("arxiv-name");
                indexTags.add("arxiv-number");
                indexTags.add("bibtex");
                indexTags.add("citation-tag");
            }
            indexTags.remove("pages");
            ArrayList<String> standardTags = ToolBox.stringToArrayList(Lib.MainFile.get("standardfields"));
            ArrayList<String> peopleTags = ToolBox.stringToArrayList(Lib.MainFile.get("people"));
            ArrayList<String> excludeTags = new ArrayList<>();
            excludeTags.add("links");
            excludeTags.add("pages");
            excludeTags.add("plaintxt");
            excludeTags.add("keywords");
            excludeTags.add("fullpath");
            excludeTags.add("location");
            excludeTags.add("filetype");
            String sql;
            Statement stmt;
            boolean success;
            
            // Start conversion
            
            RSC.out("Creating search index");
            sql = "CREATE VIRTUAL TABLE search USING FTS5 (text, tokenize = 'porter ascii', content='');";
            stmt = Lib.searchConn.createStatement();
            success=stmt.execute(sql);

            RSC.out("Creating configuration file");
            sql = "CREATE TABLE IF NOT EXISTS configuration (\n    key text UNIQUE,\n";
            sql+= "    value text);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            success=stmt.execute(sql);
            String searchtags="";
            
            // Create Configuration HashMap
            HashMap<String,String> configDictionary=new HashMap<>();
            for (String tag : Lib.MainFile.XMLTags) {
                RSC.out("Writing config:"+tag);
                if (!tag.equals("differentiating-fields")) {
                    String value = Lib.MainFile.get(tag);
                    if (tag.equals("standardfields")) tag="standard-item-fields";
                    if (tag.equals("columnheaders")) tag="item-table-column-headers";
                    if (tag.equals("columntypes")) tag="item-table-column-types";
                    if (tag.equals("columnsizes")) tag="item-table-column-sizes";
                    if (tag.equals("tablecolumns")) tag="item-table-column-fields";
                    if (tag.equals("naming-convention")) tag="item-naming-convention";
                    if (tag.equals("people")) tag="person-fields";
                    if (tag.equals("plugins-manual")) tag="plugins-manual-items";
                    if (tag.equals("plugins-auto")) tag="plugins-auto-items";
                    
                    // adjust tags, but not for naming convention!
                    if ((value != null) && (!tag.equals("item-naming-convention"))) {
                        int pos=value.indexOf("&1");
                        while(pos > -1) {
                            int limit1=value.lastIndexOf('#', pos)+1;
                            int limit2=value.lastIndexOf('|', pos)+1;
                            if (limit1<limit2) limit1=limit2;
                            if (limit1==-1) limit1=0;
                            value=value.substring(0,limit1)+"short_"+value.substring(limit1,pos)+value.substring(pos+2);
                            pos=value.indexOf("&1");
                        }
                        value = Parser.replace(value, "|pages|", "|");
                    }
                    if (tag.equals("searchtags")) {
                        tag="item-search-fields";
                        if (isArXivDB) {
                            searchtags="type|short_authors|title|identifier|arxiv-ref|doi|citation-tag";
                            value=searchtags;                        
                        }
                    }
                    configDictionary.put(tag, value);
                }
            }
            if (isArXivDB) {
                configDictionary.put("item-unique-fields","inspirekey|doi|arxiv-ref|citation-tag|search");
            }
            configDictionary.remove("directory");
            configDictionary.remove("autosortcolumn");
            configDictionary.remove("style");
            configDictionary.put("item-autosortcolumn","title");
            String[] columnFields=ToolBox.stringToArray(configDictionary.get("item-table-column-fields"));
            List<String> peopleFields=Arrays.asList(ToolBox.stringToArray(configDictionary.get("person-fields")));
            String out="";
            for (String field : columnFields) {
                if (peopleFields.contains(field)) {
                    out+="|short_"+field;
                } else {
                    out+="|"+field;
                }
            }
            configDictionary.put("item-table-column-fields",out);
            configDictionary.put("person-autosortcolumn","last_name");
            configDictionary.put("person-table-column-fields", "last_name|first_name");
            configDictionary.put("person-table-column-headers", "Last name|First name");
            configDictionary.put("person-table-column-sizes", "200|100");
            configDictionary.put("person-table-column-types", "text|text");
            configDictionary.put("person-search-fields", "first_name|last_name");
            configDictionary.put("plugins-import", "Basic Import");
            configDictionary.put("plugins-people", "");
            configDictionary.put("css-style", TextFile.ReadOutFile(Lib.completeDir(Lib.MainFile.get("style"),"")));
            if (configDictionary.get("item-folder").startsWith("LD::documents")) configDictionary.put("item-folder","LD::items");
            configDictionary.put("item-naming-convention", Parser.replace(configDictionary.get("item-naming-convention"), "#filetype#", "#$$filetype#"));
            // Write HashMap To File, sorted by keys.
            sql = "INSERT INTO configuration (key,value) VALUES(?,?)";
            ArrayList<String> keys=new ArrayList<>(configDictionary.keySet());
            Collections.sort(keys);
            for (String key : keys) {
                PreparedStatement statement = Lib.conn.prepareStatement(sql);
                statement.setString(1, key);
                statement.setString(2, configDictionary.get(key));                
                statement.execute();
            }
            
            RSC.out("Copying HTML templates");
            sql = "CREATE TABLE IF NOT EXISTS html_templates(\n    mode text UNIQUE,\n";
            sql+= "    template text);";
            stmt = Lib.conn.createStatement();
            Lib.HTMLtemplates.put("-1", "<html><body><h2>Currently selected library: #library.name#</h2><hr></body></html>");
            RSC.out(sql);
            success=stmt.execute(sql);
            sql = "INSERT INTO html_templates (mode,template) VALUES(?,?)";
            Lib.HTMLtemplates.toFirstElement();
            while (!Lib.HTMLtemplates.endReached) {
                RSC.out("Writing template:"+Lib.HTMLtemplates.get("infoMode"));
                PreparedStatement statement= Lib.conn.prepareStatement(sql);
                String template=Lib.HTMLtemplates.get("template");
                String s1="#if#altversions# #altversions#";
                String s2 = "#if#attachment-path-0#<a href=\"http://$$view-attachment-0\">#attachment-label-0#</a> (#attachment-filetype-0#-file, #attachment-pages-0# pages, #$attachment-filesize-0#)<br>\n"
                        + "#if#attachment-path-1#<a href=\"http://$$view-attachment-1\">#attachment-label-1#</a> (#attachment-filetype-1#-file, #attachment-pages-1# pages, #$attachment-filesize-1#)\n"
                        + "#if#attachment-path-1#<a href=\"http://$$view-attachment-2\">#attachment-label-2#</a> (#attachment-filetype-2#-file, #attachment-pages-2# pages, #$attachment-filesize-2#)\n"
                        + "#if#attachment-path-1#<a href=\"http://$$view-attachment-3\">#attachment-label-3#</a> (#attachment-filetype-3#-file, #attachment-pages-3# pages, #$attachment-filesize-3#)\n"
                        + "#if#attachment-path-1#<a href=\"http://$$view-attachment-4\">#attachment-label-4#</a> (#attachment-filetype-4#-file, #attachment-pages-4# pages, #$attachment-filesize-4#)\n"
                        + "#if#attachment-path-1#<a href=\"http://$$view-attachment-5\">#attachment-label-5#</a> (#attachment-filetype-5#-file, #attachment-pages-5# pages, #$attachment-filesize-5#)";
                template=Parser.replace(template, s1, s2);
                template=Parser.replace(template,"#person#","#first_name# #last_name#");

                statement.setString(1, Lib.HTMLtemplates.get("infoMode"));
                statement.setString(2, template);
                statement.execute();
                Lib.HTMLtemplates.nextElement();
            }
            
            RSC.out("Creating item index");
            sql = "CREATE TABLE IF NOT EXISTS items (\n    id integer primary key,\n";
            for (String tag : indexTags) {
                if (excludeTags.indexOf(tag)==-1) {
                    if (peopleTags.contains(tag)) {
                        sql+= "    [short_"+tag+"] text,\n";
                    } else {
                        sql+= "    ["+tag+"] text,\n";
                    }
                }
            }
            if (standardTags.contains("abstract")) {
                sql+= "    abstract text,\n";
                excludeTags.add("abstract");
            }
            if (standardTags.contains("remarks")) {
                sql+= "    remarks text,\n";
                excludeTags.add("remarks");
            }
            sql+= "    attributes blob,\n";
            sql+= "    search text,\n";
            sql+= "    createdTS integer,\n";
            sql+= "    last_modifiedTS integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);
            
            RSC.out("Creating persons index");
            sql = "CREATE TABLE IF NOT EXISTS persons (\n    id integer primary key,\n";
            sql+= "    last_name text,\n";
            sql+= "    first_name text,\n";
            sql+= "    search text,\n";
            sql+= "    attributes blob,\n";
            sql+= "    remarks text,\n";
            if (indexTags.indexOf("inspirekey")>-1) {
                sql+= "    orcid text,\n";
                sql+= "    inspirebai text,\n";
                sql+= "    inspirekey long,\n";
            }            
            sql+= "    createdTS integer);";
            sql+= "    last_modifiedTS integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);
            RSC.out("Creating attachments index");
            sql = "CREATE TABLE IF NOT EXISTS attachments (\n    id integer primary key,\n";
            sql+= "    name text,\n";
            sql+= "    filetype text,\n";
            sql+= "    path text,\n";
            sql+= "    pages integer,\n";
            sql+= "    source text,\n";
            sql+= "    md5 text,\n";
            sql+= "    createdTS integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);
            RSC.out("Creating item_person_links");
            sql = "CREATE TABLE IF NOT EXISTS item_person_links (item_id integer, person_id integer, link_type integer, ord integer, CONSTRAINT unq UNIQUE (item_id , person_id));";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            

            RSC.out("Creating item_attachment_links");
            sql = "CREATE TABLE IF NOT EXISTS item_attachment_links (item_id integer, attachment_id integer, ord integer, CONSTRAINT unq UNIQUE (item_id , attachment_id));";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            

            RSC.out("Creating categories");
            sql = "CREATE TABLE IF NOT EXISTS item_categories (\n    id integer primary key,\n label text UNIQUE,\n remarks text);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            
            
            RSC.out("Creating categorytree");
            sql = "CREATE TABLE IF NOT EXISTS category_tree (\n    id integer primary key,\n category integer,\n children text,\n parent integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            
            // read in categories as Hash:
            HashMap<String,Integer> categoryDictionary=new HashMap<>();
            // translate Structure tree to SQLite:
            saveNodeAndChildren(Lib.conn,Lib.Structure.Root,categoryDictionary,0);

            RSC.out("Creating item_category_links");
            sql = "CREATE TABLE IF NOT EXISTS item_category_links (item_id integer, category_id integer, link_type integer, CONSTRAINT unq UNIQUE (item_id , category_id));";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            

            RSC.out("Creating item_item_links");
            sql = "CREATE TABLE IF NOT EXISTS item_item_links (item1_id integer, item2_id integer, link_type integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            

            RSC.out("Creating person_item_links");
            sql = "CREATE TABLE IF NOT EXISTS person_item_links (person_id integer, item_id integer, link_type integer);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            

            RSC.out("Creating keywords");
            sql = "CREATE TABLE IF NOT EXISTS keywords (\n    id integer primary key,\n label text UNIQUE,\n remarks text);";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql);            
            RSC.out("Creating item_keyword_links");
            sql = "CREATE TABLE IF NOT EXISTS item_keyword_links (item_id integer, keyword_id integer, CONSTRAINT unq UNIQUE (item_id , keyword_id));";
            stmt = Lib.conn.createStatement();
            RSC.out(sql);
            stmt.execute(sql); 
            
            
            // commit up to here
            Lib.conn.commit();
            // Close Library3 and open new library for writing
            Lib.conn.close();
            RSC.guiNotify=false;
            
            // open library
            Library library=new Library(folder,RSC);
            
            // copy over elements
            
            Lib.Index.position=-1;
            Lib.Index.nextElement();
            
            while (!Lib.Index.endReached) {
                
                Item3 item3=new Item3(Lib,Lib.Index.get("id"));
                item3.ensureAddInfo();
                /* get doi from BibTeX */
                if (indexTags.indexOf("doi")>-1) {
                    if (item3.getS("doi").equals("") && !item3.getS("bibtex").equals("")) {
                        BibTeXRecord BTR=new BibTeXRecord(item3.getS("bibtex"));
                        if (!BTR.getS("doi").equals("")) {
                            item3.put("doi",BTR.get("doi"));
                        }
                    }
                }
                
                Item item=new Item(library);
                for (String tag: indexTags) {
                    if (!tag.equals("addinfo") && !tag.equals("id")) {
                        if (tag.equals("thumbnail")) {
                            item.put(tag, Parser.cutFrom(item3.get(tag),"AI::"));
                        } else {
                            item.put(tag, item3.get(tag));
                        }
                    }
                }

                if (standardTags.contains("abstract")) {
                    item.put("abstract",item3.get("abstract"));
                }

                if (standardTags.contains("remarks")) {
                    item.put("remarks",item3.get("remarks"));
                }
                
                // links conversion
                String[] links=ToolBox.stringToArray(item3.get("links"));
                if (links.length>0) {
                    String citations="";
                    String references="";
                    for (String link : links) {
                        String[] pair=link.trim().split(":");
                        if (pair.length==3) {
                            if (pair[0].equals("refers to")) {
                                references += "|" + pair[1] + ":" + pair[2];
                            }
                            if (pair[0].equals("citation")) {
                                citations += "|" + pair[1] + ":" + pair[2];
                            }
                        }
                    }
                    if (references.length()>0) item.put("references",references.substring(1));
                    if (citations.length()>0) item.put("citations",citations.substring(1));
                }

                // attributes
                for(String tag : item3.getAITags()) {
                    if (!excludeTags.contains(tag) && (!tag.startsWith("altversion-")) && !tag.equals("id")) {
                        if (tag.equals("thumbnail")) {
                            item.put(tag, Parser.cutFrom(item3.get(tag),"AI::"));
                        } else {
                            item.put(tag,item3.get(tag));
                        }
                    }
                }

                // adjust registration:
                String categories="";
                if (!Lib.Index.getS("registered").equals("")) categories+="|"+Lib.Index.getS("registered");
                if (!Lib.Index.getS("autoregistered").equals("")) categories+="|"+Lib.Index.getS("autoregistered");
                if (categories.length()>0) item.put("categories",categories.substring(1));
                
                // adjust keywords
                if (!Lib.Index.getS("keywords").equals("")) item.put("keywords",Lib.Index.getS("keywords"));
                
                // save the item
                item.save();
                
                // adjust time
                String lastUpdated=String.valueOf((Files.getLastModifiedTime(Paths.get(item3.getCompleteDirS("addinfo")))).toMillis()/1000);
                library.executeEX("UPDATE items SET createdTS=?, last_modifiedTS=? WHERE id=?", new String[]{lastUpdated,lastUpdated,item.id});

                // write Thumbnails
                if (!item.isEmpty("thumbnail")) {
                    String thumbName=item3.completeDir(item3.get("thumbnail"));
                    try {
                        FileTools.copyFile(thumbName, item.getThumbnailPath());
                    } catch (Exception e) {
                        RSC.out(">>E>> Couldn't copy thumbnail: "+thumbName);
                    }
                }
                
                if (!item3.getS("location").isBlank()) {
                    PreparedStatement statement = library.dbConnection.prepareStatement("INSERT INTO attachments (name,path,filetype,pages,md5,createdTS) VALUES(?,?,?,?,?,?);");
                    statement.setString(1, "Main file");
                    statement.setString(2, Parser.replace(Parser.replace(item3.getS("location"), "LD::documents/", "LD::items/"), "LD::/documents/", "LD::items/"));
                    statement.setString(3, item3.getS("filetype"));
                    statement.setInt(4, intV(item3.getS("pages")));
                    statement.setString(5,FileTools.md5checksum(item3.completeDir(item3.getS("location"))));
                    statement.setString(6,lastUpdated);
                    statement.executeUpdate();
                    ResultSet rs = statement.getGeneratedKeys();
                    rs.next();
                    int attID = rs.getInt(1);
                    statement = library.dbConnection.prepareStatement("INSERT INTO item_attachment_links (item_id,attachment_id,ord) VALUES(?,?,?);");
                    statement.setString(1, item.id);
                    statement.setInt(2, attID);
                    statement.setInt(3, 0);
                    statement.execute();
                    StringBuffer search = Lib.getPlainText(item3, "plaintxt");
                    search.append("\n");
                    for (String tag : library.itemSearchFields) {
                        search.append(item3.getS(tag));
                        search.append("\n");
                    }
                    if ((search != null) && (!search.isEmpty())) {
                        PreparedStatement searchStatement = Lib.searchConn.prepareStatement("INSERT INTO search(rowid,text) VALUES(?,?);");
                        searchStatement.setString(1, String.valueOf(attID));
                        searchStatement.setString(2, search.toString());
                        searchStatement.execute();
                    }
                    int ord = 0;
                    while (!item3.getS("altversion-location-" + formatInt3(ord)).isBlank()) {
                        String nmb = formatInt3(ord);
                        statement = library.dbConnection.prepareStatement("INSERT INTO attachments (name,path,filetype,pages,md5,createdTS) VALUES(?,?,?,?,?,?);");
                        statement.setString(1, item3.getS("altversion-label-" + nmb));
                        statement.setString(2, Parser.replace(Parser.replace(item3.getS("altversion-location-" + nmb), "LD::documents/", "LD::items/"), "LD::/documents/", "LD::items/"));
                        statement.setString(3, item3.getS("altversion-filetype-" + nmb));
                        statement.setInt(4, intV(item3.getS("altversion-pages-" + nmb)));
                        statement.setString(5, FileTools.md5checksum(item3.completeDir(item3.getS("altversion-location-" + nmb))));
                        statement.setString(6, lastUpdated);
                        statement.executeUpdate();
                        rs = statement.getGeneratedKeys();
                        rs.next();
                        attID = rs.getInt(1);
                        statement = library.dbConnection.prepareStatement("INSERT INTO item_attachment_links (item_id,attachment_id,ord) VALUES(?,?,?);");
                        statement.setString(1, item.id);
                        statement.setInt(2, attID);
                        statement.setInt(3, ord + 1);
                        statement.execute();
                        search = Lib.getPlainText(item3, "plaintxt");
                        search.append("\n");
                        for (String tag : library.itemSearchFields) {
                            search.append(item3.getS(tag));
                            search.append("\n");
                        }
                        if ((search != null) && (!search.isEmpty())) {
                            PreparedStatement searchStatement = Lib.searchConn.prepareStatement("INSERT INTO search(rowid,text) VALUES(?,?);");
                            searchStatement.setString(1, String.valueOf(attID));
                            searchStatement.setString(2, search.toString());
                            searchStatement.execute();
                        }
                        ord++;
                    }
                }
                Lib.Index.nextElement();
                done++;
                if (done % 20 == 0) {
                    RSC.out("Complete: "+String.valueOf(done)+" entries.");
                }
            }
            
            // END of conversion
            library.close();
            
            // move item directory 
            FileTools.moveFile(Lib.completeDir("LD::documents","0"), Lib.completeDir("LD::items","0"));
                    
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String toValues1(int id,String listString) {
        String[] list=listString.substring(1).split(",");
        String out="";
        for (String catID : list) {
            out+=", ("+String.valueOf(id)+","+catID+",0)";
        }
        return(out.substring(2));
    }
    
    private static int intV(String i) {
        try {
            return(Integer.valueOf(i));
        } catch (NumberFormatException e) {
            return(0);
        }
    }
    
    private static int saveNodeAndChildren(Connection conn,RulesNode node, HashMap<String,Integer> categoryDictionary, int parent) throws SQLException {
        String children="";
        System.out.println("Working on node:"+node.toString());
        PreparedStatement stm=conn.prepareStatement("INSERT INTO category_tree (category,children,parent) VALUES (?,?,?);");
        if (!categoryDictionary.containsKey(node.toString())) {
            PreparedStatement stm2=conn.prepareStatement("INSERT INTO item_categories (label) VALUES (?);");
            stm2.setString(1,node.toString());
            stm2.execute();
            ResultSet rs2 = stm2.getGeneratedKeys();
            rs2.next();
            categoryDictionary.put(node.toString(), rs2.getInt(1));
        }
        stm.setInt(1,categoryDictionary.get(node.toString()));
        stm.setString(2,"");
        stm.setInt(3,parent);
        stm.execute();
        ResultSet rs = stm.getGeneratedKeys();
        rs.next();
        int id=rs.getInt(1);
        for (RulesNode child : node.childNodes) {
            int childID=saveNodeAndChildren(conn,child,categoryDictionary,id);
            children+=","+childID;
        }
        if (children.length()>0) {
            stm=conn.prepareStatement("UPDATE category_tree SET children=? WHERE ID=?");
            stm.setString(1,children.substring(1));
            stm.setInt(2,id);
            stm.execute();
        }
        return(id);
    }
    
    private void register(int docID, String cat, int linktype) throws SQLException {
        // get id of category
        if ((cat==null) || (cat.isEmpty())) return;
        String sql="Select id from item_categories where label=?;";
        PreparedStatement statement= conn.prepareStatement(sql);
        statement.setString(1,cat);
        ResultSet rs = statement.executeQuery();
        int catID=0;
        if (rs.next()) {
            catID=rs.getInt(1);
        } else {
            sql="INSERT INTO item_categories (label,created) VALUES (?,?);";
            statement= conn.prepareStatement(sql);
            statement.setString(1,cat);
            statement.setLong(2,ToolBox.now());
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            rs.next();
            catID=rs.getInt(1);
        }
        sql="INSERT OR IGNORE INTO item_category_links (item_id, category_id, linktype) VALUES (?,?,?);";
        statement= conn.prepareStatement(sql);
        statement.setInt(1,docID);
        statement.setInt(2,catID);
        statement.setInt(3,linktype);
        statement.executeUpdate();
        /*if (linktype==0) {
            if (cat.equals("List of Publications")) RSC.out("UPDATING");
            sql="UPDATE item_category_links SET linktype=? where item_id=? AND category_id=?;";
            statement= conn.prepareStatement(sql);
            statement.setInt(1,linktype);
            statement.setInt(2,docID);
            statement.setInt(3,catID);
            statement.executeUpdate();
        }*/
    }

    /*private void addKey(int docID, String key) throws SQLException {
        // get id of category
        if ((key==null) || (key.isEmpty())) return;
        String sql="Select id from keywords where label=?;";
        PreparedStatement statement= conn.prepareStatement(sql);
        statement.setString(1,key);
        ResultSet rs = statement.executeQuery();
        int keyWordID=0;
        if (rs.next()) {
            keyWordID=rs.getInt(1);
        } else {
            sql="INSERT INTO keywords (label,created) VALUES (?,?);";
            statement= conn.prepareStatement(sql);
            statement.setString(1,key);
            statement.setLong(2,ToolBox.now());
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            rs.next();
            keyWordID=rs.getInt(1);
        }
        sql="INSERT OR IGNORE INTO item_keyword_links (item_id, keyword_id) VALUES (?,?);";
        statement= conn.prepareStatement(sql);
        statement.setInt(1,docID);
        statement.setInt(2,keyWordID);
        statement.executeUpdate();
    }*/
    
    class CompTable implements Comparator<Item> {

        private String tag;
        private boolean forwards;
        private int type;

        public CompTable(final String t,boolean f, int ty) {
            tag=t;forwards=f;type=ty;
        }

        private int compare(String a, String b) {
            if (type==1) {
                int i=0;
                while ((i<a.length()) && (!Character.isLetter(a.charAt(i)))) i++;
                double d1=Double.valueOf(a.substring(0,i).trim());
                i=0;
                while ((i<b.length()) && (!Character.isLetter(b.charAt(i)))) i++;
                double d2=Double.valueOf(b.substring(0,i).trim());
                if (d1>d2) return(1);
                if (d1<d2) return(-1);
                return(0);
            }
            return(a.compareTo(b));
        }

        @Override
        public int compare(final Item A, final Item B) {
            if (tag==null) {
                if (!forwards) return(compare(B.toSort(),A.toSort()));
                return(compare(A.toSort(),B.toSort()));
            } else {
                if (!forwards) return(compare(B.getExtended(tag),A.getExtended(tag)));
            }
            return(compare(A.getExtended(tag),B.getExtended(tag)));
        }

        public boolean equals() {
            return (false);
        }

    }

    class CompChoice implements Comparator<Item> {

        private String tag;
        private boolean forwards;
        private ArrayList<String> fields;

        public CompChoice(final String t,boolean f,ArrayList<String> fl) {
            tag=t;forwards=f;fields=fl;
        }

        @Override
        public int compare(final Item A, final Item B) {
            if (!forwards) return(fields.indexOf(A.getExtended(tag))-fields.indexOf(B.getExtended(tag)));
            return (fields.indexOf(B.getExtended(tag))-fields.indexOf(A.getExtended(tag)));
        }

        public boolean equals() {
            return (false);
        }

    }

}
