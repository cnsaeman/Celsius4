//
// Celsius Library System v2
// (w) by C. Saemann
//
// MainFrame.java
//
// This class contains the main frame and the main class of the Celsius Library System
//
// typesafe
//
// ##checked 15.09.2007
// adjusted for long authors
//
package celsius.gui;

import celsius.SwingWorkers.SWFinalizer;
import celsius.SwingWorkers.SWApplyPlugin;
import celsius.SwingWorkers.SWBibTeXIntegrity;
import celsius.SwingWorkers.SWShowCited;
import experimental.AddTransferHandler;
import celsius.CelsiusMain;
import celsius.data.StructureNode;
import celsius3.Library3;
import celsius.data.Library;
import celsius.Resources;
import celsius.data.Item;
import celsius.SplashScreen;
import celsius.SwingWorkers.SWLibraryCheck;
import celsius.data.Attachment;
import celsius.tools.Parser;
import celsius.data.Category;
import celsius.data.DoubletteResult;
import celsius.data.KeywordListModel;
import celsius.data.PeopleListModel;
import celsius.data.Person;
import celsius.data.RecentLibraryCache;
import celsius.data.TableRow;
import celsius.tools.*;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

/**
 *
 * @author  cnsaeman
a */
public class MainFrame extends javax.swing.JFrame implements 
        DropTargetListener, TreeModelListener, DocumentListener, KeyListener, KeyEventDispatcher, GenericCelsiusEventListener,
        ClipboardOwner, HasManagedStates {

    public Resources RSC;
    public SplashScreen StartUp;   // splash screen
    public DefaultTreeModel StructureTreeModel;         // libraries structure tree model

    public EditConfiguration dialogConfiguration;     // Configuration dialog
    public DeepSearch deepSearch;
    public InformationPanel guiInfoPanel;
    public SearchPanel guiSearchPanel;
    public PluginPanel guiPluginPanel;
    
    private int bufmousex,  bufmousey;                    // buffers for popup menu over categories

    // GUI flags
    public boolean buildingNewTab;
    public boolean adjustingStates;
    
    public Future eastClearer;
    
    public int lastkeycode;
    
    public int searchState;
    
    public ClearEdit jCE1;
    public ClearEdit jCE3;
    
    /** Creates new form MainFrame */
    public MainFrame() {
    }

    public void gui1() {
        initComponents();
        
        DefaultListModel DLM=new DefaultListModel();
        for (String entry : RSC.HistoryFields) {
            DLM.addElement(entry);
        }
        jLWhenAdded.setModel(DLM);
        
        setTitle("Celsius Library System "+RSC.VersionNumber);
        
        guiInfoPanel=new InformationPanel(RSC);
        jSPMain.setBottomComponent(guiInfoPanel);
        
        guiSearchPanel=new SearchPanel(this);
        jPanel5.add(guiSearchPanel,BorderLayout.CENTER);
        guiPluginPanel=new PluginPanel(RSC);
        jTPTechnical.addTab("", guiPluginPanel);

        jTPSearches.setTabComponentAt(0, new TabLabel("",Resources.categoriesSearchTabIcon,RSC,null,false));
        jTPSearches.setTabComponentAt(1, new TabLabel("",Resources.keyTabIcon,RSC,null,false));
        jTPSearches.setTabComponentAt(2, new TabLabel("",Resources.historyTabIcon,RSC,null,false));
        jTPTechnical.setTabComponentAt(0, new TabLabel("",Resources.bibliographyTabIcon,RSC,null,false));
        jTPTechnical.setTabComponentAt(1, new TabLabel("","plugin",RSC,null,false));
        
        jPanel3.setBorder(RSC.stdBorder());
        jCE1=new ClearEdit(RSC,"Enter a category (CTRL+C)");
        jPanel3.add(jCE1, java.awt.BorderLayout.NORTH);
        jPanel7.setBorder(RSC.stdBorder());
        jPanel9.setBorder(RSC.stdBorder());
        jCE3=new ClearEdit(RSC,"Enter a keyword (CTRL+C)");
        jPanel9.add(jCE3, java.awt.BorderLayout.NORTH);
        initFurther();
    }

    public void gui2() {
        this.setLocationByPlatform(true);
        jCE1.getDocument().addDocumentListener(this);
        jCE3.getDocument().addDocumentListener(this);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
        dialogConfiguration = new EditConfiguration(this, RSC.configuration);
        RSC.out("Libraries loaded");
        guiPluginPanel.adjustPluginList();
        jTBAdd.setTransferHandler(new AddTransferHandler(this));
        StartUp.setStatus("Ready...");
        RSC.adjustComponents(this.getComponents());
        jSPMain3.setMinimumSize(new Dimension(RSC.guiScale(280),RSC.guiScale(0)));
        jSPMain3.setDividerLocation(jSPMain3.getMaximumDividerLocation());
        guiInfoPanel.setMinimumSize(new Dimension(RSC.guiScale(0),RSC.guiScale(280)));
        jSPMain.setDividerLocation(jSPMain.getMaximumDividerLocation());
        jSPMain3.setDividerLocation(0.7);
        pack();
        setVisible(true);
        RSC.out("packed");
        guiInfoPanel.updateGUI();
        RSC.out("divider locations set");
        StartUp.toFront();
        StartUp.setTimeTrigger();
        buildingNewTab=false;
        RSC.out("all done, all systems ready.");
        this.setExtendedState(this.getExtendedState() | JFrame.MAXIMIZED_BOTH);
    }

    /**
     * Remaining initialization
     */
    private void initFurther() {

        // Init Structuretree
        jTStructureTree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        jTStructureTree.setShowsRootHandles(true);
        DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer();
        renderer.setLeafIcon(RSC.icons.getIcon("folder"));
        renderer.setClosedIcon(RSC.icons.getIcon("folder"));
        renderer.setOpenIcon(RSC.icons.getIcon("folder_table"));
        jTStructureTree.setCellRenderer(renderer);
        StructureTreeModel = new DefaultTreeModel(null);
        jTStructureTree.setModel(StructureTreeModel);
        @SuppressWarnings("unused")
        DropTarget dt = (new DropTarget(jTStructureTree, this));
        
        // STATEMANAGER
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","itemSelected", new JComponent[] { jMItems, jMICitationTagClipboard, jMIBibClipboard, jMICitationTagClipboard, jBtnExpSel, jMIPeople});
        // TODO: adjust 
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","personSelected", new JComponent[] { jMIMerge1});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","librarySelected", new JComponent[] { jMICloseLib, jMISaveLib, jMIDeleteLib, jMIShowCitedinFile, jMIConsistencyCheck, jMICheckBib, jCE1, jMIEditLib,jMIFullBibToFile, jMIEditDS, jMIAddToLib, jMIDeepSearch, jTBAdd, jCE3, guiSearchPanel, jBtnExpAll,  guiPluginPanel.jBMPlugins});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","tabAvailable", new JComponent[] { jMICopyTab, jMICopyTab2, jMITab2Cat, jMITab2Cat2, jMICloseTab, jMICloseTab2, jBtnExpTab});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","categorySelected", new JComponent[] { jMCategories, jMIInsertCat });
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","pluginSelected", new JComponent[] { });
        RSC.guiStates.setState("mainFrame","librarySelected", false);
        RSC.guiStates.setState("mainFrame","categorySelected", false);
        RSC.guiStates.setState("mainFrame","itemSelected", false);
        RSC.guiStates.setState("mainFrame","pluginSelected", false);
        RSC.guiStates.setState("mainFrame","tabAvailable",false);        
        RSC.guiStates.registerListener("mainFrame", this);
        RSC.guiStates.adjustStates("mainFrame");

        final Image image = Toolkit.getDefaultToolkit().getImage(CelsiusMain.class.getResource("images/celsius.gif"));
        setIconImage(image);
   }

    public void setShortCuts() {
        for (int i = 0; i < jMainMenu.getMenuCount(); i++) {
            JMenu jM = jMainMenu.getMenu(i);
            for (int j = 0; j < jM.getItemCount(); j++) {
                if (jM.getItem(j) == null) {
                    j++;
                } else {
                    try {
                        if (RSC.shortCuts.containsKey(jM.getItem(j).getText())) {
                            jM.getItem(j).setAccelerator(KeyStroke.getKeyStroke(RSC.shortCuts.get(jM.getItem(j).getText())));
                        }
                    } catch (Exception ex) {
                        RSC.outEx(ex);
                    }
                }
            }
        }
    }

    /**
     * Terminate Program
     */
    private void closeCelsius() {
        // write init data and close everything
        RSC.closeResources();
        dispose();
        System.exit(0);
    }

    public void addLib(final Library Lib) {
        JMenuItem jmi = new JMenuItem(Lib.name);
        jmi.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemToLibrary(Lib);
            }
        });
        jMCopyToDiff.add(jmi);
        jmi = new JMenuItem(Lib.name);
        jmi.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemToLibrary(Lib);
            }
        });

        jMCopyToDiff1.add(jmi);

        DefaultComboBoxModel DCBM=(DefaultComboBoxModel)jCBLibraries.getModel();
        DCBM.addElement(Lib.name);
        DCBM.setSelectedItem(Lib.name);
        RSC.guiStates.adjustState("mainFrame","librarySelected", true);
    }

    /** 
     * Switch to library Lib, if Lib==null, then to the currently selected one.
     * @param i
     */
    public void switchToLibrary(Library Lib) {
        // Library already selected
        if ((RSC.getCurrentlySelectedLibNo()>-1) && (RSC.getCurrentlySelectedLibNo()<RSC.libraries.size()))
            if (RSC.getCurrentlySelectedLibrary()==Lib) return;
        // No library remaining
        if (RSC.libraries.isEmpty()) {
            RSC.guiStates.adjustState("mainFrame", "librarySelected", false);
            return;
        }
        
        // switch to currently selected or other library
        if (Lib==null) RSC.currentLib=jCBLibraries.getSelectedIndex();
        else RSC.currentLib=RSC.libraries.indexOf(Lib);
        
        if (RSC.currentLib==-1) {
            RSC.guiStates.adjustState("mainFrame","librarySelected", false);
            return;
        }
        RSC.guiStates.adjustState("mainFrame", "librarySelected", true);
        if (RSC.currentLib!=jCBLibraries.getSelectedIndex())
            jCBLibraries.setSelectedIndex(RSC.currentLib);
        StructureTreeModel.setRoot(RSC.getCurrentlySelectedLibrary().structureTreeRoot);
        updateStatusBar(true);
        RSC.plugins.updatePlugins();
    }
    
    /**
     * Copies the currently selected items to library targetLibrary.
     * @param targetLibrary
     */
    private void copyItemToLibrary(Library targetLibrary) {
        Library sourceLibrary = RSC.getCurrentlySelectedLibrary();
        if (sourceLibrary != targetLibrary) {
            for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
                Item item=(Item)tableRow;
                DoubletteResult dr=new DoubletteResult(0,null);
                try {
                    dr = targetLibrary.isDoublette(item);
                } catch (IOException ex) {
                    RSC.outEx(ex);
                    dr.type=12;
                }
                boolean add=true;
                if (dr.type==12) {
                    add=false;
                    RSC.showWarning("I/O Error while checking for doublettes.", "Error:");
                }
                if (dr.type==10) {
                    int j=RSC.askQuestionOC("An exact copy of the item "+item.toText(false)+"\nalready exists in the library. Should another copy be added?", "Warning:");
                    if (j==JOptionPane.NO_OPTION) add=false;
                }
                if (dr.type==4) {
                    int j=RSC.askQuestionOC("A paper with the same key information as the item "+item.toText(false)+"\nalready exists in the library. Should another copy be added?", "Warning:");
                    if (j==JOptionPane.NO_OPTION) add=false;
                }
                if (add) targetLibrary.acquireCopyOfItem(item);
            }
        } else {
            RSC.showWarning("Items can only be copied to different libraries.", "Warning!");
        }
    }
    
    public void adjustStates() {
        if (RSC.guiStates.getState("mainFrame","librarySelected")) {
            if (RSC.getCurrentlySelectedLibrary().hideFunctionality.contains("Menu:Bibliography")) {
                jMBibTeX.setVisible(false);
            } else {
                jMBibTeX.setVisible(true);
            }
            jTStructureTree.setComponentPopupMenu(jPMCategories);
            if (StructureTreeModel.getRoot()!=RSC.getCurrentlySelectedLibrary().structureTreeRoot)
                StructureTreeModel.setRoot(RSC.getCurrentlySelectedLibrary().structureTreeRoot);
        } else {
            jTStructureTree.setComponentPopupMenu(null);
            StructureTreeModel.setRoot(null);
            updateStatusBar(false);
        }
        if (!RSC.guiStates.getState("mainFrame","tabAvailable")) {
            guiInfoPanel.updateGUI();
        }
    }

    /**
     * Set thread message
     */
    public void setThreadMsg(final String s) {
        jLThreadStatus.setText(s);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jDialog1 = new javax.swing.JDialog();
        jScrollBar1 = new javax.swing.JScrollBar();
        jPasswordField1 = new javax.swing.JPasswordField();
        jPMItemList = new javax.swing.JPopupMenu();
        jMINew = new javax.swing.JMenuItem();
        jMICopyTab2 = new javax.swing.JMenuItem();
        jMITab2Cat2 = new javax.swing.JMenuItem();
        jMICloseTab2 = new javax.swing.JMenuItem();
        jPMCategories = new javax.swing.JPopupMenu();
        jMIOpenNewTab = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JSeparator();
        jMIInsertCat1 = new javax.swing.JMenuItem();
        jMIRenameCat1 = new javax.swing.JMenuItem();
        jMIDelCat1 = new javax.swing.JMenuItem();
        jSeparator15 = new javax.swing.JSeparator();
        jMICatUp1 = new javax.swing.JMenuItem();
        jMICatDown1 = new javax.swing.JMenuItem();
        jMICatSub1 = new javax.swing.JMenuItem();
        jMICatSuper1 = new javax.swing.JMenuItem();
        jSeparator31 = new javax.swing.JPopupMenu.Separator();
        jMIExpand = new javax.swing.JMenuItem();
        jMICollapse = new javax.swing.JMenuItem();
        buttonGroup1 = new javax.swing.ButtonGroup();
        jPMItemTable = new javax.swing.JPopupMenu();
        jMIView1 = new javax.swing.JMenuItem();
        jMActions = new javax.swing.JMenu();
        jMShow = new javax.swing.JMenu();
        jMShowCombined = new javax.swing.JMenuItem();
        jMShowLinked = new javax.swing.JMenuItem();
        jSeparator30 = new javax.swing.JPopupMenu.Separator();
        jMIViewPlain1 = new javax.swing.JMenuItem();
        jMIReExtract1 = new javax.swing.JMenuItem();
        jMIPeople2 = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JSeparator();
        jMIRemoveFromTab1 = new javax.swing.JMenuItem();
        jMIUnregisterDoc1 = new javax.swing.JMenuItem();
        jMIDeleteFile1 = new javax.swing.JMenuItem();
        jMIRemoveHalf1 = new javax.swing.JMenuItem();
        jSeparator22 = new javax.swing.JSeparator();
        jMICites1 = new javax.swing.JMenuItem();
        jSeparator24 = new javax.swing.JSeparator();
        jMIAssociateFile1 = new javax.swing.JMenuItem();
        jMIJoin1 = new javax.swing.JMenuItem();
        jMICreateCombiner1 = new javax.swing.JMenuItem();
        jSeparator26 = new javax.swing.JSeparator();
        jMCopyToDiff1 = new javax.swing.JMenu();
        jMIExportTab1 = new javax.swing.JMenuItem();
        jMIEmail1 = new javax.swing.JMenuItem();
        buttonGroup2 = new javax.swing.ButtonGroup();
        bGSearch = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jPMPeopleTable = new javax.swing.JPopupMenu();
        jMIMerge = new javax.swing.JMenuItem();
        jPanel5 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jCBLibraries = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        jPanel20 = new javax.swing.JPanel();
        jTBAdd = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLStatusBar = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jPBSearch = new javax.swing.JProgressBar();
        jPanel11 = new javax.swing.JPanel();
        jLThreadStatus = new javax.swing.JLabel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jPanel2 = new javax.swing.JPanel();
        jSPMain3 = new javax.swing.JSplitPane();
        jPanel8 = new javax.swing.JPanel();
        jTPSearches = new javax.swing.JTabbedPane();
        jPanel3 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTStructureTree = new javax.swing.JTree();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jLSearchKeys = new javax.swing.JList();
        jPanel23 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLWhenAdded = new javax.swing.JList<>();
        jTPTechnical = new javax.swing.JTabbedPane();
        jPanel18 = new javax.swing.JPanel();
        jPanel30 = new javax.swing.JPanel();
        jPanel27 = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        jPanel28 = new javax.swing.JPanel();
        jCBExpFilter = new javax.swing.JComboBox();
        jPanel24 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        jRBExpFile = new javax.swing.JRadioButton();
        jRBExpClip = new javax.swing.JRadioButton();
        jPanel25 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jBtnExpSel = new javax.swing.JButton();
        jBtnExpTab = new javax.swing.JButton();
        jBtnExpAll = new javax.swing.JButton();
        jPanel26 = new javax.swing.JPanel();
        jTFExpFile = new javax.swing.JTextField();
        jBtnSelExpFile = new javax.swing.JButton();
        jPanel21 = new javax.swing.JPanel();
        jPanel15 = new javax.swing.JPanel();
        jSPMain = new javax.swing.JSplitPane();
        jTPTabList = new javax.swing.JTabbedPane();
        jMainMenu = new javax.swing.JMenuBar();
        jMFile = new javax.swing.JMenu();
        jMIConfig = new javax.swing.JMenuItem();
        jMIClearLoggingFile = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        jMIQuit = new javax.swing.JMenuItem();
        jMLibraries = new javax.swing.JMenu();
        jMIEditLib = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JSeparator();
        jMICreateLib = new javax.swing.JMenuItem();
        jMILoadLib = new javax.swing.JMenuItem();
        jMRecent = new javax.swing.JMenu();
        jMISaveLib = new javax.swing.JMenuItem();
        jMICloseLib = new javax.swing.JMenuItem();
        jMIDeleteLib = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMIAddToLib = new javax.swing.JMenuItem();
        jSeparator28 = new javax.swing.JSeparator();
        jMIDeepSearch = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        jMIEditDS = new javax.swing.JMenuItem();
        jSeparator23 = new javax.swing.JPopupMenu.Separator();
        jMCDisplayHidden = new javax.swing.JRadioButtonMenuItem();
        jSeparator33 = new javax.swing.JPopupMenu.Separator();
        jMIConvLib = new javax.swing.JMenuItem();
        jMIEditLibTemplates = new javax.swing.JMenuItem();
        jMIConsistencyCheck = new javax.swing.JMenuItem();
        jMTabs = new javax.swing.JMenu();
        jMIAddTab = new javax.swing.JMenuItem();
        jMICopyTab = new javax.swing.JMenuItem();
        jMITab2Cat = new javax.swing.JMenuItem();
        jMICloseTab = new javax.swing.JMenuItem();
        jSeparator5 = new javax.swing.JPopupMenu.Separator();
        jMPeople = new javax.swing.JMenu();
        jMIMerge1 = new javax.swing.JMenuItem();
        jMItems = new javax.swing.JMenu();
        jMIView = new javax.swing.JMenuItem();
        jMShow1 = new javax.swing.JMenu();
        jMShowCombined1 = new javax.swing.JMenuItem();
        jMShowLinked1 = new javax.swing.JMenuItem();
        jMIViewPlain = new javax.swing.JMenuItem();
        jMICreateTxt = new javax.swing.JMenuItem();
        jMIPeople = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMIRemoveFromTab = new javax.swing.JMenuItem();
        jMIUnregisterDoc = new javax.swing.JMenuItem();
        jMIDeleteFile = new javax.swing.JMenuItem();
        jMIRemoveHalf = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        jMIAssociateFile = new javax.swing.JMenuItem();
        jMIJoin = new javax.swing.JMenuItem();
        jMICreateCombiner = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        jMCopyToDiff = new javax.swing.JMenu();
        jMIExportTab = new javax.swing.JMenuItem();
        jMIEmail = new javax.swing.JMenuItem();
        jMCategories = new javax.swing.JMenu();
        jMIInsertCat = new javax.swing.JMenuItem();
        jMIRenameCat = new javax.swing.JMenuItem();
        jMIDelCat = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JSeparator();
        jMICatUp = new javax.swing.JMenuItem();
        jMICatDown = new javax.swing.JMenuItem();
        jMICatSub = new javax.swing.JMenuItem();
        jMICatSuper = new javax.swing.JMenuItem();
        jMBibTeX = new javax.swing.JMenu();
        jMICitationTagClipboard = new javax.swing.JMenuItem();
        jMIBibClipboard = new javax.swing.JMenuItem();
        jMIFullBibToFile = new javax.swing.JMenuItem();
        jSeparator19 = new javax.swing.JPopupMenu.Separator();
        jMIShowCitedinFile = new javax.swing.JMenuItem();
        jSeparator7 = new javax.swing.JPopupMenu.Separator();
        jMICheckBib = new javax.swing.JMenuItem();
        jMHelp = new javax.swing.JMenu();
        JMIManual = new javax.swing.JMenuItem();
        jMIUpdate = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JSeparator();
        jMIAbout = new javax.swing.JMenuItem();

        jPasswordField1.setText("jPasswordField1");

        javax.swing.GroupLayout jDialog1Layout = new javax.swing.GroupLayout(jDialog1.getContentPane());
        jDialog1.getContentPane().setLayout(jDialog1Layout);
        jDialog1Layout.setHorizontalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addGap(87, 87, 87)
                .addComponent(jScrollBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(296, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog1Layout.createSequentialGroup()
                .addContainerGap(208, Short.MAX_VALUE)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(83, 83, 83))
        );
        jDialog1Layout.setVerticalGroup(
            jDialog1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jDialog1Layout.createSequentialGroup()
                .addGap(94, 94, 94)
                .addComponent(jScrollBar1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(145, Short.MAX_VALUE))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jDialog1Layout.createSequentialGroup()
                .addContainerGap(155, Short.MAX_VALUE)
                .addComponent(jPasswordField1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(126, 126, 126))
        );

        jMINew.setText("New Tab");
        jMINew.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMINewActionPerformed(evt);
            }
        });
        jPMItemList.add(jMINew);

        jMICopyTab2.setText("Copy Tab");
        jMICopyTab2.setEnabled(false);
        jMICopyTab2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICopyTabActionPerformed(evt);
            }
        });
        jPMItemList.add(jMICopyTab2);

        jMITab2Cat2.setText("Create Category from Tab");
        jMITab2Cat2.setEnabled(false);
        jMITab2Cat2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMITab2Cat2ActionPerformed(evt);
            }
        });
        jPMItemList.add(jMITab2Cat2);

        jMICloseTab2.setText("Close Tab");
        jMICloseTab2.setEnabled(false);
        jMICloseTab2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICloseTab2TabActionPerformed(evt);
            }
        });
        jPMItemList.add(jMICloseTab2);

        jPMCategories.addPropertyChangeListener(new java.beans.PropertyChangeListener() {
            public void propertyChange(java.beans.PropertyChangeEvent evt) {
                jPMCategoriesPropertyChange(evt);
            }
        });

        jMIOpenNewTab.setText("Open Category in New Tab");
        jMIOpenNewTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIOpenNewTabActionPerformed(evt);
            }
        });
        jPMCategories.add(jMIOpenNewTab);
        jPMCategories.add(jSeparator16);

        jMIInsertCat1.setText("Insert Subcategory");
        jMIInsertCat1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIInsertCatActionPerformed(evt);
            }
        });
        jPMCategories.add(jMIInsertCat1);

        jMIRenameCat1.setText("Rename Category");
        jMIRenameCat1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRenameCatActionPerformed(evt);
            }
        });
        jPMCategories.add(jMIRenameCat1);

        jMIDelCat1.setText("Delete Category");
        jMIDelCat1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDelCatActionPerformed(evt);
            }
        });
        jPMCategories.add(jMIDelCat1);
        jPMCategories.add(jSeparator15);

        jMICatUp1.setText("Move up");
        jMICatUp1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatUpActionPerformed(evt);
            }
        });
        jPMCategories.add(jMICatUp1);

        jMICatDown1.setText("Move down");
        jMICatDown1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatDownActionPerformed(evt);
            }
        });
        jPMCategories.add(jMICatDown1);

        jMICatSub1.setText("Turn into subcategory of above");
        jMICatSub1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatSubActionPerformed(evt);
            }
        });
        jPMCategories.add(jMICatSub1);

        jMICatSuper1.setText("Turn into supercategory");
        jMICatSuper1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatSuperActionPerformed(evt);
            }
        });
        jPMCategories.add(jMICatSuper1);
        jPMCategories.add(jSeparator31);

        jMIExpand.setText("Expand tree");
        jMIExpand.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIExpandActionPerformed(evt);
            }
        });
        jPMCategories.add(jMIExpand);

        jMICollapse.setText("Collapse tree");
        jMICollapse.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICollapseActionPerformed(evt);
            }
        });
        jPMCategories.add(jMICollapse);

        jMIView1.setText("Open selected item");
        jMIView1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIView1);

        jMActions.setText("Action");
        jPMItemTable.add(jMActions);

        jMShow.setText("Show");

        jMShowCombined.setText("Show Combined");
        jMShowCombined.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMShowCombinedActionPerformed(evt);
            }
        });
        jMShow.add(jMShowCombined);

        jMShowLinked.setText("Show Linked");
        jMShowLinked.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMShowLinkedActionPerformed(evt);
            }
        });
        jMShow.add(jMShowLinked);

        jPMItemTable.add(jMShow);
        jPMItemTable.add(jSeparator30);

        jMIViewPlain1.setText("View plain text");
        jMIViewPlain1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewPlainActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIViewPlain1);

        jMIReExtract1.setText("Re-extract plain text");
        jMIReExtract1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICreateTxtActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIReExtract1);

        jMIPeople2.setText("Show associated people");
        jMIPeople2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIPeople2ActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIPeople2);
        jPMItemTable.add(jSeparator20);

        jMIRemoveFromTab1.setText("Remove from current table");
        jMIRemoveFromTab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveFromTabActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIRemoveFromTab1);

        jMIUnregisterDoc1.setText("Remove from current category");
        jMIUnregisterDoc1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIUnregisterDocActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIUnregisterDoc1);

        jMIDeleteFile1.setText("Remove from library and delete files");
        jMIDeleteFile1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeleteFileActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIDeleteFile1);

        jMIRemoveHalf1.setText("Remove from library and keep files");
        jMIRemoveHalf1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveHalfActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIRemoveHalf1);
        jPMItemTable.add(jSeparator22);

        jMICites1.setText("Add rule \"cites\"");
        jMICites1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICitesActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMICites1);
        jPMItemTable.add(jSeparator24);

        jMIAssociateFile1.setText("Associate file to current entry");
        jMIAssociateFile1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAssociateFileActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIAssociateFile1);

        jMIJoin1.setText("Combine the selected items");
        jMIJoin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIJoinActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIJoin1);

        jMICreateCombiner1.setText("Create a combining item");
        jMICreateCombiner1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICreateCombiner1ActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMICreateCombiner1);
        jPMItemTable.add(jSeparator26);

        jMCopyToDiff1.setText("Copy to library");
        jPMItemTable.add(jMCopyToDiff1);

        jMIExportTab1.setText("Export files of selected items");
        jMIExportTab1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIExportTabActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIExportTab1);

        jMIEmail1.setText("Send selected items in email");
        jMIEmail1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEmailActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIEmail1);

        jMIMerge.setText("Merge selected people");
        jMIMerge.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIMergeActionPerformed(evt);
            }
        });
        jPMPeopleTable.add(jMIMerge);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("Celsius Library System v2.0");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(3, 10, 1, 10));
        jPanel5.setLayout(new java.awt.BorderLayout(100, 0));

        jPanel7.setPreferredSize(new java.awt.Dimension(RSC.guiScale(180), RSC.guiScale(49)));

        jCBLibraries.setPreferredSize(new java.awt.Dimension(RSC.guiScale(202), RSC.guiScale(26)));
        jCBLibraries.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBLibrariesActionPerformed(evt);
            }
        });

        jLabel1.setText("Active Library:");

        javax.swing.GroupLayout jPanel7Layout = new javax.swing.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jCBLibraries, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel7Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBLibraries, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel5.add(jPanel7, java.awt.BorderLayout.WEST);

        java.awt.FlowLayout flowLayout1 = new java.awt.FlowLayout();
        flowLayout1.setAlignOnBaseline(true);
        jPanel20.setLayout(flowLayout1);

        jTBAdd.setIcon(RSC.getScaledIcon("Add Icon"));
        jTBAdd.setToolTipText("Add items to current library");
        jTBAdd.setEnabled(false);
        jTBAdd.setPreferredSize(new java.awt.Dimension(RSC.guiScale(42), RSC.guiScale(42)));
        jTBAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAddToLibActionPerformed(evt);
            }
        });
        jPanel20.add(jTBAdd);

        jPanel5.add(jPanel20, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel5, java.awt.BorderLayout.NORTH);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jPanel1.setToolTipText("Status-Bar");
        jPanel1.setPreferredSize(new java.awt.Dimension(RSC.guiScale(100), RSC.guiScale(20)));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel10.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel10.setPreferredSize(new java.awt.Dimension(RSC.guiScale(200), RSC.guiScale(100)));
        jPanel10.setLayout(new java.awt.BorderLayout());

        jLStatusBar.setText("Status-Bar:");
        jLStatusBar.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 0));
        jPanel10.add(jLStatusBar, java.awt.BorderLayout.CENTER);

        jPanel1.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel12.setLayout(new java.awt.GridLayout(1, 0));

        jPanel13.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        jPBSearch.setBorder(null);
        jPBSearch.setMinimumSize(new java.awt.Dimension(10, 6));
        jPBSearch.setPreferredSize(new java.awt.Dimension(RSC.guiScale(146), RSC.guiScale(6)));

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 208, Short.MAX_VALUE)
            .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPBSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 208, Short.MAX_VALUE))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 6, Short.MAX_VALUE)
            .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jPBSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 6, Short.MAX_VALUE))
        );

        jPanel12.add(jPanel13);

        jPanel11.setBorder(javax.swing.BorderFactory.createEtchedBorder());
        jPanel11.setPreferredSize(new java.awt.Dimension(RSC.guiScale(184), RSC.guiScale(100)));
        jPanel11.setLayout(new java.awt.BorderLayout());

        jLThreadStatus.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLThreadStatus.setText("Threads: all ended");
        jLThreadStatus.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 4, 0, 0));
        jLThreadStatus.setMinimumSize(new java.awt.Dimension(180, 15));
        jPanel11.add(jLThreadStatus, java.awt.BorderLayout.CENTER);

        jPanel12.add(jPanel11);

        jPanel1.add(jPanel12, java.awt.BorderLayout.EAST);

        jPanel6.add(jPanel1, java.awt.BorderLayout.SOUTH);

        jSplitPane1.setOneTouchExpandable(true);

        jPanel2.setLayout(new java.awt.GridLayout(1, 0));

        jSPMain3.setDividerLocation(300);
        jSPMain3.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSPMain3.setResizeWeight(1.0);
        jSPMain3.setMinimumSize(new java.awt.Dimension(256, 321));
        jSPMain3.setOneTouchExpandable(true);
        jSPMain3.setPreferredSize(new java.awt.Dimension(RSC.guiScale(256), RSC.guiScale(948)));

        jPanel8.setPreferredSize(new java.awt.Dimension(RSC.guiScale(205), RSC.guiScale(427)));
        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        jTPSearches.setPreferredSize(new java.awt.Dimension(204, 186));

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setPreferredSize(new java.awt.Dimension(RSC.guiScale(200), RSC.guiScale(600)));
        jPanel3.setLayout(new java.awt.BorderLayout());

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("JTree");
        javax.swing.tree.DefaultMutableTreeNode treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("colors");
        javax.swing.tree.DefaultMutableTreeNode treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("blue");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("violet");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("red");
        treeNode2.add(treeNode3);
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("yellow");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        treeNode2 = new javax.swing.tree.DefaultMutableTreeNode("sports");
        treeNode3 = new javax.swing.tree.DefaultMutableTreeNode("basketball");
        treeNode2.add(treeNode3);
        treeNode1.add(treeNode2);
        jTStructureTree.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        jTStructureTree.setComponentPopupMenu(jPMCategories);
        jTStructureTree.addTreeSelectionListener(new javax.swing.event.TreeSelectionListener() {
            public void valueChanged(javax.swing.event.TreeSelectionEvent evt) {
                jTStructureTreeValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jTStructureTree);

        jPanel3.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jTPSearches.addTab("", new javax.swing.ImageIcon(getClass().getResource("/celsius/images/iconmonstr-folder-30.svg.24.png")), jPanel3); // NOI18N

        jPanel9.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel9.setPreferredSize(new java.awt.Dimension(200, 157));
        jPanel9.setLayout(new java.awt.BorderLayout());

        jScrollPane7.setPreferredSize(new java.awt.Dimension(RSC.guiScale(259), RSC.guiScale(31)));

        jLSearchKeys.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jLSearchKeys.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLSearchKeysMouseClicked(evt);
            }
        });
        jLSearchKeys.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLSearchKeysValueChanged(evt);
            }
        });
        jScrollPane7.setViewportView(jLSearchKeys);

        jPanel9.add(jScrollPane7, java.awt.BorderLayout.CENTER);

        jTPSearches.addTab("", new javax.swing.ImageIcon(getClass().getResource("/celsius/images/iconmonstr-key-2.svg.24.png")), jPanel9); // NOI18N

        jPanel23.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel23.setLayout(new java.awt.BorderLayout());

        jLWhenAdded.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jLWhenAdded.setMaximumSize(new java.awt.Dimension(32000, 32000));
        jLWhenAdded.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLWhenAddedValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jLWhenAdded);

        jPanel23.add(jScrollPane2, java.awt.BorderLayout.CENTER);

        jTPSearches.addTab("", new javax.swing.ImageIcon(getClass().getResource("/celsius/images/iconmonstr-calendar-5.svg.24.png")), jPanel23); // NOI18N

        jPanel8.add(jTPSearches);

        jSPMain3.setTopComponent(jPanel8);

        jPanel18.setMinimumSize(new java.awt.Dimension(100, 115));
        jPanel18.setPreferredSize(new java.awt.Dimension(200, 83));
        jPanel18.setLayout(new java.awt.BorderLayout());

        jPanel30.setLayout(new java.awt.GridLayout(5, 0, 0, RSC.guiScale(5)));

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel3.setText("Export:");
        jLabel3.setVerticalAlignment(javax.swing.SwingConstants.BOTTOM);
        jLabel3.setAlignmentX(Component.LEFT_ALIGNMENT);

        javax.swing.GroupLayout jPanel27Layout = new javax.swing.GroupLayout(jPanel27);
        jPanel27.setLayout(jPanel27Layout);
        jPanel27Layout.setHorizontalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel27Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(0, 191, Short.MAX_VALUE))
        );
        jPanel27Layout.setVerticalGroup(
            jPanel27Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel27Layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabel3)
                .addContainerGap())
        );

        jPanel30.add(jPanel27);

        jPanel28.setPreferredSize(new java.awt.Dimension(190,RSC.guiScale(24)));
        jPanel28.setLayout(new java.awt.GridLayout(1, 0));

        jCBExpFilter.setPreferredSize(new java.awt.Dimension(190,RSC.guiScale(24)));
        jCBExpFilter.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBExpFilterActionPerformed(evt);
            }
        });
        jPanel28.add(jCBExpFilter);

        jPanel30.add(jPanel28);

        jPanel24.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, RSC.guiScale(5), 5));

        jLabel6.setText("Target:");
        jPanel24.add(jLabel6);

        buttonGroup3.add(jRBExpFile);
        jRBExpFile.setSelected(true);
        jRBExpFile.setText("File");
        jRBExpFile.addItemListener(new java.awt.event.ItemListener() {
            public void itemStateChanged(java.awt.event.ItemEvent evt) {
                jRBExpFileItemStateChanged(evt);
            }
        });
        jPanel24.add(jRBExpFile);

        buttonGroup3.add(jRBExpClip);
        jRBExpClip.setText("Clipboard");
        jPanel24.add(jRBExpClip);

        jPanel30.add(jPanel24);

        jPanel25.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT, RSC.guiScale(5), 5));

        jLabel5.setText("Export");
        jPanel25.add(jLabel5);

        jBtnExpSel.setText("selected");
        jBtnExpSel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnExpSelActionPerformed(evt);
            }
        });
        jPanel25.add(jBtnExpSel);

        jBtnExpTab.setText("current table");
        jBtnExpTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnExpTabActionPerformed(evt);
            }
        });
        jPanel25.add(jBtnExpTab);

        jBtnExpAll.setText("all items");
        jBtnExpAll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnExpAllActionPerformed(evt);
            }
        });
        jPanel25.add(jBtnExpAll);

        jPanel30.add(jPanel25);

        jBtnSelExpFile.setText("Choose");
        jBtnSelExpFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnSelExpFileActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel26Layout = new javax.swing.GroupLayout(jPanel26);
        jPanel26.setLayout(jPanel26Layout);
        jPanel26Layout.setHorizontalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addComponent(jTFExpFile, javax.swing.GroupLayout.DEFAULT_SIZE, 155, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnSelExpFile)
                .addContainerGap())
        );
        jPanel26Layout.setVerticalGroup(
            jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel26Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel26Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTFExpFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnSelExpFile))
                .addGap(5, 5, 5))
        );

        jPanel30.add(jPanel26);

        jPanel18.add(jPanel30, java.awt.BorderLayout.NORTH);

        javax.swing.GroupLayout jPanel21Layout = new javax.swing.GroupLayout(jPanel21);
        jPanel21.setLayout(jPanel21Layout);
        jPanel21Layout.setHorizontalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 236, Short.MAX_VALUE)
        );
        jPanel21Layout.setVerticalGroup(
            jPanel21Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 0, Short.MAX_VALUE)
        );

        jPanel18.add(jPanel21, java.awt.BorderLayout.CENTER);

        jTPTechnical.addTab("", jPanel18);

        jSPMain3.setRightComponent(jTPTechnical);

        jPanel2.add(jSPMain3);

        jSplitPane1.setLeftComponent(jPanel2);

        jPanel15.setLayout(new java.awt.GridLayout(1, 0));

        jSPMain.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSPMain.setMinimumSize(new java.awt.Dimension(300, 37));
        jSPMain.setOneTouchExpandable(true);

        jTPTabList.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTPTabList.setComponentPopupMenu(jPMItemList);
        jTPTabList.setMinimumSize(new java.awt.Dimension(0, 0));
        jTPTabList.setOpaque(true);
        jTPTabList.setPreferredSize(new java.awt.Dimension(300, 600));
        jTPTabList.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTPTabListStateChanged(evt);
            }
        });
        jTPTabList.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                jTPTabListComponentResized(evt);
            }
        });
        jSPMain.setTopComponent(jTPTabList);

        jPanel15.add(jSPMain);

        jSplitPane1.setRightComponent(jPanel15);

        jPanel6.add(jSplitPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel6, java.awt.BorderLayout.CENTER);

        jMFile.setText("File");

        jMIConfig.setText("Configuration");
        jMIConfig.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIConfigActionPerformed(evt);
            }
        });
        jMFile.add(jMIConfig);

        jMIClearLoggingFile.setText("Clear logging file");
        jMIClearLoggingFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIClearLoggingFileActionPerformed(evt);
            }
        });
        jMFile.add(jMIClearLoggingFile);
        jMFile.add(jSeparator10);

        jMIQuit.setText("Quit");
        jMIQuit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIQuitActionPerformed(evt);
            }
        });
        jMFile.add(jMIQuit);

        jMainMenu.add(jMFile);

        jMLibraries.setText("Libraries");

        jMIEditLib.setText("Edit library properties");
        jMIEditLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIEditLib);
        jMLibraries.add(jSeparator11);

        jMICreateLib.setText("Create new library");
        jMICreateLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICreateLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMICreateLib);

        jMILoadLib.setText("Load library");
        jMILoadLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMILoadLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMILoadLib);

        jMRecent.setText("Open recent libraries");
        jMLibraries.add(jMRecent);

        jMISaveLib.setText("Save current library");
        jMISaveLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMISaveLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMISaveLib);

        jMICloseLib.setText("Close current library");
        jMICloseLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICloseLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMICloseLib);

        jMIDeleteLib.setText("Delete current library");
        jMIDeleteLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeleteLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIDeleteLib);
        jMLibraries.add(jSeparator2);

        jMIAddToLib.setText("Add items to library");
        jMIAddToLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAddToLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIAddToLib);
        jMLibraries.add(jSeparator28);

        jMIDeepSearch.setText("Search in Library");
        jMIDeepSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeepSearchActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIDeepSearch);
        jMLibraries.add(jSeparator4);

        jMIEditDS.setText("Edit HTML template");
        jMIEditDS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditDSActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIEditDS);
        jMLibraries.add(jSeparator23);

        jMCDisplayHidden.setText("Show hidden items");
        jMCDisplayHidden.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jMCDisplayHiddenStateChanged(evt);
            }
        });
        jMLibraries.add(jMCDisplayHidden);
        jMLibraries.add(jSeparator33);

        jMIConvLib.setText("Convert Library");
        jMIConvLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIConvLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIConvLib);

        jMIEditLibTemplates.setText("Edit library templates");
        jMIEditLibTemplates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditLibTemplatesActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIEditLibTemplates);

        jMIConsistencyCheck.setText("Check library consistency");
        jMIConsistencyCheck.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIConsistencyCheckActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIConsistencyCheck);

        jMainMenu.add(jMLibraries);

        jMTabs.setText("Tables");

        jMIAddTab.setText("New table");
        jMIAddTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAddTabActionPerformed(evt);
            }
        });
        jMTabs.add(jMIAddTab);

        jMICopyTab.setText("Copy table");
        jMICopyTab.setEnabled(false);
        jMICopyTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICopyTabActionPerformed(evt);
            }
        });
        jMTabs.add(jMICopyTab);

        jMITab2Cat.setText("Create category from table");
        jMITab2Cat.setEnabled(false);
        jMITab2Cat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMITab2Cat2ActionPerformed(evt);
            }
        });
        jMTabs.add(jMITab2Cat);

        jMICloseTab.setText("Close table");
        jMICloseTab.setEnabled(false);
        jMICloseTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICloseTab2TabActionPerformed(evt);
            }
        });
        jMTabs.add(jMICloseTab);
        jMTabs.add(jSeparator5);

        jMainMenu.add(jMTabs);

        jMPeople.setText("People");

        jMIMerge1.setText("Merge People");
        jMIMerge1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIMerge1ActionPerformed(evt);
            }
        });
        jMPeople.add(jMIMerge1);

        jMainMenu.add(jMPeople);

        jMItems.setText("Items");

        jMIView.setText("Open selected item");
        jMIView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewActionPerformed(evt);
            }
        });
        jMItems.add(jMIView);

        jMShow1.setText("Show");

        jMShowCombined1.setText("Show Combined");
        jMShowCombined1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMShowCombinedActionPerformed(evt);
            }
        });
        jMShow1.add(jMShowCombined1);

        jMShowLinked1.setText("Show Linked");
        jMShowLinked1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMShowLinkedActionPerformed(evt);
            }
        });
        jMShow1.add(jMShowLinked1);

        jMItems.add(jMShow1);

        jMIViewPlain.setText("View plain text");
        jMIViewPlain.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewPlainActionPerformed(evt);
            }
        });
        jMItems.add(jMIViewPlain);

        jMICreateTxt.setText("Re-extract plain text ");
        jMICreateTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICreateTxtActionPerformed(evt);
            }
        });
        jMItems.add(jMICreateTxt);

        jMIPeople.setText("Show associated people");
        jMIPeople.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIPeopleActionPerformed(evt);
            }
        });
        jMItems.add(jMIPeople);
        jMItems.add(jSeparator3);

        jMIRemoveFromTab.setText("Remove from current table");
        jMIRemoveFromTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveFromTabActionPerformed(evt);
            }
        });
        jMItems.add(jMIRemoveFromTab);

        jMIUnregisterDoc.setText("Remove from current category");
        jMIUnregisterDoc.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIUnregisterDocActionPerformed(evt);
            }
        });
        jMItems.add(jMIUnregisterDoc);

        jMIDeleteFile.setText("Remove from library and delete attachments");
        jMIDeleteFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeleteFileActionPerformed(evt);
            }
        });
        jMItems.add(jMIDeleteFile);

        jMIRemoveHalf.setText("Remove from library but keep attachments");
        jMIRemoveHalf.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveHalfActionPerformed(evt);
            }
        });
        jMItems.add(jMIRemoveHalf);
        jMItems.add(jSeparator6);

        jMIAssociateFile.setText("Associate file to current entry");
        jMIAssociateFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAssociateFileActionPerformed(evt);
            }
        });
        jMItems.add(jMIAssociateFile);

        jMIJoin.setText("Combine the selected items");
        jMIJoin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIJoinActionPerformed(evt);
            }
        });
        jMItems.add(jMIJoin);

        jMICreateCombiner.setText("Create a combining item");
        jMICreateCombiner.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICreateCombiner1ActionPerformed(evt);
            }
        });
        jMItems.add(jMICreateCombiner);
        jMItems.add(jSeparator8);

        jMCopyToDiff.setText("Copy to library");
        jMItems.add(jMCopyToDiff);

        jMIExportTab.setText("Export files of selected items");
        jMIExportTab.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIExportTabActionPerformed(evt);
            }
        });
        jMItems.add(jMIExportTab);

        jMIEmail.setText("Send selected items in email");
        jMIEmail.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEmailActionPerformed(evt);
            }
        });
        jMItems.add(jMIEmail);

        jMainMenu.add(jMItems);

        jMCategories.setText("Categories");

        jMIInsertCat.setText("Insert subcategory");
        jMIInsertCat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIInsertCatActionPerformed(evt);
            }
        });
        jMCategories.add(jMIInsertCat);

        jMIRenameCat.setText("Rename category");
        jMIRenameCat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRenameCatActionPerformed(evt);
            }
        });
        jMCategories.add(jMIRenameCat);

        jMIDelCat.setText("Delete category");
        jMIDelCat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDelCatActionPerformed(evt);
            }
        });
        jMCategories.add(jMIDelCat);
        jMCategories.add(jSeparator14);

        jMICatUp.setText("Move up");
        jMICatUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatUpActionPerformed(evt);
            }
        });
        jMCategories.add(jMICatUp);

        jMICatDown.setText("Move down");
        jMICatDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatDownActionPerformed(evt);
            }
        });
        jMCategories.add(jMICatDown);

        jMICatSub.setText("Turn into subcategory of above");
        jMICatSub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatSubActionPerformed(evt);
            }
        });
        jMCategories.add(jMICatSub);

        jMICatSuper.setText("Turn into supercategory");
        jMICatSuper.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatSuperActionPerformed(evt);
            }
        });
        jMCategories.add(jMICatSuper);

        jMainMenu.add(jMCategories);

        jMBibTeX.setText("Bibliography");

        jMICitationTagClipboard.setText("Citation tag to clipboard");
        jMICitationTagClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICitationTagClipboardActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMICitationTagClipboard);

        jMIBibClipboard.setText("BibTeX for selected items to clipboard");
        jMIBibClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIBibClipboardActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMIBibClipboard);

        jMIFullBibToFile.setText("Create BibTeX file from library");
        jMIFullBibToFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIFullBibToFileActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMIFullBibToFile);
        jMBibTeX.add(jSeparator19);

        jMIShowCitedinFile.setText("Show all papers cited in a TeX file");
        jMIShowCitedinFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIShowCitedinFileActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMIShowCitedinFile);
        jMBibTeX.add(jSeparator7);

        jMICheckBib.setText("Check BibTeX integrity");
        jMICheckBib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICheckBibActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMICheckBib);

        jMainMenu.add(jMBibTeX);

        jMHelp.setText("Help");

        JMIManual.setText("Manual");
        JMIManual.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                JMIManualActionPerformed(evt);
            }
        });
        jMHelp.add(JMIManual);

        jMIUpdate.setText("Celsius Homepage");
        jMIUpdate.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIUpdateActionPerformed(evt);
            }
        });
        jMHelp.add(jMIUpdate);
        jMHelp.add(jSeparator1);

        jMIAbout.setText("About");
        jMIAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAboutActionPerformed(evt);
            }
        });
        jMHelp.add(jMIAbout);

        jMainMenu.add(jMHelp);

        setJMenuBar(jMainMenu);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jMIJoinActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIJoinActionPerformed
        if (RSC.guiStates.getState("mainFrame", "tabAvailable")) {
            // TODO
            /*            Library Lib = RSC.getCurrentlySelectedLibrary();
            ArrayList<Item> docs=RSC.getCurrentItemTable().getSelectedRows();
            if (docs.size() > 2) {
                RSC.showWarning("Only the first two entries will be joined.", "Warning:");
            } else {
                if (docs.size()<2) {
                    RSC.showWarning("Please selected two items to be joined.", "Warning:");
                    return;
                }
            }
            String id0=docs.get(0).get("id");
            String id=docs.get(0).id;
            String id1=docs.get(1).get("id");
            if ((Lib,id1,id0)) {
                RSC.getCurrentItemTable().reloadItem(new Item(Lib,id1));
                int i=RSC.getCurrentItemTable().getSelectedRow();

                DefaultListSelectionModel DLSM=(DefaultListSelectionModel) RSC.getCurrentItemTable().jtable.getSelectionModel();
                DLSM.addSelectionInterval(i, i);
                RSC.getCurrentItemTable().jtable.setSelectionModel(DLSM);
                RSC.getCurrentItemTable().removeID(id);
                updateStatusBar(true);
                jIP.updateHTMLview();
                jIP.updateCurrentItemInGUI();
            }*/
        }

    }//GEN-LAST:event_jMIJoinActionPerformed

    private void jMIAssociateFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAssociateFileActionPerformed
        associateFileToCurrentItem();
    }//GEN-LAST:event_jMIAssociateFileActionPerformed

    private void jMICopyTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICopyTabActionPerformed
        RSC.guiStates.adjustState("mainFrame", "itemSelected", false);
        CelsiusTable DT=new CelsiusTable(RSC.getCurrentTable());
        RSC.celsiusTables.add(DT);

        jMICloseTab2.setEnabled(true);
        jMICloseTab.setEnabled(true);
        final JScrollPane scrollpane = new JScrollPane(DT.jtable);
        jTPTabList.add(scrollpane);
        TabLabel TL=(TabLabel)jTPTabList.getTabComponentAt(jTPTabList.getSelectedIndex());
        jTPTabList.setTabComponentAt(jTPTabList.getTabCount() - 1, new TabLabel(TL.title + "'",TL.II,RSC,DT,true));
        DT.title=TL.title + "'";
        jTPTabList.setSelectedComponent(scrollpane);
        jTPTabList.setSelectedIndex(jTPTabList.getTabCount() - 1);
        int cordx = bufmousex - jTStructureTree.getLocationOnScreen().x;
        int cordy = bufmousey - jTStructureTree.getLocationOnScreen().y;
        jTStructureTree.setSelectionPath(null);
        jTStructureTree.setSelectionPath(jTStructureTree.getPathForLocation(cordx, cordy));
    }//GEN-LAST:event_jMICopyTabActionPerformed

    private void jPMCategoriesPropertyChange(java.beans.PropertyChangeEvent evt) {//GEN-FIRST:event_jPMCategoriesPropertyChange
        String s1 = Parser.cutUntil(Parser.cutFrom(evt.toString(), "desiredLocationX="), ",");
        String s2 = Parser.cutUntil(Parser.cutFrom(evt.toString(), "desiredLocationY="), ",");
        bufmousex = Integer.valueOf(s1);
        bufmousey = Integer.valueOf(s2);
    }//GEN-LAST:event_jPMCategoriesPropertyChange

    private void jMICreateTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICreateTxtActionPerformed
        // plaintxt already existing? TODO!!
        Library library = RSC.getCurrentlySelectedLibrary();
        boolean doit=false;
        int h=0;
        for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
            ((Item)tableRow).redoPlainText();
        }
        guiInfoPanel.updateHTMLview();
        guiInfoPanel.updateGUI();
        updateStatusBar(true);
    }//GEN-LAST:event_jMICreateTxtActionPerformed

    private void jMIConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConfigActionPerformed
        dialogConfiguration.open();
    }//GEN-LAST:event_jMIConfigActionPerformed

    private void jMIRenameCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRenameCatActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        final Library library = RSC.getCurrentlySelectedLibrary();
        if (node==library.structureTreeRoot) return;
        final SingleLineEditor DSLE = new SingleLineEditor(RSC, "Please enter a new name for the category", node.toString(),true);
        DSLE.setVisible(true);
        if (!DSLE.cancel) {
            jPBSearch.setIndeterminate(true);
            String Tnew = DSLE.text.trim();
            library.renameCategory(node,Tnew);
            StructureTreeModel.reload();
            final StructureNode child = node;
            jTStructureTree.scrollPathToVisible(new TreePath(child.getPath().toArray()));
            jPBSearch.setIndeterminate(false);
            updateStatusBar(false);
        }
        DSLE.dispose();
    }//GEN-LAST:event_jMIRenameCatActionPerformed

    private void jMIShowCitedinFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIShowCitedinFileActionPerformed
        String filename=RSC.selectFile("Indicate the LaTeX source file", "showcited", ".tex", "LaTeX files");
        if (filename!=null) {
            CelsiusTable celsiusTable=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Cited in " + filename,"search");
            celsiusTable.resizeTable(true);
            RSC.guiStates.adjustState("mainFrame","itemSelected", false);
            ProgressMonitor progressMonitor = new ProgressMonitor(this, "Looking for papers ...", "", 0, RSC.getCurrentlySelectedLibrary().getSize());
            SWShowCited swAP = new SWShowCited(celsiusTable,0,filename);
            swAP.execute();
        }
    }//GEN-LAST:event_jMIShowCitedinFileActionPerformed

    private void jMIOpenNewTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIOpenNewTabActionPerformed
        RSC.makeNewTabAvailable(CelsiusTable.EMPTY_TABLE, "", "default");
        jTPTabList.setSelectedIndex(jTPTabList.getTabCount() - 1);
        int cordx = bufmousex - jTStructureTree.getLocationOnScreen().x;
        int cordy = bufmousey - jTStructureTree.getLocationOnScreen().y;
        jTStructureTree.setSelectionPath(null);
        jTStructureTree.setSelectionPath(jTStructureTree.getPathForLocation(cordx, cordy));
    }//GEN-LAST:event_jMIOpenNewTabActionPerformed

    private void jMIConsistencyCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConsistencyCheckActionPerformed
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "", "", 0, RSC.getCurrentlySelectedLibrary().getSize());                    // Progress label
        setThreadMsg("Working...");
        SWLibraryCheck swLibraryCheck=new SWLibraryCheck(RSC,progressMonitor);
        swLibraryCheck.execute();
    }//GEN-LAST:event_jMIConsistencyCheckActionPerformed

    private void jMIEditLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditLibActionPerformed
        (new EditLibrary(this,RSC.getCurrentlySelectedLibrary())).setVisible(true);
    }//GEN-LAST:event_jMIEditLibActionPerformed

    private void jMIEditDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditDSActionPerformed
        Library library = RSC.getCurrentlySelectedLibrary();
        String tmp = library.getHTMLTemplate(guiInfoPanel.currentTemplate).templateString;
        MultiLineEditor MLE = new MultiLineEditor(RSC, "Edit HTML template", tmp);
        MLE.setVisible(true);
        if (!MLE.cancel) {
            library.setHTMLTemplate(guiInfoPanel.currentTemplate,MLE.text);
            guiInfoPanel.updateHTMLview();
        }
    }//GEN-LAST:event_jMIEditDSActionPerformed

    private void jMICatSuperActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICatSuperActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if ((node == null) || (node.getParent() == null)) {
            return;
        }
        if (node.getParent().getParent()==null) {
            return;
        }
        StructureNode node2 = node.getParent();
        StructureNode node3 = node2.getParent();
        if ((node2 != RSC.getCurrentlySelectedLibrary().structureTreeRoot) || (node3 != null)) {
            final StructureNode TNT = node;
            StructureTreeModel.removeNodeFromParent(node);
            StructureTreeModel.insertNodeInto(TNT, node3, node3.getChildCount());
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node2);
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node3);
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeParent(node);
            jTStructureTree.scrollPathToVisible(new TreePath(TNT.getPath().toArray()));
        }
    }//GEN-LAST:event_jMICatSuperActionPerformed

    private void jMICatSubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICatSubActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        if (node.isRoot()) {
            return;
        }
        StructureNode node2 = node.getParent();
        final int i = node2.getIndex(node);
        if (i > 0) {
            StructureNode node3=node2.getChildAt(i - 1);
            final StructureNode node4 = node;
            StructureTreeModel.removeNodeFromParent(node);
            StructureTreeModel.insertNodeInto(node4,node3, node3.getChildCount());
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node2);
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node3);
            RSC.getCurrentlySelectedLibrary().updateCategoryNodeParent(node);
            jTStructureTree.scrollPathToVisible(new TreePath(node4.getPath().toArray()));
        }
    }//GEN-LAST:event_jMICatSubActionPerformed

    private void jMICatDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICatDownActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node != null) {
            if (node.isRoot()) {
                return;
            }
            StructureNode node2 = node.getParent();
            final int i = node2.getIndex(node);
            final int j = (jTStructureTree.getSelectionRows())[0];
            if (i < node2.getChildCount() - 1) {
                node2.remove(node);
                node2.insert(node, i + 1);
                StructureTreeModel.nodeStructureChanged(node2);
                jTStructureTree.addSelectionPath(jTStructureTree.getPathForRow(j + 1));
                RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node2);
            }
        }
    }//GEN-LAST:event_jMICatDownActionPerformed

    private void jMICatUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICatUpActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node != null) {
            if (node.isRoot()) {
                return;
            }
            StructureNode node2 = node.getParent();
            final int i = node2.getIndex(node);
            final int j = (jTStructureTree.getSelectionRows())[0];
            if (i > 0) {
                node2.remove(node);
                node2.insert(node, i - 1);
                StructureTreeModel.nodeStructureChanged(node2);
                jTStructureTree.addSelectionPath(jTStructureTree.getPathForRow(j - 1));
                RSC.getCurrentlySelectedLibrary().updateCategoryNodeChildren(node2);
            }
        }
    }//GEN-LAST:event_jMICatUpActionPerformed

    private void jMIDelCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDelCatActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node != null) {
            if (node.isRoot()) {
                return;
            }
            final int i = RSC.askQuestionOC("Click OK to delete subcategory.", "Warning");
            if (i == JOptionPane.YES_OPTION) {
                StructureTreeModel.removeNodeFromParent(node);
                RSC.getCurrentlySelectedLibrary().deleteCategoryNode(node);
            }
        }
    }//GEN-LAST:event_jMIDelCatActionPerformed

    private void jMIInsertCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIInsertCatActionPerformed
        StructureNode node = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (node == null) {
            return;
        }
        SingleLineEditor SLE = new SingleLineEditor(RSC, "Please enter a name for the category", "Category",false);
        SLE.setVisible(true);
        if (!SLE.cancel) {
            Library library = RSC.getCurrentlySelectedLibrary();
            final HashMap<String,String> data = new HashMap<String,String>();
            final StructureNode child = library.createCategory(SLE.text.trim(),node);
            StructureTreeModel.reload();
            jTStructureTree.scrollPathToVisible(new TreePath(child.getPath().toArray()));
        }
        SLE.dispose();
    }//GEN-LAST:event_jMIInsertCatActionPerformed

    private void jMICheckBibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICheckBibActionPerformed
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Items with corrupt BibTeX","search");
        setThreadMsg("Working...");
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "Checking BibTeX integrity ...", "", 0, RSC.getCurrentlySelectedLibrary().getSize());
        SWBibTeXIntegrity swBibTeXIntegrity =new SWBibTeXIntegrity(celsiusTable,celsiusTable.postID);
        swBibTeXIntegrity.execute();
    }//GEN-LAST:event_jMICheckBibActionPerformed

    private void jMIEmailActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEmailActionPerformed
        int i = jTPTabList.getSelectedIndex();
        String names = "";
        Library Lib = RSC.getCurrentlySelectedLibrary();
        if (i > -1) {
            for (TableRow tableRow : RSC.celsiusTables.get(i).getSelectedRows())
                names += " '" + tableRow.getCompletedDirKey("location") + "'";
        } else {
            return;
        }
        names = names.trim();
        try {
            String cmdln = RSC.configuration.getConfigurationProperty("email");
            cmdln = cmdln.replace("%from%", names);
            ExecutionShell ES = new ExecutionShell(cmdln, 0, true);
            ES.start();
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }//GEN-LAST:event_jMIEmailActionPerformed

    /**
     * Copy the selected items into another folder, appropriately renamed
     */
    private void jMIExportTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIExportTabActionPerformed
        if (!RSC.guiStates.getState("mainFrame","tabAvailable")) {
            return;
        }
        String folder=RSC.selectFolder("Select the target folder for exporting","exportTab");
        if (folder!=null) {
            try {
                if (!(new File(folder)).exists()) {
                    (new File(folder)).mkdir();
                }
                for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
                    Item item=(Item)tableRow;
                    item.loadLevel(3);
                    if (item.linkedAttachments.size()>0) {
                        Attachment attachment=item.linkedAttachments.get(0);
                        String filename = attachment.standardFileName();
                        (new InteractiveFileCopy(this,attachment.getFullPath(), folder + "/" + filename,RSC)).go();
                        /*if (item.getS("filetype").equals("m3u")) {
                            TextFile PL=new TextFile(item.getCompleteDirS("location"));
                            while (PL.ready()) {
                                String fn=PL.getString();
                                (new InteractiveFileCopy(this,(new File(item.getCompleteDirS("location"))).getParent()+"/"+fn, folder + "/" + fn,RSC)).go();
                            }
                            PL.close();
                        }*/
                    }
                }
            } catch (Exception ex) {
                RSC.outEx(ex);
                RSC.showWarning("Error while exporting files:\n" + ex.toString(), "Exception:");
            }
        }
    }//GEN-LAST:event_jMIExportTabActionPerformed

   // +
    private void jMICitesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICitesActionPerformed
        Library Lib = RSC.getCurrentlySelectedLibrary();
        String paper = RSC.getCurrentTable().getSelectedRows().get(0).get("identifier");
        String target=getSelectedCategory().toString();
        if ((paper==null) || (target==null)) {
            RSC.showWarning("No item or category selected!", "Warning:");
            return;
        }
        HashMap<String,String> data = new HashMap<String,String>();
        data.put("paper",paper);
        data.put("target",target);
        updateStatusBar(false);
        //TODO updateRulesByCategory();
    }//GEN-LAST:event_jMICitesActionPerformed

    private void jMIUnregisterDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIUnregisterDocActionPerformed
        StructureNode structureNode = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (structureNode != null) {
            CelsiusTable DT=RSC.getCurrentTable();
            if (DT!=null) {
                Library library = RSC.getCurrentlySelectedLibrary();
                Category category=structureNode.category;
                boolean doit=false;
                int h=0;
                for (TableRow tableRow : DT.getSelectedRows()) {
                    if (!doit) h=RSC.askQuestionABCD("Remove the item \n" + tableRow.toText(false) + "\nfrom the current category?",
                            "Warning","Yes","No","Yes to all","Cancel");
                    if (h==3) break;
                    if (h==2) doit=true;
                    if (doit || (h == 0)) {
                        try {
                            library.unRegisterItem((Item)tableRow,Integer.valueOf(category.id));
                        } catch (Exception e) {
                            RSC.outEx(e);
                        }
                        goToCategory(category);
                        updateStatusBar(false);
                    }
                }
            }
        }
    }//GEN-LAST:event_jMIUnregisterDocActionPerformed

    private void jMIDeleteFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDeleteFileActionPerformed
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            CelsiusTable DT = RSC.celsiusTables.get(jTPTabList.getSelectedIndex());
            Library CSL = DT.library;
            boolean doit=false;
            int h=0;
            for (TableRow tableRow : DT.getSelectedRows()) {
                if (!doit) h=RSC.askQuestionABCD("Delete the item \n" + tableRow.toText(false) + "\nand all related information?",
                                "Warning","Yes","No","Yes to all","Cancel");
                if (h==3) break;
                if (h==2) doit=true;
                if (doit || (h == 0)) {
                    DT.removeRow(tableRow);
                    tableRow.destroy(true);
                }
            }
            updateStatusBar(true);
            guiInfoPanel.updateGUI();
        }
    }//GEN-LAST:event_jMIDeleteFileActionPerformed

    private void jMIRemoveFromTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRemoveFromTabActionPerformed
        CelsiusTable celsiusTable = RSC.getCurrentTable();
        if (celsiusTable!=null) {
            for (TableRow tableRow : celsiusTable.getSelectedRows())
                celsiusTable.removeRow(tableRow);
        }
        guiInfoPanel.updateGUI();
    }//GEN-LAST:event_jMIRemoveFromTabActionPerformed

    private void jMIViewPlainActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIViewPlainActionPerformed
        TableRow tableRow=RSC.getCurrentTable().getSelectedRows().get(0);
        if (tableRow==null) return;
        viewPlainText((Item)tableRow);
    }//GEN-LAST:event_jMIViewPlainActionPerformed

    private void jMIFullBibToFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIFullBibToFileActionPerformed
        Library library=RSC.getCurrentlySelectedLibrary();
        String filename=RSC.selectFile("Indicate the target bib file", "export", "_ALL", "All files");
        boolean completed=false;
        if (filename!=null) {
            try {
                TextFile bibfile = new TextFile(filename, false);
                ResultSet rs=library.executeResEX("SELECT bibtex from items where bibtex IS NOT NULL order by \"citation-tag\";");
                while (rs.next()) {
                    bibfile.putString("");
                    bibfile.putString(rs.getString(1));
                    bibfile.putString("");
                }
                bibfile.close();
                completed=true;
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            if (completed) {
                RSC.showInformation("Task completed:", "BibTeX file exported");
            }
        }
    }//GEN-LAST:event_jMIFullBibToFileActionPerformed

    private void jMIBibClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIBibClipboardActionPerformed
        Clipboard Clp = getToolkit().getSystemClipboard();
        CelsiusTable celsiusTable=RSC.getCurrentTable();
        if (celsiusTable!=null) {
            StringBuilder ids=new StringBuilder();
            for (TableRow tableRow : celsiusTable.getSelectedRows())
                ids.append(","+tableRow.id);
            StringBuilder bibtex = new StringBuilder();
            try {
                ResultSet rs=RSC.getCurrentlySelectedLibrary().executeResEX("SELECT bibtex FROM items WHERE id in ("+ids.substring(1)+") AND bibtex IS NOT NULL ORDER BY \"citation-tag\";");
                while (rs.next()) {
                    bibtex.append('\n');
                    bibtex.append(rs.getString(1));
                    bibtex.append('\n');
                }
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            StringSelection cont = new StringSelection(bibtex.toString());
            Clp.setContents(cont, this);
        }
    }//GEN-LAST:event_jMIBibClipboardActionPerformed

    private void jMICloseTab2TabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICloseTab2TabActionPerformed
        RSC.getCurrentTable().close();
}//GEN-LAST:event_jMICloseTab2TabActionPerformed

    private void jMIAddTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAddTabActionPerformed
        createNewEmptyTab();
    }//GEN-LAST:event_jMIAddTabActionPerformed

    private void jMIUpdateActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIUpdateActionPerformed
        RSC.configuration.viewHTML(RSC.celsiushome);
    }//GEN-LAST:event_jMIUpdateActionPerformed

    /**
     * View a given hyperlink (usually from the document pane)
     * if http://$$view is given, then the viewer for the current document is started
     */
    private void jTPTabListStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTPTabListStateChanged
        if (buildingNewTab) return;
        CelsiusTable celsiusTable=RSC.getCurrentTable();
        if (celsiusTable == null) {
            guiPluginPanel.adjustPluginList();
            guiInfoPanel.updateHTMLview();
        } else {
            switchToLibrary(celsiusTable.library);
            guiPluginPanel.adjustPluginList();
            guiInfoPanel.updateGUI();
        }
        RSC.guiStates.adjustState("mainFrame","tabAvailable", jTPTabList.getSelectedIndex()!=-1);
    }//GEN-LAST:event_jTPTabListStateChanged

    private void jTStructureTreeValueChanged(javax.swing.event.TreeSelectionEvent evt) {//GEN-FIRST:event_jTStructureTreeValueChanged
        if (jTStructureTree.getSelectionModel().isSelectionEmpty()) {
            RSC.guiStates.adjustState("mainFrame","categorySelected", false);
            return;
        }
        StructureNode TN = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        RSC.guiStates.adjustState("mainFrame","categorySelected", true);
        RSC.out("Selected category: "+TN.category.label);
        goToCategory(TN.category);
    }//GEN-LAST:event_jTStructureTreeValueChanged
    // Search stopped by clicking on Button "Stop""    // Search started by clicking on Button "Start"    // Menu: View Selected
    private void jMIViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIViewActionPerformed
        RSC.viewCurrentlySelectedObject();
    }//GEN-LAST:event_jMIViewActionPerformed

    private void jMINewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMINewActionPerformed
        createNewEmptyTab();
    }//GEN-LAST:event_jMINewActionPerformed

    private void jCBLibrariesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBLibrariesActionPerformed
        switchToLibrary(null);
        guiInfoPanel.updateGUI();
    }//GEN-LAST:event_jCBLibrariesActionPerformed
    // Save Current Library
    private void jMISaveLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMISaveLibActionPerformed
        final MainFrame MF=this;
        (new Thread("SavingCurrentLib") {

            @Override
            public void run() {
                setThreadMsg("Saving library...");
                MF.jPBSearch.setIndeterminate(true);
                try {
                    RSC.getCurrentlySelectedLibrary().writeBack();
                } catch (Exception e) {
                    RSC.outEx(e);
                    RSC.showWarning("Saving current library failed:\n" + e.toString(), "Warning:");
                }
                setThreadMsg("Ready.");
                MF.jPBSearch.setIndeterminate(false);
                updateStatusBar(false);
            }
        }).start();
    }//GEN-LAST:event_jMISaveLibActionPerformed
    // Close Current Library
    private void jMICloseLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICloseLibActionPerformed
        closeCurrentLibrary(true);
    }//GEN-LAST:event_jMICloseLibActionPerformed
    // Load Current Library
    private void jMILoadLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMILoadLibActionPerformed
        String filename=RSC.selectFolder("Select the folder of the library you wish to open.", "loadlibraries");
        if (filename!=null) {
            // check the folder is valid
            boolean libraryFolder=(new File(filename+ToolBox.filesep+"CelsiusLibrary.sql")).exists();
            if (libraryFolder) {
                final MainFrame MF = this;
                (new Thread("LoadingLib") {

                    @Override
                    public void run() {
                        setThreadMsg("Opening library...");
                        MF.jPBSearch.setIndeterminate(true);
                        try {
                            RSC.openLibrary(filename,true);
                        } catch (Exception e) {
                            RSC.showWarning("Loading library failed:\n" + e.toString(), "Warning:");
                        }
                        setThreadMsg("Ready.");
                        MF.jPBSearch.setIndeterminate(false);
                        updateStatusBar(false);
                    }
                }).start();
            }
        }
    }//GEN-LAST:event_jMILoadLibActionPerformed
    // Create New Library
    private void jMICreateLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICreateLibActionPerformed
        CreateNewLibrary DCNL=(new CreateNewLibrary(this,RSC));
        DCNL.setVisible(true);
        if (DCNL.Lib==null) return;
        final Library Lib=DCNL.Lib;

        JMenuItem jmi=new JMenuItem(Lib.name);
        jmi.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemToLibrary(Lib);
            }
        });
        jMCopyToDiff.add(jmi);
        jmi=new JMenuItem(Lib.name);
        jmi.addActionListener(new java.awt.event.ActionListener() {

            @Override
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                copyItemToLibrary(Lib);
            }
        });
        jMCopyToDiff1.add(jmi);
        
        DefaultComboBoxModel DCBM=(DefaultComboBoxModel)jCBLibraries.getModel();
        DCBM.addElement(Lib.name);
        DCBM.setSelectedItem(Lib.name);
    }//GEN-LAST:event_jMICreateLibActionPerformed

    private void jMICitationTagClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICitationTagClipboardActionPerformed
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            Clipboard Clp = getToolkit().getSystemClipboard();
            String ref = "";
            Library Lib = RSC.getCurrentlySelectedLibrary();
            for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows())
                ref += "," + tableRow.get("citation-tag");
            ref = ref.substring(1);
            StringSelection cont = new StringSelection(ref);
            Clp.setContents(cont, this);
        }
    }//GEN-LAST:event_jMICitationTagClipboardActionPerformed
    // Window closing with x
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        closeCelsius();
    }//GEN-LAST:event_formWindowClosing
    // Window closing from Menu
    private void jMIQuitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIQuitActionPerformed
        closeCelsius();
    }//GEN-LAST:event_jMIQuitActionPerformed

    private void JMIManualActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_JMIManualActionPerformed
        RSC.configuration.view("pdf", "manual.pdf");
    }//GEN-LAST:event_JMIManualActionPerformed

    private void jMIAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAboutActionPerformed
        new SplashScreen(RSC.VersionNumber, false,RSC);
    }//GEN-LAST:event_jMIAboutActionPerformed

