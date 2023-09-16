/*
 * Resource-Class
 *
 * contains all the various resources and general data structures used by Celsius
 *
 */

package celsius;

import atlantis.tools.ExecutionShell;
import celsius.components.plugins.Plugin;
import atlantis.tools.FileTools;
import atlantis.gui.GuiStates;
import atlantis.gui.GuiTools;
import atlantis.gui.Icons;
import atlantis.tools.TextFile;
import atlantis.tools.Parser;
import celsius.components.tableTabs.CelsiusTable;
import celsius.gui.MainFrame;
import celsius.components.plugins.Plugins;
import celsius.gui.SafeMessage;
import celsius.components.bibliography.BibTeXRecord;
import celsius.gui.TabLabel;
import celsius.data.Item;
import celsius.data.ItemSelection;
import celsius.components.library.Library;
import celsius.components.library.LibraryTemplate;
import celsius.data.Person;
import celsius.data.TableRow;
import celsius.components.infopanel.InformationPanel;
import atlantis.gui.StandardResources;
import celsius.components.RegularWorker;
import celsius.components.tableTabs.CelsiusTableModel;
import celsius.tools.*;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JMenuItem;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.border.Border;


/**
 *
 * @author cnsaeman
 */
public class Resources implements StandardResources {

    public final String VersionNumber = "v4.1";
    public final String celsiushome = "https://github.com/cnsaeman/Celsius4";
    public final String stdHTMLstring;
    
    public TextFile logFile;
    public static final String logFileName="celsius.log";
    public int logLevel; // 0 : standard stuff, 10: all database interactions, 20: everything
    
    public ScheduledExecutorService executorService;

    public final String[] HistoryFields={"Today","Yesterday","Two Days Ago","This Week","Last Week","This Month","Last Month","This Year","Last Year"};
    
    public static final String historyTabIcon="iconmonstr-calendar-5.svg.24";
    public static final String personTabIcon="iconmonstr-user-22.svg.24";
    public static final String keyTabIcon="iconmonstr-key-2.svg.24";
    public static final String categoriesSearchTabIcon="iconmonstr-folder-30.svg.24";
    public static final String infoTabIcon="iconmonstr-info-6.svg.24";
    public static final String bibliographyTabIcon="iconmonstr-book-4.svg.24";
    public static final String remarksTabIcon="iconmonstr-pen-7.svg.24";
    public static final String attachmentsTabIcon="iconmonstr-save-14.svg.24";
    public static final String sourcesTabIcon="iconmonstr-save-14.svg.24";
    public static final String linksTabIcon="iconmonstr-link-1.svg.24";
    public static final String thumbTabIcon="iconmonstr-picture-1.svg.24";
    public static final String editTabIcon="iconmonstr-wrench-6.svg.24";
    public static final String internalTabIcon="iconmonstr-info-6.svg.24";

    public static final String addFromFolderTabIcon="folder";
    public static final String addSingleFileTabIcon="iconmonstr-file-14.svg.16";
    public static final String addManualTabIcon="iconmonstr-note-29.svg.16";
    public static final String addImportTabIcon="iconmonstr-download-20.svg.16";
    public static final String editTabIcon2="iconmonstr-edit-9.svg.16";
    public static final String templateTabIcon="iconmonstr-view-4.svg.16";
    public static final String styleSheetTabIcon="iconmonstr-construction-35.svg.16";

    public static final String pluginSetupIcon="iconmonstr-wrench-10.svg.16";
        
    public MainFrame MF;
    public final GuiTools guiTools;
    
    public final Icons icons;
    public InformationPanel guiInformationPanel;

    public final ArrayList<CelsiusTable> celsiusTables;

    public ArrayList<Library> libraries;
    public int currentLibrary;

    public HashMap<String, String> shortCuts; // list of shortcuts, implemented in this way to allow for shortcut editor later
    public HashMap<String,String> journalLinks;
    
    public ItemSelection lastItemSelection;

    public Plugins plugins;                    // protocol class
    public Configurator configuration;    // configuration handler

