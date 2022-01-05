/*
 * Configurator
 *
 * Bundles configuration details for celsius and handles viewing etc.
 */

package celsius;

import celsius.data.Attachment;
import celsius.data.Item;
import celsius.data.Library;
import celsius.data.RecentLibraryCache;
import celsius.tools.Parser;
import celsius.tools.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.LinkedHashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JMenuItem;

/**
 *
 * @author cnsaeman
 */
public final class Configurator {

    private final Resources RSC;
    private final String dbURL = "jdbc:sqlite:celsius.cfg";
    public final HashMap<String,String> configuration;
    public final HashMap<String,HashMap<String,String>> supportedFileTypes;
    private Connection dbConnection;
    public LinkedHashMap<String,String> initialLibraries;
    public int selectedLibrary;
    public RecentLibraryCache lastLibraries;

    public Configurator(Resources rsc) throws IOException {
        RSC=rsc;
        configuration = new HashMap<>();
        supportedFileTypes = new HashMap<>();
        selectedLibrary=-1;
        lastLibraries=new RecentLibraryCache();

        try {
            // Check if configuration database exists, if not create
            if (!(new File("celsius.cfg")).exists()) {
                RSC.out("Creating configuration file");
                openConnection();
                
                Statement stmt = dbConnection.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS configuration (key text UNIQUE, value text);");
                stmt = dbConnection.createStatement();
                stmt.execute("INSERT INTO configuration (key,value) VALUES ('proxy','FALSE'),('maxthreads1','6'),('maxthreads2','6'),('iconfolder','Icons');");
                stmt = dbConnection.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS filetypes (filetype text UNIQUE, extractor text, viewer text, ident text, secondary_viewers text);");
                stmt = dbConnection.createStatement();
                stmt.execute("CREATE TABLE IF NOT EXISTS libraries_init (name text, path text, status integer);");
            } else {
                openConnection();
            }
            
            // read in configuration
            dbConnection = DriverManager.getConnection(dbURL);
            PreparedStatement statement = dbConnection.prepareStatement("SELECT * FROM configuration;");
            ResultSet rs=statement.executeQuery();
            while (rs.next()) {
                configuration.put(rs.getString(1),rs.getString(2));
            }

            // read in supported file types
            statement=dbConnection.prepareStatement("SELECT * FROM filetypes;");
            rs=statement.executeQuery();
            while (rs.next()) {
                HashMap<String,String> filetype=new HashMap<>();
                filetype.put("extractor",rs.getString(2));
                filetype.put("viewer",rs.getString(3));
                filetype.put("ident",rs.getString(4));
                filetype.put("secondary_viewers",rs.getString(5));
                supportedFileTypes.put(rs.getString(1),filetype);
            }
            
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    public void loadLibraries() {
        try {
            // read in initial libraries
            PreparedStatement statement=dbConnection.prepareStatement("SELECT * FROM libraries_init;");
            int highest=0;
            ResultSet rs=statement.executeQuery();
            while (rs.next()) {
                if (rs.getInt(3)==0) {
                    lastLibraries.put(rs.getString(1), rs.getString(2));
                    addRecentLib(rs.getString(1), rs.getString(2));
                } else {
                    RSC.loadLibrary(rs.getString(2),false);
                    if (highest<rs.getInt(3)) {
                        highest=rs.getInt(3);
                        selectedLibrary=RSC.libraries.size()-1;
                    }
                }
            }
            closeConnection();
            RSC.setSelectedLibrary(selectedLibrary);
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    public void libraryOpened(Library library) {
        if (lastLibraries.containsKey(library.name)) {
            if (lastLibraries.get(library.name).equals(library.baseFolder)) {
                // TODO: remove jMenu and put to invisible if changedRSC.MF.jMRecent.remove(lastLibraries.keySet().);
                lastLibraries.remove(library.name);
            }
        }
    }
    
    public void libraryClosed(Library library) {
        lastLibraries.put(library.name,library.baseFolder);
        addRecentLib(library.name, library.baseFolder);
    }
    
    public void addRecentLib(String name, final String source) {
            JMenuItem jmi = new JMenuItem(name);
            jmi.addActionListener((java.awt.event.ActionEvent evt) -> {
                (new Thread("LoadingLib") {
                    
                    @Override
                    public void run() {
                        RSC.MF.setThreadMsg("Opening library...");
                        RSC.MF.jPBSearch.setIndeterminate(true);
                        try {
                            RSC.loadLibrary(source,true);
                        } catch (Exception e) {
                            RSC.showWarning("Loading library failed:\n" + e.toString(), "Warning:");
                        }
                        RSC.MF.setThreadMsg("Ready.");
                        RSC.MF.jPBSearch.setIndeterminate(false);
                        RSC.MF.updateStatusBar(false);
                    }
                }).start();
            });
            RSC.MF.jMRecent.add(jmi);
    }
    
    /**
     * Saves the current library status to Celsius's config database
     */
    public void writeBackLibraryStatus() {
        try {
            openConnection();
            PreparedStatement statement = dbConnection.prepareStatement("DELETE FROM libraries_init;");
            statement.execute();
            for(Library library : RSC.libraries) {
                statement=dbConnection.prepareStatement("INSERT INTO libraries_init (name,path,status) VALUES (?,?,?);");
                statement.setString(1, library.name);
                statement.setString(2, library.baseFolder);
                if (library==RSC.getCurrentlySelectedLibrary()) {
                    statement.setInt(3,2);
                } else {
                    statement.setInt(3,1);
                }
                statement.execute();
            }
            for (String key : lastLibraries.keySet()) {
                statement=dbConnection.prepareStatement("INSERT INTO libraries_init (name,path,status) VALUES (?,?,?);");
                statement.setString(1, key);
                statement.setString(2, lastLibraries.get(key));    
                statement.setInt(3,0);
                statement.execute();
            }
            closeConnection();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    public void openConnection() throws SQLException {
        dbConnection=DriverManager.getConnection(dbURL);
    }
    
    public void closeConnection() throws SQLException {
        dbConnection.close();
    }

    public DefaultListModel getTypeDLM() {
        DefaultListModel DLM=new DefaultListModel();
        for (String filetype : supportedFileTypes.keySet()) {
            DLM.addElement(filetype);
        }
        return(DLM);
    }

    /**
     * Returns the configuration value for the given key
     * 
     * @param key
     * @return 
     */
    public String getConfigurationProperty(String key) {
        return(configuration.get(key));
    }

    public void setConfigurationProperty(String key, String value) {
        try {
            configuration.put(key,value);
            PreparedStatement stmt = dbConnection.prepareStatement("INSERT OR REPLACE INTO configuration (key,value) VALUES (?,?);");
            stmt.setString(1,key);
            stmt.setString(2,value);
            stmt.execute();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    public void setConfigurationProperties(String email, String annotate, String proxyadd, String proxyport, boolean proxy) {
        try {
            configuration.put("email",email);
            configuration.put("annotate",annotate);
            configuration.put("proxy_address",proxyadd);
            configuration.put("proxyport",proxyport);
            configuration.put("proxy",String.valueOf(proxy));
            PreparedStatement stmt = dbConnection.prepareStatement("INSERT OR REPLACE INTO configuration (key,value) VALUES (?,?),(?,?),(?,?),(?,?),(?,?);");
            stmt.setString(1,"email");
            stmt.setString(2,email);
            stmt.setString(3,"annotate");
            stmt.setString(4,annotate);
            stmt.setString(5,"proxy_address");
            stmt.setString(6,proxyadd);
            stmt.setString(7,"proxy_port");
            stmt.setString(8,proxyport);
            stmt.setString(9,"proxy");
            stmt.setString(10,String.valueOf(proxy));
            stmt.execute();
            setProxy();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    /**
     * Returns the filetype property for the given key
     * @param fileType
     * @param key
     * @return 
     */
    public String getSupportedFileTypeProperty(String fileType,String key) {
        return(supportedFileTypes.get(fileType).get(key));
    }

    public void setSupportedFileTypeProperties(String fileType, String extractor, String viewer, String ident, String secondary) {
        HashMap<String,String> fileTypeHash=supportedFileTypes.get(fileType);
        fileTypeHash.put("extractor",extractor);
        fileTypeHash.put("viewer",viewer);
        fileTypeHash.put("ident",ident);
        fileTypeHash.put("secondary_viewers",secondary);
        try {
            PreparedStatement stmt = dbConnection.prepareStatement("INSERT OR REPLACE INTO filetypes (filetype,extractor,viewer,ident,secondary_viewers) VALUES (?,?,?,?,?);");
            stmt.setString(1, fileType);
            stmt.setString(2, extractor);
            stmt.setString(3, viewer);
            stmt.setString(4, ident);
            stmt.setString(5, secondary);
            stmt.execute();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    public void addFileType(String fileType) {
        supportedFileTypes.put(fileType,new HashMap<>());
    }

    public void removeFileType(String fileType) {
        supportedFileTypes.remove(fileType);
        try {
            PreparedStatement stmt = dbConnection.prepareStatement("DELETE FROM filetypes WHERE filetype=?;");
            stmt.setString(1, fileType);
            stmt.execute();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    public void setProxy() {
        if (Boolean.valueOf(configuration.get("proxy"))) {
            String proxyadd = configuration.get("proxy_address");
            if (proxyadd.startsWith("http://")) {
                proxyadd = proxyadd.substring(7);
            }
            RSC.out("MAIN>Setting proxy to " + proxyadd + ":" + configuration.get("proxy_port"));
            System.setProperty("proxyHost", proxyadd);
            System.setProperty("proxyPort", configuration.get("proxy_port"));
        } else {
            System.setProperty("proxySet", "false");
            RSC.out("MAIN>No proxy set.");             
        }
    }

    public void addTypesCBM(DefaultComboBoxModel DCBM) {
        for (String fileType : supportedFileTypes.keySet()) {
            DCBM.addElement(fileType);
        }
    }

    /**
     * Extract plain text (protocol, Filename source, FileName target)
     */
    public void extractText(String TI, String s,String t) throws IOException {
        String stmp;    // tatsächlicher Dateiname (evtl. ohne ".gz")
        // if gzipped, extract the file first
        if (s.toLowerCase().endsWith(".gz")) {
            TextFile f1=new TextFile(s);
            String tmp=f1.getString();
            f1.close();
            if (tmp.startsWith("\u001f")) {
                TextFile.GUnZip(s);
                stmp=Parser.cutLastCharacters(s,3);
                RSC.out(TI+"extracting...");
            } else {
                stmp=Parser.cutLastCharacters(s,3);
                (new File(s)).renameTo(new File(stmp));
                RSC.out(TI+"no extraction necessary, wrong ending, corrected");
            }
        } else { stmp=s; }
        // find suitable extractor
        String extractorstr;
        try {
            String fileType=null;
            for (String key : supportedFileTypes.keySet()) {
                if ((fileType==null) && s.toLowerCase().endsWith(key)) {
                    fileType=key;
                }
            }
            if (fileType == null) {
                RSC.out(TI + "...No configuration entry for " + s + ".");
                return;
            }
            HashMap<String,String> fileTypeProperties=supportedFileTypes.get(fileType);
            RSC.out(TI + "...Extracting Text for " + s + ".");
            // Teste Filetype, versuche dennoch zu extrahieren
            TextFile H = new TextFile(stmp);
            String tmp = H.getString();
            if (tmp==null) tmp="";
            H.close();
            if ((fileTypeProperties.get("ident") != null) && (fileTypeProperties.get("ident").length() == 0)) {
                if ((fileTypeProperties.get("Ident")!=null) && (!tmp.startsWith(fileTypeProperties.get("Ident")))) {
                    // File type does not seem to match
                    RSC.showWarning("The file " + s + " is not of type " + fileType + ".", "Incorrect File Type Assignment");
                    RSC.out(TI + "Identification of filetype failed.");
                    RSC.out(TI + fileTypeProperties.get("Ident") + "  :: vs ::  " + tmp.substring(0, 10));
                }
            }
            extractorstr = fileTypeProperties.get("extractor");
            if ((extractorstr==null) || (extractorstr.length()==0)) return;
            // Cut Spaces
            extractorstr=extractorstr.replace("%from%",stmp);
            extractorstr=extractorstr.replace("%to%",t);
            RSC.out(TI+"Extractor command: "+extractorstr);
            // Prozess starten und auf Ende warten
            ExecutionShell ES=new ExecutionShell(extractorstr,0,false);
            ES.start();
            ES.join(5000);
            if (ES.errorflag) RSC.out(TI+"Error Message: "+ES.errorMsg);
            // Textdatei und ggfs Originaldatei gzippen
            if ((new File(t)).exists()) TextFile.GZip(t);
        } catch (Exception e) { RSC.outEx(e);RSC.out(TI+"Error extracting text: "+e.toString()); }
        if (s.toLowerCase().endsWith(".ps.gz"))
            TextFile.GZip(stmp);
    }

    /**
     * View attachment of item with number pos
     * 
     * @param item
     * @param pos 
     */
    public void view(Item item, int pos) {
        Attachment attachment = item.linkedAttachments.get(pos);
        view(attachment.get("filetype"), attachment.getFullPath());
    }

    /**
     * View file of given type and at given location
     * 
     * @param fileType
     * @param location 
     */
    public void view(String fileType, String location) {
        if (!location.startsWith("http://") && !location.startsWith("https://") && !(new File(location)).exists()) {
            RSC.showWarning("The file "+location+" does not exist.", "Warning: Cannot open file");
            return;
        }
        if (fileType=="---") {
            try {
                if (fileType.equals("html"))
                    java.awt.Desktop.getDesktop().browse(new URI(location));
                    else java.awt.Desktop.getDesktop().open(new File(location));
            } catch (Exception ex) {
                RSC.showWarning("Standard viewer for " + fileType + " reports an error!", "Warning");
                RSC.outEx(ex);
            }
            return;
        } else {
            if (!supportedFileTypes.containsKey(fileType)) {
                RSC.showWarning("No viewer for " + fileType + " installed!", "Warning");
                return;
            }
            String viewer=supportedFileTypes.get(fileType).get("viewer");
            if (viewer.equals("use standard viewer")) {
                try {
                    if (fileType.equals("html"))
                        java.awt.Desktop.getDesktop().browse(new URI(location));
                        else java.awt.Desktop.getDesktop().open(new File(location));
                } catch (IOException | URISyntaxException ex) {
                    RSC.showWarning("Standard viewer for " + fileType + " reports an error!", "Warning");
                    RSC.outEx(ex);
                }
            } else {
                String cmdln = viewer + " ";
                if (cmdln.contains("'%from%'")) location=Parser.replace(location, "'", "\\'");
                cmdln = cmdln.replace("%from%", location);
                RSC.out("MAIN>Viewer command: " + cmdln);
                (new ExecutionShell(cmdln, 0, true)).start();
            }
        }
    }

    public String getSecondaryViewers(String fileType) {
        if (supportedFileTypes.containsKey(fileType)) {
            return(supportedFileTypes.get(fileType).get("secondary_viewers"));
        } else {
            return(null);
        }
    }

    /**
     * Show an external HTML page
     */
    public void viewHTML(String url) {
        view("html",url);
    }
    
    public boolean isFileTypeSupported(String fileType) {
        return(supportedFileTypes.containsKey(fileType));
    }

    /**
     * returns the actual filetype of a file according to the config file
     * @param path - path to the file
     * @return
     */
    public String getFileType(String path) throws IOException {
        String filetype=FileTools.getFileType(path);
        File file = new File(path);
        byte[] buffer = new byte[10]; 
        InputStream in = new FileInputStream(file); 
        in.read(buffer); 
        in.close();
        String tmp = new String(buffer);
        for (String key : supportedFileTypes.keySet()) {
            String ident = supportedFileTypes.get(key).get("ident");
            if ((ident != null) && (ident.length() != 0) && (tmp.startsWith(ident))) {
                filetype = key;
            }
        }
        return(filetype);
    }

    public String correctFileType(String fn) throws IOException {
        String tmp=getFileType(fn);
        if (!fn.endsWith("."+tmp)) {
            String newfile=fn+"."+tmp;
            RSC.out("CONF>Renaming "+fn+" to "+newfile);
            (new File(fn)).renameTo(new File(newfile));
            fn=newfile;
        }
        return(fn);
    }
}