private void jMIDeleteLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDeleteLibActionPerformed
    Library Lib=RSC.getCurrentlySelectedLibrary();
    if (RSC.askQuestionYN("Do you really want to delete the Library "+Lib.name+"?\nWarning: all files in the library's directory will be erased!", "Confirm:")==0) {
        closeCurrentLibrary(false);
        Lib.deleteLibrary();
    }
}//GEN-LAST:event_jMIDeleteLibActionPerformed

private void jTPTabListComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jTPTabListComponentResized
    for(CelsiusTable DT : RSC.celsiusTables) {
        DT.resizeTable(false);
    }
}//GEN-LAST:event_jTPTabListComponentResized

/*
    Create category from current table
*/
private void jMITab2Cat2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMITab2Cat2ActionPerformed
    Library Lib = RSC.getCurrentlySelectedLibrary();
    StructureNode node = Lib.structureTreeRoot;
    String cat = RSC.getCurrentTable().title;
    //RSC.getCurrentTable().setCategoryByID(node.category.id);
    StructureNode child = Lib.createCategory(cat,node);
    // ###
    String ids=RSC.getTableRowIDs(RSC.getCurrentTable().celsiusTableModel.tableRows);
    for (TableRow tableRow : RSC.getCurrentTable().celsiusTableModel.tableRows) {
        try {
            Lib.registerItem((Item)tableRow, node, 0);
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }
    StructureTreeModel.reload();
    jTStructureTree.scrollPathToVisible(new TreePath(child.getPath().toArray()));
    updateStatusBar(false);
}//GEN-LAST:event_jMITab2Cat2ActionPerformed

