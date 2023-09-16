/*

 Celsius MainFrame Class - Atlantis Software 

*/

package celsius.gui;

import atlantis.gui.MultiLineEditor;
import celsius.components.infopanel.InformationPanel;
import celsius.components.tableTabs.CelsiusTable;
import celsius.components.library.EditLibrary;
import celsius.components.library.CreateNewLibrary;
import celsius.components.bibliography.ExportBibliography;
import celsius.components.search.SearchPanel;
import celsius.components.search.DeepSearch;
import celsius.components.addItems.AddItems;
import atlantis.tools.ExecutionShell;
import celsius.components.plugins.PluginPanel;
import celsius.components.categories.CategoryTreePanel;
import atlantis.tools.FileTools;
import atlantis.gui.HasManagedStates;
import atlantis.tools.TextFile;
import celsius.components.integrity.SWBibTeXIntegrity;
import celsius.components.bibliography.SWShowCited;
import experimental.AddTransferHandler;
import celsius.components.categories.StructureNode;
import celsius3.Library3;
import celsius.components.library.Library;
import celsius.Resources;
import celsius.data.Item;
import celsius.SplashScreen;
import celsius.components.integrity.SWLibraryCheck;
import celsius.data.Attachment;
import atlantis.tools.Parser;
import celsius.components.categories.Category;
import celsius.components.addItems.DoubletteResult;
import celsius.data.ItemSelection;
import celsius.data.KeywordListModel;
import celsius.data.Person;
import celsius.components.tableTabs.CelsiusTableModel;
import celsius.data.TableRow;
import celsius.tools.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
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
import java.util.Date;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;

