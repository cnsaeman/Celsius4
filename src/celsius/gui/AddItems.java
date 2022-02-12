/*
 * Celsius v2
 *
 * DialogAdd.java
 *
 * Created on 19.03.2010, 18:19:12
 */
package celsius.gui;

import celsius.data.Library;
import celsius.data.BibTeXRecord;
import celsius.Resources;
import celsius.data.Item;
import celsius.data.TableRow;
import celsius.*;
import celsius.SwingWorkers.SWApplyPlugin;
import celsius.SwingWorkers.SWGetDetails;
import celsius.SwingWorkers.SWImportBibTeX;
import celsius.data.Attachment;
import celsius.data.DoubletteResult;
import celsius.data.LibraryChangeListener;
import celsius.tools.FFilter;
import atlantis.tools.FileTools;
import atlantis.gui.HasManagedStates;
import atlantis.tools.Parser;
import celsius.tools.Plugin;
import atlantis.tools.TextFile;
import celsius.tools.ToolBox;
import java.awt.BorderLayout;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.table.TableColumn;

/**
 *
 * @author cnsaeman
 */
public class AddItems extends javax.swing.JDialog implements HasManagedStates, LibraryChangeListener, GuiEventListener {

    private final String TI = "DIF>";             // Protocol header
    private final Library library;
    private ArrayList<String> standardKeys;
    private final ArrayList<Item> preparedItems;
    public final ArrayList<TableRow> addedItems;
    private final ArrayList<SWGetDetails> getDetailsSWs;
    private int currentEntry;
    public final Resources RSC;
    
    public final EditorPanel editorPanel;

    private boolean initializing;

    private int queuesize;

    private int importMode;

    public ThreadPoolExecutor TPE;
    public LinkedBlockingQueue<Runnable> LBQ;
    
    private Thread DoIt;

    private JList currentList;

    private boolean cancelAdding, autoDelete, autoReplace;

    /** Creates new form DialogAdd */
    public AddItems(Resources RSC) {
        super(RSC.MF, true);
        initializing = true;
        this.RSC = RSC;
        library = RSC.getCurrentlySelectedLibrary();
        library.addLibraryChangeListener(this);
        standardKeys=(new Item(library)).getEditableFields();
        setIconImage(RSC.celsiusIcon);
        preparedItems = new ArrayList<>();
        addedItems = new ArrayList<>();
        getDetailsSWs = new ArrayList<>();
        initComponents();
        editorPanel=new EditorPanel(RSC,false);
        editorPanel.addChangeListener(this);
        jPanel1.add(editorPanel,BorderLayout.CENTER);
        currentList=jLstFileList;
        jTPane.setTabComponentAt(0,new TabLabel("Add files in a folder as items",Resources.addFromFolderTabIcon,RSC,null,false));
        jTPane.setTabComponentAt(1,new TabLabel("Add single file as an item",Resources.addSingleFileTabIcon,RSC,null,false));
        jTPane.setTabComponentAt(2,new TabLabel("Add a manually entered item",Resources.addManualTabIcon,RSC,null,false));
        jTPane.setTabComponentAt(3,new TabLabel("Import items from BibTeX file",Resources.addImportTabIcon,RSC,null,false));
        
        RSC.guiStates.registerDirectlyEnabledComponent("addDialog","folder", new JComponent[] { jBtnView, jBtnDrop, jBtnDelete });
        RSC.guiStates.registerDirectlyEnabledComponent("addDialog","folder2", new JComponent[] { jBtnImpRec, jBtnImpDrop, jBtnImpDoub });
        setTitle("Add new items to active library " + library.name);
        jTFFolder.setText(RSC.getDir("toinclude"));
        jLPlugins.setModel(RSC.plugins.getPluginsDLM("manual-items",RSC.getCurrentlySelectedLibrary()));
        
        toInitState();
        LBQ=new LinkedBlockingQueue<Runnable>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
        DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        this.setSize((int)(0.8*dm.getWidth()), (int)(0.8*dm.getHeight()));
        RSC.adjustComponents(this.getComponents());
        jTABibTeX.setFont(new java.awt.Font("Monospaced", 0, RSC.guiScale(12)));
        GUIToolBox.centerDialog(this,RSC.MF);
        initializing = false;
        ThreadStatus.setText("No item selected. No threads running.");
    }

    private void toInitState() {
        currentEntry = -1;
        RSC.guiStates.adjustState("addDialog","folder",false);
        RSC.guiStates.adjustState("addDialog","folder2",false);
        jBtnAddRec.setEnabled(false);
        jBtnFindDoublettes.setEnabled(false);
        preparedItems.clear();
        jLstFileList.setModel(new DefaultListModel());
        jImpList.setModel(new DefaultListModel());
        jTFFileNameImp.setText("");
        jBtnImport.setEnabled(false);
        jTABibTeX.setText("");
        updateItemInformation();
    }

    private void viewItem() {
        Attachment attachment=preparedItems.get(jLstFileList.getSelectedIndex()).linkedAttachments.get(0);
        if (attachment==null) return;
        String fn = attachment.getFullPath();
        String ft = attachment.get("filetype");
        if ((fn != null) && (ft != null)) {
            RSC.configuration.view(ft, fn);
        }
        jLstFileList.grabFocus();
    }

    /*private Item createEntry() {
        Item item = new Item(library);
        for (String tag : library.itemPropertyKeys) {
            if (!tag.equals("addinfo") && !tag.equals("autoregistered") && !tag.equals("registered") && !tag.equals("id")) {
                item.put(tag, "");
                RSC.out(tag);
            }
        }
        if (library.config.get("standard-item-fields")!=null) 
            for (String tag : library.configToArray("standard-item-fields")) {
                item.put(tag, "");
                RSC.out(tag);
            }
        return(item);
    }*/

    private void createItemFromFile(String fileName) {
        String fileType = FileTools.getFileType(fileName);
        if (!RSC.configuration.isFileTypeSupported(fileType)) {
            return;
        }
        String sft = library.config.get("filetypes");
        if (!sft.equals("*")) {
            if (!Parser.listContains(sft, fileType)) {
                return;
            }
        }
        // create item
        Item item=new Item(library);
        // attach file to item
        Attachment attachment=new Attachment(library,item);
        attachment.put("name","Main File");
        attachment.put("path",fileName);
        attachment.put("filetype",fileType);
        attachment.attachToParent();
        attachment.order=0;
        preparedItems.add(item);
        if (jTPane.getSelectedIndex()==0) {
            if (fileName.length() > 52) {
                fileName = fileName.substring(0, 20) + "..." + fileName.substring(fileName.length() - 49);
            }
            ((DefaultListModel) jLstFileList.getModel()).addElement(fileName);
        }
    }