private void jMIEditLibTemplatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditLibTemplatesActionPerformed
    (new celsius.gui.EditLibraryTemplates(this,RSC)).setVisible(true);
}//GEN-LAST:event_jMIEditLibTemplatesActionPerformed

private void jMIAddToLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAddToLibActionPerformed
        AddItems DA=new AddItems(RSC);
        DA.setVisible(true);
        if (DA.addedItems.size()>0) {
            CelsiusTable celsiusTable=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_WHEN_ADDED, "Last added","search");
            celsiusTable.addRows(DA.addedItems);
            celsiusTable.resizeTable(true);
            guiInfoPanel.updateGUI();
            updateStatusBar(true);
        }
}//GEN-LAST:event_jMIAddToLibActionPerformed

private void jLSearchKeysMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLSearchKeysMouseClicked
    if (!(evt.getButton() == MouseEvent.BUTTON1))
        return;
    searchKeysUpdate();
}//GEN-LAST:event_jLSearchKeysMouseClicked

private void jLSearchKeysValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLSearchKeysValueChanged
    if (evt.getValueIsAdjusting()) return;
    searchKeysUpdate();
}//GEN-LAST:event_jLSearchKeysValueChanged

private void jMIDeepSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDeepSearchActionPerformed
        if (deepSearch==null) {
            deepSearch=new DeepSearch(RSC);
            RSC.adjustComponents(deepSearch.getComponents());
        }
        deepSearch.setLib(RSC.getCurrentlySelectedLibrary());
        deepSearch.setVisible(true);
}//GEN-LAST:event_jMIDeepSearchActionPerformed