    public final ArrayList<LibraryTemplate> libraryTemplates;

    public double guiScaleFactor;

    public final ExecutorService sequentialExecutor;
    public ThreadPoolExecutor TPE;
    public LinkedBlockingQueue<Runnable> LBQ;
    
    public final ScheduledExecutorService regularExecutor;
    public final RegularWorker regularWorker;
    public boolean workerRunning;
    
    public GuiStates guiStates;
    
    public String celsiusBaseFolder;

    public boolean displayHidden;

    public boolean guiNotify;
    
    public final SimpleDateFormat SDF;

    public Resources(double guiScaleFactor) {
        this.guiScaleFactor=guiScaleFactor;
        initLog();
        SDF = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
        celsiusBaseFolder = Parser.cutUntilLast((new File(".")).getAbsolutePath(), ".");
        if (!celsiusBaseFolder.endsWith(ToolBox.FILE_SEPARATOR)) {
            celsiusBaseFolder += ToolBox.FILE_SEPARATOR;
        }
        guiNotify=true;
        displayHidden=false;
        stdHTMLstring=createstdHTMLstring();
        libraryTemplates = new ArrayList<>();
        celsiusTables = new ArrayList<>();
        currentLibrary = -1;
        libraries = new ArrayList<>();
        journalLinks = new HashMap<>();
        guiStates=new GuiStates();
        sequentialExecutor=java.util.concurrent.Executors.newSingleThreadExecutor();
        executorService=java.util.concurrent.Executors.newScheduledThreadPool(5);
        LBQ=new LinkedBlockingQueue<>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
        regularExecutor=Executors.newScheduledThreadPool ( 1 );
        regularWorker=new RegularWorker(this);
        workerRunning=false;
        regularExecutor.scheduleAtFixedRate ( regularWorker, 0L , 15L , TimeUnit.SECONDS );
        icons = new Icons(CelsiusMain.class, "images", "celsius/images/", guiScaleFactor);
        guiTools=new GuiTools(this,Toolkit.getDefaultToolkit().getImage(CelsiusMain.class.getResource("images/celsius.gif")),guiScaleFactor);
    }
    
    public MainFrame getMF() {
        return(MF);
    }
    
    public void initResources() {
        try {
            out("RES>Verifying standard folders");
            if (!(new File("icons")).exists()) FileTools.makeDir("icons");
            if (!(new File("plugins")).exists()) FileTools.makeDir("plugins");
            if (!(new File("templates")).exists()) FileTools.makeDir("templates");
            out("RES>Loading configuration file...");
            configuration = new Configurator(this);
            loadExternalIcons();

            out("RES>Loading library templates...");
            String templates[] = (new File("templates")).list();
            for (String template : templates) {
                libraryTemplates.add(new LibraryTemplate(this,"templates/"+template));
            }
        } catch (Exception e) {
            outEx(e);
            out("RES>Initializing of resources failed");
            guiTools.showWarning("Exception:","Error while initializing of resources:\n" + e.toString()+"\nCelsius might not be started in the correct folder/directory.");
            System.exit(255);
        }
        out("RES>Setting Proxy server...");
        configuration.setProxy();
    }
    
    public void loadExternalIcons() {
        String baseFolder=configuration.getConfigurationProperty("iconfolder");
        if (baseFolder.endsWith(ToolBox.FILE_SEPARATOR)) baseFolder.substring(0,baseFolder.length()-1);        
        icons.readIn(baseFolder,"");
    }
    
    public void reloadIcons() {
        icons.clear();
        icons.loadInternal();
        loadExternalIcons();
    }
    
    public void emptyThreadPoolExecutor() {
        LBQ.clear();
        TPE.shutdownNow();
        LBQ=new LinkedBlockingQueue<>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
    }