    private void addFilesFromFolder(String dir) {
        if ((new File(dir)).isDirectory()) {
            String[] fileList = (new File(dir)).list();
            java.util.Arrays.sort(fileList);
            for (String fileNameShort : fileList) {
                if ((new File(dir + ToolBox.filesep + fileNameShort)).isDirectory()) {
                    addFilesFromFolder(dir + ToolBox.filesep + fileNameShort);
                } else {
                    String fn=dir + ToolBox.filesep + fileNameShort;
                    boolean found=library.doesAttachmentExist(fn);
                    if (!found) {
                        try {
                            String tmp=RSC.configuration.correctFileType(fn);
                            if (!tmp.equals(fn))
                                (new File(fn)).renameTo(new File(tmp));
                            createItemFromFile(tmp);
                        } catch(Exception e) {
                            RSC.outEx(e);
                        }
                    }
                }
            }
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTPane = new javax.swing.JTabbedPane();
        jPanel2 = new javax.swing.JPanel();
        jPanel13 = new javax.swing.JPanel();
        jTFFolder = new javax.swing.JTextField();
        jBtnSelectFolder = new javax.swing.JButton();
        jBtnStart = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTFirstPage = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLstFileList = new javax.swing.JList();
        jPanel15 = new javax.swing.JPanel();
        jBtnAddRec = new javax.swing.JButton();
        jBtnView = new javax.swing.JButton();
        jBtnDrop = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jBtnFindDoublettes = new javax.swing.JButton();
        ThreadStatus = new javax.swing.JTextArea();
        jCBPlugins = new javax.swing.JCheckBox();
        jPanel3 = new javax.swing.JPanel();
        jPanel18 = new javax.swing.JPanel();
        jTFFile = new javax.swing.JTextField();
        jBtnSelectFile = new javax.swing.JButton();
        jBtnFileOK = new javax.swing.JButton();
        jBtnView2 = new javax.swing.JButton();
        jCBPlugins2 = new javax.swing.JCheckBox();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTAFileText = new javax.swing.JTextArea();
        jPanel5 = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jTABibTeX = new javax.swing.JTextArea();
        jPanel16 = new javax.swing.JPanel();
        jBtnCreateEmpty = new javax.swing.JButton();
        jBtnNormalize1 = new javax.swing.JButton();
        jCBAddProperty = new javax.swing.JComboBox();
        jBtnAdd1 = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jBtnCreateManualEntry = new javax.swing.JButton();
        jPanel10 = new javax.swing.JPanel();
        jBtnDoneBib = new javax.swing.JButton();
        jTFBarcode = new javax.swing.JTextField();
        jPanel6 = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jTFFileNameImp = new javax.swing.JTextField();
        jBtnChooseFile = new javax.swing.JButton();
        jBtnImpRec = new javax.swing.JButton();
        jBtnImpDrop = new javax.swing.JButton();
        jBtnImpDoub = new javax.swing.JButton();
        jBtnImport = new javax.swing.JButton();
        jScrollPane6 = new javax.swing.JScrollPane();
        jImpList = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        jPanel17 = new javax.swing.JPanel();
        jPanel4 = new javax.swing.JPanel();
        jBtnAdd = new javax.swing.JButton();
        jScrollPane4 = new javax.swing.JScrollPane();
        jLPlugins = new javax.swing.JList();
        jBtnNormalize = new javax.swing.JButton();
        jBtnApplyPlugin = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTFprereg = new javax.swing.JTextField();
        jBtnClrPreReg = new javax.swing.JButton();
        jBtnChooseCat = new javax.swing.JButton();
        jBtnDone = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridLayout(2, 1));

        jTPane.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                jTPaneStateChanged(evt);
            }
        });

        jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel2.setLayout(new java.awt.BorderLayout());

        jBtnSelectFolder.setText("Select Folder");
        jBtnSelectFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnSelectFolderActionPerformed(evt);
            }
        });

        jBtnStart.setMnemonic('s');
        jBtnStart.setText("Start");
        jBtnStart.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnStartActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTFFolder, javax.swing.GroupLayout.DEFAULT_SIZE, 957, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnSelectFolder)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnStart))
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel13Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTFFolder, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnSelectFolder)
                    .addComponent(jBtnStart))
                .addGap(5, 5, 5))
        );

        jPanel2.add(jPanel13, java.awt.BorderLayout.NORTH);

        jSplitPane1.setResizeWeight(0.5);
        jSplitPane1.setOneTouchExpandable(true);

        jTFirstPage.setColumns(20);
        jTFirstPage.setRows(5);
        jScrollPane3.setViewportView(jTFirstPage);

        jSplitPane1.setRightComponent(jScrollPane3);

        jLstFileList.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLstFileListMouseClicked(evt);
            }
        });
        jLstFileList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLstFileListValueChanged(evt);
            }
        });
        jScrollPane2.setViewportView(jLstFileList);

        jSplitPane1.setLeftComponent(jScrollPane2);

        javax.swing.GroupLayout jPanel14Layout = new javax.swing.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 1143, Short.MAX_VALUE)
            .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1143, Short.MAX_VALUE))
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 269, Short.MAX_VALUE)
            .addGroup(jPanel14Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel14Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 245, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        jPanel2.add(jPanel14, java.awt.BorderLayout.CENTER);

        jBtnAddRec.setText("Add recognized files");
        jBtnAddRec.setToolTipText("Add all documents with recognition=100 to the active library");
        jBtnAddRec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddRecActionPerformed(evt);
            }
        });

        jBtnView.setText("View");
        jBtnView.setToolTipText("Open selected document in external viewer");
        jBtnView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnViewActionPerformed(evt);
            }
        });

        jBtnDrop.setMnemonic('d');
        jBtnDrop.setText("Drop");
        jBtnDrop.setToolTipText("Drop selected document from list");
        jBtnDrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDropActionPerformed(evt);
            }
        });

        jBtnDelete.setText("Delete");
        jBtnDelete.setToolTipText("Delete selected document with all associated files");
        jBtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDeleteActionPerformed(evt);
            }
        });

        jBtnFindDoublettes.setText("Find doublettes");
        jBtnFindDoublettes.setPreferredSize(null);
        jBtnFindDoublettes.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnFindDoublettesActionPerformed(evt);
            }
        });

        ThreadStatus.setColumns(20);
        ThreadStatus.setFont(new java.awt.Font("Arial", 0, RSC.guiScale(11)));
        ThreadStatus.setRows(5);
        ThreadStatus.setMinimumSize(new java.awt.Dimension(0, 0));
        ThreadStatus.setPreferredSize(null);

        jCBPlugins.setSelected(true);
        jCBPlugins.setText("Auto-Plugins");

        javax.swing.GroupLayout jPanel15Layout = new javax.swing.GroupLayout(jPanel15);
        jPanel15.setLayout(jPanel15Layout);
        jPanel15Layout.setHorizontalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addComponent(jBtnAddRec)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnView)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnDrop)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnDelete)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnFindDoublettes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ThreadStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 542, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBPlugins))
        );
        jPanel15Layout.setVerticalGroup(
            jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel15Layout.createSequentialGroup()
                .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel15Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jBtnAddRec)
                        .addComponent(jBtnView)
                        .addComponent(jBtnDrop)
                        .addComponent(jBtnDelete)
                        .addComponent(jBtnFindDoublettes, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jCBPlugins))
                .addGap(0, 0, Short.MAX_VALUE))
            .addComponent(ThreadStatus, javax.swing.GroupLayout.DEFAULT_SIZE, 0, Short.MAX_VALUE)
        );

        jPanel2.add(jPanel15, java.awt.BorderLayout.SOUTH);

        jTPane.addTab("Add files in a folder as items", new javax.swing.ImageIcon(getClass().getResource("/celsius/images/folder.png")), jPanel2); // NOI18N

        jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel3.setLayout(new java.awt.BorderLayout());

        jBtnSelectFile.setText("Select File");
        jBtnSelectFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnSelectFileActionPerformed(evt);
            }
        });

        jBtnFileOK.setText("Read in file");
        jBtnFileOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnFileOKActionPerformed(evt);
            }
        });

        jBtnView2.setText("View");
        jBtnView2.setEnabled(false);
        jBtnView2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnView2ActionPerformed(evt);
            }
        });

        jCBPlugins2.setSelected(true);
        jCBPlugins2.setText("Auto-Plugins");

        javax.swing.GroupLayout jPanel18Layout = new javax.swing.GroupLayout(jPanel18);
        jPanel18.setLayout(jPanel18Layout);
        jPanel18Layout.setHorizontalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTFFile, javax.swing.GroupLayout.DEFAULT_SIZE, 744, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnSelectFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnFileOK)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnView2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBPlugins2)
                .addContainerGap())
        );
        jPanel18Layout.setVerticalGroup(
            jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel18Layout.createSequentialGroup()
                .addGap(5, 5, 5)
                .addGroup(jPanel18Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCBPlugins2)
                    .addComponent(jBtnView2)
                    .addComponent(jBtnFileOK)
                    .addComponent(jBtnSelectFile)
                    .addComponent(jTFFile, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel3.add(jPanel18, java.awt.BorderLayout.NORTH);

        jTAFileText.setColumns(20);
        jTAFileText.setRows(5);
        jScrollPane5.setViewportView(jTAFileText);

        jPanel3.add(jScrollPane5, java.awt.BorderLayout.CENTER);

        jTPane.addTab("Add a single file as an item", jPanel3);

        jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel5.setLayout(new java.awt.BorderLayout());

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("BibTeX"));
        jPanel7.setLayout(new java.awt.BorderLayout());

        jTABibTeX.setColumns(20);
        jTABibTeX.setFont(new java.awt.Font("Monospaced", 0, RSC.guiScale(12)));
        jTABibTeX.setRows(5);
        jScrollPane7.setViewportView(jTABibTeX);

        jPanel7.add(jScrollPane7, java.awt.BorderLayout.CENTER);

        jPanel16.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 0, 5, 0));

        jBtnCreateEmpty.setText("Create empty BibTeX-record");
        jBtnCreateEmpty.setPreferredSize(null);
        jBtnCreateEmpty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCreateEmptyActionPerformed(evt);
            }
        });

        jBtnNormalize1.setText("Normalize");
        jBtnNormalize1.setPreferredSize(null);
        jBtnNormalize1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnNormalize1ActionPerformed(evt);
            }
        });

        jCBAddProperty.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "add property", "author", "title", "journal", "volume", "year", "pages", "eprint", "note", "doi" }));
        jCBAddProperty.setPreferredSize(new java.awt.Dimension(RSC.guiScale(101),23));
        jCBAddProperty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBAddPropertyActionPerformed(evt);
            }
        });

        jBtnAdd1.setText("Done editing");
        jBtnAdd1.setPreferredSize(null);
        jBtnAdd1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAdd1ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel16Layout = new javax.swing.GroupLayout(jPanel16);
        jPanel16.setLayout(jPanel16Layout);
        jPanel16Layout.setHorizontalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createSequentialGroup()
                .addComponent(jBtnCreateEmpty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnNormalize1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jCBAddProperty, 0, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnAdd1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(618, 618, 618))
        );
        jPanel16Layout.setVerticalGroup(
            jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel16Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                .addComponent(jBtnCreateEmpty, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jBtnNormalize1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jCBAddProperty, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addComponent(jBtnAdd1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        jPanel7.add(jPanel16, java.awt.BorderLayout.SOUTH);

        jPanel5.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel8.setLayout(new java.awt.BorderLayout());

        jPanel9.setBorder(javax.swing.BorderFactory.createTitledBorder(null, "Empty Item", javax.swing.border.TitledBorder.LEFT, javax.swing.border.TitledBorder.DEFAULT_POSITION));

        jBtnCreateManualEntry.setText("Create Empty Record");
        jBtnCreateManualEntry.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCreateManualEntryActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel9Layout = new javax.swing.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jBtnCreateManualEntry)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel9Layout.createSequentialGroup()
                .addComponent(jBtnCreateManualEntry)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.add(jPanel9, java.awt.BorderLayout.WEST);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Barcode"));

        jBtnDoneBib.setText("Apply Barcode Plugin");
        jBtnDoneBib.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDoneBibActionPerformed(evt);
            }
        });

        jTFBarcode.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyTyped(java.awt.event.KeyEvent evt) {
                jTFBarcodeKeyTyped(evt);
            }
        });

        javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTFBarcode, javax.swing.GroupLayout.DEFAULT_SIZE, 755, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jBtnDoneBib)
                .addContainerGap())
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel10Layout.createSequentialGroup()
                .addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtnDoneBib)
                    .addComponent(jTFBarcode, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel8.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel5.add(jPanel8, java.awt.BorderLayout.SOUTH);

        jTPane.addTab("Add item without file", jPanel5);

        jPanel6.setLayout(new java.awt.BorderLayout());

        jTFFileNameImp.setEnabled(false);

        jBtnChooseFile.setText("Select file");
        jBtnChooseFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnChooseFileActionPerformed(evt);
            }
        });

        jBtnImpRec.setText("Import list");
        jBtnImpRec.setEnabled(false);
        jBtnImpRec.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnImpRecActionPerformed(evt);
            }
        });

        jBtnImpDrop.setText("Drop current");
        jBtnImpDrop.setEnabled(false);
        jBtnImpDrop.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnImpDropActionPerformed(evt);
            }
        });

        jBtnImpDoub.setText("Remove doublettes");
        jBtnImpDoub.setEnabled(false);
        jBtnImpDoub.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnImpDoubActionPerformed(evt);
            }
        });

        jBtnImport.setText("Read in file");
        jBtnImport.setEnabled(false);
        jBtnImport.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnImportActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jTFFileNameImp, javax.swing.GroupLayout.DEFAULT_SIZE, 555, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnChooseFile)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnImport)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnImpRec)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnImpDrop, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnImpDoub)
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtnImpDoub)
                    .addComponent(jBtnImpDrop)
                    .addComponent(jBtnImpRec)
                    .addComponent(jBtnImport)
                    .addComponent(jBtnChooseFile)
                    .addComponent(jTFFileNameImp, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel6.add(jPanel11, java.awt.BorderLayout.NORTH);

        jImpList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jImpListValueChanged(evt);
            }
        });
        jScrollPane6.setViewportView(jImpList);

        jPanel6.add(jScrollPane6, java.awt.BorderLayout.CENTER);

        jTPane.addTab("Import items from BibTeX file", jPanel6);

        getContentPane().add(jTPane);
        jTPane.getAccessibleContext().setAccessibleName("");

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel17.setPreferredSize(new java.awt.Dimension(RSC.guiScale(350),RSC.guiScale(490)));

        jBtnAdd.setMnemonic('a');
        jBtnAdd.setText("Add Item");
        jBtnAdd.setToolTipText("Add the currently selected document to the active library");
        jBtnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddActionPerformed(evt);
            }
        });

        jLPlugins.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLPluginsValueChanged(evt);
            }
        });
        jScrollPane4.setViewportView(jLPlugins);

        jBtnNormalize.setMnemonic('n');
        jBtnNormalize.setText("Normalize title");
        jBtnNormalize.setToolTipText("Correct upper/lowercase for title");
        jBtnNormalize.setPreferredSize(new java.awt.Dimension(RSC.guiScale(92),RSC.guiScale(24)));
        jBtnNormalize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnNormalizeActionPerformed(evt);
            }
        });

        jBtnApplyPlugin.setMnemonic('p');
        jBtnApplyPlugin.setText("Apply plugin");
        jBtnApplyPlugin.setToolTipText("Apply the selected plugin to the currently selected document");
        jBtnApplyPlugin.setEnabled(false);
        jBtnApplyPlugin.setPreferredSize(new java.awt.Dimension(RSC.guiScale(104),RSC.guiScale(24)));
        jBtnApplyPlugin.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnApplyPluginActionPerformed(evt);
            }
        });

        jLabel3.setText("Add items to the following categories:");

        jBtnClrPreReg.setIcon(new javax.swing.ImageIcon(getClass().getResource("/celsius/images/closebtn.png"))); // NOI18N
        jBtnClrPreReg.setBorderPainted(false);
        jBtnClrPreReg.setContentAreaFilled(false);
        jBtnClrPreReg.setMargin(new java.awt.Insets(2, 2, 2, 2));
        jBtnClrPreReg.setPreferredSize(new java.awt.Dimension(16, 17));
        jBtnClrPreReg.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnClrPreRegActionPerformed(evt);
            }
        });

        jBtnChooseCat.setText("Choose");
        jBtnChooseCat.setPreferredSize(new java.awt.Dimension(RSC.guiScale(78),RSC.guiScale(24)));
        jBtnChooseCat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnChooseCatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                .addComponent(jTFprereg)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnClrPreReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jBtnChooseCat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(jBtnNormalize, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jBtnAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane4, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jBtnApplyPlugin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addComponent(jLabel3)
                .addGap(38, 38, 38))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel4Layout.createSequentialGroup()
                        .addComponent(jScrollPane4)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnApplyPlugin, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel4Layout.createSequentialGroup()
                        .addComponent(jBtnAdd, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnNormalize, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(152, 152, 152)))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jBtnClrPreReg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jBtnChooseCat, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jTFprereg, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(27, 27, 27))
        );

        jBtnDone.setMnemonic('d');
        jBtnDone.setText("Done");
        jBtnDone.setToolTipText("End dialog");
        jBtnDone.setMaximumSize(new java.awt.Dimension(640, 240));
        jBtnDone.setPreferredSize(new java.awt.Dimension(RSC.guiScale(64),RSC.guiScale(24)));
        jBtnDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDoneActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel17Layout = new javax.swing.GroupLayout(jPanel17);
        jPanel17.setLayout(jPanel17Layout);
        jPanel17Layout.setHorizontalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addContainerGap(348, Short.MAX_VALUE)
                .addComponent(jBtnDone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel17Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        jPanel17Layout.setVerticalGroup(
            jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel17Layout.createSequentialGroup()
                .addContainerGap(382, Short.MAX_VALUE)
                .addComponent(jBtnDone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
            .addGroup(jPanel17Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(jPanel17Layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jPanel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGap(44, 44, 44)))
        );

        jPanel1.add(jPanel17, java.awt.BorderLayout.EAST);

        getContentPane().add(jPanel1);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnSelectFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnSelectFolderActionPerformed
        String folderName=RSC.selectFolder("Select the directory in which the files are located.", "toinclude");
        if (folderName!=null) {
            jTFFolder.setText(folderName);
        }
    }//GEN-LAST:event_jBtnSelectFolderActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        deleteEntry(preparedItems.get(currentEntry),false);
}//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jBtnViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnViewActionPerformed
        viewItem();
}//GEN-LAST:event_jBtnViewActionPerformed

    private void jBtnFindDoublettesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnFindDoublettesActionPerformed
        eliminateDoublettes();
}//GEN-LAST:event_jBtnFindDoublettesActionPerformed

    private void jBtnDropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDropActionPerformed
        removeFromTable(preparedItems.get(currentEntry));
    }//GEN-LAST:event_jBtnDropActionPerformed

    private void jBtnAddRecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddRecActionPerformed
        // TODO redo as SwingWorker
        
        jBtnAdd.setEnabled(false);
        jBtnAddRec.setEnabled(false);
        final ArrayList<Item> items2=cloneEntries();
        final ProgressMonitor progressMonitor = new ProgressMonitor(this, "Adding fully recognized items ...", "", 0, 0);
        progressMonitor.setMillisToDecideToPopup(0);
        progressMonitor.setMillisToPopup(0);
        final AddItems DA=this;
        cancelAdding=false;
        autoDelete=false;
        autoReplace=false;
        DoIt=(new Thread() {
            @Override
            public void run() {
                int c = 0;
                progressMonitor.setMaximum(items2.size());
                int no = 0;
                for (Item item : items2) {
                    if (item.getS("recognition").equals("100")) {
                        addItem(item);
                        c++;
                    }
                    progressMonitor.setProgress(no);
                    if (cancelAdding) break;
                    no++;
                }
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()  {
                        progressMonitor.close();
                        DA.adjustJBtnAdd();
                        DA.jBtnAddRec.setEnabled(true);
                    }
                });
            }
        });
        DoIt.start();
}//GEN-LAST:event_jBtnAddRecActionPerformed

    private void jBtnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddActionPerformed
        jBtnAdd.setEnabled(false);
        jBtnAddRec.setEnabled(false);
        final ProgressMonitor progressMonitor = new ProgressMonitor(this, "Adding item ...", "", 0, 1);
        progressMonitor.setMillisToDecideToPopup(0);
        progressMonitor.setMillisToPopup(0);
        final AddItems DA=this;
        cancelAdding=false;
        autoDelete=false;
        autoReplace=false;
        DoIt=(new Thread() {
            @Override
            public void run() {
                addItem(preparedItems.get(currentEntry));
                progressMonitor.setProgress(1);
                DA.adjustJBtnAdd();
                DA.jBtnAddRec.setEnabled(true);
            }
        });
        DoIt.start();
}//GEN-LAST:event_jBtnAddActionPerformed

    private void jBtnNormalizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnNormalizeActionPerformed
        String title = Parser.lowerEndOfWords(preparedItems.get(currentEntry).get("title"));
        title = title.replace('\n', ' ');
        title = title.replace("  ", " ");
        preparedItems.get(currentEntry).put("title", title);
        updateItemInformation();
}//GEN-LAST:event_jBtnNormalizeActionPerformed

    private void jBtnDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDoneActionPerformed
        RSC.emptyThreadPoolExecutor();
        RSC.guiStates.unregister("addDialog");
        this.setVisible(false);
        this.dispose();
}//GEN-LAST:event_jBtnDoneActionPerformed

    private void jLPluginsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLPluginsValueChanged
        if (jLPlugins.isSelectionEmpty()) {
            jBtnApplyPlugin.setEnabled(false);
        } else {
            jBtnApplyPlugin.setEnabled(true);
        }
}//GEN-LAST:event_jLPluginsValueChanged

    private void jBtnApplyPluginActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnApplyPluginActionPerformed
        jBtnApplyPlugin.setEnabled(false);
        Item item=preparedItems.get(currentEntry);
        String pluginTitle=(String)jLPlugins.getSelectedValue();
        Plugin plugin=RSC.plugins.get(pluginTitle);
        try {
            SWApplyPlugin swAP = new SWApplyPlugin(library, RSC, null, plugin, "", item);
            swAP.execute();
            swAP.get();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        jBtnApplyPlugin.setEnabled(true);
        updateItemInformation();
}//GEN-LAST:event_jBtnApplyPluginActionPerformed

    private void jBtnStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnStartActionPerformed
        // adjust gui
        preparedItems.clear();
        getDetailsSWs.clear();
        jLstFileList.setModel(new DefaultListModel());

        currentEntry = -1;
        updateItemInformation();
        jBtnFindDoublettes.setEnabled(false);

        // look for all available files
        String dir = jTFFolder.getText();
        if (dir.endsWith(ToolBox.filesep)) dir=dir.substring(0,dir.length()-1);
        String[] flist = (new File(dir)).list();
        if (flist==null) {
            RSC.showWarning("The selected folder is empty.", "Warning");
            return;
        }
        String msg = "";
        for (int i = 0; (i < flist.length); i++) {
            if ((new File(dir + "/" + flist[i])).isDirectory()) {
                msg += flist[i] + "\n";
                if (Parser.howOftenContains(msg, "\n") > 10) {
                    msg += "... and others.\n";
                    i = flist.length;
                }
            }
        }
        if (msg.length() != 0) {
            int i = RSC.askQuestionYN("This folder contains the following subfolders, which will also be scanned:\n" + msg + "Are you sure about this?", "Warning:");
            if (i == JOptionPane.NO_OPTION) {
                return;
            }
        }
        // recursively add files
        addFilesFromFolder(dir);
        if (preparedItems.isEmpty()) {
            RSC.showWarning("There are no files of supported type in the selected folder.", "Warning");
            return;
        }
        jBtnAddRec.setEnabled(true);
        jBtnFindDoublettes.setEnabled(true);
        for(Item item : preparedItems) {
            SWGetDetails swGD=new SWGetDetails(RSC,item,jCBPlugins.isSelected(),this);
            getDetailsSWs.add(swGD);
            TPE.execute(swGD);
        }
        updateItemInformation();
    }//GEN-LAST:event_jBtnStartActionPerformed

    private void jLstFileListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLstFileListValueChanged
        if (evt.getValueIsAdjusting()) {
            return;
        }
        currentEntry = jLstFileList.getSelectedIndex();
        RSC.guiStates.adjustState("addDialog","folder", currentEntry !=-1);
        updateItemInformation();
    }//GEN-LAST:event_jLstFileListValueChanged

    private void jLstFileListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLstFileListMouseClicked
        if (evt.getClickCount() == 2) {
            viewItem();
        }
    }//GEN-LAST:event_jLstFileListMouseClicked

    private void jTPaneStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_jTPaneStateChanged
        if (initializing) {
            return; // if still initializing
        }
        toInitState();
        if (jTPane.getSelectedIndex()==0) currentList=jLstFileList;
        else {
            currentList=jImpList;
        }
    }//GEN-LAST:event_jTPaneStateChanged

    private void jBtnAdd1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAdd1ActionPerformed
        BibTeXRecord bibtex = new BibTeXRecord(jTABibTeX.getText());
        if (bibtex.parseError == 0) {
            Item item = new Item(library); //createEntry();
            String bibtitle=bibtex.get("title");
            if (bibtitle.startsWith("{")) {
                bibtitle = bibtitle.substring(1, bibtitle.length() - 1);
            }
            item.put("title", bibtitle);
            item.put("authors", BibTeXRecord.convertBibTeXAuthorsToCelsius(bibtex.get("author")));
            item.put("citation-tag",bibtex.getTag());
            item.put("identifier",bibtex.getIdentifier());
            if (bibtex.get("journal")!=null) {
                item.put("type","Paper");
            }
            item.put("bibtex",bibtex.toString());
            preparedItems.add(item);
            currentEntry = 0;
            updateItemInformation();
        } else {
            RSC.showWarning("The BibTeX record is inconsistent:\n" + BibTeXRecord.status[bibtex.parseError], "Warning:");
        }
}//GEN-LAST:event_jBtnAdd1ActionPerformed

    private void jBtnCreateEmptyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCreateEmptyActionPerformed
        jTABibTeX.setText((new BibTeXRecord()).toString());
}//GEN-LAST:event_jBtnCreateEmptyActionPerformed

    private void jBtnNormalize1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnNormalize1ActionPerformed
        String tmp = BibTeXRecord.normalizeBibTeX(jTABibTeX.getText());
        if (!tmp.startsWith("@")) {
            RSC.showWarning(tmp, "BibTeX Error");
        } else {
            jTABibTeX.setText(tmp);
            jTABibTeX.setCaretPosition(0);
        }
}//GEN-LAST:event_jBtnNormalize1ActionPerformed

    private void jBtnClrPreRegActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnClrPreRegActionPerformed
        jTFprereg.setText("");
}//GEN-LAST:event_jBtnClrPreRegActionPerformed

    private void jBtnChooseCatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnChooseCatActionPerformed
        ChooseItemCategory DCC=new ChooseItemCategory(RSC);
        DCC.setVisible(true);
        if (DCC.selected) {
            String tmp=jTFprereg.getText();
            if (tmp.length()!=0) tmp+="|";
            jTFprereg.setText(tmp+DCC.category);
        }
}//GEN-LAST:event_jBtnChooseCatActionPerformed

    private void jTFBarcodeKeyTyped(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTFBarcodeKeyTyped
        if (evt.getKeyChar() == KeyEvent.VK_ENTER)
            applyBarCode();
}//GEN-LAST:event_jTFBarcodeKeyTyped

    private void jBtnDoneBibActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDoneBibActionPerformed
        applyBarCode();
}//GEN-LAST:event_jBtnDoneBibActionPerformed

    private void jBtnCreateManualEntryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCreateManualEntryActionPerformed
        Item item=new Item(library); //createEntry();
        preparedItems.clear();
        preparedItems.add(item);
        currentEntry=0;
        updateItemInformation();
    }//GEN-LAST:event_jBtnCreateManualEntryActionPerformed

    private void jBtnSelectFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnSelectFileActionPerformed
        String filename=RSC.selectFile("Select the file which you want to add.", "addsingledoc", "_ALL", "All Files");
        if (filename!=null) {
            jTFFile.setText(filename);
        }
    }//GEN-LAST:event_jBtnSelectFileActionPerformed

    private void jBtnFileOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnFileOKActionPerformed
        /* TODO String fn=jTFFile.getText();
        if (!(new File(fn)).exists()) return;
        boolean found=false;
        for (Item item : library)
            if (item.getCompleteDirS("location").equals(fn)) {
                found=true;
                break;
            }
        if (found) {
            RSC.showWarning("This file is already contained in the current library!", "Reading file cancelled...");
            return;
        }
        entries.clear();
        GetDetailsThreads.clear();
        try {
            String tmp=RSC.Configuration.correctFileType(fn);
            if (!tmp.equals(fn))
                (new File(fn)).renameTo(new File(tmp));
            createItemFromFile(tmp);
        } catch(Exception e) {
            RSC.outEx(e);
            return;
        }
        if (entries.isEmpty()) {
            RSC.showWarning("This filetype is not supported.", "Warning:");
            return;
        }
        currentEntry=0;
        ThreadGetDetails TGD=new ThreadGetDetails(entries.get(currentEntry),RSC,jCBPlugins2.isSelected());
        TGD.start();
        try {
            TGD.join();
        } catch (InterruptedException ex) {
            RSC.outEx(ex);
        }
        jTAFileText.setText(ToolBox.getFirstPage(entries.get(currentEntry)));
        jTAFileText.setCaretPosition(0);
        updateItemInformation();
        jBtnView2.setEnabled(true);*/
    }//GEN-LAST:event_jBtnFileOKActionPerformed

    private void jCBAddPropertyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBAddPropertyActionPerformed
        String val=(String)jCBAddProperty.getSelectedItem();
        if (!val.equals("add property")) {
            String bib=jTABibTeX.getText();
            if (bib.length()==0) {
                RSC.showWarning("Please create a BibTeX entry first with the \"Create\" button.", "Cancelled...");
            } else {
                int i=Parser.cutFrom(bib,"\n").trim().indexOf("=");
                String tmp=Parser.cutUntilLast(Parser.cutUntilLast(bib,"}"),"\n");
                if (val.length()<i) val=val+("                ").substring(0,i-val.length());
                tmp+=",\n   "+val+"= \"\"";
                tmp+="\n}";
                jTABibTeX.setText(tmp);
                jCBAddProperty.setSelectedIndex(0);
            }
        }
}//GEN-LAST:event_jCBAddPropertyActionPerformed

    private void jBtnView2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnView2ActionPerformed
        viewItem();
    }//GEN-LAST:event_jBtnView2ActionPerformed

    private void jBtnChooseFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnChooseFileActionPerformed
        importMode=0;
        String imptype="bib";
        String impname="BibTeX-file";
        String filename=RSC.selectFile("Indicate the source of the "+impname,"import"+imptype,"."+imptype, impname+"s");
        if (filename!=null) {
            jTFFileNameImp.setText(filename);
            jBtnImport.setEnabled(true);
        }        
    }//GEN-LAST:event_jBtnChooseFileActionPerformed

    private void jBtnImportActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnImportActionPerformed
        preparedItems.clear();
        jImpList.setModel(new DefaultListModel());
        try {
            SWImportBibTeX swIB = new SWImportBibTeX(RSC, this, jTFFileNameImp.getText(), library);
            swIB.execute();
            swIB.get();
        } catch(Exception e) {
            RSC.outEx(e);
        }
        jBtnImpRec.setEnabled(true);
        jBtnImpDoub.setEnabled(true);
    }//GEN-LAST:event_jBtnImportActionPerformed

    private void jImpListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jImpListValueChanged
        if (evt.getValueIsAdjusting()) {
            return;
        }
        currentEntry = jImpList.getSelectedIndex();
        RSC.guiStates.adjustState("addDialog","folder2",currentEntry !=-1);
        updateItemInformation();
    }//GEN-LAST:event_jImpListValueChanged

    private void jBtnImpDropActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnImpDropActionPerformed
        removeFromTable(preparedItems.get(currentEntry));
    }//GEN-LAST:event_jBtnImpDropActionPerformed

    private void jBtnImpRecActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnImpRecActionPerformed
        // TODO redo as SwingWorker
        
        jBtnAdd.setEnabled(false);
        jBtnAddRec.setEnabled(false);
        final ArrayList<Item> items2=cloneEntries();
        final ProgressMonitor progressMonitor = new ProgressMonitor(this, "Adding fully recognized items ...", "", 0, 0);
        progressMonitor.setMillisToDecideToPopup(0);
        progressMonitor.setMillisToPopup(0);
        final AddItems DA=this;
        cancelAdding=false;
        autoDelete=false;
        autoReplace=false;
        DoIt=(new Thread() {
            @Override
            public void run() {
                int c = 0;
                progressMonitor.setMaximum(items2.size());
                for (Item item : items2) {
                    addItem(item);
                    c++;
                    progressMonitor.setProgress(c);
                    if (cancelAdding) break;
                }
                java.awt.EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run()  {
                        DA.adjustJBtnAdd();
                        DA.jBtnAddRec.setEnabled(true);
                    }
                });
            }
        });
        DoIt.start();
    }//GEN-LAST:event_jBtnImpRecActionPerformed

    private void jBtnImpDoubActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnImpDoubActionPerformed
        eliminateDoublettes2();
    }//GEN-LAST:event_jBtnImpDoubActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTextArea ThreadStatus;
    public javax.swing.JButton jBtnAdd;
    private javax.swing.JButton jBtnAdd1;
    private javax.swing.JButton jBtnAddRec;
    private javax.swing.JButton jBtnApplyPlugin;
    private javax.swing.JButton jBtnChooseCat;
    private javax.swing.JButton jBtnChooseFile;
    private javax.swing.JButton jBtnClrPreReg;
    private javax.swing.JButton jBtnCreateEmpty;
    private javax.swing.JButton jBtnCreateManualEntry;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnDone;
    private javax.swing.JButton jBtnDoneBib;
    private javax.swing.JButton jBtnDrop;
    private javax.swing.JButton jBtnFileOK;
    private javax.swing.JButton jBtnFindDoublettes;
    private javax.swing.JButton jBtnImpDoub;
    private javax.swing.JButton jBtnImpDrop;
    private javax.swing.JButton jBtnImpRec;
    private javax.swing.JButton jBtnImport;
    private javax.swing.JButton jBtnNormalize;
    private javax.swing.JButton jBtnNormalize1;
    private javax.swing.JButton jBtnSelectFile;
    private javax.swing.JButton jBtnSelectFolder;
    private javax.swing.JButton jBtnStart;
    private javax.swing.JButton jBtnView;
    private javax.swing.JButton jBtnView2;
    private javax.swing.JComboBox jCBAddProperty;
    private javax.swing.JCheckBox jCBPlugins;
    private javax.swing.JCheckBox jCBPlugins2;
    private javax.swing.JList jImpList;
    private javax.swing.JList jLPlugins;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JList jLstFileList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel15;
    private javax.swing.JPanel jPanel16;
    private javax.swing.JPanel jPanel17;
    private javax.swing.JPanel jPanel18;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTextArea jTABibTeX;
    private javax.swing.JTextArea jTAFileText;
    private javax.swing.JTextField jTFBarcode;
    private javax.swing.JTextField jTFFile;
    private javax.swing.JTextField jTFFileNameImp;
    private javax.swing.JTextField jTFFolder;
    private javax.swing.JTextArea jTFirstPage;
    public javax.swing.JTextField jTFprereg;
    private javax.swing.JTabbedPane jTPane;
    // End of variables declaration//GEN-END:variables

    public void eliminateDoublettes() {
        boolean always=false;
        ArrayList<Item> toDelete=new ArrayList<>();
        for (Item item : preparedItems) {
            try {
                DoubletteResult doubletteResult=library.isDoublette(item);
                if (doubletteResult.type == 10) {
                    if (always) {
                        toDelete.add(item);
                    } else {
                        int j = RSC.askQuestionABCD("Exact doublette found in current library.\nItem in library: " + doubletteResult.item.toText(false) + "\nDelete the file " + item.linkedAttachments.get(0).get("path") + "?", "Warning","Delete all exact doublettes","Delete this one","No","Cancel");
                        if (j == 0) {
                            always=true;
                            toDelete.add(item);
                        }
                        if (j == 1) {
                            toDelete.add(item);
                        }
                        if (j == 3)  {
                            return;
                        }
                    }
                }
                if (doubletteResult.type == 100) {
                    int j = RSC.askQuestionYNC("Item with overlapping unique fields found in library:\n"+doubletteResult.item.toText(false)+"\nDelete the file " + item.linkedAttachments.get(0).get("path") + "?", "Confirm");
                    if (j == JOptionPane.YES_OPTION) toDelete.add(item);
                    if (j == JOptionPane.CANCEL_OPTION) return;
                }
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
        for (Item item : toDelete) {
            item.deleteFilesOfAttachments();
            removeFromTable(item);
        }
        RSC.showInformation("Information","Doublette search complete.");        
    }

    public void eliminateDoublettes2() {
        boolean always=false;
        ArrayList<Item> toDelete=new ArrayList<>();
        for (Item item : preparedItems) {
            try {
                DoubletteResult doubletteResult=library.isDoublette(item);
                if ((doubletteResult.type == 10) || (doubletteResult.type==100)) {
                    if (always) {
                        toDelete.add(item);
                    } else {
                        int j = RSC.askQuestionABCD("Exact doublette found in current library.\nItem in library: " + doubletteResult.item.toText(false) + "\nDelete the item " + item.toText(false) + "?", "Confirm","Delete all exact doublettes","Delete this one","No","Cancel");
                        if (j == 0) {
                            always=true;
                            toDelete.add(item);
                        }
                        if (j == 1) {
                            toDelete.add(item);
                        }
                        if (j == 3)  {
                            return;
                        }
                    }
                }
            } catch (Exception e) {
                RSC.outEx(e);
            }
        }
        for (Item item : toDelete) {
            item.deleteFilesOfAttachments();
            removeFromTable(item);
        }
        RSC.showInformation("Information","Doublette search complete.");        
    }
    
    public void updateItemInformation() {
        queuesize=0;
        if (LBQ!=null) queuesize+=LBQ.size();
        if (TPE!=null) queuesize+=TPE.getActiveCount();
        if (currentEntry == -1) {
            jBtnAdd.setEnabled(false);
            editorPanel.setEditable(null);
            ThreadStatus.setText("No item selected.Threads running: " + String.valueOf(queuesize));
        } else {
            if (jTPane.getSelectedIndex()==0) {
                if (getDetailsSWs.get(currentEntry).state==0) {
                    ThreadStatus.setText("Details obtained for current item. Threads running: "+String.valueOf(queuesize));
                } else {
                    ThreadStatus.setText("Getting details for current item. Threads running: "+String.valueOf(queuesize));
                }
            }
            // rest
            adjustJBtnAdd();
            Item item = preparedItems.get(currentEntry);
            editorPanel.setEditable(item);
            String firstPage="";
            if (item.linkedAttachments.size()>0) {
                firstPage=ToolBox.getFirstPage(item.linkedAttachments.get(0).get("$plaintext"));
            }
            jTFirstPage.setText(firstPage.replaceAll("^[^\\P{C}]", "?"));
            jTFirstPage.setCaretPosition(0);
        }
    }

    private void toNext(int i) {
        i++;
        if (i + 2 > currentList.getModel().getSize()) {
            i = currentList.getModel().getSize() - 1;
        }
        currentList.grabFocus();
        currentList.setSelectedIndex(i);
        currentList.ensureIndexIsVisible(i);
    }

    private void applyBarCode() {
        /* TODO MProperties entry=createEntry();
        entries.clear();
        entries.add(entry);
        currentEntry=0;
        entry.put("barcode",jTFBarcode.getText().trim());
        entry.put("title",jTFBarcode.getText().trim());
        updateItemInformation();
        final ThreadApplyPlugin TAP=new ThreadApplyPlugin(null,RSC.plugins.get("Look at Amazon"),RSC.plugins.parameters.get("Look at Amazon"),RSC, entries.get(currentEntry),false,true);
        SwingWorker worker = new SwingWorker<Object, Object>() { //#####
            @Override
            protected Object doInBackground() {
                TAP.start();
                try {
                    TAP.join();
                } catch (InterruptedException ex) {
                    RSC.outEx(ex);
                }
                return(null);
            }

            @Override
            protected void done() {
               try {
                updateItemInformation();
               } catch (Exception ignore) {
               }
            }

        };
        worker.execute();*/
    }

    private void addItem(final Item item) {
        try {
            RSC.out("Adding Item");
            if (!item.getS("$$beingadded").equals("")) return;
            item.put("$$beingadded","true");
            final Integer[] res=new Integer[1];
            RSC.out("checking for doublettes");
            DoubletteResult dr=library.isDoublette(item);
            if ((dr.type==100) || (dr.type==4)){
                if (!autoReplace) {
                    Object[] options=new Object[6];
                    options[0]="Delete"; options[1]="Replace"; options[2]="New Version"; options[3]="Replace All"; options[4]="Ignore"; options[5]="Cancel";
                    String msg="The item \n"+library.itemRepresentation.fillIn(item,true)+
                                               "\nalready exists in the library as "+library.itemRepresentation.fillIn(item,true)+". You can\n"+
                                               "- Delete the item in the inclusion folder\n"+
                                               "- Replace the item in the library by this item\n"+
                                               "- Replace the item but treat its current file as an additional version\n"+
                                               "- Replace all items \n"+
                                               "- Ignore\n"+
                                               "- Cancel";
                    res[0]=JOptionPane.showOptionDialog(null, msg, "Warning",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null, options, options[4]);
                } else res[0]=1;
                if (res[0]==0) {
                    // Delete item in folder 
                    deleteEntry(item,false);
                    return;
                }
                if (res[0]==3) autoReplace=true;
                if ((res[0]==1) || (res[0]==3)) {
                    Item oldItem=dr.item;
                    oldItem.replaceData(item);
                    oldItem.replaceAttachment(item);
                    library.itemChanged(oldItem.id);
                    addedItems.add(oldItem);
                    FileTools.deleteIfExists(item.get("$plaintxt"));
                    removeFromTable(item);
                    return;
                }
                if (res[0]==2) {
                    Item oldItem=dr.item;
                    oldItem.replaceData(item);
                    oldItem.insertAsFirstAttachment(item);
                    library.itemChanged(oldItem.id);
                    addedItems.add(oldItem);
                    FileTools.deleteIfExists(item.get("$plaintxt"));
                    removeFromTable(item);
                    return;
                }
                if (res[0]==5) cancelAdding=true;
                return;
            }
            if (dr.type==10) {
                if (!autoDelete) {
                    res[0]=RSC.askQuestionABCD("An exact copy of the item\n"+library.itemRepresentation.fillIn(item,true)+"\nis already existing in the library:\n"+dr.item.toText(false)+" with id: "+dr.item.id+"\nDelete the file "+item.get("location")+"?","Warning","Yes","No","Always","Cancel");
                } else res[0]=0;
                item.put("$$beingadded", null);
                if (res[0]==0) {
                    deleteEntry(item,false);
                }
                if (res[0]==2) {
                    deleteEntry(item,false);
                    autoDelete=true;
                }
                if (res[0]==3) {
                    cancelAdding=true;
                }
                return;
            }
            if (dr.type==5) {
                res[0]=RSC.askQuestionYN("A file with exactly the same length as the item\n"+library.itemRepresentation.fillIn(item,true)+"\nis already existing in the library.\nProceed anyway?","Warning");
                if (res[0]==JOptionPane.NO_OPTION) {
                    return;
                }
            }
            String prereg=jTFprereg.getText();
            // TODO make preregistration work
            int i = library.addNewItem(item);
            if (i == 0) {
                jBtnAdd.setEnabled(false);
                editorPanel.setEditable(null);
                jTAFileText.setText("");
                jTFFile.setText("");
                addedItems.add(item);
                FileTools.deleteIfExists(item.get("$plaintxt"));
                removeFromTable(item);
                jBtnView2.setEnabled(false);
            } else {
               item.put("$$beingadded",null);
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }
    }

    private void deleteEntry(Item item, boolean confirmed) {
        if (item==null) return;
        if (item.linkedAttachments.size()>0) {
            Attachment attachment = item.linkedAttachments.get(0);
            if (!confirmed) {
                confirmed=(RSC.askQuestionOC("Really delete the file " + attachment.get("path") + "?", "Warning")!=JOptionPane.NO_OPTION);
            } 
            if (confirmed) {
                item.deleteFilesOfAttachments();
                RSC.out(TI + "Deleted :: " + attachment.get("name") + ":: path :: " + attachment.get("path"));
            } else {
                return;
            }
        }
        removeFromTable(item);
    }

    private void removeFromTable(Item item) {
        int currentItemIndex=preparedItems.indexOf(item);
        int selectedItemIndex=jLstFileList.getSelectedIndex();
        preparedItems.remove(currentItemIndex);
        if (currentList.getModel().getSize()>0) {
            ((DefaultListModel) currentList.getModel()).remove(currentItemIndex);
            currentList.repaint();
            if (currentItemIndex==selectedItemIndex)
                toNext(currentItemIndex - 1);
        }
        if (getDetailsSWs.size()>0) {
            getDetailsSWs.remove(currentItemIndex);
        }
        if (preparedItems.isEmpty()) {
            toInitState();
        }
    }

    private ArrayList<Item> cloneEntries() {
        ArrayList<Item> clone=new ArrayList<>();
        for (Item item : preparedItems) clone.add(item);
        return(clone);
    }

    private void adjustJBtnAdd() {
        boolean en = false;
        String[] ef = library.configToArray("essential-fields");
        if (currentEntry>-1) {
            en=true;
            Item item  = preparedItems.get(currentEntry);
            for (int k = 0; k < ef.length; k++) {
                if ((item.get(ef[k]) == null))// || (item.get(ef[k]).length()==0))
                    en = false;
            }
        }
        jBtnAdd.setEnabled(en);
    }

    @Override
    public void adjustStates() {
        
    }

    @Override
    public void libraryElementChanged(String type, String id) {
        adjustJBtnAdd();
    }

    @Override
    public void guiEventHappened(String id, String message) {
        if (id.equals("001")) {
            // editing happend
            adjustJBtnAdd();
        }
    }
    
    public void addImportedItem(Item item) {
        preparedItems.add(item);
        if (item.get("title").startsWith("Parsing error:")) {
            ((DefaultListModel) jImpList.getModel()).addElement(item.get("title"));
        } else {
            ((DefaultListModel) jImpList.getModel()).addElement(item.toText(true));
        }
    }

}