private void jMICreateCombiner1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICreateCombiner1ActionPerformed
    // TODO
    /*CreateCombiner CC=new CreateCombiner(this,RSC.getCurrentItemTable().getSelectedRows());
    CC.setVisible(true);
    if (CC.addedCombiner != null) {
        CelsiusTable CDT=RSC.makeNewTabAvailable(8, "Last added","search");
        CDT.addRow(CC.addedCombiner);
        CDT.resizeTable(true);
        guiInfoPanel.updateHTMLview();
        guiInfoPanel.updateCurrentItemInGUI();
        updateStatusBar(true);
    }*/
}//GEN-LAST:event_jMICreateCombiner1ActionPerformed

private void jRBExpFileItemStateChanged(java.awt.event.ItemEvent evt) {//GEN-FIRST:event_jRBExpFileItemStateChanged
    boolean b=jRBExpFile.isSelected();
    jTFExpFile.setEnabled(b);
    jBtnSelExpFile.setEnabled(b);
}//GEN-LAST:event_jRBExpFileItemStateChanged

private void jBtnSelExpFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnSelExpFileActionPerformed
        String filename=RSC.selectFile("Indicate the target file", "export", "_ALL", "All files");
        if (filename!=null) {
            jTFExpFile.setText(filename);
        }
}//GEN-LAST:event_jBtnSelExpFileActionPerformed

