//
// Celsius Library System v2
// (w) by C. Saemann
//
// Library.java
//
// This class combines all necessary data for a library
//
// typesafesh
//
// checked 16.09.2007
//

package celsius.data;

import celsius.Resources;
import celsius.gui.ItemTable;
import celsius.gui.RulesNode;
import celsius.gui.SafeMessage;
import celsius.Threads.ThreadRegister;
import celsius.gui.GUIToolBox;
import celsius.tools.*;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.text.html.StyleSheet;
import javax.swing.tree.DefaultTreeModel;
import java.sql.*;

public final class Library implements Iterable<Item> {
    
    // Status messages after adding a item
    public static final String[] status={
        "Everything OK",  // 0
        "No title or author information found in record.", // 1
        "File with the same name existed. Adding aborted.", // 2
        "Error deleting item.", // 3
        "Item not found in library.", //4
        "Additional information could not be loaded in going to document.", // 5
        "Item id not found.", // 6
        "IOError with Index/addinfo file while adding item reference.", // 7
        "Error deleting library.", //8
        "Couldn't move item file", //9
        "Library index and data files may be out of synch." //10
    };

    public final Resources RSC;
    public Connection dbConnection;
    
    public String mainLibraryFile;
    public HashMap<String,String> config;
    public HashMap<String,CelsiusTemplate> htmlTemplates;
    public CelsiusTemplate itemRepresentation;
    public CelsiusTemplate itemSortRepresentation;
    public CelsiusTemplate namingConvention;
    public CelsiusTemplate itemFolder;
    public StructureNode structureTreeRoot;
    public HashMap<Integer,Category> categories;
    //public XMLTree Structure;
    public XMLTree Rules;
    public XMLHandler PeopleRemarks; //TODO
    public Item lastAddedItem;
    private boolean PeopleOrKeywordsChanged;
    private boolean modfile;
    private int totalpages; // total number of pages in current library
    private int totalduration; // total duration in seconds

    // Metainformation on the Library
    public String name;
    public String libraryBaseDir;
    public String celsiusBaseDir;
    public ArrayList<String> indexFields;
    public ArrayList<String> Hide;
    public ArrayList<String> TableTags;
    public ArrayList<String> TableHeaders;
    public ArrayList<String> StyleSheetRules;
    public LinkedHashMap<String,ArrayList<String>> usbdrives;
    public HashMap<String,ArrayList<String>> ChoiceFields;
    public HashMap<String,String> IconDictionary;
    public ArrayList<Integer> ColumnSizes;
    public ArrayList<String> ColumnTypes;

    // Buffered information on the Library
    public String[] peopleFields;
    public String[] iconFields;
    public ArrayList<String> Positions;      // List with String IDs storing the position
    public ArrayList<Integer> PositionsI;    // List with String IDs storing the position
    public HashMap<String,ArrayList<String>> Links; // Links is set from createLinksTree
    public HashMap<String,ArrayList<String>> LinksRef; // Links is set from createLinksTree
    
    public HashSet<String> itemPropertyKeys;
    public HashSet<String> personPropertyKeys;
    
    public final ArrayList<Item> recentlyAdded;
    
    public final ArrayList<LibraryChangeListener> libraryChangeListeners;
    
    public String shortSQLTags;
    
    // Size information
    public int noOfItems;
    public int sizeOfItems;

    public String LastErrorMessage;

    public int currentStatus;
    
    public Item marker;
    
    // TODO
    private RulesNode createNode(int i,String s,String l) {
        RulesNode SN;
        if (i>-1) {
            SN=new RulesNode((new Item(this,i)).toText());
            //SN.getData().put("id",Index.get(i,"id"));
        } else {
            SN=new RulesNode("Item not in Library: "+s);
            SN.getData().put("id","?");
        }
        SN.representation="/name/";
        SN.getData().put("pos",Integer.toString(i));
        SN.getData().put("ref",s);
        SN.getData().put("link",l);
        return(SN);
    }

    // TODO
    private RulesNode resRef(String s,String l) {
        String field=Parser.cutUntil(s, ":");
        String value=Parser.cutFrom(s, ":");
        int i=0;
        /*int k=Index.XMLTags.indexOf(field);
        if (k>-1) {
            while (i<Index.getSize()) {
                if (value.equals(Index.getDataElement(i,k)))
                    return(createNode(i,s,l));
                i++;
            }
        }*/
        return(createNode(-1,s,l));
    }

    public DefaultTreeModel createLinksTree(Item doc) {
        Links=new HashMap<String,ArrayList<String>>();
        LinksRef=new HashMap<String,ArrayList<String>>();
        RulesNode root=new RulesNode("Available Links");
        if (doc.get("links")!=null) {
            String[] links=doc.get("links").split("\\|");
            if (links[0].length()!=0) {
                ArrayList<String> types=new ArrayList<String>();
                String type,target;
                for (int i=0;i<links.length;i++) {
                    type=Parser.cutUntil(links[i],":");
                    target=Parser.cutFrom(links[i],":");
                    RulesNode SNT;
                    if (!Links.containsKey(type)) {
                        Links.put(type, new ArrayList<String>());
                        LinksRef.put(type, new ArrayList<String>());
                        SNT=new RulesNode(type);
                        root.add(SNT);
                    } else {
                        int n=0;
                        while (!root.getChildAt(n).getLabel().equals(type)) n++;
                        SNT=root.getChildAt(n);
                    }
                    RulesNode SN=resRef(target,links[i]);
                    Links.get(type).add(SN.get("id"));
                    LinksRef.get(type).add(SN.getLabel());
                    SNT.add(SN);
                }
            }
        }
        return(new DefaultTreeModel(root));
    }

    /**
     * TODO: rewrite and adjust
     * @param template
     * @throws IOException 
     */
    private void createHTMLTemplates(String template) throws IOException {
        XMLHandler.Create("celsiusv2.2.htmltemplates",libraryBaseDir+"htmltemplates.xml");
        /*HTMLtemplates=new XMLHandler(basedir+"htmltemplates.xml");
        XMLHandler XH=RSC.LibraryTemplates.get(template);
        for(int i=0;i<8;i++) {
            String n="infoMode-"+String.valueOf(i).trim();
            if (XH.get(n)!=null) {
                HTMLtemplates.addEmptyElement();
                HTMLtemplates.put("infoMode",String.valueOf(i));
                HTMLtemplates.put("template",XH.get(n));
            }
        }
        HTMLtemplates.writeBack();*/
    }