    /**
     * Initialize the logging system
     */
    private void initLog() {
        try {
            logFile=new TextFile(Resources.logFileName, true);
            out("");
            out("RES>============================================");
            out("RES>Celsius Library System " + VersionNumber);
            out("RES>============================================");
            out("RES>Started at: " + ToolBox.getCurrentDate());
            out("");
            logLevel=20;
        } catch (final Exception e) {
            outEx(e);
            out("Warning:Logging system initialization failed!");
            guiTools.showWarning("Warning!","Logging system initialization failed!");
            System.exit(100);
        }
    }

    public void resetLogFile() {
        try {
            logFile.close();
            FileTools.deleteIfExists(Resources.logFileName);
            initLog();
        } catch (Exception ex) {
            guiTools.showWarning("Exception:","Error while resetting log file:\n" + ex.toString());
            outEx(ex);
        }
    }

    public void loadPlugins() {
        plugins=new Plugins(this);
        plugins.readInAvailablePlugins();
    }

    /**
     * This fixes the standard html string output
     */
    private String createstdHTMLstring() {
        return ("<html><body><font size=\"6\" style=\"sans\">Celsius Library System " + VersionNumber + "</font><br>" +
                "<font size=\"5\" style=\"sans\">(w) by Christian Saemann</font><hr><br>" +
                "Welcome to the Celsius Library System, the flexible database and file storage system!<br>" +
                "<p align=\"justify\">Celsius can help you with the administration of your books, your movies, your " +
                "music files, your scientific papers, your BibTeX collection, your sheet music, your geocaches and many " +
                "other things.</p>" +
                "<p align=\"justify\">Below, you find quick start guides for various usage cases.</p>" +
                "<p><a href=\"#gettingstarted\">Getting started</a>&nbsp;&nbsp;&nbsp;<a href=\"#moreinfo\">More " +
                "information</a>&nbsp;&nbsp;&nbsp;</p>" +
                "<br><hr><br><a name=\"gettingstarted\"><font size=\"5\" style=\"sans\">Getting started</font></a>" +
                "<ul><li>When starting Celsius for the first time, click on the tool icon in the toolbar to enter the " +
                "configuration dialog. Make sure that file support is set up properly for any file type you might want " +
                "to use. Under Linux, e.g. the entries for \"Extract text\" might read as \"pdftotext -q '%from%' '%to%'\" and for " +
                "\"Viewer\", it might be \"okular '%from%'\". See the web page and the manual for more details, if you don't know what to do.</li>" +
                "<li>Next, you have to create a <i>library</i>, i.e. a database in which all your items will be stored.</li>" +
                "<li>Now you can start adding items by clicking on the green cross in the toolbar and choosing the " +
                "appropriate method.</li>" +
                "<br><hr><br><a name=\"moreinfo\"><font size=\"5\" style=\"sans\">More information</font></a>" +
                "<p align=\"justify\">Celsius's homepage is located at <a href=\"" + celsiushome + "\">" + celsiushome + "</a>. Its files are hosted <a href=\"http://sourceforge.net/projects/celsiusls/\">here" +
                "</a> at sourceforge, where you can submit reviews, ask questions in the forums and make suggestions for improvements. Updates to Celsius as " +
                "well as further plugins can be found there, too.</p>" +
                "<br><hr><br><a name=\"moreinfo\"><font size=\"5\" style=\"sans\">Copyright information</font></a>" +
                "<p align=\"justify\">Celsius is open source and released under the GNU General Public License v3.</p>" +
                "</body></html>");
    }
    
    public String timestampToString(String ts) {
        if (ts==null) return "Not Set";
        return(SDF.format(new Date(Long.parseLong(ts) * 1000)));        
    }

    public Library getCurrentlySelectedLibrary() {
        if (currentLibrary==-1) return(null);
        return(libraries.get(currentLibrary));
    }

    public int getCurrentlySelectedLibNo() {
        return(currentLibrary);
    }

    public CelsiusTable getCurrentTable() {
        if (MF.jTPTabList.getSelectedIndex()==-1) return(null);
        return(celsiusTables.get(MF.jTPTabList.getSelectedIndex()));
    }
    
    public Item getCurrentlySelectedItem() {
        return(guiInformationPanel.getItem());        
    }