private void jBtnExpSelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnExpSelActionPerformed
        int plugin=jCBExpFilter.getSelectedIndex();
        if (plugin == -1) {
            return;
        }
        CelsiusTable celsiusTable=RSC.getCurrentTable();
        if (celsiusTable == null) return;
        ArrayList<TableRow> tableRows=celsiusTable.getSelectedRows();
        exportBibInfo(celsiusTable.library,tableRows);
}//GEN-LAST:event_jBtnExpSelActionPerformed

private void jBtnExpAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnExpAllActionPerformed
        int plugin=jCBExpFilter.getSelectedIndex();
        if (plugin == -1) {
            return;
        }
        Library library=RSC.getCurrentlySelectedLibrary();
        setThreadMsg("Exporting...");
        exportBibInfo(library,null);
}//GEN-LAST:event_jBtnExpAllActionPerformed

private void jMIRemoveHalfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRemoveHalfActionPerformed
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            CelsiusTable IT = RSC.celsiusTables.get(jTPTabList.getSelectedIndex());
            boolean doit=false;
            int h=0;
            for (TableRow tableRow : IT.getSelectedRows()) {
                Item item=(Item)tableRow;
                if (!doit) h=RSC.askQuestionABCD("Delete the document \n" + item.toText(false) + ",\nkeeping the associated file?",
                                "Warning","Yes","No","Yes to all","Cancel");
                if (h==3) break;
                if (h==2) doit=true;
                if (doit || (h == 0)) {
                    IT.removeRow(item);
                    item.destroy(false);
                }
            }
            updateStatusBar(true);
            guiInfoPanel.updateGUI();
        }
}//GEN-LAST:event_jMIRemoveHalfActionPerformed