    public void getFieldsFromConfig() {
        name=config.get("name");
        iconFields=config.get("icon-fields").split("\\|");
        
        peopleFields=config.get("people").split("\\|");
        
        usbdrives=new LinkedHashMap<String,ArrayList<String>>();
        if (config.get("usbdrives")!=null) {
            String[] usbfields=config.get("usbdrives").split("\\|");
            for (int i=0;i<usbfields.length;i++) {
                ArrayList<String> list=new ArrayList<String>();
                String[] lst=Parser.cutFrom(usbfields[i],":").split("\\:");
                list.addAll(Arrays.asList(lst));
                usbdrives.put(Parser.cutUntil(usbfields[i],":"),list);
            }
        }
        ChoiceFields=new HashMap<String,ArrayList<String>>();
        if (config.get("choice-fields")!=null) {
            String[] choicefields=config.get("choice-fields").split("\\|");
            for (int i=0;i<choicefields.length;i++) {
                String field=Parser.cutUntil(choicefields[i],":");
                String[] possibilities=Parser.cutFrom(choicefields[i], ":").split(",");
                ArrayList<String> poss=new ArrayList<String>();
                poss.addAll(Arrays.asList(possibilities));
                ChoiceFields.put(field, poss);
            }
        }
        IconDictionary=new HashMap<String,String>();
        if (config.get("icon-dictionary")!=null) {
            String[] icondict=config.get("icon-dictionary").split("\\|");
            for (int i=0;i<icondict.length;i++) {
                String field=Parser.cutUntil(icondict[i],":");
                String value=Parser.cutFrom(icondict[i],":");
                IconDictionary.put(field,value);
            }
        }
        initTableColumns();
    }
    
    
    /** 
     * Creates a new Library 
     * 
     * TODO fix adjust
     */
    public Library(String bd,String mainfile,String nm,Resources rsc, String template) throws Exception {
        currentStatus=0;
        lastAddedItem=null;
        peopleFields=null;
        PeopleOrKeywordsChanged=true;
        RSC=rsc;
        celsiusBaseDir=Parser.cutUntilLast((new File(".")).getAbsolutePath(),".");
        StyleSheetRules=new ArrayList<String>();
        libraryBaseDir=bd;
        if (!celsiusBaseDir.endsWith(ToolBox.filesep)) celsiusBaseDir+=ToolBox.filesep;
        if (!libraryBaseDir.endsWith(ToolBox.filesep)) libraryBaseDir+=ToolBox.filesep;
        name=nm;
        
        (new File(libraryBaseDir)).mkdir();
        (new File(libraryBaseDir+"information")).mkdir();
        /*
        XMLHandler.Create("celsiusv2.1.library", mainfile);
        MainFile=new XMLHandler(mainfile);
        MainFile.addEmptyElement();
        String p=Parser.CutTill((new File(mainfile)).getCanonicalPath(),(new File(mainfile)).getName());
        if (bd.startsWith(p)) bd=Parser.CutFrom(bd, p);
        MainFile.put("directory",bd);
        for (String field : RSC.LibraryFields) {
            MainFile.put(field,RSC.LibraryTemplates.get(template).get(field));
        }
        MainFile.put("name",name);
        MainFile.put("style", "LD::style.css");
        MainFile.writeBack();*/

        initIndexTags();

        TextFile TD=new TextFile(libraryBaseDir+"style.css",false);
        TD.putString(RSC.libraryTemplates.get(template).get("stylesheet"));
        TD.close();

        loadStyleSheetRules();
        
        TD=new TextFile(libraryBaseDir+"librarystructure.xml",false);
        TD.putString(RSC.libraryTemplates.get(template).get("librarystructure"));
        TD.close();
        
        TD=new TextFile(libraryBaseDir+"rules.xml",false);
        TD.putString(RSC.libraryTemplates.get(template).get("libraryrules"));
        TD.close();
        
        Rules=new XMLTree(libraryBaseDir+"rules.xml","$full");
        
        /*XMLHandler.Create("celsiusv3.0.libraryindexfile",basedir+"libraryindex.xml");
        Index=new IndexedXMLHandler(basedir+"libraryindex.xml",IndexTags);
        
        XMLHandler.Create("celsiusv3.0peopleremarksfile",basedir+"peopleremarks.xml");*/
        PeopleRemarks=new XMLHandler(libraryBaseDir+"peopleremarks.xml");

        createHTMLTemplates(template);
        
        Positions=new ArrayList<String>();
        PositionsI=new ArrayList<Integer>();
        
        TextFile TF = new TextFile(libraryBaseDir + "/lock", false);
        TF.close();
        getFieldsFromConfig();

        libraryChangeListeners=new ArrayList<>();
        recentlyAdded=new ArrayList<>();
    }
    
    /** Loads an existing library */
    public Library(String fn,Resources rsc) {
        currentStatus=0;
        lastAddedItem=null;
        peopleFields=null;
        libraryChangeListeners=new ArrayList<>();
        PeopleOrKeywordsChanged=true;
        RSC=rsc;
        recentlyAdded=new ArrayList<>();
        try {
            for (Library lib : RSC.libraries) {
                if (lib.name.equals(name)) {
                    (new SafeMessage("A library with this name is already loaded.","Library not loaded:",0)).showMsg();
                    name="??#$Library with this name is already loaded.";
                    return;
                }
            }
            mainLibraryFile=fn;
            String url="jdbc:sqlite:"+fn;
            dbConnection = DriverManager.getConnection(url);
            RSC.out("Connection established to SQLite database "+url);
            
            // Read configuration
            config=new HashMap<>();
            ResultSet rs=dbConnection.prepareStatement("SELECT * FROM configuration;").executeQuery();
            while (rs.next()) {
                config.put(rs.getString(1), rs.getString(2));
            }
            getFieldsFromConfig();
            shortSQLTags="items.id";
            for (String tag : this.config.get("tablecolumns").split("\\|")) {
                shortSQLTags+=","+tag;
            }
            // adjust for amount information
            if (config.get("index").contains("|pages|")) {
                shortSQLTags+=",pages";
            }
            
            // read in table fields
            rs=dbConnection.prepareStatement("PRAGMA table_info('items');").executeQuery();
            itemPropertyKeys=new HashSet<String>();
            while (rs.next()) {
                String key=rs.getString(2);
                if (!key.equals("id") && !key.equals("attributes")) {
                    itemPropertyKeys.add(key);
                }
            }
            rs=dbConnection.prepareStatement("PRAGMA table_info('persons');").executeQuery();
            personPropertyKeys=new HashSet<String>();
            while (rs.next()) {
                String key=rs.getString(2);
                if (!key.equals("id") && !key.equals("attributes")) {
                    personPropertyKeys.add(key);
                }
            }

            String relbase=Parser.cutUntil(fn,(new File(fn)).getName());
            if (relbase.endsWith(ToolBox.filesep)) relbase=relbase.substring(0,relbase.length()-1);
            celsiusBaseDir=Parser.cutUntilLast((new File(".")).getAbsolutePath(),".");
            libraryBaseDir=config.get("directory");
            if (!celsiusBaseDir.endsWith(ToolBox.filesep)) celsiusBaseDir+=ToolBox.filesep;
            if (!libraryBaseDir.endsWith(ToolBox.filesep)) libraryBaseDir+=ToolBox.filesep;
            if ((name.length()==0) || (libraryBaseDir.length()==0)) {
                (new SafeMessage("The library file seems to be corrupt. Cancelling...","Warning:",0)).showMsg();
                name="??##Library file corrupt.";
                return;
            }
            if (!(new File(libraryBaseDir)).exists())
            libraryBaseDir=relbase+"/"+libraryBaseDir;
            if ((new File(libraryBaseDir + "/lock")).exists()) {
                int i=RSC.askQuestionAB("The library "+name+" is locked. If no other instance of Celsius is accessing it," +
                        "\nyou can select \"Ignore Lock\" and open it anyway. This can happen " +
                        "\nwhen Celsius has not been shut down properly.", "Library locked", "Cancel", "Ignore Lock");
                if (i==0) {
                    name="??##cancelled";
                    return;
                }
            }
            if ((new File(libraryBaseDir + "/modified")).exists()) {
                (new SafeMessage("This library ("+name+") has not been closed properly. Please run the\n\"Synchronized Library\" command as soon as possible.", "Warning:", 0)).showMsg();
            }
            loadStyleSheetRules();
            for (String field : RSC.LibraryFields) {
                ensure(field);
            }
            
            // read categories
            categories=new HashMap<>();
            rs=dbConnection.createStatement().executeQuery("SELECT * FROM categories;");
            while (rs.next()) {
                categories.put(rs.getInt(1), new Category(this,rs));
            }
            // read category structures
            rs=dbConnection.createStatement().executeQuery("SELECT * FROM category_tree;");
            structureTreeRoot=StructureNode.readInFromResultSet(this, rs);

            initIndexTags();

            try {
                PeopleRemarks=new XMLHandler(libraryBaseDir+"peopleremarks.xml");
            } catch(IOException e) {
                XMLHandler.Create("celsiusv3.0.peopleremarksfile",libraryBaseDir+"peopleremarks.xml");
                PeopleRemarks=new XMLHandler(libraryBaseDir+"peopleremarks.xml");
            }
            Rules=new XMLTree(libraryBaseDir+"rules.xml","$full");
            //XXStructure=new XMLTree(basedir+"librarystructure.xml","/title/");
            
            // Read html templates
            htmlTemplates=new HashMap<>();
            rs=dbConnection.prepareStatement("SELECT * FROM html_templates;").executeQuery();
            while (rs.next()) {
                htmlTemplates.put(rs.getString(1), new CelsiusTemplate(rs.getString(2)));
            }
            itemRepresentation=new CelsiusTemplate(config.get("item-representation"));
            itemSortRepresentation=new CelsiusTemplate(config.get("item-sort-representation"));
            namingConvention=new CelsiusTemplate(config.get("naming-convention"));
            itemFolder=new CelsiusTemplate(config.get("item-folder"));

            Positions=new ArrayList<String>();
            PositionsI=new ArrayList<Integer>();
            
            //TODO needs to be rewitten:

            // get header names
            /*String sql="PRAGMA table_info(items);)";
            PreparedStatement statement=conn.prepareStatement(sql);
            ResultSet rs=statement.executeQuery();
            while (rs.next()) {
                System.out.println(rs.getString(1));
                System.out.println(rs.getString(2));
                System.out.println(rs.getString(3));
            }*/

            TextFile TF=new TextFile(libraryBaseDir+"lock",false);
            TF.close();
            getFieldsFromConfig();
            /* Old code to correct stuff...
             String[] files = (new File(basedir + "/information")).list();
            for (int i = 0; i < files.length; i++) {
                String r = Parser.CutTill(files[i], ".");
                if (r.length() == 7) {
                    TextFile.moveFile(basedir + "/information/" + files[i], basedir + "/information/1" + files[i].substring(1));
                }
            }*/

        } catch (Exception ex) {
            RSC.outEx(ex);
            name="??##Error"+ex.toString();
            dbConnection=null;
        }
    }

