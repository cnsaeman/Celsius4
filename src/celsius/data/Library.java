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
import celsius.gui.CelsiusTable;
import celsius.gui.RulesNode;
import celsius.gui.SafeMessage;
import celsius.gui.GUIToolBox;
import celsius.tools.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.util.zip.GZIPInputStream;

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
    public Connection searchDBConnection;
    
    public String mainLibraryFile;
    public HashMap<String,String> config;
    public HashMap<String,CelsiusTemplate> htmlTemplates;

    public CelsiusTemplate itemRepresentation;
    public CelsiusTemplate itemSortRepresentation;
    public CelsiusTemplate itemNamingConvention;
    public CelsiusTemplate itemFolder;

    public StructureNode structureTreeRoot;
    public HashMap<Integer,Category> itemCategories;

    // Metainformation on the Library
    public String name;
    public String baseFolder;
    public String[] itemSearchFields;
    public String[] personSearchFields;
    public ArrayList<String> hideFunctionality;
    public ArrayList<String> linkedFields;

    public ArrayList<String> itemTableHeaders;
    public ArrayList<String> itemTableTags;
    public ArrayList<String> itemTableColumnTypes;
    public ArrayList<Integer> itemTableColumnSizes;
    public String itemTableSQLTags;

    public ArrayList<String> personTableHeaders;
    public ArrayList<String> personTableTags;
    public ArrayList<String> personTableColumnTypes;
    public ArrayList<Integer> personTableColumnSizes;
    public String personTableSQLTags;

    public ArrayList<String> styleSheetRules;
    public LinkedHashMap<String,ArrayList<String>> usbdrives;
    public HashMap<String,String> iconDictionary;

    // Buffered information on the Library
    public String[] peopleFields;
    public String[] iconFields;
    public HashMap<String,ArrayList<String>> choiceFields;
    public HashMap<String,ArrayList<String>> Links; // Links is set from createLinksTree
    public HashMap<String,ArrayList<String>> LinksRef; // Links is set from createLinksTree
    
    public ArrayList<String> orderedItemPropertyKeys; 
    public HashSet<String> itemPropertyKeys;
    public ArrayList<String> orderedPersonPropertyKeys; 
    public HashSet<String> personPropertyKeys;
    public final HashSet<String> attachmentPropertyKeys=new HashSet<String>((List<String>)Arrays.asList("name","filetype","path","pages","source","md5","createdTS"));     
    
    public final ArrayList<LibraryChangeListener> libraryChangeListeners;
    
    public String[] shortKeys;
    
    // Size information
    public int numberOfItems;
    public int numberOfPeople;
    public int sizeOfItems;

    public String LastErrorMessage;

    public int currentStatus;
    
    public int addItemMode;
    
    public int addingMode; // 0 : leave things where they are, 1 : move to items folder
    
    /** 
     * Creates a new Library 
     * 
     * TODO fix adjust
     */
    public Library(String bd,String mainfile,String nm,Resources rsc, String template) throws Exception {
        addItemMode = 0;
        currentStatus=0;
        peopleFields=null;
        RSC=rsc;
        //celsiusBaseDir=Parser.cutUntilLast((new File(".")).getAbsolutePath(),".");
        styleSheetRules=new ArrayList<String>();
        baseFolder=bd;
        //if (!celsiusBaseDir.endsWith(ToolBox.filesep)) celsiusBaseDir+=ToolBox.filesep;
        if (!baseFolder.endsWith(ToolBox.filesep)) baseFolder+=ToolBox.filesep;
        name=nm;
        
        (new File(baseFolder)).mkdir();
        (new File(baseFolder+"information")).mkdir();
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

        TextFile TD=new TextFile(baseFolder+"style.css",false);
        TD.putString(RSC.libraryTemplates.get(template).get("stylesheet"));
        TD.close();

        loadStyleSheetRules();
        
        TD=new TextFile(baseFolder+"librarystructure.xml",false);
        TD.putString(RSC.libraryTemplates.get(template).get("librarystructure"));
        TD.close();
        
        TD=new TextFile(baseFolder+"rules.xml",false);
        TD.putString(RSC.libraryTemplates.get(template).get("libraryrules"));
        TD.close();
                
        createHTMLTemplates(template);
                
        getFieldsFromConfig();

        libraryChangeListeners=new ArrayList<>();
        addingMode=1;
    }
    
    /** Loads an existing library, standard method to open Library */
    public Library(String folderName,Resources rsc) {
        addItemMode = 0;
        currentStatus=0;
        peopleFields=null;
        libraryChangeListeners=new ArrayList<>();
        RSC=rsc;
        try {
            // check that library is not open already
            for (Library lib : RSC.libraries) {
                if (lib.name.equals(name)) {
                    (new SafeMessage("A library with this name is already loaded.","Library not loaded:",0)).showMsg();
                    name="??#$Library with this name is already loaded.";
                    return;
                }
            }

            // set base folder and file 
            baseFolder=folderName;
            if (!baseFolder.endsWith(ToolBox.filesep)) baseFolder+=ToolBox.filesep;

            // open main database
            mainLibraryFile=baseFolder+"CelsiusLibrary.sql";
            String url="jdbc:sqlite:"+mainLibraryFile;
            dbConnection = DriverManager.getConnection(url);
            // check if connection locked
            try {
                dbConnection.prepareStatement("BEGIN EXCLUSIVE").execute();
                dbConnection.prepareStatement("COMMIT").execute();
            } catch (Exception e) {
                currentStatus=20;
                this.name="??##";
                return;
            }
            RSC.out("Lib>Connection established to SQLite database "+url);
            
            // Read configuration
            config=new HashMap<>();
            ResultSet rs=dbConnection.prepareStatement("SELECT * FROM configuration;").executeQuery();
            while (rs.next()) {
                config.put(rs.getString(1), rs.getString(2));
            }
            getFieldsFromConfig();
            
            // open search database
            String surl="jdbc:sqlite:"+baseFolder+"CelsiusSearchIndex.sql";
            searchDBConnection = DriverManager.getConnection(surl);
            RSC.out("Lib>Connection established to index database "+surl);

            // identify links
            linkedFields=new ArrayList<>();
            rs=dbConnection.prepareStatement("SELECT name FROM sqlite_master WHERE type='table';").executeQuery();
            while (rs.next()) {
                String table=rs.getString(1);
                if (table.startsWith("item_") && table.endsWith("_links")) {
                    table=table.substring(5,table.length()-6);
                    linkedFields.add(table);
                }
            }
            
            // read in item fields
            StringBuffer shorts=new StringBuffer(); // create list of shorts
            rs=dbConnection.prepareStatement("PRAGMA table_info('items');").executeQuery();
            itemPropertyKeys=new HashSet<String>();
            StringBuffer tmpKeys=new StringBuffer("id");
            while (rs.next()) {
                String key=rs.getString(2);
                if (!key.equals("id") && !key.equals("attributes")) {
                    itemPropertyKeys.add(key);
                    tmpKeys.append('|');
                    if (key.startsWith("short_")) {
                        shorts.append('|');
                        shorts.append(key.substring(6));
                        tmpKeys.append(key.substring(6));                        
                    } else {
                        tmpKeys.append(key);
                    }
                }
            }
            orderedItemPropertyKeys=new ArrayList<>();
            for (String key : ToolBox.stringToArray(tmpKeys.toString())) {
                orderedItemPropertyKeys.add(key);
            }
            shortKeys=ToolBox.stringToArray(shorts.substring(1));
            
            rs=dbConnection.prepareStatement("PRAGMA table_info('persons');").executeQuery();
            personPropertyKeys=new HashSet<String>();
            tmpKeys=new StringBuffer("id");
            while (rs.next()) {
                String key=rs.getString(2);
                if (!key.equals("id") && !key.equals("attributes")) {
                    tmpKeys.append('|');
                    if (!key.startsWith("short_")) {
                        tmpKeys.append(key);
                    } else {
                        tmpKeys.append(key.substring(6));
                    }
                    personPropertyKeys.add(key);
                }
            }
            orderedPersonPropertyKeys=new ArrayList<>();
            for (String key : ToolBox.stringToArray(tmpKeys.toString())) {
                orderedPersonPropertyKeys.add(key);
            }

            if ((name.length()==0) || (baseFolder.length()==0)) {
                (new SafeMessage("The library file seems to be corrupt. Cancelling...","Warning:",0)).showMsg();
                name="??##Library file corrupt.";
                return;
            }
            loadStyleSheetRules();
            for (String field : RSC.LibraryFields) {
                ensure(field);
            }
            
            // read categories
            itemCategories=new HashMap<>();
            rs=dbConnection.createStatement().executeQuery("SELECT * FROM item_categories;");
            while (rs.next()) {
                Category category=new Category(this,rs);
                itemCategories.put(rs.getInt(1), category);
            }
            // read category structures
            rs=dbConnection.createStatement().executeQuery("SELECT * FROM category_tree;");
            structureTreeRoot=StructureNode.readInFromResultSet(this, rs);

            // Read html templates
            htmlTemplates=new HashMap<>();
            htmlTemplates.put("-1",new CelsiusTemplate(RSC, "<html><body><h2>Currently selected library: #library.name#</h2><hr></body></html>"));
            rs=dbConnection.prepareStatement("SELECT * FROM html_templates;").executeQuery();
            while (rs.next()) {
                htmlTemplates.put(rs.getString(1), new CelsiusTemplate(RSC,rs.getString(2)));
            }
            itemRepresentation=new CelsiusTemplate(RSC,config.get("item-representation"));
            itemSortRepresentation=new CelsiusTemplate(RSC,config.get("item-sort-representation"));
            itemNamingConvention=new CelsiusTemplate(RSC,config.get("item-naming-convention"));
            String itemFolderTemplate=config.get("item-folder");
            if ((itemFolderTemplate==null) || (itemFolderTemplate.equals(""))) itemFolderTemplate="LD::documents";
            itemFolder=new CelsiusTemplate(RSC,itemFolderTemplate);

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
        addingMode=1;
    }
    
    // TODO
    private RulesNode createNode(int i,String s,String l) {
        RulesNode SN;
        if (i>-1) {
            SN=new RulesNode((new Item(this,i)).toText(false));
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

    public DefaultTreeModel createLinksTree(Item item) {
        Links=new HashMap<String,ArrayList<String>>();
        LinksRef=new HashMap<String,ArrayList<String>>();
        RulesNode root=new RulesNode("Available Links");
        if (item.get("links")!=null) {
            String[] links=item.get("links").split("\\|");
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
        XMLHandler.Create("celsiusv2.2.htmltemplates",baseFolder+"htmltemplates.xml");
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
        iconFields=configToArray("icon-fields");
        
        peopleFields=configToArray("person-fields");

        hideFunctionality=configToArrayList("hide");
        
        usbdrives=new LinkedHashMap<String,ArrayList<String>>();
        if (config.get("usbdrives")!=null) {
            String[] usbfields=configToArray("usbdrives");
            for (String usbfield : usbfields) {
                ArrayList<String> list=new ArrayList<>();
                String[] lst = Parser.cutFrom(usbfield, ":").split("\\:");
                list.addAll(Arrays.asList(lst));
                usbdrives.put(Parser.cutUntil(usbfield, ":"), list);
            }
        }
        choiceFields=new HashMap<String,ArrayList<String>>();
        if (config.get("choice-fields")!=null) {
            String[] choicefields=configToArray("choice-fields");
            for (String choicefield : choicefields) {
                String field = Parser.cutUntil(choicefield, ":");
                String[] possibilities = Parser.cutFrom(choicefield, ":").split(",");
                ArrayList<String> poss=new ArrayList<>();
                poss.addAll(Arrays.asList(possibilities));
                choiceFields.put(field, poss);
            }
        }
        iconDictionary=new HashMap<String,String>();
        if (config.get("icon-dictionary")!=null) {
            String[] iconDictionary=ToolBox.stringToArray(config.get("icon-dictionary"));
            for (String icon : iconDictionary) {
                String field = Parser.cutUntil(icon, ":");
                String value = Parser.cutFrom(icon, ":");
                this.iconDictionary.put(field,value);
            }
        }
        itemSearchFields=ToolBox.stringToArray(config.get("item-search-fields"));
        personSearchFields=ToolBox.stringToArray(config.get("person-search-fields"));
        initTablePresets();
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
    public String[] configToArray(String key) {
        return(ToolBox.stringToArray(config.get(key)));
    }

    public ArrayList<String> configToArrayList(String key) {
        return(ToolBox.stringToArrayList(config.get(key)));
    }
    
    private void initTablePresets() {
        itemTableTags = new ArrayList<>();
        itemTableHeaders = new ArrayList<>();
        itemTableColumnSizes = new ArrayList<>();
        itemTableColumnTypes = new ArrayList<>();
        personTableTags = new ArrayList<>();
        personTableHeaders = new ArrayList<>();
        personTableColumnSizes = new ArrayList<>();
        personTableColumnTypes = new ArrayList<>();
        
        if (emptyConfigFor("item-table-column-fields")) {
            fillItemTableTagsWithDefaults();
        } else {
            itemTableTags=configToArrayList("item-table-column-fields");
            String[] list={};
            if (config.get("item-table-column-sizes") != null) {
                list = configToArray("item-table-column-sizes");
                for (String element : list) {
                    itemTableColumnSizes.add(Integer.valueOf(element));
                }
                if (list.length<itemTableTags.size()) {
                    for (int i=list.length;i<itemTableTags.size();i++)
                        itemTableColumnSizes.add(1);
                }
            } else {
                for (String list1 : list) {
                    itemTableColumnSizes.add(1);
                }
            }
            if (config.get("item-table-column-types") != null) {
                itemTableColumnTypes=configToArrayList("item-table-column-types");
                for (int i = itemTableColumnTypes.size(); i < itemTableTags.size(); i++) {
                    itemTableColumnTypes.add("text");
                }
            } else {
                for (String list1 : list) {
                    itemTableColumnTypes.add("text");
                }
            }
            itemTableHeaders=configToArrayList("item-table-column-headers");
        }
        itemTableSQLTags = "items.id";
        for (String tag : itemTableTags) {
            itemTableSQLTags += "," + tag;
        }
        
        if (emptyConfigFor("person-table-column-fields")) {
            fillPersonTableTagsWithDefaults();
        } else {
            personTableTags=configToArrayList("person-table-column-fields");
            String[] list={};
            if (config.get("table-column-sizes") != null) {
                list = configToArray("person-table-column-sizes");
                for (String element : list) {
                    personTableColumnSizes.add(Integer.valueOf(element));
                }
                if (list.length<itemTableTags.size()) {
                    for (int i=list.length;i<itemTableTags.size();i++)
                        personTableColumnSizes.add(1);
                }
            } else {
                for (String list1 : list) {
                    personTableColumnSizes.add(1);
                }
            }
            if (config.get("person-table-column-types") != null) {
                personTableColumnTypes=configToArrayList("person-table-column-types");
                for (int i = personTableColumnTypes.size(); i < personTableTags.size(); i++) {
                    personTableColumnTypes.add("text");
                }
            } else {
                for (String list1 : list) {
                    personTableColumnTypes.add("text");
                }
            }
            personTableHeaders=configToArrayList("person-table-column-headers");
        }        
        personTableSQLTags = "persons.id";
        for (String tag : personTableTags) {
            personTableSQLTags += "," + tag;
        }
    }

    public void setColumnSize(int c,int w) {
        itemTableColumnSizes.set(c, w);
        String s="";
        for (Integer i : itemTableColumnSizes) {
            if (i==0) i=1;
            s+="|"+Integer.toString(i);
        }
        s=s.substring(1);
        putConfig("columnsizes", s);
    }
    
    public void close() {
        FileTools.deleteIfExists(baseFolder + "/lock");
        FileTools.deleteIfExists(baseFolder + "/modified");
        try { 
            dbConnection.close();
            searchDBConnection.close();
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
            RSC.out(TI + "Removing base folder " + baseFolder);
            if (!(FileTools.removeFolder(baseFolder))) {
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
        numberOfItems=0;
        sizeOfItems=0;
        try {
            ResultSet rs=this.dbConnection.prepareStatement("SELECT COUNT(*) from items;").executeQuery();
            if (rs.next()) numberOfItems=rs.getInt(1);
            rs=this.dbConnection.prepareStatement("SELECT COUNT(*) from persons;").executeQuery();
            if (rs.next()) numberOfPeople=rs.getInt(1);
            rs=this.dbConnection.prepareStatement("SELECT SUM(pages) FROM attachments;").executeQuery();
            if (rs.next()) sizeOfItems=rs.getInt(1);
        } catch (Exception e) {
            RSC.outEx(e);
        }    
    }
    
    // Status string for status bar.
    public String Status(boolean count) {
        String tmp = "Current library "+name+": ";
        updateSizeInfo();
        if (numberOfItems==0) return("No items in current library.");
        tmp+=Integer.toString(sizeOfItems) + " pages in ";
        tmp+=Integer.toString(numberOfItems) + " items";
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
    
    public void registerItem(Item item, StructureNode node, int linktype) throws SQLException {
        // get id of category
        if (node==null) return;
        registerItem(item,node.category.id,linktype);
    }

    public void registerItem(Item item, String catID, int linkType) throws SQLException {
        String[] data={item.id, catID, String.valueOf(linkType)};
        executeEX("INSERT OR IGNORE INTO item_category_links (item_id, category_id, link_type) VALUES (?,?,?);", data);
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
    
    public boolean isChoiceField(String key) {
        return(choiceFields.keySet().contains(key));
    }
    
    /**
     * Check for doublettes of the first attachment
     *
     * Return values: 100 : unique-fields not unique, 10 : exact doublette, 5: file might be doublette, 4 : apparent Doublette,  0 : no doublette
     * 
     */
    public DoubletteResult isDoublette(Item item) throws IOException {
        // Look for doublettes
        Attachment attachment=null;
        if (item.linkedAttachments.size()>0) attachment=item.linkedAttachments.get(0);
        try {
            String[] uf = configToArray("unique-fields");
            String sql = "SELECT "+itemTableSQLTags+" FROM items WHERE ";
            String cond = "";
            ArrayList<String> params = new ArrayList<>();
            for (String key : uf) {
                String cV = item.get(key);
                if ((cV != null) && (!cV.equals("<unknown>")) && (!cV.equals(""))) {
                    cond += " OR `"+key+"`=?";
                    params.add(cV);
                }
            }
            if (!cond.equals("")) {
                sql = sql + cond.substring(4) + " LIMIT 1;";
                RSC.out("Lib>Doublette check: "+sql);
                ResultSet rs=executeResEX(sql, params);
                if (rs.next()) {
                    Item doublette=new Item(this,rs);
                    RSC.out("Lib>Found doublette item: " + doublette.toText(false));
                    return (new DoubletteResult(100,doublette));
                }
            }
            if (attachment==null){
                return (new DoubletteResult(0, null));
            } else {
                String md5 = FileTools.md5checksum(item.getCompletedDir(attachment.get("path")));
                if (md5 != null) {
                    ResultSet rs = executeResEX("SELECT item_id from attachments LEFT JOIN item_attachment_links ON attachments.id=attachment_id WHERE md5=?;",md5);
                    if (rs.next()) {
                        Item doublette = new Item(this, rs.getString(1));
                        doublette.loadLevel(1);
                        return (new DoubletteResult(10, doublette));
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
        String[] essentialFields=configToArray("essential-fields");
        for (int i=0; i<essentialFields.length;i++) {
            if (sourceItem.get(essentialFields[i])==null) {
                RSC.showWarning("The item "+sourceItem.toText(false)+"\ncould not be copied, as the field "+essentialFields[i]+",\nrequired by the library "+this.name+" is not set.", "Copying cancelled...");
                return;
            }
        }
        RSC.out(TI+"Copying item "+sourceItem.toText(false)+" from library "+sourceItem.library.name+" to "+name);

        String filename="";
        String fullfilename;
        
        Item targetItem;
        
        // TODO rewrite this, rest is done

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
    
    public String compressFilePath(String s) {
        if (s.startsWith(baseFolder)) return("LD::"+Parser.cutFrom(s,baseFolder));
        if (s.startsWith(RSC.celsiusBaseFolder)) return("BD::"+Parser.cutFrom(s,RSC.celsiusBaseFolder));
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
    public String getRawData(int i) {
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
        String tmp = "";
        /*XMLHandler xh = PeopleRemarks;
        String s2;
        if (xh == null) {
            return ("Corresponding XMLHandler does not exist.");
        }
        for (String tag : xh.XMLTags) {
            s2 = xh.get(tag);
            if ((s2 != null) && !(s2.indexOf("\n") > -1)) {
                tmp += tag + ": " + s2 + ToolBox.linesep;
            }
        }*/
        return (tmp);
    }
    
    private void register(int docID, String cat, int type) throws SQLException {
        // get id of category
        String sql="Select id from item_categories where label=?;";
        PreparedStatement statement= dbConnection.prepareStatement(sql);
        statement.setString(1,cat);
        ResultSet rs = statement.executeQuery();
        int catID=0;
        if (rs.next()) {
            catID=rs.getInt(1);
        } else {
            sql="INSERT INTO item_categories (label,created) VALUES (?,?);";
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

    public void autoSortColumn(CelsiusTable celsiusTable) {
        int c=0;
        if (config.get("autosortcolumn")!=null)
            c=Integer.valueOf(config.get("autosortcolumn"));
        celsiusTable.sortItems(c,true);
    }

    /**
     * List all items in given category in celsiusTable
     * 
     * @param category
     * @param celsiusTable
     * @throws SQLException 
     */
    public void showItemsInCategory(Category category,CelsiusTable celsiusTable) throws SQLException {
        if (category==null) {
            return;
        }
        celsiusTable.setLibraryAndTableType(this,CelsiusTable.TABLETYPE_ITEMS_IN_CATEGORY);
        celsiusTable.addRows(executeResEX("SELECT "+itemTableSQLTags+" FROM item_category_links LEFT JOIN items ON item_category_links.item_id=items.id WHERE category_id=?;", category.id));
        autoSortColumn(celsiusTable);
        celsiusTable.celsiusTableModel.fireTableDataChanged();
        celsiusTable.resizeTable(true);
    }
    
    public String getNumberOfItemsForPerson(TableRow tr) {
        try {
            ResultSet rs = executeResEX("SELECT COUNT(*) FROM item_person_links JOIN items ON item_person_links.item_id=items.id WHERE person_id=" + tr.id + ";");
            return (rs.getString(1));
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return("Error");
    }

    public String getNumberOfPagesForPerson(TableRow tr) {
        try {
            ResultSet rs = executeResEX("SELECT SUM(pages) FROM item_person_links JOIN item_attachment_links ON item_person_links.item_id=item_attachment_links.item_id JOIN attachments ON item_attachment_links.attachment_id=attachments.id WHERE person_id=" + tr.id + ";");
            return (rs.getString(1));
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return("Error");
    }

    public int getNumberOfItemsForPeople(ArrayList<TableRow> tableRows) {
        int total=0;
        if (tableRows.size()>0) {
            StringBuffer ids = new StringBuffer();
            for (TableRow item : tableRows) {
                ids.append(",");
                ids.append(item.id);
            }
            String sql = "SELECT COUNT(*) FROM item_person_links JOIN items ON item_person_links.item_id=items.id WHERE person_id IN (" + ids.toString().substring(1) + ");";
            ResultSet rs = executeResEX(sql);
            try {
                if (rs.next()) {
                    total = rs.getInt(1);
                }
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
        return(total);        
    }
    
    public int getPagesForItems(ArrayList<TableRow> tableRows) {
        int pages=0;
        if (tableRows.size()>0) {
            StringBuffer ids = new StringBuffer();
            for (TableRow item : tableRows) {
                ids.append(",");
                ids.append(item.id);
            }
            String sql = "SELECT SUM(pages) FROM item_attachment_links JOIN attachments ON item_attachment_links.attachment_id=attachments.id WHERE item_attachment_links.item_id in (" + ids.toString().substring(1) + ");";
            ResultSet rs = executeResEX(sql);
            try {
                if (rs.next()) {
                    pages = rs.getInt(1);
                }
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
        return(pages);        
    }

    /**
     * Add all items in category tmp to the item table IT
     */
    public void showItemsAddedAt(int i,CelsiusTable itemTable) {
        itemTable.setLibraryAndTableType(this,CelsiusTable.TABLETYPE_ITEM_WHEN_ADDED);
        long upper=System.currentTimeMillis()/1000;
        long lower=0;
        long currentday=LocalDate.now().atStartOfDay().atZone(ZoneId.systemDefault()).toEpochSecond();
        long fullday=60*60*24;
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
            itemTable.addRows(executeResEX("SELECT "+itemTableSQLTags+" FROM items WHERE createdTS > "+Long.toString(lower)+" AND createdTS < "+Long.toString(upper)+";"));
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        autoSortColumn(itemTable);
        itemTable.celsiusTableModel.fireTableDataChanged();
        itemTable.resizeTable(true);
    }
    
    /**
     * Add the items with person to itemTable
     * 
     * @param person
     * @param itemTable 
     */
    public void showItemsWithPersonIDs(String ids,CelsiusTable itemTable) {
        itemTable.setLibraryAndTableType(this,CelsiusTable.TABLETYPE_ITEMS_OF_PERSONS);
        try {
            itemTable.addRows(executeResEX("SELECT "+itemTableSQLTags+" FROM items INNER JOIN item_person_links ON item_person_links.item_id=items.id WHERE item_person_links.person_id IN ("+ids+");"));
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
     * @param celsiusTable 
     */
    public void showItemsWithKeyword(String id,CelsiusTable celsiusTable) {
        celsiusTable.setLibraryAndTableType(this,CelsiusTable.TABLETYPE_ITEM_WITH_KEYWORD);        String keys;
        try {
            celsiusTable.addRows(executeResEX("SELECT "+itemTableSQLTags+" FROM items INNER JOIN item_keyword_links ON item_keyword_links.item_id=items.id WHERE item_keyword_links.keyword_id="+id+";"));
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        autoSortColumn(celsiusTable);
        celsiusTable.resizeTable(true);
    }
    
    public void setHTMLTemplate(int infoMode, String template) {
        String mode=String.valueOf(infoMode).trim();
        if (!htmlTemplates.containsKey(mode) || (htmlTemplates.get(mode)==null) || !htmlTemplates.get(mode).equals(template)) {
            htmlTemplates.put(mode,new CelsiusTemplate(RSC,template));
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
                return(new CelsiusTemplate(RSC,RSC.libraryTemplates.get("Default").get(n)));
            } else return(new CelsiusTemplate(RSC,"Error loading display strings from HTMLtemplates!"));
        }
    }

    public String getCollaborators(String person) {
        final ArrayList<String> coll = new ArrayList<String>();
        String collabs,colab;
        boolean ok;
        for (Item item : this) {
            for (String peopletag : configToArray("people")) {
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


    public Iterator<Item> iterator() {
        return(new LibraryIterator(this));
    }

    private void fillItemTableTagsWithDefaults() {
        itemTableTags.add("type");
        itemTableTags.add("title");
        itemTableTags.add("authors");
        itemTableTags.add("identifier");
        itemTableHeaders.add("");
        itemTableHeaders.add("Title");
        itemTableHeaders.add("Authors");
        itemTableHeaders.add("Identifiers");
        itemTableColumnSizes.add(-20);
        itemTableColumnSizes.add(200);
        itemTableColumnSizes.add(100);
        itemTableColumnSizes.add(80);
    }

    private void fillPersonTableTagsWithDefaults() {
        personTableTags.add("last_name");
        personTableTags.add("first_name");
        personTableHeaders.add("Last name");
        personTableHeaders.add("First name");
        personTableColumnSizes.add(200);
        personTableColumnSizes.add(100);
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
     * modes : 0: move to item folder, 1: leave where it is
     * return: 0: success, 1: name occupied, 2: couldn't create folder
     */
    public int addNewItem(Item item) {
        item.currentLoadLevel=3;

        // save the item
        item.save();

        RSC.out("done.");

        // Notify MF of change in library and write addinfo
        return(0);
    }

    /**
     * modes : 0: move to doc folder, 1: leave where it is
     * return: 0: success, 1: name occupied, 2: couldn't create folder
     */
    public String includeFile(String path) {
        String tmp="location";
        if (configToArray("essential-fields").length>0) tmp=configToArray("essential-fields")[0];
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
    
    public String completeDir(String s,String id) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.filesep)) s2=s2.substring(1);
        if (sig.equals("LD")) return(baseFolder+s2);
        if (sig.equals("BD")) return(RSC.celsiusBaseFolder+s2);
        return(s);
    }
    
    public String completeDir(String s) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.filesep)) s2=s2.substring(1);
        if (sig.equals("LD")) return(baseFolder+s2);
        if (sig.equals("BD")) return(RSC.celsiusBaseFolder+s2);
        return(s);
    }
    

    public void adjustStyleSheet(StyleSheet styleSheet) {
        for (String rule : styleSheetRules) {
            styleSheet.addRule(rule);
        }
    }

    public void loadStyleSheetRules() throws IOException {
        styleSheetRules = new ArrayList<String>();
        if (config.get("style") != null) {
            TextFile style = new TextFile(completeDir(config.get("style"),""));
            while (style.ready()) {
                styleSheetRules.add(style.getString());
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
        if (choiceFields.containsKey(t)) {
            return(new ObjectComparatorSelection(t,invertSort,choiceFields.get(t)));
        } 
        return (new ObjectComparatorText(t, invertSort,ty));
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
        if (newLabel.equals(node.category.label)) return;
        // in memory rename
        node.category.label=newLabel;
        // save in database
        try {
            String sql = "UPDATE item_categories SET label=? where id=?;";
            PreparedStatement statement = dbConnection.prepareStatement(sql);
            statement.setString(1, newLabel);
            statement.setString(2, node.category.id);
            statement.executeUpdate();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    // TODO: write: create Category and return corresponding structureNode
    public StructureNode createCategory(String cat, StructureNode parent) {
        StructureNode child=null;
        try {
            ResultSet rs=executeResEX("SELECT id FROM item_categories where label=? LIMIT 1;",cat);
            if (rs.next()) {
                int i=RSC.askQuestionYN( "Re-use existing category?", "Category name exists");
                if (i==0) {
                    child=new StructureNode(this,null,0); //rs.getInt(1),0);
                }
            }
            if (child==null) {
                PreparedStatement statement=dbConnection.prepareStatement("INSERT INTO item_categories (label) VALUES (?);");
                statement.setString(1,cat);
                statement.execute();
                rs = statement.getGeneratedKeys();
                rs.next();
                int id=rs.getInt(1);
                Category category=new Category(this,String.valueOf(id),cat);
                itemCategories.put(id, category);
                //
                child=new StructureNode(this,category,0);
            }
            parent.add(child);
            PreparedStatement statement=dbConnection.prepareStatement("INSERT INTO category_tree (category,parent) VALUES (?,?);");
            statement.setString(1,child.category.id);
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
        RSC.out(10,"DB::"+sql);
        notifyDBInteraction();
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            statement.execute();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
    }

    public ResultSet executeResEX(String sql) {
        RSC.out(10,"DB::"+sql);
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
        return(executeResEX(sql,new String[] {data}));
    }
    
    public ResultSet executeResEX(String sql,ArrayList<String> data) {
        RSC.out(10,"DB::"+sql);
        notifyDBInteraction();
        ResultSet rs=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            int i=1;
            for (String datum : data) {
                statement.setString(i,datum);
                i++;
            }
            rs=statement.executeQuery();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(rs);
    }

    public ResultSet executeResEX(String sql,String[] data) {
        RSC.out(10,"DB::"+sql);
        notifyDBInteraction();
        ResultSet rs=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            int i=1;
            for (String datum : data) {
                statement.setString(i,datum);
                i++;
            }
            rs=statement.executeQuery();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(rs);
    }
    
    public String executeInsertEX(String sql,String[] data) {
        RSC.out("DB::"+sql);
        notifyDBInteraction();
        String out=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            int i=1;
            for (String datum : data) {
                statement.setString(i,datum);
                i++;
            }
            statement.execute();
            ResultSet generatedKeys = statement.getGeneratedKeys();
            out=String.valueOf(generatedKeys.getLong(1));
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
        return(out);
    }

    public void executeEX(String sql,String[] data) {
        RSC.out(10,"DB::"+sql);
        notifyDBInteraction();
        ResultSet rs=null;
        try {
            PreparedStatement statement=dbConnection.prepareStatement(sql);
            int i=1;
            for (String datum : data) {
                statement.setString(i,datum);
                i++;
            }
            statement.execute();
        } catch(Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    private void notifyDBInteraction() {
        RSC.setTempStatus("Database read/write");
    }
    
    /**
     * Find ids in table of entries described by list in description
     * 
     * @param table
     * @param description
     * @return
     * @throws SQLException 
     */
    public ArrayList<String> findIDs(String table, String description) throws SQLException {
        ArrayList<String> out=new ArrayList<>();
        if ((description!=null) && !description.isEmpty()) {
            String[] descriptionList=ToolBox.stringToArray(description);
            ArrayList<String> data=new ArrayList<>();
            StringBuffer sql=new StringBuffer();
            sql.append("SELECT GROUP_CONCAT(id, ',') AS result FROM ");
            sql.append(table);
            sql.append(" WHERE ");
            for(String itemDescription : descriptionList) {
                String[] desc=itemDescription.split(":");
                sql.append('`');
                sql.append(desc[0]);
                sql.append('`');
                sql.append("=? OR ");
                data.add(desc[1]);
            }
            sql.delete(sql.length()-4, sql.length());
            sql.append(';');
            ResultSet rs=executeResEX(sql.toString(),data);
            if (rs.next()) {
                String[] ids=ToolBox.stringToArray2(rs.getString(1));
                for (String id : ids) {
                    out.add(id);
                }
            }
        }
        return(out);
    }
    
    public ArrayList<String> getIDArrayList(String sql) throws SQLException {
        // get currently used keyword IDs
        ArrayList<String> out = new ArrayList<>();
        ResultSet rs = executeResEX(sql);
        if (rs.next()) {
            String result=rs.getString(1);
            if ((result!=null) && (result.length()>0)) out = new ArrayList<>(Arrays.asList(result.split(",")));
        };
        return(out);
    }

    public void deleteUnusedLinkedObjects(String linkType, ArrayList<String> ids) {
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
    
    public void itemChanged(String id) {
        for (LibraryChangeListener lCL : libraryChangeListeners) {
            lCL.libraryElementChanged("item",id);
        }
    }
    
    public void personChanged(String id) {
        for (LibraryChangeListener lCL : libraryChangeListeners) {
            lCL.libraryElementChanged("person",id);
        }
    }
    
    public boolean doesAttachmentExist(String fn) {
        String cfn=compressFilePath(fn);
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

    public String getStandardFolder(TableRow tableRow) {
        String ifolder=config.get("item-folder");
        if ((ifolder==null) || (ifolder.equals(""))) ifolder="LD::items";
        String folder=itemFolder.fillIn(tableRow,true);
        folder=Parser.replace(folder, "\\\\", "\\");
        return(folder);
    }
    

    /**
     * Returns true if field needs special attention when saving
     * 
     * @param field 
     */
    public boolean isPersonField(String field) {
        for (String person : peopleFields) {
            if (field.equals(person)) return(true);
            if (field.equals(person+"_ids")) return(true);
            if (field.equals(person+"short_")) return(true);
        }
        return(false);
    }

    /**
     * Returns true if field needs special attention when saving
     * 
     * @param field 
     */
    public boolean isLinkedField(String field) {
        for (String key : linkedFields) {
            if (field.equals(key+"s")) return(true);
            // check categories vs categorys:
            if ((field.length()>3) && (key.endsWith("y"))
                    && key.substring(0,key.length()-1).equals(field.substring(0,field.length()-3))) return(true);
            if (field.endsWith(key+"-")) return(true);
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
    
    public static String arrayListToSQLiteList(ArrayList<String> ids) {
        StringBuffer out=new StringBuffer();
        for (String id : ids) {
            out.append(',');
            out.append(id);
        }
        out.setCharAt(0, '(');
        out.append(')');
        return(out.toString());
    }

    public void updateLinks(String idField, ArrayList<String> newIDs, String itemID) {
        String table="item_"+idField+"_links";
        idField=idField+"_id";
        // special treatment: add link_type 0 for categories:
        if (idField.equals("category_id")) idField+=", link_type";
        try {
            // get old linked ids
            ResultSet old = executeResEX("SELECT " + idField + " FROM " + table + " WHERE item_id=" + String.valueOf(itemID));
            ArrayList<String> oldIDs = new ArrayList<>();
            ArrayList<String> toDelete = new ArrayList<>();
            while (old.next()) {
                oldIDs.add(old.getString(1));
                toDelete.add(old.getString(1));
            }
            // find the differences between the ID lists
            toDelete.removeAll(newIDs);
            newIDs.removeAll(oldIDs);
            
            // Delete ids, adjust idField in case there's a , for categories
            if (toDelete.size()>0) executeEX("DELETE FROM " + table + " WHERE item_id=" + itemID + " AND " + Parser.cutUntil(idField, ",") + " IN " + arrayListToSQLiteList(toDelete) + ";");

            // Insert new ids:
            StringBuffer sql=new StringBuffer();
            for (String id : newIDs) {
                sql.append(',');
                sql.append('(');
                sql.append(itemID);
                sql.append(',');
                sql.append(id);
                // special treatment for categories
                if (idField.startsWith("category_id")) {
                    sql.append(",0");
                }
                sql.append(')');
            }
            if (sql.length()>0) {
                sql.setCharAt(0, ' ');
                sql.insert(0, "INSERT INTO " + table + " (item_id," + idField + ") VALUES");
                sql.append(';');
                executeEX(sql.toString());
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }
    
    public Category findOrCreateCategory(String description) throws SQLException {
        HashMap<String,String> describedAttributes=new HashMap<>();
        Category category=null;
        String[] split = description.split("::");
        String key="label";
        String value="";
        if (split.length>1) {
            key=split[0];
            value=split[1];
        } else {
            value=split[0];
        }
        if (key.equals("id")) {
            category=itemCategories.get(Integer.valueOf(value));
        } else {
            for (Integer id : itemCategories.keySet()) {
                Category c=itemCategories.get(id);
                if (c.label.equals(value)) category=c;
            }
        }
        
        if (category == null) {
            ResultSet rs = executeResEX("SELECT * FROM item_categories WHERE `" + key + "`=? LIMIT 1;", value);
            if (rs.next()) {
                category = new Category(this, rs);
            } else {
                if (key.equals("label")) {
                    category = new Category(this, null, value);
                    category.save();
                }
            }
            itemCategories.put(Integer.valueOf(category.id),category);
        }
            
        return(category);
    }
    
    /**
     * Find or create person by name
     * 
     * @param description
     * @return
     * @throws SQLException 
     */
    public Person findOrCreatePerson(String description) throws SQLException {
        HashMap<String,String> describedAttributes=new HashMap<>();
        Person person=null;
        String itemDescription="";
        String[] descList = {};
        if (description.contains("#")) {
            itemDescription = Parser.cutFrom(description, "#").trim();
            if (itemDescription.length()>1) descList=itemDescription.split("#");
            description = Parser.cutUntil(description, "#").trim();
            StringBuffer sql=new StringBuffer("SELECT * FROM persons WHERE ");
            for (String descEntry : descList) {
                String[] descPair=descEntry.split("::");
                describedAttributes.put(descPair[0],descPair[1]);
                sql.append(descPair[0]);
                sql.append("='");
                sql.append(descPair[1]);
                sql.append("' OR ");
            }
            sql.delete(sql.length()-4, sql.length());
            sql.append(" LIMIT 1;");
            ResultSet rs=executeResEX(sql.toString());
            if (rs.next()) person=new Person(this,rs);
        }
        String firstName;
        String lastName;
        if (description.contains(",")) {
            firstName = Parser.cutFrom(description, ",").trim();
            lastName = Parser.cutUntil(description, ",").trim();
        } else {
            firstName = Parser.cutUntilLast(description," ").trim();
            lastName = Parser.cutFromLast(description," ").trim();
        }
        
        if (person==null) {
            ResultSet rs = executeResEX("SELECT * FROM persons where last_name = ? AND first_name = ? LIMIT 1;", new String[]{lastName, firstName});
            if (rs.next()) person=new Person(this,rs);
        }
        
        if (person==null) {
            // create Person
            person=new Person(this,(String)null);
            person.put("first_name", firstName);
            person.put("last_name", lastName);
            person.put("search",Person.toSearch(firstName, lastName));
        }
        
        // Write all describing attributes
        for (String key : describedAttributes.keySet()) {
            person.put(key, describedAttributes.get(key));
        }
        
        return(person);
    }
    
    public HashMap<String,String> getDataHash() {
        HashMap<String,String> data=new HashMap();
        data.put("library.name",name);
        data.put("library.numberofitems",String.valueOf(numberOfItems));
        data.put("library.numberofpeople",String.valueOf(numberOfPeople));
        return(data);
    }


    class ObjectComparatorText implements Comparator<TableRow> {

        private String tag;
        private boolean forwards;
        private int type;

        public ObjectComparatorText(final String t,boolean f, int ty) {
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
        public int compare(final TableRow A, final TableRow B) {
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

    class ObjectComparatorSelection implements Comparator<TableRow> {

        private String tag;
        private boolean forwards;
        private ArrayList<String> fields;

        public ObjectComparatorSelection(final String t,boolean f,ArrayList<String> fl) {
            tag=t;forwards=f;fields=fl;
        }

        @Override
        public int compare(final TableRow A, final TableRow B) {
            if (!forwards) return(fields.indexOf(A.getExtended(tag))-fields.indexOf(B.getExtended(tag)));
            return (fields.indexOf(B.getExtended(tag))-fields.indexOf(A.getExtended(tag)));
        }

        public boolean equals() {
            return (false);
        }

    }

}