    public Person getCurrentlySelectedPerson() {
        return(guiInformationPanel.getPerson());        
    }
    
    public void out() {
        out("");
    }

    @Override
    public void out(String s) {
        try {
            logFile.putString(s);
        } catch (Exception e) {
            outEx(e);
        }
        System.out.println(s);
    }

    public void out(int ll,String s) {
        if (ll <= logLevel) out(s);
    }
    
    @Override
    public void outEx(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        out(sw.toString());
        if (MF!=null) {
            String out=sw.toString();
            if (out.length()>10001) out=out.substring(0,1000);
            guiTools.showLongInformation("Exception:", out);
        }
    }

    public void outEr(Error e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        out(sw.toString());
        if (MF!=null) {
            String out=sw.toString();
            if (out.length()>10001) out=out.substring(0,1000);
            guiTools.showLongInformation("Exception:", out);
        }
    }

    public void outEr(String msg, Error e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        out(sw.toString());
        if (MF!=null) {
            String out=msg+sw.toString();
            if (out.length()>10001) out=out.substring(0,1000);
            guiTools.showLongInformation("Exception:", out);
        }
    }
    
    public void setTempStatus(String status) {
        if (guiNotify && (MF!=null)) MF.setTempStatus(status);
    }
    
    public Color getLightGray() {
        return(new java.awt.Color(204,204,204));
    }