    /**
     * Notifies the Library of a change to a certain tag
     *
     * @param t - the tag whose value has been changed.
     */
    public void notifyChange(String t) {
        //if (isPeopleOrKeywordTag(t)) PeopleOrKeywordsChanged=true;
    }

    public void ensure(String k) {
        if (emptyConfigFor(k)) {
            putConfig(k,RSC.libraryTemplates.get("Default").get(k));
        }
    }
    
    /**
     * Turns a configuration value that contains a list into a string array
     * 
     * @param key
     * @return list of values as string array
     */
    public String[] listOf(String key) {
        String t=config.get(key);
        if (t==null) return(new String[0]);
        return(t.split("\\|"));
    }

    private void initIndexTags() {
        Hide=new ArrayList<String>(Arrays.asList(listOf("hide")));
        indexFields = new ArrayList<String>();
        if (config.get("index") != null) {
            String[] list = listOf("index");
            indexFields.addAll(Arrays.asList(list));
        } else {
            indexFields.add("type");
            indexFields.add("title");
            indexFields.add("authors");
            indexFields.add("pages");
            indexFields.add("identifier");
            indexFields.add("keywords");
        }
        indexFields.add("id");
        indexFields.add("registered");
        indexFields.add("autoregistered");
        indexFields.add("location");
        indexFields.add("addinfo");
        indexFields.add("filetype");
        indexFields.add("attributes");
    }

    private void initTableColumns() {
        TableTags = new ArrayList<String>();
        TableHeaders = new ArrayList<String>();
        ColumnSizes = new ArrayList<Integer>();
        ColumnTypes = new ArrayList<String>();
        if (emptyConfigFor("tablecolumns")) {
            fillTableTagsWithDefaults();
        } else {
            String[] list = listOf("tablecolumns");
            TableTags.addAll(Arrays.asList(list));
            if (config.get("columnsizes") != null) {
                list = listOf("columnsizes");
                for (int i = 0; i < list.length; i++) {
                    ColumnSizes.add(Integer.valueOf(list[i]));
                }
                if (list.length<TableTags.size()) {
                    for (int i=list.length;i<TableTags.size();i++)
                        ColumnSizes.add(1);
                }
            } else {
                for (int i=0;i<list.length; i++)
                    ColumnSizes.add(1);
            }
            if (config.get("columntypes") != null) {
                list = listOf("columntypes");
                ColumnTypes.addAll(Arrays.asList(list));
                if (list.length<TableTags.size()) {
                    for (int i=list.length;i<TableTags.size();i++)
                    ColumnTypes.add("text");
                }
            } else {
                for (int i=0;i<list.length; i++)
                    ColumnTypes.add("text");
            }
            list = listOf("columnheaders");
            TableHeaders.addAll(Arrays.asList(list));
        }
    }

    public void setColumnSize(int c,int w) {
        ColumnSizes.set(c, Integer.valueOf(w));
        String s="";
        for (Integer i : ColumnSizes) {
            if (i==0) i=1;
            s+="|"+Integer.toString(i);
        }
        s=s.substring(1);
        putConfig("columnsizes", s);
    }
    