/**
 *
 * @author  cnsaeman
a */
public class MainFrame extends javax.swing.JFrame implements 
        DropTargetListener, TreeModelListener, DocumentListener, KeyListener, KeyEventDispatcher, GenericCelsiusEventListener,
        ClipboardOwner, HasManagedStates {

    public Resources RSC;
    public SplashScreen StartUp;   // splash screen

    public EditConfiguration dialogConfiguration;     // Configuration dialog
    public ExportBibliography dialogExportBibliography;
    public DeepSearch deepSearch;
    public CategoryTreePanel categoryTreePanel;
    public InformationPanel guiInfoPanel;
    public SearchPanel guiSearchPanel;
    public PluginPanel guiPluginPanel;
    
    public int bufmousex,  bufmousey;                    // buffers for popup menu over categories

    // GUI flags
    public boolean buildingNewTab;
    public boolean adjustingStates;
    
    public Future eastClearer;
    
    public int lastkeycode;
    
    public int searchState;
    
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
        jSPMain3.setBottomComponent(guiPluginPanel);
        categoryTreePanel=new CategoryTreePanel(RSC);
        jTPSearches.add(categoryTreePanel, 0);

        jTPSearches.setTabComponentAt(0, new TabLabel("",Resources.categoriesSearchTabIcon,RSC,null,false));
        jTPSearches.setTabComponentAt(1, new TabLabel("",Resources.keyTabIcon,RSC,null,false));
        jTPSearches.setTabComponentAt(2, new TabLabel("",Resources.historyTabIcon,RSC,null,false));
        
        jTPSearches.setSelectedIndex(0);
        
        jPanel7.setBorder(RSC.stdBorder());
        jPanel9.setBorder(RSC.stdBorder());
        jCE3=new ClearEdit(RSC,"Enter a keyword (CTRL+C)");
        jPanel9.add(jCE3, java.awt.BorderLayout.NORTH);
        initFurther();
    }

    public void gui2() {
        this.setLocationByPlatform(true);
        jCE3.getDocument().addDocumentListener(this);
        KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
        manager.addKeyEventDispatcher(this);
        RSC.out("Libraries loaded");
        guiPluginPanel.adjustPluginList();
        jTBAdd.setTransferHandler(new AddTransferHandler(this));
        StartUp.setStatus("Ready...");
        RSC.adjustComponents(this.getComponents());
        jSPMain3.setMinimumSize(new Dimension(RSC.guiTools.guiScale(280),RSC.guiTools.guiScale(0)));
        jSPMain3.setDividerLocation(jSPMain3.getMaximumDividerLocation());
        guiInfoPanel.setMinimumSize(new Dimension(RSC.guiTools.guiScale(0),RSC.guiTools.guiScale(280)));
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
        
        // STATEMANAGER TODO: check
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","itemSelected", new JComponent[] { jMItems, jMICitationTagClipboard, jMIBibClipboard, jMICitationTagClipboard, jMIPeople, jMIRememberSelectedItems, jMIRememberSelectedItems1});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","personSelected", new JComponent[] { jMPeople,jMIMerge1,jMIRemoveFromTable2,jMIShowItems});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","librarySelected", new JComponent[] { jMICloseLib, jMIDeleteLib, jMIShowCitedinFile, jMIConsistencyCheck, jMICheckBib, jMIEditLib,jMIFullBibToFile, jMIEditDS, jMIAddToLib, jTBAdd, jCE3, guiSearchPanel, guiPluginPanel.jBMPlugins, jMIExportBibliography, categoryTreePanel.searchCategories, jMIAutoImport});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","tabAvailable", new JComponent[] { jMICopyTab, jMICopyTab2, jMITab2Cat, jMITab2Cat2, jMICloseTab, jMICloseTab2});
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","personTabAvailable", new JComponent[] { });
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","itemTabAvailable", new JComponent[] { });
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","categorySelected", new JComponent[] { jMCategories, jMIInsertCat });
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame","pluginSelected", new JComponent[] { });
        RSC.guiStates.setState("mainFrame","librarySelected", false);
        RSC.guiStates.setState("mainFrame","categorySelected", false);
        RSC.guiStates.setState("mainFrame","itemSelected", false);
        RSC.guiStates.setState("mainFrame","personSelected", false);
        RSC.guiStates.setState("mainFrame","pluginSelected", false);
        RSC.guiStates.setState("mainFrame","tabAvailable",false);        
        RSC.guiStates.setState("mainFrame","itemTabAvailable",false);        
        RSC.guiStates.setState("mainFrame","personTabAvailable",false);        
        RSC.guiStates.registerListener("mainFrame", this);
        RSC.guiStates.adjustStates("mainFrame");

        setIconImage(RSC.guiTools.appIcon);
        dialogExportBibliography = new ExportBibliography(RSC);
        dialogConfiguration = new EditConfiguration(RSC);
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
        RSC.close();
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
     * @param library
     */
    public void switchToLibrary(Library library) {
        // Library already selected
        if ((RSC.getCurrentlySelectedLibNo()>-1) && (RSC.getCurrentlySelectedLibNo()<RSC.libraries.size()))
            if (RSC.getCurrentlySelectedLibrary()==library) return;
        // No library remaining
        if (RSC.libraries.isEmpty()) {
            RSC.guiStates.adjustState("mainFrame", "librarySelected", false);
            return;
        }
        
        // switch to currently selected or other library
        if (library==null) RSC.currentLibrary=jCBLibraries.getSelectedIndex();
        else RSC.currentLibrary=RSC.libraries.indexOf(library);
        
        if (RSC.currentLibrary==-1) {
            RSC.guiStates.adjustState("mainFrame","librarySelected", false);
            return;
        }
        RSC.guiStates.adjustState("mainFrame", "librarySelected", true);
        if (RSC.currentLibrary!=jCBLibraries.getSelectedIndex())
            jCBLibraries.setSelectedIndex(RSC.currentLibrary);
        categoryTreePanel.structureTreeModel.setRoot(RSC.getCurrentlySelectedLibrary().structureTreeRoot);
        updateStatusBar(true);
        jMIAutoImport.setSelected((RSC.currentLibrary!=-1) && RSC.getCurrentlySelectedLibrary().autoImport());
        RSC.plugins.updateExportPlugins();
    }
    
    /**
     * Copies the currently selected items to library targetLibrary.
     * @param targetLibrary
     */
    private void copyItemToLibrary(Library targetLibrary) {
        Library sourceLibrary = RSC.getCurrentTable().library;
        if (sourceLibrary != targetLibrary) {
            for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
                Item item=(Item)tableRow;
                DoubletteResult dr=new DoubletteResult(0,null,null);
                try {
                    dr = targetLibrary.isDoublette(item);
                } catch (IOException ex) {
                    RSC.outEx(ex);
                    dr.type=12;
                }
                boolean add=true;
                if (dr.type==12) {
                    add=false;
                    RSC.guiTools.showWarning("Error:","I/O Error while checking for doublettes.");
                }
                if (dr.type==10) {
                    int j=RSC.guiTools.askQuestionOC("An exact copy of the item "+item.toText(false)+"\nalready exists in the library. Should another copy be added?", "Warning:");
                    if (j==JOptionPane.NO_OPTION) add=false;
                }
                if (dr.type==4) {
                    int j=RSC.guiTools.askQuestionOC("A paper with the same key information as the item "+item.toText(false)+"\nalready exists in the library. Should another copy be added?", "Warning:");
                    if (j==JOptionPane.NO_OPTION) add=false;
                }
                if (add) targetLibrary.acquireCopyOfItem(item);
            }
        } else {
            RSC.guiTools.showWarning("Warning!","Items can only be copied to different libraries.");
        }
    }
    
    @Override
    public void adjustStates() {
        if (RSC.guiStates.getState("mainFrame","librarySelected")) {
            if (RSC.getCurrentlySelectedLibrary().hideFunctionality.contains("Menu:Bibliography")) {
                jMBibTeX.setVisible(false);
            } else {
                jMBibTeX.setVisible(true);
            }
            categoryTreePanel.structureTree.setComponentPopupMenu(categoryTreePanel.pmCategories);
            if (categoryTreePanel.structureTreeModel.getRoot()!=RSC.getCurrentlySelectedLibrary().structureTreeRoot)
                categoryTreePanel.structureTreeModel.setRoot(RSC.getCurrentlySelectedLibrary().structureTreeRoot);
        } else {
            categoryTreePanel.structureTree.setComponentPopupMenu(null);
            categoryTreePanel.structureTreeModel.setRoot(null);
            updateStatusBar(false);
        }
        if (!RSC.guiStates.getState("mainFrame","tabAvailable")) {
            guiInfoPanel.updateGUI();
        }
    }

    /**
     * Set thread message
     * @param s
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
        buttonGroup1 = new javax.swing.ButtonGroup();
        jPMItemTable = new javax.swing.JPopupMenu();
        jMIView1 = new javax.swing.JMenuItem();
        jMIPeople2 = new javax.swing.JMenuItem();
        jSeparator20 = new javax.swing.JSeparator();
        jMIRemoveFromTab1 = new javax.swing.JMenuItem();
        jMIUnregisterDoc1 = new javax.swing.JMenuItem();
        jMIDeleteFile1 = new javax.swing.JMenuItem();
        jMIRemoveHalf1 = new javax.swing.JMenuItem();
        jSeparator24 = new javax.swing.JSeparator();
        jMIJoin1 = new javax.swing.JMenuItem();
        jSeparator26 = new javax.swing.JSeparator();
        jMIRememberSelectedItems1 = new javax.swing.JMenuItem();
        jMCopyToDiff1 = new javax.swing.JMenu();
        jMIExportTab1 = new javax.swing.JMenuItem();
        jMIEmail1 = new javax.swing.JMenuItem();
        buttonGroup2 = new javax.swing.ButtonGroup();
        bGSearch = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        jPMPeopleTable = new javax.swing.JPopupMenu();
        jMIMerge = new javax.swing.JMenuItem();
        jMIDeletePerson = new javax.swing.JMenuItem();
        jMIRemoveFromTable = new javax.swing.JMenuItem();
        jMIShowItems1 = new javax.swing.JMenuItem();
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
        jPanel9 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jLSearchKeys = new javax.swing.JList();
        jPanel23 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLWhenAdded = new javax.swing.JList<>();
        jPanel15 = new javax.swing.JPanel();
        jSPMain = new javax.swing.JSplitPane();
        jTPTabList = new javax.swing.JTabbedPane();
        jMainMenu = new javax.swing.JMenuBar();
        jMFile = new javax.swing.JMenu();
        jMIConfig = new javax.swing.JMenuItem();
        jMIEditLibTemplates = new javax.swing.JMenuItem();
        jMIClearLoggingFile = new javax.swing.JMenuItem();
        jSeparator9 = new javax.swing.JPopupMenu.Separator();
        jMIConvertLibrary = new javax.swing.JMenuItem();
        jSeparator10 = new javax.swing.JSeparator();
        jMIQuit = new javax.swing.JMenuItem();
        jMLibraries = new javax.swing.JMenu();
        jMICreateLib = new javax.swing.JMenuItem();
        jMILoadLib = new javax.swing.JMenuItem();
        jMRecent = new javax.swing.JMenu();
        jMICloseLib = new javax.swing.JMenuItem();
        jMIDeleteLib = new javax.swing.JMenuItem();
        jSeparator11 = new javax.swing.JSeparator();
        jMIAddToLib = new javax.swing.JMenuItem();
        jMIAutoImport = new javax.swing.JCheckBoxMenuItem();
        jSeparator2 = new javax.swing.JSeparator();
        jMIEditLib = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JSeparator();
        jMIEditDS = new javax.swing.JMenuItem();
        jSeparator33 = new javax.swing.JPopupMenu.Separator();
        jMIHistory = new javax.swing.JMenuItem();
        jMIConsistencyCheck = new javax.swing.JMenuItem();
        jMTabs = new javax.swing.JMenu();
        jMIAddTab = new javax.swing.JMenuItem();
        jMICopyTab = new javax.swing.JMenuItem();
        jMITab2Cat = new javax.swing.JMenuItem();
        jMICloseTab = new javax.swing.JMenuItem();
        jMPeople = new javax.swing.JMenu();
        jMIMerge1 = new javax.swing.JMenuItem();
        jMIDeletePerson1 = new javax.swing.JMenuItem();
        jMIRemoveFromTable2 = new javax.swing.JMenuItem();
        jMIShowItems = new javax.swing.JMenuItem();
        jMItems = new javax.swing.JMenu();
        jMIView = new javax.swing.JMenuItem();
        jMIPeople = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JSeparator();
        jMIRemoveFromTab = new javax.swing.JMenuItem();
        jMIUnregisterDoc = new javax.swing.JMenuItem();
        jMIDeleteFile = new javax.swing.JMenuItem();
        jMIRemoveHalf = new javax.swing.JMenuItem();
        jSeparator6 = new javax.swing.JSeparator();
        jMIJoin = new javax.swing.JMenuItem();
        jSeparator8 = new javax.swing.JSeparator();
        jMIRememberSelectedItems = new javax.swing.JMenuItem();
        jMCopyToDiff = new javax.swing.JMenu();
        jMIExportTab = new javax.swing.JMenuItem();
        jMIEmail = new javax.swing.JMenuItem();
        jMCategories = new javax.swing.JMenu();
        jMIInsertCat = new javax.swing.JMenuItem();
        jMIRenameCat = new javax.swing.JMenuItem();
        jMIDelCat = new javax.swing.JMenuItem();
        jSeparator14 = new javax.swing.JSeparator();
        jMICatDragDrop = new javax.swing.JCheckBoxMenuItem();
        jMBibTeX = new javax.swing.JMenu();
        jMICitationTagClipboard = new javax.swing.JMenuItem();
        jMIBibClipboard = new javax.swing.JMenuItem();
        jMIExportBibliography = new javax.swing.JMenuItem();
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

        jMIView1.setText("Open selected item");
        jMIView1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIView1);

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
        jPMItemTable.add(jSeparator24);

        jMIJoin1.setText("Combine the selected items");
        jMIJoin1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIJoinActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIJoin1);
        jPMItemTable.add(jSeparator26);

        jMIRememberSelectedItems1.setText("Remember selected items");
        jMIRememberSelectedItems1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRememberSelectedItemsActionPerformed(evt);
            }
        });
        jPMItemTable.add(jMIRememberSelectedItems1);

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

        jMIDeletePerson.setText("Delete person");
        jMIDeletePerson.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeletePersonActionPerformed(evt);
            }
        });
        jPMPeopleTable.add(jMIDeletePerson);

        jMIRemoveFromTable.setText("Remove from current table");
        jMIRemoveFromTable.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveFromTableActionPerformed(evt);
            }
        });
        jPMPeopleTable.add(jMIRemoveFromTable);

        jMIShowItems1.setText("Show associated items");
        jMIShowItems1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIShowItemsActionPerformed(evt);
            }
        });
        jPMPeopleTable.add(jMIShowItems1);

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

        jTBAdd.setIcon(RSC.icons.getScaledIcon("Add Icon"));
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

        jMIEditLibTemplates.setText("Edit library templates");
        jMIEditLibTemplates.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditLibTemplatesActionPerformed(evt);
            }
        });
        jMFile.add(jMIEditLibTemplates);

        jMIClearLoggingFile.setText("Clear logging file");
        jMIClearLoggingFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIClearLoggingFileActionPerformed(evt);
            }
        });
        jMFile.add(jMIClearLoggingFile);
        jMFile.add(jSeparator9);

        jMIConvertLibrary.setText("Convert library");
        jMIConvertLibrary.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIConvertLibraryActionPerformed(evt);
            }
        });
        jMFile.add(jMIConvertLibrary);
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
        jMLibraries.add(jSeparator11);

        jMIAddToLib.setText("Add items to library");
        jMIAddToLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAddToLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIAddToLib);

        jMIAutoImport.setSelected(true);
        jMIAutoImport.setText("Automatically import items");
        jMIAutoImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAutoImportActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIAutoImport);
        jMLibraries.add(jSeparator2);

        jMIEditLib.setText("Edit library properties");
        jMIEditLib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditLibActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIEditLib);
        jMLibraries.add(jSeparator4);

        jMIEditDS.setText("Edit HTML template");
        jMIEditDS.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditDSActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIEditDS);
        jMLibraries.add(jSeparator33);

        jMIHistory.setText("Show item view history last 24h");
        jMIHistory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIHistoryActionPerformed(evt);
            }
        });
        jMLibraries.add(jMIHistory);

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

        jMainMenu.add(jMTabs);

        jMPeople.setText("People");

        jMIMerge1.setText("Merge people");
        jMIMerge1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIMerge1ActionPerformed(evt);
            }
        });
        jMPeople.add(jMIMerge1);

        jMIDeletePerson1.setText("Delete person");
        jMIDeletePerson1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIDeletePersonActionPerformed(evt);
            }
        });
        jMPeople.add(jMIDeletePerson1);

        jMIRemoveFromTable2.setText("Remove from current table");
        jMIRemoveFromTable2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRemoveFromTable2ActionPerformed(evt);
            }
        });
        jMPeople.add(jMIRemoveFromTable2);

        jMIShowItems.setText("Show associated items");
        jMIShowItems.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIShowItemsActionPerformed(evt);
            }
        });
        jMPeople.add(jMIShowItems);

        jMainMenu.add(jMPeople);

        jMItems.setText("Items");

        jMIView.setText("Open selected item");
        jMIView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIViewActionPerformed(evt);
            }
        });
        jMItems.add(jMIView);

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

        jMIJoin.setText("Combine the selected items");
        jMIJoin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIJoinActionPerformed(evt);
            }
        });
        jMItems.add(jMIJoin);
        jMItems.add(jSeparator8);

        jMIRememberSelectedItems.setText("Remember selected items");
        jMIRememberSelectedItems.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIRememberSelectedItemsActionPerformed(evt);
            }
        });
        jMItems.add(jMIRememberSelectedItems);

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

        jMICatDragDrop.setText("Allow drag and drop of categories");
        jMICatDragDrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMICatDragDropActionPerformed(evt);
            }
        });
        jMCategories.add(jMICatDragDrop);

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

        jMIExportBibliography.setText("Export bibliography");
        jMIExportBibliography.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIExportBibliographyActionPerformed(evt);
            }
        });
        jMBibTeX.add(jMIExportBibliography);

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
        performItemMerge();
    }//GEN-LAST:event_jMIJoinActionPerformed

    private void jMICopyTabActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICopyTabActionPerformed
        RSC.guiStates.adjustState("mainFrame", "itemSelected", false);
        CelsiusTable DT=new CelsiusTable(RSC.getCurrentTable());
        RSC.celsiusTables.add(DT);

        jMICloseTab2.setEnabled(true);
        jMICloseTab.setEnabled(true);
        final JScrollPane scrollpane = new JScrollPane(DT.jtable);
        jTPTabList.add(scrollpane);
        TabLabel TL=(TabLabel)jTPTabList.getTabComponentAt(jTPTabList.getSelectedIndex());
        jTPTabList.setTabComponentAt(jTPTabList.getTabCount() - 1, new TabLabel(TL.title + "'",TL.icon,RSC,DT,true));
        DT.title=TL.title + "'";
        jTPTabList.setSelectedComponent(scrollpane);
        jTPTabList.setSelectedIndex(jTPTabList.getTabCount() - 1);
        int cordx = bufmousex - categoryTreePanel.structureTree.getLocationOnScreen().x;
        int cordy = bufmousey - categoryTreePanel.structureTree.getLocationOnScreen().y;
        categoryTreePanel.structureTree.setSelectionPath(null);
        categoryTreePanel.structureTree.setSelectionPath(categoryTreePanel.structureTree.getPathForLocation(cordx, cordy));
    }//GEN-LAST:event_jMICopyTabActionPerformed

    private void jMIConfigActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConfigActionPerformed
        dialogConfiguration.open();
    }//GEN-LAST:event_jMIConfigActionPerformed

    private void jMIRenameCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRenameCatActionPerformed
        categoryTreePanel.renameCategory();
    }//GEN-LAST:event_jMIRenameCatActionPerformed

    private void jMIShowCitedinFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIShowCitedinFileActionPerformed
        String filename=RSC.guiTools.selectFileForOpen("Indicate the LaTeX source file", "showcited", ".tex", "LaTeX files");
        if (filename!=null) {
            CelsiusTable celsiusTable=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Cited in " + filename,"search");
            celsiusTable.resizeTable(true);
            RSC.guiStates.adjustState("mainFrame","itemSelected", false);
            ProgressMonitor progressMonitor = new ProgressMonitor(this, "Looking for papers ...", "", 0, RSC.getCurrentlySelectedLibrary().getSize());
            SWShowCited swAP = new SWShowCited(celsiusTable,0,filename);
            swAP.execute();
        }
    }//GEN-LAST:event_jMIShowCitedinFileActionPerformed

    private void jMIConsistencyCheckActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConsistencyCheckActionPerformed
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "", "", 0, RSC.getCurrentlySelectedLibrary().getSize());                    // Progress label
        setThreadMsg("Working...");
        SWLibraryCheck swLibraryCheck=new SWLibraryCheck(RSC,progressMonitor);
        swLibraryCheck.execute();
    }//GEN-LAST:event_jMIConsistencyCheckActionPerformed

    private void jMIEditLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditLibActionPerformed
        (new EditLibrary(RSC,0)).setVisible(true);
    }//GEN-LAST:event_jMIEditLibActionPerformed

    private void jMIEditDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditDSActionPerformed
        Library library = RSC.getCurrentlySelectedLibrary();
        String tmp = library.getHTMLTemplate(guiInfoPanel.currentTemplate).templateString;
        MultiLineEditor MLE = new MultiLineEditor(RSC.guiTools, "Edit HTML template", tmp);
        MLE.setVisible(true);
        if (!MLE.cancelled) {
            library.setHTMLTemplate(guiInfoPanel.currentTemplate,MLE.text);
            guiInfoPanel.updateGUI();
        }
    }//GEN-LAST:event_jMIEditDSActionPerformed

    private void jMIDelCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDelCatActionPerformed
        categoryTreePanel.deleteCategory();        
    }//GEN-LAST:event_jMIDelCatActionPerformed

    private void jMIInsertCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIInsertCatActionPerformed
        categoryTreePanel.insertCategory();        
    }//GEN-LAST:event_jMIInsertCatActionPerformed

    private void jMICheckBibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICheckBibActionPerformed
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Items with corrupt BibTeX","search");
        setThreadMsg("Working...");
        ProgressMonitor progressMonitor = new ProgressMonitor(this, "Checking BibTeX integrity ...", "", 0, RSC.getCurrentlySelectedLibrary().getSize());
        SWBibTeXIntegrity swBibTeXIntegrity=new SWBibTeXIntegrity(celsiusTable,celsiusTable.postID);
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
        String folder=RSC.guiTools.selectFolder("Select the target folder for exporting","exportTab");
        if (folder!=null) {
            try {
                if (!(new File(folder)).exists()) {
                    (new File(folder)).mkdir();
                }
                for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
                    Item item=(Item)tableRow;
                    item.loadLevel(3);
                    if (!item.linkedAttachments.isEmpty()) {
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
                RSC.guiTools.showWarning("Exception:","Error while exporting files:\n" + ex.toString());
            }
        }
    }//GEN-LAST:event_jMIExportTabActionPerformed

    private void jMIUnregisterDocActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIUnregisterDocActionPerformed
        StructureNode structureNode = categoryTreePanel.getSelectedNode();
        if (structureNode != null) {
            CelsiusTable DT=RSC.getCurrentTable();
            if (DT!=null) {
                Library library = RSC.getCurrentlySelectedLibrary();
                Category category=structureNode.category;
                boolean doit=false;
                int h=0;
                for (TableRow tableRow : DT.getSelectedRows()) {
                    if (!doit) h=RSC.guiTools.askQuestionABCD("Remove the item \n" + tableRow.toText(false) + "\nfrom the current category?",
                            "Warning","Yes","No","Yes to all","Cancel");
                    if (h==3) break;
                    if (h==2) doit=true;
                    if (doit || (h == 0)) {
                        try {
                            library.unRegisterItem((Item)tableRow,Integer.parseInt(category.id));
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
                if (!doit) h=RSC.guiTools.askQuestionABCD("Delete the item \n" + tableRow.toText(false) + "\nand all related information?",
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
        RSC.removeSelectedFromCurrentTable();
        guiInfoPanel.updateGUI();
    }//GEN-LAST:event_jMIRemoveFromTabActionPerformed

    private void jMIFullBibToFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIFullBibToFileActionPerformed
        Library library=RSC.getCurrentlySelectedLibrary();
        String filename=RSC.guiTools.selectFileForOpen("Indicate the target bib file", "export", "_ALL", "All files");
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
                RSC.guiTools.showInformation("Task completed:", "BibTeX file exported");
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
            guiInfoPanel.updateGUI();
            RSC.guiStates.adjustState("mainFrame","tabAvailable", false);
        } else {
            switchToLibrary(celsiusTable.library);
            guiPluginPanel.adjustPluginList();
            guiInfoPanel.updateGUI();
            RSC.guiStates.adjustState("mainFrame","tabAvailable", true);
            if (celsiusTable.getObjectType()==CelsiusTableModel.CELSIUS_TABLE_ITEM_TYPE) {
                RSC.guiStates.adjustState("mainFrame","itemTabAvailable", true);
                RSC.guiStates.adjustState("mainFrame","personTabAvailable", false);
            } else if (celsiusTable.getObjectType()==CelsiusTableModel.CELSIUS_TABLE_ITEM_TYPE) {
                RSC.guiStates.adjustState("mainFrame","itemTabAvailable", false);
                RSC.guiStates.adjustState("mainFrame","personTabAvailable", true);
            }
        }
        RSC.guiStates.adjustState("mainFrame","tabAvailable", jTPTabList.getSelectedIndex()!=-1);
    }//GEN-LAST:event_jTPTabListStateChanged
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

   // Close Current Library
    private void jMICloseLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICloseLibActionPerformed
        closeCurrentLibrary(true);
    }//GEN-LAST:event_jMICloseLibActionPerformed
    // Load Current Library
    private void jMILoadLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMILoadLibActionPerformed
        String filename=RSC.guiTools.selectFolder("Select the folder of the library you wish to open.", "loadlibraries");
        if (filename!=null) {
            // check the folder is valid
            boolean libraryFolder=(new File(filename+ToolBox.FILE_SEPARATOR+"CelsiusLibrary.sql")).exists();
            if (libraryFolder) {
                final MainFrame MF = this;
                (new Thread("LoadingLib") {

                    @Override
                    public void run() {
                        setThreadMsg("Opening library...");
                        MF.jPBSearch.setIndeterminate(true);
                        try {
                            RSC.loadLibrary(filename,true);
                        } catch (Exception e) {
                            RSC.guiTools.showWarning("Warning:","Loading library failed:\n" + e.toString());
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
            for (TableRow tableRow : RSC.getCurrentTable().getSelectedRows()) {
                tableRow.loadLevel(2);
                ref += "," + tableRow.get("citation-tag");
            }
            ref = ref.substring(1).trim();
            if (ref.equals("null")) {
                RSC.guiTools.showWarning("Warning", "No citation tags found for selected items.");
            } else {
                StringSelection cont = new StringSelection(ref);
                Clp.setContents(cont, this);
            }
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
    if (RSC.guiTools.askQuestionYN("Do you really want to delete the Library "+Lib.name+"?\nWarning: all files in the library's directory will be erased!", "Confirm:")==0) {
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
    categoryTreePanel.turnTabIntoCategory();
    updateStatusBar(false);
}//GEN-LAST:event_jMITab2Cat2ActionPerformed

private void jMIEditLibTemplatesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditLibTemplatesActionPerformed
    (new celsius.components.library.EditLibraryTemplates(RSC)).setVisible(true);
}//GEN-LAST:event_jMIEditLibTemplatesActionPerformed

private void jMIAddToLibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAddToLibActionPerformed
        AddItems DA=new AddItems(RSC);
        DA.setVisible(true);
        if (!DA.addedItems.isEmpty()) {
            CelsiusTable celsiusTable=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_WHEN_ADDED, "Last added","search");
            celsiusTable.addRows(DA.addedItems);
            celsiusTable.resizeTable(true);
            updateStatusBar(true);
            guiInfoPanel.updateGUI();
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

private void jMIRemoveHalfActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRemoveHalfActionPerformed
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            CelsiusTable IT = RSC.celsiusTables.get(jTPTabList.getSelectedIndex());
            boolean doit=false;
            int h=0;
            for (TableRow tableRow : IT.getSelectedRows()) {
                Item item=(Item)tableRow;
                if (!doit) h=RSC.guiTools.askQuestionABCD("Delete the document \n" + item.toText(false) + ",\nkeeping the associated file?",
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

    private void jLWhenAddedValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLWhenAddedValueChanged
        goToHistory(jLWhenAdded.getSelectedIndex());
    }//GEN-LAST:event_jLWhenAddedValueChanged

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

    private void jMIRemoveFromTableActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRemoveFromTableActionPerformed
        RSC.removeSelectedFromCurrentTable();
        guiInfoPanel.updateGUI();
    }//GEN-LAST:event_jMIRemoveFromTableActionPerformed

    private void jMIRemoveFromTable2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRemoveFromTable2ActionPerformed
        RSC.removeSelectedFromCurrentTable();
        guiInfoPanel.updateGUI();
    }//GEN-LAST:event_jMIRemoveFromTable2ActionPerformed

    private void jMIExportBibliographyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIExportBibliographyActionPerformed
        dialogExportBibliography.adjustButtons();
        dialogExportBibliography.showCentered();
    }//GEN-LAST:event_jMIExportBibliographyActionPerformed

    private void jMIShowItemsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIShowItemsActionPerformed
        showAssociatedItems();
    }//GEN-LAST:event_jMIShowItemsActionPerformed

    private void jMIConvertLibraryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIConvertLibraryActionPerformed
        String filename=RSC.guiTools.selectFileForOpen("Select the main file of the library you wish to open.", "loadlibraries", "_ALL", "All Files");
        if (filename!=null) {
            jPBSearch.setIndeterminate(true);
            Thread DoIt = (new Thread() {
                @Override
                public void run() {
                    Library3.convertLib(filename,RSC);
                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            jPBSearch.setIndeterminate(false);
                            RSC.guiTools.showInformation("The library has been converted.", "Action completed.");
                        }
                    });
                }
            });
            DoIt.start();
        }
    }//GEN-LAST:event_jMIConvertLibraryActionPerformed

    private void jMIRememberSelectedItemsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIRememberSelectedItemsActionPerformed
        ItemSelection itemSelection=new ItemSelection(RSC.getCurrentlySelectedLibrary());
        itemSelection.addAll(RSC.getCurrentTable().getSelectedRows());
        RSC.lastItemSelection=itemSelection;
    }//GEN-LAST:event_jMIRememberSelectedItemsActionPerformed

    private void jMIDeletePersonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIDeletePersonActionPerformed
        Person person=RSC.getCurrentlySelectedPerson();
        if (person.hasLinkedItems()) {
            RSC.guiTools.showInformation("Cannot delete person", "The currently selected person still has linked items in the library. Cancelling.");
        } else {
            int i=RSC.guiTools.askQuestionOC("Should the person "+person.toText(false)+" really be deleted?","Confirm");
            if (i==JOptionPane.YES_OPTION) {
                String id=person.id;
                person.destroy();
                RSC.getCurrentlySelectedLibrary().personChanged(id);
            }
        }
    }//GEN-LAST:event_jMIDeletePersonActionPerformed

    private void jMICatDragDropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMICatDragDropActionPerformed
        System.out.println("MAINFRAME adjust");
        categoryTreePanel.miCatDragAndDrop.setState(jMICatDragDrop.getState());
        categoryTreePanel.setDragAndDropState(jMICatDragDrop.getState());
    }//GEN-LAST:event_jMICatDragDropActionPerformed

    private void jMIAutoImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAutoImportActionPerformed
        RSC.getCurrentlySelectedLibrary().setAutoImport(jMIAutoImport.isSelected());
    }//GEN-LAST:event_jMIAutoImportActionPerformed

    private void jMIHistoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIHistoryActionPerformed
        showHistory(RSC.getCurrentlySelectedLibrary());
    }//GEN-LAST:event_jMIHistoryActionPerformed
        
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem JMIManual;
    private javax.swing.ButtonGroup bGSearch;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    public javax.swing.JComboBox jCBLibraries;
    private javax.swing.JDialog jDialog1;
    private javax.swing.JList jLSearchKeys;
    public javax.swing.JLabel jLStatusBar;
    private javax.swing.JLabel jLThreadStatus;
    private javax.swing.JList<String> jLWhenAdded;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JMenu jMBibTeX;
    private javax.swing.JMenu jMCategories;
    public javax.swing.JMenu jMCopyToDiff;
    public javax.swing.JMenu jMCopyToDiff1;
    private javax.swing.JMenu jMFile;
    private javax.swing.JMenu jMHelp;
    private javax.swing.JMenuItem jMIAbout;
    private javax.swing.JMenuItem jMIAddTab;
    private javax.swing.JMenuItem jMIAddToLib;
    public javax.swing.JCheckBoxMenuItem jMIAutoImport;
    private javax.swing.JMenuItem jMIBibClipboard;
    public javax.swing.JCheckBoxMenuItem jMICatDragDrop;
    private javax.swing.JMenuItem jMICheckBib;
    private javax.swing.JMenuItem jMICitationTagClipboard;
    private javax.swing.JMenuItem jMIClearLoggingFile;
    private javax.swing.JMenuItem jMICloseLib;
    public javax.swing.JMenuItem jMICloseTab;
    public javax.swing.JMenuItem jMICloseTab2;
    private javax.swing.JMenuItem jMIConfig;
    private javax.swing.JMenuItem jMIConsistencyCheck;
    private javax.swing.JMenuItem jMIConvertLibrary;
    private javax.swing.JMenuItem jMICopyTab;
    private javax.swing.JMenuItem jMICopyTab2;
    private javax.swing.JMenuItem jMICreateLib;
    private javax.swing.JMenuItem jMIDelCat;
    private javax.swing.JMenuItem jMIDeleteFile;
    private javax.swing.JMenuItem jMIDeleteFile1;
    private javax.swing.JMenuItem jMIDeleteLib;
    private javax.swing.JMenuItem jMIDeletePerson;
    private javax.swing.JMenuItem jMIDeletePerson1;
    public javax.swing.JMenuItem jMIEditDS;
    private javax.swing.JMenuItem jMIEditLib;
    private javax.swing.JMenuItem jMIEditLibTemplates;
    private javax.swing.JMenuItem jMIEmail;
    private javax.swing.JMenuItem jMIEmail1;
    private javax.swing.JMenuItem jMIExportBibliography;
    private javax.swing.JMenuItem jMIExportTab;
    private javax.swing.JMenuItem jMIExportTab1;
    private javax.swing.JMenuItem jMIFullBibToFile;
    private javax.swing.JMenuItem jMIHistory;
    private javax.swing.JMenuItem jMIInsertCat;
    private javax.swing.JMenuItem jMIJoin;
    private javax.swing.JMenuItem jMIJoin1;
    private javax.swing.JMenuItem jMILoadLib;
    private javax.swing.JMenuItem jMIMerge;
    private javax.swing.JMenuItem jMIMerge1;
    private javax.swing.JMenuItem jMINew;
    private javax.swing.JMenuItem jMIPeople;
    private javax.swing.JMenuItem jMIPeople2;
    private javax.swing.JMenuItem jMIQuit;
    private javax.swing.JMenuItem jMIRememberSelectedItems;
    private javax.swing.JMenuItem jMIRememberSelectedItems1;
    private javax.swing.JMenuItem jMIRemoveFromTab;
    private javax.swing.JMenuItem jMIRemoveFromTab1;
    private javax.swing.JMenuItem jMIRemoveFromTable;
    private javax.swing.JMenuItem jMIRemoveFromTable2;
    private javax.swing.JMenuItem jMIRemoveHalf;
    private javax.swing.JMenuItem jMIRemoveHalf1;
    private javax.swing.JMenuItem jMIRenameCat;
    private javax.swing.JMenuItem jMIShowCitedinFile;
    private javax.swing.JMenuItem jMIShowItems;
    private javax.swing.JMenuItem jMIShowItems1;
    private javax.swing.JMenuItem jMITab2Cat;
    private javax.swing.JMenuItem jMITab2Cat2;
    private javax.swing.JMenuItem jMIUnregisterDoc;
    private javax.swing.JMenuItem jMIUnregisterDoc1;
    private javax.swing.JMenuItem jMIUpdate;
    private javax.swing.JMenuItem jMIView;
    private javax.swing.JMenuItem jMIView1;
    public javax.swing.JMenu jMItems;
    private javax.swing.JMenu jMLibraries;
    private javax.swing.JMenu jMPeople;
    public javax.swing.JMenu jMRecent;
    private javax.swing.JMenu jMTabs;
    private javax.swing.JMenuBar jMainMenu;
    public javax.swing.JProgressBar jPBSearch;
    public javax.swing.JPopupMenu jPMItemList;
    public javax.swing.JPopupMenu jPMItemTable;
    public javax.swing.JPopupMenu jPMPeopleTable;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel20;
    private javax.swing.JPanel jPanel23;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JPasswordField jPasswordField1;
    private javax.swing.JSplitPane jSPMain;
    private javax.swing.JSplitPane jSPMain3;
    private javax.swing.JScrollBar jScrollBar1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JSeparator jSeparator10;
    private javax.swing.JSeparator jSeparator11;
    private javax.swing.JSeparator jSeparator14;
    private javax.swing.JPopupMenu.Separator jSeparator19;
    private javax.swing.JSeparator jSeparator2;
    private javax.swing.JSeparator jSeparator20;
    private javax.swing.JSeparator jSeparator24;
    private javax.swing.JSeparator jSeparator26;
    private javax.swing.JSeparator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator33;
    private javax.swing.JSeparator jSeparator4;
    private javax.swing.JSeparator jSeparator6;
    private javax.swing.JPopupMenu.Separator jSeparator7;
    private javax.swing.JSeparator jSeparator8;
    private javax.swing.JPopupMenu.Separator jSeparator9;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JButton jTBAdd;
    public javax.swing.JTabbedPane jTPSearches;
    public javax.swing.JTabbedPane jTPTabList;
    // End of variables declaration//GEN-END:variables

    public void reloadPlugins() {
        RSC.plugins.readInAvailablePlugins();
        guiPluginPanel.objectType=-2;
        guiPluginPanel.adjustPluginList();
        RSC.plugins.updateExportPlugins();
    }
    
    public void showHistory(Library library) {
        CelsiusTable table=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_HISTORY, "History", "search");
        try {
            ResultSet RS = library.executeResEX("SELECT timestamp AS \"$$numtimestamp\", items.* from item_views JOIN items on item_id=items.id WHERE timestamp >"+String.valueOf(ToolBox.now()-60*60*24)+";");
            while (RS.next()) {
                Item item = new Item(library, RS);
                item.currentLoadLevel = 2;
                item.put("$$timestamp", RSC.SDF.format(new Date(Long.valueOf(item.get("$$numtimestamp"))*1000)));
                table.addRow(item);
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        table.resizeTable(true);
        RSC.MF.guiInfoPanel.updateGUI();
    }
    
    public void showAssociatedItems() {
        String ids=RSC.getCurrentTable().getSelectedIDsString();
        Library library=RSC.getCurrentlySelectedLibrary();
        CelsiusTable table=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Associated Items", "search");
        try {
            ResultSet RS=library.executeResEX("SELECT items.* FROM item_person_links INNER JOIN items ON items.id=item_person_links.item_id WHERE person_id IN ("+ids+");");
            while (RS.next()) {
                table.addRow(new Item(library,RS));
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        table.resizeTable(true);
        RSC.MF.guiInfoPanel.updateGUI();
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
        table.resizeTable(true);
        RSC.MF.guiInfoPanel.updateGUI();
    }
    
    public void performPeopleMerge() {
        String ids=RSC.getCurrentTable().getSelectedIDsString();
        if ((ids.length()>0) && (ids.contains(","))) {
            MergePeopleDialog MP=new MergePeopleDialog(RSC,ids);
            MP.setVisible(true);
        } else {
            RSC.guiTools.showWarning("Cancelled:","You have to selected more than one person to merge.");
        }
    }
        
    public void performItemMerge() {
        if (RSC.getCurrentTable().jtable.getSelectedRowCount()==2) {
            String ids=RSC.getCurrentTable().getSelectedIDsString();
            MergeItems MI=new MergeItems(RSC,ids);
            MI.setVisible(true);
        } else {
            RSC.guiTools.showWarning("Cancelled:","You have to selected exactly two items to merge.");
        }
    }
    
    public Category getSelectedCategory() {
        StructureNode structureNode = categoryTreePanel.getSelectedNode();
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
        if (categoryTreePanel.structureTree.getPathForLocation(p.x,p.y)==null) {
            RSC.guiTools.showWarning("Warning","Could not find category to drop into. Cancelling...");
        } else {
            StructureNode structureNode = (StructureNode) ((categoryTreePanel.structureTree.getPathForLocation(p.x,p.y)).getLastPathComponent());
            CelsiusTable DT = RSC.getCurrentTable();
            // TODO DT.setCategoryByID(structureNode.category.id);
            if (DT != null) {
                for (TableRow tableRow : DT.getSelectedRows()) {
                    Item item=(Item)tableRow;
                    try {
                        DT.library.registerItem(item, structureNode, 0);
                        guiInfoPanel.updateGUI();
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
                    int i = RSC.guiTools.askQuestionABC("Which information should be kept for the tag:\n" + tag + "?\nA: " + item1.get(tag) + "\nB: " + item2.get(tag), "Please decide:", "A", "B", "Cancel");
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
        if (item1.error==6) RSC.guiTools.showWarning("Warning","Error writing back information");
        item2.put("location", null);
        // TODO item2.library.deleteItem(item2.id);
        return(true);
    }
    
    public boolean closeCurrentLibrary(boolean rememberLib) {
        if (RSC.currentLibrary==-1) return(false);
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

    public void goToItem(String itemID) {
        Library library=RSC.getCurrentlySelectedLibrary();
        CelsiusTable celsiusTable=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Linked item",Resources.linksTabIcon);
        RSC.getCurrentlySelectedLibrary().showItemWithID(itemID, celsiusTable);
        RSC.getCurrentTable().selectFirst();
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
        guiInfoPanel.currentObject=category;
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(CelsiusTable.TABLETYPE_ITEMS_IN_CATEGORY, category.getS("label"), "folder_table");
        Library library=RSC.getCurrentlySelectedLibrary();
        try {
            library.showItemsInCategory(category, celsiusTable);
        } catch (Exception e) {
            RSC.outEx(e);
        }
        celsiusTable.properties.put("category", category.getS("label"));
        guiInfoPanel.updateGUI();
    }
    
    public void goToCategory(String categoryID) {
        if (categoryID==null) {
            return;
        }
        Category category=new Category(RSC.getCurrentlySelectedLibrary(),categoryID);
        goToCategory(category);
    }
    
    /**
     * Updates the statusbar, argument indicates, whether or not the document
     * content should be analyzed.
     */
    public void updateStatusBar(final boolean pagenumber) {
        if (RSC.currentLibrary==-1) {
            jLStatusBar.setText("No Library loaded.");
            return;
        }
        jLStatusBar.setText(RSC.getCurrentlySelectedLibrary().getStatusString(pagenumber));
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

    @Override
    public void genericEventOccured(GenericCelsiusEvent e) {
        if (e.source==null) {
            //
        }
    }

    public void clearCategorySelection() {
        categoryTreePanel.clearTreeSelection();
    }
    
}