    public void close() {
        configuration.writeBackLibraryStatus();
        for (Library library : libraries)
            library.close();
        try {
            out();
            out("RES>Application closed at: " + ToolBox.getCurrentDate());
        } catch (final Exception e) {
            outEx(e);
            (new SafeMessage("Protocol file finalization failed:" + e.toString(), "Exception:", 0)).showMsg();
            guiTools.showWarning("Warning!","RES>Messager finalization failed!");
        }
        try {
            logFile.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void loadLibrary(String fileName,boolean remember) {
        final Library library;
        try {
            library = new Library(fileName, this);
        } catch (Exception ex) {
            return;
        }
        if (library.name.equals("??##cancelled")) return;
        if (library.name.startsWith("??#$")) return;
        if (library.name.startsWith("??##")) {
            if (library.currentStatus==20) {
                (new SafeMessage("Error loading library, library " + fileName + " is locked. Library has not been opened.", "Error", 0)).showMsg();
            } else {
                (new SafeMessage("Error loading library, library " + fileName + " has not been loaded:\n" + ToolBox.stripError(library.name), "Warning:", 0)).showMsg();
            }
        } else {
            if (library.currentStatus==10) {
                (new SafeMessage("The library "+library.name+" may be out of synch. Please sychnronize it as soon as possible.", "Warning:", 0)).showMsg();
            }
            libraries.add(library);
            if (remember) configuration.libraryOpened(library);
            for (Component cmp : MF.jMRecent.getMenuComponents()) {
                if (((JMenuItem) cmp).getText().equals(library.name)) {
                    MF.jMRecent.remove(cmp);
                }
            }
            MF.addLib(library);
            out("RSC>Library " + getCurrentlySelectedLibrary().name + " loaded.");
        }
    }

    public void loadLibraries() {
        try {
            configuration.loadLibraries();
            if (configuration.selectedLibrary>-1) setSelectedLibrary(configuration.selectedLibrary);

            /*lastLibraries.put(s1, s2);
                        addRecentLib(s1, s2);*/
        } catch (Exception ex) {
            outEx(ex);
        }
    }
    
    public void adjustComponents(Component[] comp) {
        for (Component comp1 : comp) {
            if (comp1 instanceof Container) {
                adjustComponents(((Container) comp1).getComponents());
            }
            try {
                if (comp1.getFont().getSize() < 20) {
                    comp1.setFont(new java.awt.Font("Arial", 0, guiTools.guiScale(12)));
                }
                if (guiScaleFactor>1) {
                    if (comp1 instanceof JRadioButton) {
                        ((JRadioButton) comp1).setIcon(icons.getScaledIcon("iconmonstr-shape-20.svg.16"));
                        ((JRadioButton) comp1).setSelectedIcon(icons.getScaledIcon("iconmonstr-checkbox-28.svg.16"));
                        ((JRadioButton) comp1).setRolloverIcon(icons.getScaledIcon("iconmonstr-shape-20.svg.16"));
                        ((JRadioButton) comp1).setIconTextGap(guiTools.guiScale(3));
                    }
                    if (comp1 instanceof JCheckBox) {
                        ((JCheckBox) comp1).setIcon(icons.getScaledIcon("iconmonstr-square-4.svg.16"));
                        ((JCheckBox) comp1).setSelectedIcon(icons.getScaledIcon("iconmonstr-checkbox-4.svg.16"));
                        ((JCheckBox) comp1).setRolloverIcon(icons.getScaledIcon("iconmonstr-square-4.svg.16"));
                        ((JCheckBox) comp1).setIconTextGap(guiTools.guiScale(3));
                    }
                }
            }catch(Exception e){}//do nothing
        }
    }    
    
    /**
     * Create the journal link command
     * @param item
     * @return 
     */
    public String getJournalLinkCmd(Item item) {
        if (item == null)
            return("");
        BibTeXRecord BR=new BibTeXRecord(item.get("bibtex"));
        String gtag;
        String tmp1=journalLinks.get(BR.get("journal"));
        if (tmp1 == null)
            return("");
        if (tmp1.length() > 0) {
            // Substitute addinfo tags
            for (String key : BR.keySet()) {
                gtag = BR.get(key);
                if (gtag.length()==0) {
                    gtag = "";
                }
                if (key.equals("pages")) {
                    if (gtag.indexOf('-')>0) gtag=Parser.cutUntil(gtag, "-");
                    if (gtag.indexOf('-')>0) gtag=Parser.cutUntil(gtag, "-");
                }
                tmp1 = tmp1.replace("#" + key + "#", gtag);
            }
        }
        return(tmp1);
    }

    public void rememberDir(String dir, JFileChooser FC) {
        File f=FC.getSelectedFile();
        if (!f.isDirectory()) f=f.getParentFile();
        rememberDir(dir,f.getAbsolutePath());
    }

    @Override
    public void rememberDir(String dir, String folderPath) {
        if (getCurrentlySelectedLibrary()==null) return;
        getCurrentlySelectedLibrary().setConfiguration("dir::"+dir,folderPath);
    }
    
    
    @Override
    public String getDir(String dir) {
        if (getCurrentlySelectedLibrary()==null) return(".");
        String ret=getCurrentlySelectedLibrary().config.get("dir::"+dir);
        if ((ret==null) || (ret.length()==0)) ret=".";
        return(ret);
    }

    public void setCurrentItemTable(String title, String icon) {
        getCurrentTable().title=title;
        MF.jTPTabList.setTabComponentAt(MF.jTPTabList.getSelectedIndex(), new TabLabel(title,icon,this,getCurrentTable(),true));
    }
   
    // guarantees tab and turns it into one with given description
    public CelsiusTable guaranteeTableAvailable(int tableType, String title, String icon) {
        CelsiusTable celsiusTable;
        if (!guiStates.getState("mainFrame", "tabAvailable") || (!getCurrentTable().celsiusTableModel.tableview)) {
            celsiusTable=new CelsiusTable(MF,getCurrentlySelectedLibrary(),title,tableType);
            celsiusTable.setHeader(title);
            celsiusTables.add(celsiusTable);
            MF.buildingNewTab=true;
            final JScrollPane scrollpane = new JScrollPane(celsiusTable.jtable);
            MF.jTPTabList.add(scrollpane);
            MF.jTPTabList.setTabComponentAt(MF.jTPTabList.getTabCount() - 1, new TabLabel(title,icon,this,celsiusTable,true));
            MF.jTPTabList.setSelectedComponent(scrollpane);
            guiStates.adjustState("mainFrame", "tabAvailable", true);
            MF.buildingNewTab=false;            
        } else {
            celsiusTable=getCurrentTable();
            celsiusTable.setLibraryAndTableType(getCurrentlySelectedLibrary(), tableType);
            setCurrentItemTable(title,icon);
        }
        if (celsiusTable.getObjectType() == CelsiusTableModel.CELSIUS_TABLE_ITEM_TYPE) {
            guiStates.adjustState("mainFrame", "itemTabAvailable", true);
            guiStates.adjustState("mainFrame", "personTabAvailable", false);
        } else if (celsiusTable.getObjectType() == CelsiusTableModel.CELSIUS_TABLE_ITEM_TYPE) {
            guiStates.adjustState("mainFrame", "itemTabAvailable", false);
            guiStates.adjustState("mainFrame", "personTabAvailable", true);
        }
        guiStates.adjustState("mainFrame", "itemSelected", false);
        guiStates.adjustState("mainFrame", "personSelected", false);
        MF.guiPluginPanel.adjustPluginList();
        return(celsiusTable);
    }

    // creates a new tab and turns it into one with given description
    public CelsiusTable makeNewTabAvailable(int tableType, String title, String icon) {
        CelsiusTable celsiusTable;
        celsiusTable=new CelsiusTable(MF,getCurrentlySelectedLibrary(),title,tableType);
        celsiusTable.setHeader(title);
        celsiusTables.add(celsiusTable);
        MF.buildingNewTab=true;
        final JScrollPane scrollpane = new JScrollPane(celsiusTable.jtable);
        MF.jTPTabList.add(scrollpane);
        MF.jTPTabList.setTabComponentAt(MF.jTPTabList.getTabCount() - 1, new TabLabel(title,icon,this,celsiusTable,true));
        MF.jTPTabList.setSelectedComponent(scrollpane);
        guiStates.adjustState("mainFrame", "tabAvailable", true);
        guiStates.adjustState("mainFrame", "itemSelected", false);
        guiStates.adjustState("mainFrame", "personSelected", false);
        MF.buildingNewTab=false;
        MF.guiPluginPanel.adjustPluginList();
        return(celsiusTable);
    }
    
    public int guiScale(int i) {
        return(guiTools.guiScale(i));
    }
    
    public Font stdFontMono() {
        return(new java.awt.Font("Monospaced", 0, guiTools.guiScale(12)));        
    }
    
    public Border stdBorder() {
        return(BorderFactory.createEmptyBorder(guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(5)));
    }

    public Border stdBordermN() {
        return(BorderFactory.createEmptyBorder(guiTools.guiScale(0), guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(5)));
    }

    public Border stdBordermS() {
        return(BorderFactory.createEmptyBorder(guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(0), guiTools.guiScale(5)));
    }
    
    public Border stdBordermE() {
        return(BorderFactory.createEmptyBorder(guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(5), guiTools.guiScale(0)));
    }
    
    public Border stdBordermW() {
        return(BorderFactory.createEmptyBorder(guiTools.guiScale(5), guiTools.guiScale(0), guiTools.guiScale(5), guiTools.guiScale(5)));
    }
    
    public String getTableRowIDs(ArrayList<TableRow> tableRows) {
        String ids="";
        for (TableRow tableRow : tableRows) {
            ids+=","+String.valueOf(tableRow.id);
        }
        return("("+ids.substring(1)+")");
    }

    /**
     * Sets the MainFrame here and in controller
     * 
     * @param MF 
     */
    public void setMainFrame(MainFrame MF) {
        this.MF=MF;
        guiTools.MF=MF;
    }
    
    public final static Object[] optionsYNC = { "Yes", "No", "Cancel" };
    public final static Object[] optionsOC = { "OK", "Cancel" };
    public final static Object[] optionsYN = { "Yes", "No" };    

     
    public void setSelectedLibrary(int i) {
        if ((i > -1) && (i < MF.jCBLibraries.getItemCount())) {
            MF.jCBLibraries.setSelectedIndex(i);
        }
    }

    public String getBibOutput(Item item) {
        String n = (String) MF.guiInfoPanel.jCBBibPlugins.getSelectedItem();
        if (n == null) {
            return "";
        }
        Plugin plugin = plugins.get(n);
        if (item == null) {
            return "";
        }
        ArrayList<String> msg = new ArrayList<>();
        HashMap<String,String> communication=new HashMap<>();
        try {
            Thread tst = plugin.Initialize(item, communication, msg);
            tst.start();
            tst.join();
        } catch (Exception ex) {
            out("jIP>Error while running BibPlugin: " + plugin.metaData.get("title") + ". Exception: " + ex.toString());
            guiTools.showWarning("Exception:","Error while applying Bibplugins:\n" + ex.toString());
            outEx(ex);
        }
        return communication.get("output");
    }

    /**
     * Display item in a form available, following a hierarchy of option
     */
    public void viewItem(Item item) {
        item.logView();
        if (item == null) {
            return;
        }
        if (!item.linkedAttachments.isEmpty()) {
            configuration.view(item, 0);
        } else {
            String cmdln = getJournalLinkCmd(item);
            if (cmdln.length() > 0) {
                out("JM>Journal link command: " + cmdln);
                (new ExecutionShell(cmdln, 0, true)).start();
            } else {
                if (!item.getS("url").isBlank()) {
                    configuration.viewHTML(item.get("url"));
                } else {
                    if (item.getS("links").length() > 0) {
                        if (item.getS("links").contains("combines")) {
                            MF.showLinksOfType("combines");
                        }
                        MF.showLinksOfType("Available Links");
                    } else {
                        guiTools.showWarning("Warning","No file or journal link associated with this entry.");
                    }
                }
            }
        }
    }

    public void viewCurrentlySelectedObject() {
        CelsiusTable celsiusTable=getCurrentTable();
        if ((celsiusTable.getObjectType()==CelsiusTable.ITEM_TABLE) || (celsiusTable.getObjectType()==CelsiusTable.ITEM_HISTORY_TABLE)) {
            viewItem(getCurrentlySelectedItem());
        } else if (celsiusTable.getObjectType()==CelsiusTable.PERSON_TABLE) {
            String personIDs=celsiusTable.getSelectedIDsString();
            String name="Several people";
            if (celsiusTable.jtable.getSelectedRowCount()==1) {
                TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
                name=tableRow.toText(false);
            }
            CelsiusTable targetTable=makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEMS_OF_PERSONS, name, Resources.personTabIcon);
            targetTable.library.showItemsWithPersonIDs(personIDs, targetTable);
            getCurrentTable().selectFirst();
        }
    }

    public void loadShortCuts() {
        shortCuts = new HashMap<>();
        try {
            TextFile TD = new TextFile("celsius.shortcuts");
            String tmp;
            while (TD.ready()) {
                tmp = TD.getString();
                if (tmp.indexOf("::") > 0) {
                    shortCuts.put(Parser.cutUntil(tmp, "::").trim(), Parser.cutFrom(tmp, "::").trim());
                }
            }
            TD.close();
            TD = new TextFile("celsius.journallinks");
            while (TD.ready()) {
                tmp = TD.getString();
                if (tmp.indexOf("::") > 0) {
                    journalLinks.put(Parser.cutUntil(tmp, "::").trim(), Parser.cutFrom(tmp, "::").trim());
                }
            }
            TD.close();
        } catch (IOException ex) {
            out("MAIN>Error while loading shortcut/journallinks file:\n" + ex.toString());
            (new SafeMessage("Error while loading shortcut/journallinks file:\n" + ex.toString(), "Exception:", 0)).showMsg();
            outEx(ex);
        }
    }
    
    public void removeSelectedFromCurrentTable() {
        CelsiusTable celsiusTable = getCurrentTable();
        if (celsiusTable!=null) {
            for (TableRow tableRow : celsiusTable.getSelectedRows())
                celsiusTable.removeRow(tableRow);
        }
    }

    @Override
    public GuiTools getGuiTools() {
        return(guiTools);
    }
    
    @Override
    public Icons getIcons() {
        return(icons);
    }
    
}