    public void closeLibrary() {
        TextFile.Delete(libraryBaseDir + "/lock");
        TextFile.Delete(libraryBaseDir + "/modified");
        try { 
            dbConnection.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    public boolean emptyConfigFor(String key) {
        return (!config.containsKey(key) || config.get(key)==null || config.get(key).isBlank());
    }
    
    public void putConfig(String key, String value) {
        if (!config.containsKey(key) || (config.get(key)==null) || !config.get(key).equals(value)) {
            config.put(key,value);
            try {
                PreparedStatement pstmt=dbConnection.prepareStatement("INSERT OR REPLACE INTO configuration (key,value) VALUES(?,?);");
                pstmt.setString(1,key);
                pstmt.setString(2,value);
                pstmt.execute();
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
    }
    
    /**
     * Deletes all files associated with library
     * 
     * TODO: adjust to new files
     */
    public int deleteLibrary() {
        String TI = "LIB" + name + ">";
        try {
            RSC.out(TI + "Removing base folder " + libraryBaseDir);
            if (!(TextFile.removeFolder(libraryBaseDir))) {
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

    /**
     * Save the whole library
     * @throws IOException
     * 
     * TODO adjust if necessary. Create a copy of the library at startup!
     * 
     */
    public void writeBack() throws IOException {
        /*TextFile.Delete(Index.source+".bck");
        TextFile.moveFile(Index.source, Index.source+".bck");
        Index.writeBack();*/
        /*TextFile.Delete(Rules.source+".bck");
        TextFile.moveFile(Rules.source, Rules.source+".bck");
        Rules.writeBack();*/
        /*TextFile.Delete(Structure.source+".bck");
        TextFile.moveFile(Structure.source, Structure.source+".bck");
        Structure.writeBack();*/
    }
    
    public void updateSizeInfo() {
        noOfItems=0;
        sizeOfItems=0;
        try {
            ResultSet rs=this.dbConnection.prepareStatement("SELECT pages from items;").executeQuery();
            while (rs.next()) {
                noOfItems++;
                sizeOfItems+=rs.getInt(1);
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }    
    }
    
    // Status string for status bar.
    public String Status(boolean count) {
        String tmp = "Current library "+name+":";
        updateSizeInfo();
        if (noOfItems==0) return("No items in current library.");
        tmp+=Integer.toString(sizeOfItems) + " pages in ";
        tmp+=Integer.toString(noOfItems) + " items";
        tmp += ".";
        return(tmp);
    }

    public int getSize() {
        try {
            ResultSet rs=dbConnection.prepareStatement("SELECT COUNT(*) FROM items;").executeQuery();
            if (rs.next()) return(rs.getInt(1));
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return(0);
    }
    
    public void registerItem(Item item, StructureNode node, int type) throws SQLException {
        // get id of category
        if (node==null) return;
        int catID=node.categoryID;
        String sql="INSERT OR IGNORE INTO item_category_links (item_id, category_id, type) VALUES (?,?,?);";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setInt(1,Integer.valueOf(item.id));
        statement.setInt(2,catID);
        statement.setInt(3,type);
        statement.executeUpdate();
        if (type==0) {
            if (node.library.categories.get(node.categoryID).label.equals("List of Publications")) RSC.out("UPDATING");
            sql="UPDATE item_category_links SET type=? where item_id=? AND category_id=?;";
            statement= dbConnection.prepareStatement(sql);
            statement.setInt(1,type);
            statement.setInt(2,Integer.valueOf(item.id));
            statement.setInt(3,catID);
            statement.executeUpdate();
        }
    }

    public void registerItem(Item item, int catID, int type) throws SQLException {
        if (catID<1) return;
        String sql="INSERT OR IGNORE INTO item_category_links (item_id, category_id, type) VALUES (?,?,?);";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setInt(1,Integer.valueOf(item.id));
        statement.setInt(2,catID);
        statement.setInt(3,type);
        statement.executeUpdate();
        if (type==0) {
            sql="UPDATE item_category_links SET type=? where item_id=? AND category_id=?;";
            statement= dbConnection.prepareStatement(sql);
            statement.setInt(1,type);
            statement.setInt(2,Integer.valueOf(item.id));
            statement.setInt(3,catID);
            statement.executeUpdate();
        }
    }
    
    public void unRegisterItem(Item item, int catID) throws SQLException {
        if (catID<1) return;
        String sql="DELETE FROM item_category_links WHERE item_id=? AND category_id=?;";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setInt(1,Integer.valueOf(item.id));
        statement.setInt(2,catID);
        statement.execute();
    }
    
    public boolean isPeopleField(String key) {
        return(Arrays.stream(peopleFields).anyMatch(key::equals));
    }

    /**
     * Check for doublettes...
     *
     * Return values: 10 : exact doublette, 5: file might be doublette, 4 : apparent Doublette,  0 : no doublette
     * 
     * TODO
     */
    public DoubletteResult isDoublette(Item item) throws IOException {
        // Look for doublettes
        // TODO check unique fields
        String[] uf = listOf("unique-fields");
        for (String key:uf) {
            String cV=item.get(key);
            if ((cV!=null) && (!cV.equals("<unknown>"))) {
                String sql="SELECT * FROM items WHERE ";
            }
        }
        // TODO check differentiating fields via shortsearch
        String[] df = listOf("differentiating-fields");
        for (String key:df) {
            
        }
        String fn = item.get("path");
        if ((fn==null) || (fn.equals(""))) return(new DoubletteResult(0,null));
        try {
            ResultSet rs=executeResEX("SELECT id,path FROM attachments");
            while (rs.next()) {
                // Literal doublette found?
                String path=rs.getString(2);
                if (path != null) {
                    int i = TextFile.IsDoublette(completeDir(path), fn);
                    if (i == 1) {
                        Item item2=getItemForAttachment(rs.getInt(1));
                        RSC.out("Ending Doublette for "+item2.toString());
                        return (new DoubletteResult(10,item2));
                    }
                    if (i == 0) {
                        Item item2=getItemForAttachment(rs.getInt(1));
                        RSC.out("Ending Doublette for "+item2.toString());
                        return (new DoubletteResult(5,item2));
                    }
                }
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        return(new DoubletteResult(0,null));
    }
    
    /**
     * Inserts an empty record into the Index and returns a doc object refering there
     * @return the document object
     * 
     * TODO remove altogether
     * 
     */
    public Item createEmptyItem() {
        Item item;
        String id;
        // synchronize Library, such that a taken slot is not overwritten.
        /*synchronized(Index) {
            int k=Index.position;
            // Get free registration space and look for already existing papers
            // File does not exists yet, make entry
            id=String.valueOf(Index.addEmptyElement());
            item=new Item(this,id);
            Index.position=k;
        }
        item.put("id",id);*/
        return(null);
    }

    public void acquireCopyOfItem(Item sourceItem) {
        String TI="LIB"+name+">";
        String[] essentialFields=config.get("essential-fields").split("\\|");
        for (int i=0; i<essentialFields.length;i++) {
            if (sourceItem.get(essentialFields[i])==null) {
                RSC.showWarning("The item "+sourceItem.toText()+"\ncould not be copied, as the field "+essentialFields[i]+",\nrequired by the library "+this.name+" is not set.", "Copying cancelled...");
                return;
            }
        }
        RSC.out(TI+"Copying item "+sourceItem.toText()+" from library "+sourceItem.library.name+" to "+name);

        String filename="";
        String fullfilename;
        
        Item targetItem;

        /*if (sourceItem.get("location").startsWith("LD")) {
            filename=standardFileName(sourceItem);
            if (!guaranteeStandardFolder(sourceItem)) {
                RSC.showWarning("Item folder could not be created.", "Warning:");
                return;
            }
            targetItem=createEmptyItem();
            guaranteeStandardFolder(targetItem);
            filename=getStandardFolder(targetItem)+ToolBox.filesep+filename;
            fullfilename=completeDir(filename);
        } else {
            targetItem=createEmptyItem();
            fullfilename=sourceItem.getCompleteDirS("location");
        }*/

        /*String id=targetItem.get("id");
        targetItem.put("location",filename);
        
        // The actual move document procedure
        // Item
        // TODO Rewrite this: 
        RSC.out(TI+"Registering under name: "+filename+", "+fullfilename);*/
        try {
            //if (filename.startsWith("LD::"))
              //  TextFile.CopyFile(sourceItem.getCompleteDirS("location"),fullfilename);
            /*ArrayList<String> tags=src.getAITags();
            for (String key : tags) {
                if ((src.get(key)!=null) && (!key.equals("id")) && (!key.equals("location"))) {
                    if (src.get(key).indexOf("::")==2) {
                        String from=src.getCompleteDirS(key);
                        String end=Parser.Substitute(src.get(key),src.get("id"),doc.get("id"));
                        try {
                            TextFile.CopyFile(src.getCompleteDirS(key),doc.completeDir(end));
                        } catch(Exception e) {
                            e.printStackTrace();
                        }
                        doc.put(key,end);
                    } else {
                        doc.put(key,src.get(key));
                    }
                }
            }
            // Plaintext
            if (src.get("plaintxt")!=null) {
                doc.put("parse","all");
            } else {
                doc.put("parse","header");
            }
            doc.save();*/
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        
        
        // update AuthorList
        //addPeople(doc);
        
    }
    
    public String compressDir(String s) {
        if (s.startsWith(libraryBaseDir)) return("LD::"+Parser.cutFrom(s,libraryBaseDir));
        if (s.startsWith(celsiusBaseDir)) return("BD::"+Parser.cutFrom(s,celsiusBaseDir));
        return(s);
    }
    
    /**
     * Returns raw data for editing in text area
     * 
     * TODO: change to items from database
     * 
     * @param i
     * @return 
     */
    public String rawData(int i) {
        if (i==1) {
            //IndexedXMLHandler xh=Index;
            String tmp = "";
            String s2;
            /*if (xh == null) {
                return ("Corresponding XMLHandler does not exist.");
            }
            for (String tag : xh.XMLTags) {
                s2 = xh.get(tag);
                if ((s2 != null) && !(s2.indexOf("\n") > -1)) {
                    tmp += tag + ": " + s2 + toolbox.linesep;
                }
            }*/
            return (tmp);
        } 
        XMLHandler xh = PeopleRemarks;
        String tmp = "";
        String s2;
        if (xh == null) {
            return ("Corresponding XMLHandler does not exist.");
        }
        for (String tag : xh.XMLTags) {
            s2 = xh.get(tag);
            if ((s2 != null) && !(s2.indexOf("\n") > -1)) {
                tmp += tag + ": " + s2 + ToolBox.linesep;
            }
        }
        return (tmp);
    }
    
    private void register(int docID, String cat, int type) throws SQLException {
        // get id of category
        String sql="Select id from categories where label=?;";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setString(1,cat);
        ResultSet rs = statement.executeQuery();
        int catID=0;
        if (rs.next()) {
            catID=rs.getInt(1);
        } else {
            sql="INSERT INTO categories (label,created) VALUES (?,?);";
            statement= dbConnection.prepareStatement(sql);
            statement.setString(1,cat);
            statement.setLong(2,ToolBox.now());
            statement.executeUpdate();
            rs = statement.getGeneratedKeys();
            rs.next();
            catID=rs.getInt(1);
        }
        sql="INSERT OR IGNORE INTO item_category_links (item_id, category_id, type) VALUES (?,?,?);";
        statement= dbConnection.prepareStatement(sql);
        statement.setInt(1,docID);
        statement.setInt(2,catID);
        statement.setInt(3,type);
        statement.executeUpdate();
    }

    public void autoSortColumn(ItemTable DT) {
        int c=0;
        if (config.get("autosortcolumn")!=null)
            c=Integer.valueOf(config.get("autosortcolumn"));
        DT.sortItems(c,true);
    }

    /**
     * Add all items in category tmp to the item table IT
     */
    public void showItemsInCategory(Category category,ItemTable IT) throws SQLException {
        if (category==null) {
            return;
        }
        IT.setLibrary(this);
        String sql="SELECT "+shortSQLTags+" FROM item_category_links LEFT JOIN items ON item_category_links.item_id=items.id WHERE category_id=?;";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setInt(1,IT.currentCategory.id);
        ResultSet rs = statement.executeQuery();
        while (rs.next()) {
            IT.addItemFast(new Item(this,rs));
        }
        autoSortColumn(IT);
        IT.ITM.fireTableDataChanged();
        IT.resizeTable(true);
    }

    /**
     * Add all items in category tmp to the item table IT
     */
    public void showItemsAddedAt(int i,ItemTable IT) {
        IT.setLibrary(this);
        long upper=System.currentTimeMillis();
        long lower=0;
        long currentday=LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond()*1000;
        long fullday=1000*60*60*24;
        switch (i) {
            case 0 : 
                lower=currentday;
                break;
            case 1 :
                upper=currentday;
                lower=upper-fullday;
                break;
            case 2 :
                upper=currentday-fullday;
                lower=upper-fullday;
                break;
            case 3 : 
                lower=upper-7*fullday;
                break;
            case 4 : 
                upper=upper-7*fullday;
                lower=upper-7*fullday;
                break;
            case 5 : 
                lower=upper-30*fullday;
                break;
            case 6 : 
                upper=upper-30*fullday;
                lower=upper-30*fullday;
                break;
            case 7 : 
                lower=upper-365*fullday;
                break;
            case 8 : 
                upper=upper-365*fullday;
                lower=upper-365*fullday;
                break;
            default :     
        }
        try {
            String sql="SELECT "+shortSQLTags+" FROM items WHERE created > ? AND created < ?;";
            PreparedStatement statement= dbConnection.prepareStatement(sql);
            statement.setLong(1,lower);
            statement.setLong(2,upper);
            ResultSet rs = statement.executeQuery();
            while (rs.next()) {
                IT.addItemFast(new Item(this,rs));
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        autoSortColumn(IT);
        IT.ITM.fireTableDataChanged();
        IT.resizeTable(true);
    }
    
    /**
     * Add the items with person to itemTable
     * 
     * @param person
     * @param itemTable 
     */
    public void addItemsWithPersonIDs(String ids,ItemTable itemTable) {
        itemTable.setLibrary(this);
        try {
            ResultSet rs=executeResEX("SELECT "+shortSQLTags+" FROM items INNER JOIN item_person_links ON item_person_links.item_id=items.id WHERE item_person_links.person_id IN ("+ids+");");
            while (rs.next()) {
                Item item=new Item(this,rs);
                itemTable.addItemFast(item);
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        autoSortColumn(itemTable);
        itemTable.resizeTable(true);
    }

    /**
     * Add the items with keyword to itemTable.
     * 
     * @param keyword
     * @param itemTable 
     */
    public void addItemsWithKeyword(String id,ItemTable itemTable) {
        itemTable.setLibrary(this);        String keys;
        try {
            ResultSet rs=RSC.getCurrentlySelectedLibrary().executeResEX("SELECT "+shortSQLTags+" FROM items INNER JOIN item_keyword_links ON item_keyword_links.item_id=items.id WHERE item_keyword_links.keyword_id="+id+";");
            while (rs.next()) {
                Item item=new Item(this,rs);
                itemTable.addItemFast(item);
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        autoSortColumn(itemTable);
        itemTable.resizeTable(true);
    }
    
    public void setHTMLTemplate(int infoMode, String template) {
        String mode=String.valueOf(infoMode).trim();
        if (!htmlTemplates.containsKey(mode) || (htmlTemplates.get(mode)==null) || !htmlTemplates.get(mode).equals(template)) {
            htmlTemplates.put(mode,new CelsiusTemplate(template));
            try {
                PreparedStatement pstmt=dbConnection.prepareStatement("INSERT OR REPLACE INTO html_templates (mode,template) VALUES(?,?);");
                pstmt.setString(1,mode);
                pstmt.setString(2,template);
                pstmt.execute();
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
    }

    public CelsiusTemplate getHTMLTemplate(int infoMode) {
        String mode=String.valueOf(infoMode).trim();
        if (htmlTemplates.containsKey(mode)) {
            return htmlTemplates.get(mode);
        } else {
            String n="infoMode-"+mode;
            if (RSC.libraryTemplates.get(n)!=null) {
                return(new CelsiusTemplate(RSC.libraryTemplates.get("Default").get(n)));
            } else return(new CelsiusTemplate("Error loading display strings from HTMLtemplates!"));
        }
    }

    public String getCollaborators(String person) {
        final ArrayList<String> coll = new ArrayList<String>();
        String collabs,colab;
        boolean ok;
        for (Item item : this) {
            for (String peopletag : listOf("people")) {
                collabs = item.get(peopletag)+"|";
                int i=collabs.indexOf(person);
                int j=i+person.length();
                if (i>-1) {
                    ok=false;
                    if ((i==0) && ((collabs.charAt(j)==',') || (collabs.charAt(j)=='|'))) ok=true;
                    if ((i>0) && (collabs.charAt(i-1)=='|') && ((collabs.charAt(j)==',') || (collabs.charAt(j)=='|'))) ok=true;
                    if (ok) {
                        while (collabs.length() > 0) {
                            colab=Parser.returnFirstItem(collabs);
                            if (!coll.contains(colab)) {
                                coll.add(colab);
                            }
                            collabs = Parser.cutFirstItem(collabs);
                        }
                    }
                }
            }
        }
        coll.remove(person);
        Collections.sort(coll);
        for (int i=0;i<coll.size();i++) {
            collabs=coll.get(i);
            coll.set(i, Parser.cutFrom(collabs,",").trim()+" "+Parser.cutUntil(collabs, ","));
        }
        String collaborators = coll.toString();
        collaborators = collaborators.substring(1, collaborators.length() - 1);
        return(collaborators);
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

    public Iterator<Item> iterator() {
        return(new LibraryIterator(this));
    }

    private void fillTableTagsWithDefaults() {
        TableTags.add("type");
        TableTags.add("title");
        TableTags.add("authors");
        TableTags.add("identifier");
        ColumnSizes.add(-20);
        ColumnSizes.add(200);
        ColumnSizes.add(100);
        ColumnSizes.add(80);
    }
    
    /*
     * Routines that might be used in the future to split up directory size
    public String adj(int i) {
        String tmp=Integer.toString(i);
        return(tmp+"000".substring(3-tmp.length()));
    }
    
    public String getTargetFolderNumber() {
        int tfn=0;
        String basepath=this.compressDir("LD::items");
        boolean complete=false;
        while (!complete) {
            String dir=basepath+adj(tfn);
            if (!(new File(dir)).exists()) {
                (new File(dir)).mkdir();
            }
            if ((new File(dir)).listFiles().length>999) {
                tfn++;
            } else {
                complete=true;
            }
        }
        return(adj(tfn));
    }
     */
    

    /**
     * modes : 0: move to doc folder, 1: leave where it is
     * return: 0: success, 1: name occupied, 2: couldn't create folder
     */
    public int addNewItem(Item item,String prereg,int mode) {
        String tmp="path";
        if (listOf("essential-fields").length>0) tmp=listOf("essential-fields")[0];
        String TI="LIB"+name+"::"+item.get(tmp)+">";

        // deal with preparation for file
        boolean dealwithfile=false;
        /*String maintrg="";
        if (!props.isEmpty("path")) {
            dealwithfile=true;
            if ((mode ==0) && (!guaranteeStandardFolder(item))) {
                RSC.showWarning("Item folder could not be created.", "Warning:");
                return(2);
            }
            maintrg=getStandardFolder(item)+ToolBox.filesep+standardFileName(item);

            if ((mode==0) && (new File(completeDir(maintrg,""))).exists()) {
                RSC.out(TI+"There is already a file named "+completeDir(maintrg,"")+"!");
                int j=RSC.askQuestionYN("There is already a file named "+maintrg+"!\n Overwrite?","Warning");
                if (j==JOptionPane.NO_OPTION) {
                    return(1);
                }
            }
        }*/
        RSC.out(TI+"Creating new item and preregistering.");
        Item newdoc=createEmptyItem();
        if (prereg.length()!=0) newdoc.put("registered",prereg);

        // Copying all relevant information.
        /*RSC.out(TI+"Copying relevant information.");
        for (String k : item.getFields()) {
//            System.out.println("Writing::"+k+"::"+doc.get(k));
            if ((!k.startsWith("$$")) && (!k.equals("id"))) if (item.get(k)!=null) newdoc.put(k,item.get(k));
        }
        newdoc.put("fullpath", null);
        newdoc.save();*/

        // move addinfo files
        /*for (String key : newdoc.getFields()) {
            if ((newdoc.get(key)!=null) && newdoc.get(key).startsWith("/$")) {
                String a1=newdoc.get(key).substring(2);
                String a2=Parser.CutFrom(a1,"/$");
                a1=Parser.CutTill(a1,"/$");
                if ((new File(a1)).exists()) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + a2,"")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        RSC.out(TI + "Moving file " + a1 + " to " + newdoc.completeDir(tmp + a2));
                        TextFile.moveFile(a1, newdoc.completeDir(tmp + a2));
                        newdoc.put(key, tmp + a2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newdoc.put(key, null);
                }
            } 
        }

        // move item
        if (mode==0) {
            if (dealwithfile) {
                RSC.out(TI+"Moving item and plaintxt.");
                try {
                    TextFile.moveFile(newdoc.get("location"), newdoc.completeDir(maintrg));
                    newdoc.put("location", maintrg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // move plaintxt
                String fn=newdoc.get("plaintxt");
                if (fn!=null) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + ".txt.gz","")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        TextFile.moveFile(fn, newdoc.completeDir(tmp+".txt.gz"));
                        newdoc.put("plaintxt", tmp+".txt.gz");
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (dealwithfile) {
                tmp=newdoc.get("location");
                newdoc.put("location",compressDir(tmp));
            }
        }*/
        newdoc.save();
        RSC.out(TI+"done.");

        // Register item and update AuthorList
        ThreadRegister RT=new ThreadRegister(this,newdoc);
        RT.run();
        // Add new authors to AuthorList
        //addPeople(newdoc);

        // Notify MF of change in library and write addinfo
        lastAddedItem=newdoc;
        return(0);
    }

    /**
     * modes : 0: move to doc folder, 1: leave where it is
     * return: 0: success, 1: name occupied, 2: couldn't create folder
     */
    public int addItem(Item item,String prereg,int mode) {
        String tmp="location";
        if (listOf("essential-fields").length>0) tmp=listOf("essential-fields")[0];
        String TI="LIB"+name+"::"+item.get(tmp)+">";

        // deal with preparation for file
        boolean dealwithfile=false;
        /*String maintrg="";
        if (!item.isEmpty("location")) {
            dealwithfile=true;
            if ((mode ==0) && (!guaranteeStandardFolder(item))) {
                RSC.showWarning("Item folder could not be created.", "Warning:");
                return(2);
            }
            maintrg=getStandardFolder(item)+ToolBox.filesep+standardFileName(item);

            if ((mode==0) && (new File(completeDir(maintrg,""))).exists()) {
                RSC.out(TI+"There is already a file named "+completeDir(maintrg,"")+"!");
                int j=RSC.askQuestionYN("There is already a file named "+maintrg+"!\n Overwrite?","Warning");
                if (j==JOptionPane.NO_OPTION) {
                    return(1);
                }
            }
        }*/
        RSC.out(TI+"Creating new item and preregistering.");
        Item newdoc=createEmptyItem();
        if (prereg.length()!=0) newdoc.put("registered",prereg);

        // Copying all relevant information.
        RSC.out(TI+"Copying relevant information.");
        for (String k : item.getFields()) {
//            System.out.println("Writing::"+k+"::"+doc.get(k));
            if ((!k.startsWith("$$")) && (!k.equals("id"))) if (item.get(k)!=null) newdoc.put(k,item.get(k));
        }
        newdoc.put("fullpath", null);
        newdoc.save();

        // move addinfo files
        /*for (String key : newdoc.getFields()) {
            if ((newdoc.get(key)!=null) && newdoc.get(key).startsWith("/$")) {
                String a1=newdoc.get(key).substring(2);
                String a2=Parser.CutFrom(a1,"/$");
                a1=Parser.CutTill(a1,"/$");
                if ((new File(a1)).exists()) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + a2,"")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        RSC.out(TI + "Moving file " + a1 + " to " + newdoc.completeDir(tmp + a2));
                        TextFile.moveFile(a1, newdoc.completeDir(tmp + a2));
                        newdoc.put(key, tmp + a2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newdoc.put(key, null);
                }
            } 
        }

        // move item
        if (mode==0) {
            if (dealwithfile) {
                RSC.out(TI+"Moving item and plaintxt.");
                try {
                    TextFile.moveFile(newdoc.get("location"), newdoc.completeDir(maintrg));
                    newdoc.put("location", maintrg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // move plaintxt
                String fn=newdoc.get("plaintxt");
                if (fn!=null) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + ".txt.gz","")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        TextFile.moveFile(fn, newdoc.completeDir(tmp+".txt.gz"));
                        newdoc.put("plaintxt", tmp+".txt.gz");
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (dealwithfile) {
                tmp=newdoc.get("location");
                newdoc.put("location",compressDir(tmp));
            }
        }*/
        newdoc.save();
        RSC.out(TI+"done.");

        // Register item and update AuthorList
        ThreadRegister RT=new ThreadRegister(this,newdoc);
        RT.run();
        // Add new authors to AuthorList
        //addPeople(newdoc);

        // Notify MF of change in library and write addinfo
        lastAddedItem=newdoc;
        return(0);
    }

    /**
     * modes : 0: move to doc folder, 1: leave where it is
     * return: 0: success, 1: name occupied, 2: couldn't create folder
     */
    public String includeFile(String path) {
        String tmp="location";
        if (listOf("essential-fields").length>0) tmp=listOf("essential-fields")[0];
        Item item=new Item(this);
        String TI="LIB"+name+"::"+item.get(tmp)+">";

        // deal with preparation for file
        boolean dealwithfile=false;
        String maintrg="";
        /*if (!item.isEmpty("location")) {
            dealwithfile=true;
            if ((mode ==0) && (!guaranteeStandardFolder(item))) {
                RSC.showWarning("Item folder could not be created.", "Warning:");
                return("?");
            }
            maintrg=getStandardFolder(item)+ToolBox.filesep+standardFileName(item);

            if ((mode==0) && (new File(completeDir(maintrg,""))).exists()) {
                RSC.out(TI+"There is already a file named "+completeDir(maintrg,"")+"!");
                int j=RSC.MC.askQuestionYN("There is already a file named "+maintrg+"!\n Overwrite?","Warning");
                if (j==JOptionPane.NO_OPTION) {
                    return(1);
                }
            }
        }
        RSC.out(TI+"Creating new item and preregistering.");
        Item newdoc=createEmptyItem();
        if (prereg.length()!=0) newdoc.put("registered",prereg);

        // Copying all relevant information.
        RSC.out(TI+"Copying relevant information.");
        for (String k : item.getFields()) {
//            System.out.println("Writing::"+k+"::"+doc.get(k));
            if ((!k.startsWith("$$")) && (!k.equals("id"))) if (item.get(k)!=null) newdoc.put(k,item.get(k));
        }
        newdoc.put("fullpath", null);
        newdoc.save();*/

        // move addinfo files
        /*for (String key : newdoc.getFields()) {
            if ((newdoc.get(key)!=null) && newdoc.get(key).startsWith("/$")) {
                String a1=newdoc.get(key).substring(2);
                String a2=Parser.CutFrom(a1,"/$");
                a1=Parser.CutTill(a1,"/$");
                if ((new File(a1)).exists()) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + a2,"")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        RSC.out(TI + "Moving file " + a1 + " to " + newdoc.completeDir(tmp + a2));
                        TextFile.moveFile(a1, newdoc.completeDir(tmp + a2));
                        newdoc.put(key, tmp + a2);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    newdoc.put(key, null);
                }
            } 
        }

        // move item
        if (mode==0) {
            if (dealwithfile) {
                RSC.out(TI+"Moving item and plaintxt.");
                try {
                    TextFile.moveFile(newdoc.get("location"), newdoc.completeDir(maintrg));
                    newdoc.put("location", maintrg);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                // move plaintxt
                String fn=newdoc.get("plaintxt");
                if (fn!=null) {
                    tmp = "LD::information/" + newdoc.get("id");
                    // check if this file is already existing
                    while ((new File(completeDir(tmp + ".txt.gz","")).exists())) {
                        tmp += "X";
                    }
                    if (tmp.indexOf('X')==-1) tmp="AI::";
                    try {
                        TextFile.moveFile(fn, newdoc.completeDir(tmp+".txt.gz"));
                        newdoc.put("plaintxt", tmp+".txt.gz");
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            if (dealwithfile) {
                tmp=newdoc.get("location");
                newdoc.put("location",compressDir(tmp));
            }
        }*/
        /*newdoc.save();
        RSC.out(TI+"done.");

        // Register item and update AuthorList
        ThreadRegister RT=new ThreadRegister(this,newdoc,Msg1);
        RT.run();
        // Add new authors to AuthorList
        //addPeople(newdoc);

        // Notify MF of change in library and write addinfo
        lastAddedItem=newdoc;
        setChanged(true);*/
        return("");
    }
    
    
    /**
     * Replace a found doublette with file contained in doc.
     * @param doc
     */
    public void replaceItem(Item item) {
        Item item2=marker;
        if (item.getS("location").length()>0) {
            String f1=item2.getCompleteDirS("location");
            String ft;
            if (f1.equals("")) {
                f1=completeDir(getStandardFolder(item)+ToolBox.filesep+standardFileName(item),null);
                ft=item.get("filetype");
                item2.put("filetype",ft);
                item2.put("plaintxt", "AI::.txt.gz");
            } else {
                ft=TextFile.getFileType(item.get("location"));
            }
            String f3=item2.getCompleteDirS("plaintxt");
            TextFile.Delete(f1);
            TextFile.Delete(f3);
            if ((ft!=null) && (ft.length()>1)){
                f1=Parser.cutUntilLast(f1,item2.get("filetype"))+ft;
                String src1=item.get("location");
                String src2=item.get("plaintxt");
                item2.put("filetype",ft);
                item2.put("location",compressDir(f1));
                try {
                    TextFile.moveFile(src1, f1);
                    if (src2!=null) TextFile.moveFile(src2, f3);
                } catch(Exception e) {
                    e.printStackTrace();
                }
            }
        }
        for (String t : item.getFields()) {
            if (!(t.equals("location") || (t.equals("plaintxt"))))
                if ((item.get(t)!=null) && (item.get(t).length()>0))
                    item2.put(t,item.get(t));
        }
        item2.save();
        lastAddedItem=item2;
    }

    public String completeDir(String s,String id) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.filesep)) s2=s2.substring(1);
        if (sig.equals("LD")) return(libraryBaseDir+s2);
        if (sig.equals("BD")) return(celsiusBaseDir+s2);
        return(s);
    }
    
    public String completeDir(String s) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.filesep)) s2=s2.substring(1);
        if (sig.equals("LD")) return(libraryBaseDir+s2);
        if (sig.equals("BD")) return(celsiusBaseDir+s2);
        return(s);
    }
    

    public void adjustStyleSheet(StyleSheet styleSheet) {
        for (String rule : StyleSheetRules) {
            styleSheet.addRule(rule);
        }
    }

    public void loadStyleSheetRules() throws IOException {
        StyleSheetRules = new ArrayList<String>();
        if (config.get("style") != null) {
            TextFile style = new TextFile(completeDir(config.get("style"),""));
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

    public void deleteCategoryNode(StructureNode node) {
        try {
            PreparedStatement statement=dbConnection.prepareStatement("DELETE FROM category_tree WHERE id = ?;");
            statement.setInt(1,node.id);
            statement.execute();
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }

    public void updateCategoryNodeParent(StructureNode node) {
        try {
            PreparedStatement statement=dbConnection.prepareStatement("UPDATE category_tree SET parent = ? where id = ?;");
            statement.setInt(1,node.getParent().id);
            statement.setInt(2,node.id);
            statement.execute();
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }
    
    public void updateCategoryNodeChildren(StructureNode node) {
        try {
            PreparedStatement statement=dbConnection.prepareStatement("UPDATE category_tree SET children = ? where id = ?;");
            statement.setString(1,node.getChildListString());
            statement.setInt(2,node.id);
            statement.execute();
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }

    public void renameCategory(StructureNode node, String newLabel) {
        // check if update necessary?
        if (newLabel.equals(categories.get(node.categoryID).label)) return;
        // in memory rename
        categories.get(node.categoryID).label=newLabel;
        // save in database
        try {
            String sql = "UPDATE categories SET label=? where id=?;";
            PreparedStatement statement = dbConnection.prepareStatement(sql);
            statement.setString(1, newLabel);
            statement.setInt(2, node.categoryID);
            statement.executeUpdate();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    // TODO: write: create Category and return corresponding structureNode
    public StructureNode createCategory(String cat, StructureNode parent) {
        StructureNode child=null;
        try {
            ResultSet rs=executeResEX("SELECT id FROM categories where label=? LIMIT 1;",cat);
            if (rs.next()) {
                int i=RSC.askQuestionYN( "Re-use existing category?", "Category name exists");
                if (i==0) {
                    child=new StructureNode(this,rs.getInt(1),0);
                }
            }
            if (child==null) {
                PreparedStatement statement=dbConnection.prepareStatement("INSERT INTO categories (label,created) VALUES (?,?);");
                statement.setString(1,cat);
                long now=ToolBox.now();
                statement.setLong(2,now);
                statement.execute();
                rs = statement.getGeneratedKeys();
                rs.next();
                int id=rs.getInt(1);
                categories.put(id, new Category(this,id,cat,now));
                //
                child=new StructureNode(this,id,0);
            }
            parent.add(child);
            PreparedStatement statement=dbConnection.prepareStatement("INSERT INTO category_tree (category,parent) VALUES (?,?);");
            statement.setInt(1,child.categoryID);
            statement.setInt(2,parent.id);
            statement.execute();
            rs = statement.getGeneratedKeys();
            rs.next();
            int cid=rs.getInt(1);
            child.id=cid;
            updateCategoryNodeChildren(parent);
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        return(child);
    }
    
    
    public void executeEX(String sql) {
        RSC.out("DB::"+sql);
        notifyDBInteraction();
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            statement.execute();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
    }

    public ResultSet executeResEX(String sql) {
        RSC.out("DB::"+sql);
        notifyDBInteraction();
        ResultSet rs=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            rs=statement.executeQuery();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(rs);
    }

    public ResultSet executeResEX(String sql,String data) {
        RSC.out("DB::"+sql);
        notifyDBInteraction();
        ResultSet rs=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            statement.setString(1, data);
            rs=statement.executeQuery();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(rs);
    }
    
    private void notifyDBInteraction() {
        RSC.setTempStatus("Database read/write");
    }

    /**
     * 
     * Delete the item with id from library. Item cleans up after itself, but Library needs to take care of updating all tables etc.
     * 
     * @param id 
     */
    public void deleteItem(String id) {
        /*if (files)
            deleteFiles();
        else {
            TextFile.Delete(getCompleteDirS("plaintxt"));
            put("location",null);
            library.setChanged(true);
        }*/
        executeEX("DELETE * FROM items where id="+id+";");
        // delete all links and remove Keywords/authors if no longer used, integrate into saving mechanism
        try {
            ResultSet old=executeResEX("SELECT keyword_id FROM item_keyword_links WHERE item_id="+id+";");
            ArrayList<String> oldIDs=new ArrayList<>();
            while (old.next()) oldIDs.add(old.getString(1));
            deleteUnusedLinks("keyword",oldIDs);
            executeEX("DELETE * FROM item_keyword_links where item_id="+id+";");
            old=executeResEX("SELECT person_id FROM item_person_links WHERE item_id="+id+";");
            oldIDs=new ArrayList<>();
            while (old.next()) oldIDs.add(old.getString(1));
            deleteUnusedLinks("person",oldIDs);
            executeEX("DELETE * FROM item_person_links where item_id="+id+";");
            old=executeResEX("SELECT attachment_id FROM item_attachment_links WHERE item_id="+id+";");
            String idString="";
            while (old.next()) idString+=","+old.getString(1);
            executeEX("DELETE FROM attachments WHERE id IN ("+idString.substring(1)+");");
            executeEX("DELETE * FROM item_attachment_links where item_id="+id+";");
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        //deleteUnusedLinks(...)
    }
    
    public void deleteUnusedLinks(String linkType, ArrayList<String> ids) {
        try {
            for (String id : ids) {
                ResultSet rs=executeResEX("SELECT * FROM item_"+linkType+"_links WHERE "+linkType+"_id="+id+" LIMIT 1;");
                if (!rs.next()) {
                    executeEX("DELETE FROM "+linkType+"s WHERE id="+id+";");
                }
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        
    }
    
    public void addLibraryChangeListener(LibraryChangeListener lCL) {
        libraryChangeListeners.add(lCL);
    }

    public void removeLibraryChangeListener(LibraryChangeListener lCL) {
        libraryChangeListeners.remove(lCL);
    }
    
    public void itemChanged(String type, String id) {
        for (LibraryChangeListener lCL : libraryChangeListeners) {
            lCL.libraryElementChanged(type,id);
        }
    }

    public boolean doesAttachmentExist(String fn) {
        String cfn=compressDir(fn);
        boolean found=false;
        try {
            PreparedStatement statement=dbConnection.prepareStatement("SELECT * FROM attachments WHERE path = ? LIMIT 1;");
            statement.setString(1,cfn);
            ResultSet rs=statement.executeQuery();
            if (rs.next()) found=true;
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(found);
    }
    
    public String standardFileName(TableRow tableRow) {
        String n=Parser.cutProhibitedChars2(namingConvention.fillIn(tableRow));
        String ft=tableRow.get("attachment-filetype");
        if (n.length()>150) {
            if (n.endsWith("."+ft)) {
                n=n.substring(0,149-ft.length())+"."+ft;
            } else n=n.substring(0,150);
        }
        return(n);
    }

    public String getStandardFolder(TableRow tableRow) {
        String ifolder=config.get("item-folder");
        if ((ifolder==null) || (ifolder.equals(""))) ifolder="LD::items";
        String folder=itemFolder.fillIn(tableRow);
        folder=Parser.replace(folder, "\\\\", "\\");
        return(folder);
    }
    

    /**
     * Returns true if field needs special attention when saving
     * 
     * @param field 
     */
    public boolean linkedField(String field) {
        if (field.equals("keywords")) return(true);
        if (field.startsWith("attachment-")) return(true);
        for (String person : peopleFields) {
            if (field.equals(person)) return(true);
            if (field.equals(person+"_ids")) return(true);
            if (field.equals(person+"short_")) return(true);
        }
        return(false);
    }

    private Item getItemForAttachment(int aid) {
        ResultSet rs=executeResEX("SELECT item_id FROM item_attachment_links WHERE attachment_id="+String.valueOf(aid)+" LIMIT 1;");
        try {
            while (rs.next()) {
                return(new Item(this,rs.getInt(1)));
            };
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        return(null);
    }

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