private void jTFMainSearchKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTFMainSearchKeyTyped
    /*if (evt.getKeyChar() == KeyEvent.VK_ENTER) {
        if (searchstate == 1) {
            if ((isTabAvailable) && (RSC.getCurrentItemTable().jtable.getModel().getRowCount() > 0)) {
                RSC.getCurrentItemTable().jtable.clearSelection();
                RSC.getCurrentItemTable().jtable.setRowSelectionInterval(0, 0);
                RSC.viewCurrentlySelectedItem();
            }
        } else {
            stopSearch();
            int mode=0;
            if (jRBSearchMeta.isSelected()) mode=1;
            if (jRBSearchDeep.isSelected()) mode=2;
            if (jTFMainSearch.getText().equals("")) return;
            if (RSC.getCurrentItemTable()!=null)
                if (!RSC.getCurrentItemTable().tableview) return;
            String srch = jTFMainSearch.getText();
            if ((srch.length() > 0) && (!srch.equals(jTFMainSearch.getDefaultText()))) {
                noDocSelected();
                //System.out.println(String.valueOf(System.currentTimeMillis())+"Request send.");
                startSearch(srch,mode);
            }
            searchstate = 1;
        }
    } else {
        searchstate=0;
    }*/
}//GEN-LAST:event_jTFMainSearchKeyTyped

private void jMCDisplayHiddenStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jMCDisplayHiddenStateChanged
    RSC.displayHidden=jMCDisplayHidden.isSelected();
}//GEN-LAST:event_jMCDisplayHiddenStateChanged

private void jMShowCombinedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMShowCombinedActionPerformed
        showCombined();
}//GEN-LAST:event_jMShowCombinedActionPerformed

private void jMShowLinkedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMShowLinkedActionPerformed
        showLinksOfType("Available Links");
}//GEN-LAST:event_jMShowLinkedActionPerformed

private void jMIExpandActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIExpandActionPerformed
    GUIToolBox.expandAll(jTStructureTree, true);
}//GEN-LAST:event_jMIExpandActionPerformed

private void jMICollapseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICollapseActionPerformed
    GUIToolBox.expandAll(jTStructureTree, false);
}//GEN-LAST:event_jMICollapseActionPerformed

    private void jLWhenAddedValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLWhenAddedValueChanged
        goToHistory(jLWhenAdded.getSelectedIndex());
    }//GEN-LAST:event_jLWhenAddedValueChanged

    private void jCBExpFilterActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBExpFilterActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jCBExpFilterActionPerformed

    private void jMIConvLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConvLibActionPerformed
        String filename=RSC.selectFile("Select the main file of the library you wish to open.", "loadlibraries", "_ALL", "All Files");
        if (filename!=null) {
            // convert Library to SQLite
            Library3.convertLib(this,filename,RSC);
            RSC.showInformation("The library has been converted.", "Action completed.");
        }
    }//GEN-LAST:event_jMIConvLibActionPerformed

    private void jBtnExpTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnExpTabActionPerformed
        int plugin=jCBExpFilter.getSelectedIndex();
        if (plugin == -1) {
            return;
        }
        CelsiusTable DT=RSC.getCurrentTable();
        if (DT == null) return;
        exportBibInfo(DT.library,DT.celsiusTableModel.tableRows);
    }//GEN-LAST:event_jBtnExpTabActionPerformed

    private void jMIMergeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIMergeActionPerformed
        performPeopleMerge();
    }//GEN-LAST:event_jMIMergeActionPerformed

    private void jMIMerge1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIMerge1ActionPerformed
        performPeopleMerge();
    }//GEN-LAST:event_jMIMerge1ActionPerformed

    private void jMIPeopleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIPeopleActionPerformed
        showAssociatedPeople();
    }//GEN-LAST:event_jMIPeopleActionPerformed

    private void jMIPeople2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIPeople2ActionPerformed
        showAssociatedPeople();
    }//GEN-LAST:event_jMIPeople2ActionPerformed

    private void jMIClearLoggingFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIClearLoggingFileActionPerformed
        RSC.resetLogFile();
    }//GEN-LAST:event_jMIClearLoggingFileActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem JMIManual;
    private javax.swing.ButtonGroup bGSearch;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.JButton jBtnExpAll;
    private javax.swing.JButton jBtnExpSel;
    private javax.swing.JButton jBtnExpTab;
    private javax.swing.JButton jBtnSelExpFile;
    public javax.swing.JComboBox jCBExpFilter;
    public javax.swing.JComboBox jCBLibraries;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JList jLSearchKeys;
    public javax.swing.JLabel jLStatusBar;
    private javax.swing.JLabel jLThreadStatus;
    private javax.swing.JList<String> jLWhenAdded;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    public javax.swing.JMenu jMActions;
    private javax.swing.JMenu jMBibTeX;
    private javax.swing.JRadioButtonMenuItem jMCDisplayHidden;
    private javax.swing.JMenu jMCategories;
    public javax.swing.JMenu jMCopyToDiff;
    public javax.swing.JMenu jMCopyToDiff1;
    private javax.swing.JMenu jMFile;
    private javax.swing.JMenu jMHelp;
    private javax.swing.JMenuItem jMIAbout;
    private javax.swing.JMenuItem jMIAddTab;
    private javax.swing.JMenuItem jMIAddToLib;
    public javax.swing.JMenuItem jMIAssociateFile;
    public javax.swing.JMenuItem jMIAssociateFile1;
    private javax.swing.JMenuItem jMIBibClipboard;
    private javax.swing.JMenuItem jMICatDown;
    private javax.swing.JMenuItem jMICatDown1;
    private javax.swing.JMenuItem jMICatSub;
    private javax.swing.JMenuItem jMICatSub1;
    private javax.swing.JMenuItem jMICatSuper;
    private javax.swing.JMenuItem jMICatSuper1;
    private javax.swing.JMenuItem jMICatUp;
    private javax.swing.JMenuItem jMICatUp1;
    private javax.swing.JMenuItem jMICheckBib;
    private javax.swing.JMenuItem jMICitationTagClipboard;
    private javax.swing.JMenuItem jMICites1;
    private javax.swing.JMenuItem jMIClearLoggingFile;
    private javax.swing.JMenuItem jMICloseLib;
    public javax.swing.JMenuItem jMICloseTab;
    public javax.swing.JMenuItem jMICloseTab2;
    private javax.swing.JMenuItem jMICollapse;
    private javax.swing.JMenuItem jMIConfig;
    private javax.swing.JMenuItem jMIConsistencyCheck;
    private javax.swing.JMenuItem jMIConvLib;
    private javax.swing.JMenuItem jMICopyTab;
    private javax.swing.JMenuItem jMICopyTab2;
    private javax.swing.JMenuItem jMICreateCombiner;
    private javax.swing.JMenuItem jMICreateCombiner1;
    private javax.swing.JMenuItem jMICreateLib;
    private javax.swing.JMenuItem jMICreateTxt;
    private javax.swing.JMenuItem jMIDeepSearch;
    private javax.swing.JMenuItem jMIDelCat;
    private javax.swing.JMenuItem jMIDelCat1;
    private javax.swing.JMenuItem jMIDeleteFile;
    private javax.swing.JMenuItem jMIDeleteFile1;
    private javax.swing.JMenuItem jMIDeleteLib;
    public javax.swing.JMenuItem jMIEditDS;
    private javax.swing.JMenuItem jMIEditLib;
    private javax.swing.JMenuItem jMIEditLibTemplates;
    private javax.swing.JMenuItem jMIEmail;
    private javax.swing.JMenuItem jMIEmail1;
    private javax.swing.JMenuItem jMIExpand;
    private javax.swing.JMenuItem jMIExportTab;
    private javax.swing.JMenuItem jMIExportTab1;
    private javax.swing.JMenuItem jMIFullBibToFile;
    private javax.swing.JMenuItem jMIInsertCat;
    private javax.swing.JMenuItem jMIInsertCat1;
    private javax.swing.JMenuItem jMIJoin;
    private javax.swing.JMenuItem jMIJoin1;
    private javax.swing.JMenuItem jMILoadLib;
    private javax.swing.JMenuItem jMIMerge;
    private javax.swing.JMenuItem jMIMerge1;
    private javax.swing.JMenuItem jMINew;
    private javax.swing.JMenuItem jMIOpenNewTab;
    private javax.swing.JMenuItem jMIPeople;
    private javax.swing.JMenuItem jMIPeople2;
    private javax.swing.JMenuItem jMIQuit;
    private javax.swing.JMenuItem jMIReExtract1;
    private javax.swing.JMenuItem jMIRemoveFromTab;
    private javax.swing.JMenuItem jMIRemoveFromTab1;
    private javax.swing.JMenuItem jMIRemoveHalf;
    private javax.swing.JMenuItem jMIRemoveHalf1;
    private javax.swing.JMenuItem jMIRenameCat;
    private javax.swing.JMenuItem jMIRenameCat1;
    private javax.swing.JMenuItem jMISaveLib;
    private javax.swing.JMenuItem jMIShowCitedinFile;
    private javax.swing.JMenuItem jMITab2Cat;
    private javax.swing.JMenuItem jMITab2Cat2;
    private javax.swing.JMenuItem jMIUnregisterDoc;
    private javax.swing.JMenuItem jMIUnregisterDoc1;
    private javax.swing.JMenuItem jMIUpdate;
    private javax.swing.JMenuItem jMIView;
    private javax.swing.JMenuItem jMIView1;
    public javax.swing.JMenuItem jMIViewPlain;
    public javax.swing.JMenuItem jMIViewPlain1;
    public javax.swing.JMenu jMItems;
    private javax.swing.JMenu jMLibraries;
    private javax.swing.JMenu jMPeople;
    public javax.swing.JMenu jMRecent;
    private javax.swing.JMenu jMShow;
    private javax.swing.JMenu jMShow1;
    private javax.swing.JMenuItem jMShowCombined;
    private javax.swing.JMenuItem jMShowCombined1;
    private javax.swing.JMenuItem jMShowLinked;
    private javax.swing.JMenuItem jMShowLinked1;
    private javax.swing.JMenu jMTabs;
    private javax.swing.JMenuBar jMainMenu;
    public javax.swing.JProgressBar jPBSearch;
    private javax.swing.JPopupMenu jPMCategories;
    public javax.swing.JPopupMenu jPMItemList;
    public javax.swing.JPopupMenu jPMItemTable;
    public javax.swing.JPopupMenu jPMPeopleTable;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel21;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel24;
    private javax.swing.JPanel jPanel25;
    private javax.swing.JPanel jPanel26;
    private javax.swing.JPanel jPanel27;
    private javax.swing.JPanel jPanel28;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel30;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JRadioButton jRBExpClip;
    private javax.swing.JRadioButton jRBExpFile;
    private javax.swing.JSplitPane jSPMain;
    private javax.swing.JSplitPane jSPMain3;
    private javax.swing.JScrollBar jScrollBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JSeparator jSeparator15;
    private javax.swing.JSeparator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator20;
    private javax.swing.JSeparator jSeparator22;
    private javax.swing.JPopupMenu.Separator jSeparator23;
    private javax.swing.JSeparator jSeparator24;
    private javax.swing.JSeparator jSeparator26;
    private javax.swing.JSeparator jSeparator28;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator30;
    private javax.swing.JPopupMenu.Separator jSeparator31;
    private javax.swing.JPopupMenu.Separator jSeparator33;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JPopupMenu.Separator jSeparator5;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton jTBAdd;
    private javax.swing.JTextField jTFExpFile;
    public javax.swing.JTabbedPane jTPSearches;
    public javax.swing.JTabbedPane jTPTabList;
    private javax.swing.JTabbedPane jTPTechnical;
    private javax.swing.JTree jTStructureTree;
    // End of variables declaration//GEN-END:variables

    public void reloadPlugins() {
        RSC.plugins.readInAvailablePlugins();
        guiPluginPanel.objectType=-2;
        guiPluginPanel.adjustPluginList();
        DefaultComboBoxModel DCBM=RSC.plugins.getPluginsDCBM("export",RSC.getCurrentlySelectedLibrary());
        guiInfoPanel.jCBBibPlugins.setModel(DCBM);
        jCBExpFilter.setModel(DCBM);
    }
    
    public void showAssociatedPeople() {
        String ids=RSC.getCurrentTable().getSelectedIDsString();
        Library library=RSC.getCurrentlySelectedLibrary();
        CelsiusTable table=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_PERSON_SEARCH, "Associated People", Resources.personTabIcon);
        try {
            ResultSet RS=library.executeResEX("SELECT persons.* FROM item_person_links INNER JOIN persons ON persons.id=item_person_links.person_id WHERE item_id IN ("+ids+");");
            while (RS.next()) {
                table.addRow(new Person(library,RS));
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }
    
    public void performPeopleMerge() {
        String ids=RSC.getCurrentTable().getSelectedIDsString();
        if ((ids.length()>0) && (ids.contains(","))) {
            MergePeople MP=new MergePeople(RSC,ids);
            MP.setVisible(true);
        } else {
            RSC.showWarning("You have to selected more than one person to merge.", "Cancelled:");
        }
    }
    
    public Category getSelectedCategory() {
        StructureNode structureNode = (StructureNode) jTStructureTree.getLastSelectedPathComponent();
        if (structureNode==null) return(null);
        return(structureNode.category);
    }
    
    public void createNewEmptyTab() {
        RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_EMPTY, "New" + Integer.toString(jTPTabList.getTabCount()), "default");
        guiInfoPanel.updateGUI();
    }
    
    @Override
    public void dragEnter(DropTargetDragEvent dtde) {
    }
    
    @Override
    public void dragOver(DropTargetDragEvent dtde) {
    }
    
    @Override
    public void dropActionChanged(DropTargetDragEvent dtde) {
    }
    
    @Override
    public void dragExit(DropTargetEvent dte) {
    }
    
    @Override
    public void drop(DropTargetDropEvent dtde) {
        final Point p = dtde.getLocation();
        if (jTStructureTree.getPathForLocation(p.x,p.y)==null) {
            RSC.showWarning("Could not find category to drop into. Cancelling...", "Warning");
        } else {
            StructureNode structureNode = (StructureNode) ((jTStructureTree.getPathForLocation(p.x,p.y)).getLastPathComponent());
            CelsiusTable DT = RSC.getCurrentTable();
            // TODO DT.setCategoryByID(structureNode.category.id);
            if (DT != null) {
                for (TableRow tableRow : DT.getSelectedRows()) {
                    Item item=(Item)tableRow;
                    try {
                        DT.library.registerItem(item, structureNode, 0);
                        guiInfoPanel.updateGUI();
                        guiInfoPanel.updateHTMLview();
                        updateStatusBar(false);
                    } catch (Exception e) {
                        RSC.outEx(e);
                    }
                }
            }
        }
    }
    
    @Override
    public void lostOwnership(Clipboard clipboard, Transferable contents) {
    }
    
    @Override
    public void treeNodesInserted(TreeModelEvent e) {
    }
    
    @Override
    public void treeNodesRemoved(TreeModelEvent e) {
    }
    
    @Override
    public void treeStructureChanged(TreeModelEvent e) {
    }

    public void treeNodesChanged(TreeModelEvent e) {
    }

    public void keyPressed(DocumentEvent e) {
        if (e.getDocument().equals(jCE1.getDocument())) performCategoriesSearch();
        if (e.getDocument().equals(jCE3.getDocument())) performKeySearch();
    }

    public void insertUpdate(DocumentEvent e) {
        keyPressed(e);
    }

    public void removeUpdate(DocumentEvent e) {
        keyPressed(e);
    }

    public void changedUpdate(DocumentEvent e) {
        keyPressed(e);
    }
    
    @Override
    public void keyTyped(KeyEvent evt) {
    }

    @Override
    public void keyPressed(KeyEvent ke) {
        
    }

    @Override
    public void keyReleased(KeyEvent ke) {
        
    }

    public Insets standardBorder() {
        return(new Insets(RSC.guiScale(25),RSC.guiScale(25),RSC.guiScale(25),RSC.guiScale(25)));
    }

    public void searchKeysUpdate() {
        KeywordListModel KLM=(KeywordListModel) jLSearchKeys.getModel();
        if (!KLM.containsData) return;
        int i = jLSearchKeys.getSelectedIndex();
        if (i == -1) {
            return;
        }
        String key = KLM.labels.get(i);
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEM_WITH_KEYWORD, key,Resources.keyTabIcon);
        RSC.getCurrentlySelectedLibrary().showItemsWithKeyword(KLM.ids.get(i), celsiusTable);
        celsiusTable.properties.put("keyword", key);
        guiInfoPanel.updateGUI();
    }

    public void performCategoriesSearch() {
        String srch = jCE1.getText();
        if (srch.length() > 0) {
            StructureNode structureNode = RSC.getCurrentlySelectedLibrary().structureTreeRoot.nextOccurence(srch.toLowerCase());
            if (!(structureNode == null)) {
                jTStructureTree.setSelectionPath(new TreePath(structureNode.getPath().toArray()));
                jTStructureTree.scrollPathToVisible(new TreePath(structureNode.getPath().toArray()));
                goToCategory(structureNode.category);
            }
        }
    }
    
    public void performKeySearch() {
        String search = jCE3.getText().toLowerCase();
        KeywordListModel KLM = new KeywordListModel();
        if (search.length()>1) {
            try {
                ResultSet rs=RSC.getCurrentlySelectedLibrary().executeResEX("SELECT id, label, remarks FROM keywords WHERE label LIKE ? COLLATE NOCASE ORDER BY label ASC;", "%"+search+"%");
                KLM=new KeywordListModel(rs);
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
        }
        jLSearchKeys.setModel(KLM);
    }
    
    public boolean joinItems(Library Lib,String id1,String id2) {
        Item item1=new Item(Lib,id1);
        Item item2=new Item(Lib,id2);
        RSC.out("MAIN>Combining items:  + " + item1.toText(false) + "\n and \n" + item2.toText(false));
        // Move everything into doc1
        for (String tag : item2.getFields()) {
            if ((tag.length()!=0) && !tag.equals("location") && !tag.equals("plaintxt") && !tag.equals("id") && 
                    !tag.equals("pages") && !tag.equals("filetype") && !tag.startsWith("altversion") &&
                    !tag.equals("registered") && !tag.equals("autoregistered")) {
                if (item1.get(tag)==null) {
                    item1.put(tag, item2.get(tag));
                    if (item1.getS(tag).indexOf("::")==2) {
                        String f1 = item2.getCompletedDirKey(tag);
                        String f2 = "AI::" + FileTools.getFileType(f1);
                        item1.put(tag, f2);
                        FileTools.deleteIfExists(item1.library.completeDir(f2));
                        try {
                            FileTools.moveFile(f1, item1.library.completeDir(f2));
                        } catch (IOException ex) {
                            RSC.outEx(ex);
                        }
                    }
                } else if (!item1.get(tag).equals(item2.get(tag))) {
                    int i = RSC.askQuestionABC("Which information should be kept for the tag:\n" + tag + "?\nA: " + item1.get(tag) + "\nB: " + item2.get(tag), "Please decide:", "A", "B", "Cancel");
                    if (i == 2) {
                        return(false);
                    }
                    if (i == 1) {
                        if (item2.getS(tag).indexOf("::")==2) {
                            String f1 = item2.getCompletedDirKey(tag);
                            String f2 = "AI::"+ FileTools.getFileType(f1);
                            item1.put(tag, f2);
                            FileTools.deleteIfExists(item1.library.completeDir(f2));
                            try {
                                FileTools.moveFile(f1, item1.library.completeDir(f2));
                            } catch (IOException ex) {
                                RSC.outEx(ex);
                            }
                        }
                        item1.put(tag, item2.get(tag));
                    }
                }
            }
        }
        String reg=item2.getS("registered");
        if (reg.length()>0) {
            reg=item1.getS("registered")+"|"+reg;
            if (reg.startsWith("|")) reg=reg.substring(1);
            item1.put("registered", Parser.replace(reg, "||", "|"));
        }
        reg=item2.getS("autoregistered");
        if (reg.length()>0) {
            reg=item1.getS("autoregistered")+"|"+reg;
            if (reg.startsWith("|")) reg=reg.substring(1);
            item1.put("autoregistered", Parser.replace(reg, "||", "|"));
        }
        /*try {
            if (item2.get("location")!=null) {
                // Associate document 2 with document 1
                
                String avn=item1.getFreeAltVerNo();
                String filename=Parser.CutProhibitedChars2(item1.get("title")+" ("+toolbox.shortenNames(item1.get("authors"))+")");
                item1.guaranteeStandardFolder();
                filename=item1.getStandardFolder()+toolbox.filesep+filename+"."+avn+"."+item2.get("filetype");
                item1.put("altversion-location-"+avn,filename);
                TextFile.moveFile(item2.getCompleteDirS("location"), item1.getCompleteDirS("altversion-location-"+avn));
                if (item2.get("plaintxt")!=null) {
                    String txttarget="AI::"+avn+".txt.gz";
                    item1.put("altversion-plaintxt-"+avn,txttarget);
                    TextFile.moveFile(item2.getCompleteDirS("plaintxt"), item1.getCompleteDirS("altversion-plaintxt-"+avn));
                }
                item1.put("altversion-label-"+avn,"Alt version "+avn);
                item1.put("altversion-filetype-"+avn,item2.get("filetype"));
                item1.put("altversion-pages-"+avn,item2.get("pages"));
            }
            for (String t : item2.getKeys()) {
                // Associate document 2 altversions with document 1
                if (t.startsWith("altversion-location-")) {
                    
                    String vn=Parser.CutFromLast(t, "-");
                    String avn=item1.getFreeAltVerNo();
                    String filename=Parser.CutProhibitedChars2(item1.get("title")+" ("+toolbox.shortenNames(item1.get("authors"))+")");
                    item1.guaranteeStandardFolder();
                    filename=item1.getStandardFolder()+toolbox.filesep+filename+"."+avn+"."+item2.get("altversion-filetype-"+vn);
                    item1.put("altversion-location-"+avn,filename);
                    TextFile.moveFile(item2.getCompleteDirS("altversion-location-"+vn), item1.getCompleteDirS("altversion-location-"+avn));
                    if (item2.get("altversion-plaintxt-"+vn)!=null) {
                        String txttarget="AI::"+avn+".txt.gz";
                        item1.put("altversion-plaintxt-"+avn,txttarget);
                        TextFile.moveFile(item2.getCompleteDirS("altversion-plaintxt-"+vn), item1.getCompleteDirS("altversion-plaintxt-"+avn));
                    }
                    item1.put("altversion-label-"+avn,item2.get("altversion-label-"+vn));
                    item1.put("altversion-filetype-"+avn,item2.get("altversion-filetype-"+vn));
                    item1.put("altversion-pages-"+avn,item2.get("altversion-pages-"+vn));
                }
            }
        } catch (IOException ex) {
            RSC.Msg1.printStackTrace(ex);
            return(false);
        }*/ //TODO
        item1.save();
        if (item1.error==6) RSC.showWarning("Error writing back information", "Warning");
        item2.put("location", null);
        // TODO item2.library.deleteItem(item2.id);
        return(true);
    }
    
    public boolean closeCurrentLibrary(boolean rememberLib) {
        if (RSC.currentLib==-1) return(false);
        final Library library=RSC.getCurrentlySelectedLibrary();
        int CSL=RSC.getCurrentlySelectedLibNo();
        // Remove all RSC.DocumentTables corresponding to library library
        for (int i=RSC.celsiusTables.size()-1;i>-1;i--) {
            if (library==RSC.celsiusTables.get(i).library)
                RSC.celsiusTables.get(i).close();
        }
        DefaultComboBoxModel DCBM=(DefaultComboBoxModel)jCBLibraries.getModel();

        jMCopyToDiff.remove(CSL);
        jMCopyToDiff1.remove(CSL);
        DCBM.removeElementAt(CSL);
        RSC.libraries.remove(CSL);
        // Remember Library
        if (rememberLib) {
            RSC.configuration.libraryClosed(library);
        }

        library.close();
        switchToLibrary(null);
        return(true);
    }
    
    public void goToPeople(String name, String personIDs) {
        if ((personIDs==null) || (personIDs.isBlank())) return;
        if (personIDs.indexOf(',')==-1) {
            goToPerson(personIDs);
        } else {
            RSC.out("Listing people: " + personIDs);
            Library library = RSC.getCurrentlySelectedLibrary();
            CelsiusTable IT = RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEMS_OF_PERSONS, name, Resources.personTabIcon);
            RSC.getCurrentlySelectedLibrary().showItemsWithPersonIDs(personIDs, IT);
        }
    }

    public void goToPerson(String personID) {
        Library library=RSC.getCurrentlySelectedLibrary();
        Person person=new Person(library,personID);
        person.loadCollaborators();
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEMS_OF_PERSON, person.get("last_name"),Resources.personTabIcon);
        RSC.getCurrentlySelectedLibrary().showItemsWithPersonIDs(personID, celsiusTable);
        guiInfoPanel.updateGUI();
    }
    
    /**
     * Update Table according to selected adding interval
     */
    public void goToHistory(int selIndex) {
        Library library=RSC.getCurrentlySelectedLibrary();
        String title = "Added "+RSC.HistoryFields[selIndex];
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEM_WHEN_ADDED, title, Resources.historyTabIcon);
        celsiusTable.resizeTable(true);
        library.showItemsAddedAt(selIndex, celsiusTable);
        celsiusTable.properties.put("when", title);
        guiInfoPanel.updateGUI();
    }
    
    /**
     * Update Table according to selected category
     */
    public void goToCategory(Category category) {
        if (category==null) {
            guiInfoPanel.jTARemarks.setText("No remarks found.");
            return;
        }
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEMS_IN_CATEGORY, category.label, "folder_table");
        Library library=RSC.getCurrentlySelectedLibrary();
        try {
            library.showItemsInCategory(category, celsiusTable);
        } catch (Exception e) {
            RSC.outEx(e);
        }
        celsiusTable.properties.put("category", category.label);
        guiInfoPanel.updateGUI();
    }
    
    /**
     * Updates the statusbar, argument indicates, whether or not the document
     * content should be analyzed.
     */
    public void updateStatusBar(final boolean pagenumber) {
        if (RSC.currentLib==-1) {
            jLStatusBar.setText("No Library loaded.");
            return;
        }
        jLStatusBar.setText(RSC.getCurrentlySelectedLibrary().Status(pagenumber));
    }

    public void setTempStatus(String status) {
        if (eastClearer != null) {
            eastClearer.cancel(false);
            eastClearer = null;
        }
        eastClearer = RSC.executorService.schedule(new Thread() {
            @Override
            public void run() {
                setThreadMsg("");
            }
        }, 2, TimeUnit.SECONDS);
        setThreadMsg(status);
    }

    /**
     * Shows the links of given type. If type is null, then it shows all links
     * @param type
     */
    public void showCombined() {
        // TODO
        /*
        Item item = RSC.getCurrentlySelectedItem();
        String name = item.toText();
        CelsiusTable IT = new CelsiusTable(this, item.library, name, 7);
        RSC.itemTables.add(IT);
        jMICloseTab2.setEnabled(true);
        jMICloseTab.setEnabled(true);
        final JScrollPane scrollpane = new JScrollPane(IT.jtable);
        if (name == null) {
            name = "Links";
        }
        jTPTabList.add(scrollpane);
        jTPTabList.setTabComponentAt(jTPTabList.getTabCount() - 1, new TabLabel(name, "folder_link", RSC, IT, true));
        jTPTabList.setSelectedComponent(scrollpane);
        RSC.setCurrentItemTable(name, "folder_link");
        IT.setType(7);
        IT.lastHTMLview = guiInfoPanel.displayString(7, null);
        IT.creationHTMLview = IT.lastHTMLview;
        int pages = 0;
        double duration = 0;
        int items = 0;
        IT.setLibrary(item.library);
        IT.addRows(item.getCombined());
        //IT.mproperties.put("linktype", name);
        IT.jtable.setVisible(true);
        IT.library.autoSortColumn(IT);
        IT.resizeTable(true);*/
    }

    /**
     * Shows the links of given type. If type is null, then it shows all links
     * @param type
     */
    public void showLinksOfType(String type) {
        // TODO
        /*Item item = RSC.getCurrentlySelectedItem();
        ArrayList<Item> AL = item.getLinksOfType(type);
        String name = type;
        if (name == null) {
            name = "Links";
        }
        Library library = RSC.getCurrentlySelectedLibrary();
        CelsiusTable IT = RSC.makeNewTabAvailable(7, name, "folder_link");
        jMICloseTab2.setEnabled(true);
        jMICloseTab.setEnabled(true);
        int pages = 0;
        double duration = 0;
        int items = 0;
        //System.out.println(Lib.Links.keySet().size());
        IT.addRows(AL);
        guiInfoPanel.updateHTMLview();
        IT.jtable.setVisible(true);
        IT.library.autoSortColumn(IT);
        IT.resizeTable(true);*/
    }

    public boolean dispatchKeyEvent(KeyEvent e) {
        if (e.getKeyCode()==lastkeycode) return(false);
        Class cls=e.getSource().getClass();
        if (cls.getName().startsWith("celsius.Dialogs.")) {
            System.out.println("AA");
            return(false);
        }
        if (e.getSource().getClass().getCanonicalName()==null) return(false);
        if (e.getSource().getClass().getCanonicalName().equals("celsius.SplashScreen")) return(false);
        if (e.getSource().getClass().getCanonicalName().equals("javax.swing.JDialog")) return(false);
        if (e.getSource().getClass().getCanonicalName().equals("celsius.gui.MainFrame")) return(false);
        if (e.getSource().getClass().getCanonicalName().equals("celsius.gui.SingleLineEditor")) return(false);
        if (e.getSource().getClass().getCanonicalName().equals("celsius.gui.PeopleEditor")) return(false);
        if (getRootPane()!=((JComponent)e.getSource()).getRootPane()) return(false);
        lastkeycode=e.getKeyCode();
        // ctrl+f : look for
        if (e.isControlDown() && (e.getKeyCode()==70)) {
            guiSearchPanel.focus(0);
            e.consume();
            return(true);
        }
        // ctrl+t : switch thumbnailview
        if (e.isControlDown() && (e.getKeyCode()==84)) {
            if (RSC.getCurrentTable()!=null)
                RSC.getCurrentTable().switchView();
            e.consume();
            return(true);
        }
        // ctrl+p : people search
        if (e.isControlDown() && (e.getKeyCode()==80)) {
            guiSearchPanel.focus(2);
            e.consume();
            return(true);
        }
        return(false);
    }

    public void viewPlainText(Item item) {
        String fn = item.getCompletedDirKey("plaintxt");
        if (fn == null) {
            return;
        }
        try {
            if (new File(fn).exists()) {
                FileTools.copyFile(fn, item.library.baseFolder + "/viewer.tmp.txt.gz");
                TextFile.GUnZip(item.library.baseFolder + "/viewer.tmp.txt.gz");
                (new ViewerText(RSC, item.library.baseFolder + "/viewer.tmp.txt", "Plain text for document: " + item.get("title") + " by " + item.getNames("authors",3))).setVisible(true);
                //RSC.Configuration.viewHTML(Lib.basedir + "/viewer.tmp.txt");
            } else {
                RSC.showWarning("The associated plain text file:\n" + fn + "\ncould not be found.", "Error:");
            }
        } catch (Exception ex) {
            RSC.showWarning("Error while viewing plain text:\n" + ex.toString(), "Exception:");
            RSC.outEx(ex);
        }
    }
    
    public void exportBibInfo(Library library, ArrayList<TableRow> tableRows) {
        setThreadMsg("Exporting...");
        int size=library.numberOfItems;
        if (tableRows!=null) size=tableRows.size();
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "Exporting selected items ...", "", 0, size);
        String n=(String)jCBExpFilter.getSelectedItem();
        SWApplyPlugin swAP = (new SWApplyPlugin(library, RSC, progressMonitor, RSC.plugins.get(n), RSC.plugins.parameters.get(n), tableRows));
        swAP.swFinalizer=new SWFinalizer() {
                public void finalize(HashMap<String,String> communication, StringBuffer buf) {
                if (jRBExpClip.isSelected()) {
                    Clipboard Clp = RSC.MF.getToolkit().getSystemClipboard();
                    StringSelection cont = new StringSelection(buf.toString());
                    Clp.setContents(cont, RSC.MF);
                } else {
                    try {
                        TextFile f1 = new TextFile(jTFExpFile.getText(), false);
                        f1.putString(buf.toString());
                        f1.close(); 
                    } catch (Exception e) {
                        RSC.outEx(e);
                    }
                }
            }
        };
        RSC.TPE.execute(swAP);        
    }
    
    public void associateFileToCurrentItem() {
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            Library library = RSC.getCurrentlySelectedLibrary();
            Item item=(Item)RSC.getCurrentTable().getSelectedRows().get(0);
            String filename = RSC.selectFile("Indicate the file to be associated with the selected record", "associate", "_ALL", "All files");
            if (filename != null) {
                String name = null;
                if (item.linkedAttachments.size() == 0) {
                    name = "";
                } else {
                    final SingleLineEditor DSLE = new SingleLineEditor(RSC, "Please enter a description for the associated file", "", true);
                    DSLE.setVisible(true);
                    if (!DSLE.cancel) {
                        name = DSLE.text.trim();
                    }
                    DSLE.dispose();
                }
                if (name != null) {
                    if (name.length() == 0) {
                        name = "Main file";
                    }
                    try {
                        item.associateWithFile(filename, name);
                        library.itemChanged(item.id);
                        RSC.out("LIBFA>Added file " + filename + " to record with ID: " + item.get("id"));
                    } catch (IOException ex) {
                        RSC.out("LIBFA>Failed::Adding file " + filename + " to record with ID: " + item.get("id"));
                        RSC.outEx(ex);
                    }
                }
            }
        }
        updateStatusBar(true);
        guiInfoPanel.updateHTMLview();
        guiInfoPanel.updateGUI();        
    }

    @Override
    public void genericEventOccured(GenericCelsiusEvent e) {
        if (e.source==null) {
            //
        }
    }
    
}

