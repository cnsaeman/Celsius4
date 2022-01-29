/*
 *   InformationPanel.java
 *
 *   Main GUI component 
 */

package celsius.gui;

import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import celsius.data.Library;
import celsius.data.BibTeXRecord;
import celsius.Resources;
import celsius.SwingWorkers.SWApplyPlugin;
import celsius.SwingWorkers.SWShowCited;
import celsius.data.Attachment;
import celsius.data.Item;
import celsius.data.Category;
import celsius.data.Person;
import celsius.data.TableRow;
import celsius.tools.*;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import javax.imageio.ImageIO;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.tree.DefaultTreeCellRenderer;


/**
 *
 * @author cnsaeman
 */
public final class InformationPanel extends javax.swing.JPanel implements GuiEventListener, DropTargetListener {

    public final static int TabMode_START_UP=-1;
    public final static int TabMode_EMPTY=0;
    public final static int TabMode_ITEM=1;
    public final static int TabMode_PERSON=2;
    public final static int TabMode_CATEGORY=3;
    public final static int TabMode_ITEM_SEARCH=4;
    public final static int TabMode_PERSON_SEARCH=5;
    
    // 4 and 7 missing?

    private final Resources RSC;
    public EditorPanel jPEdit;

    public CelsiusTable celsiusTable;
    private Library library;

    public int tabMode;
    
    public String removeLinkSQL;
    public String addLinkSQL;
    
    public int currentTemplate;
    
    public static final String[] addBibFields = {"add property       ", "author", "editor", "publisher", "title", "journal", "volume", "number", "series", "year", "pages", "note", "doi", "eprint", "archiveprefix", "primaryclass", "slaccitation"};

    public HTMLEditorKit kit;
    
    /** 
     *  Creates new form jInfoPanel
     *  @param rsc 
     */
    public InformationPanel(Resources rsc) {
        RSC=rsc;
        initComponents();
        jCBLinkType.setMinimumSize(new Dimension(RSC.guiScale(300),RSC.guiScale(25)));
        jPanel7.setBorder(RSC.stdBordermW());
        jPanel8.setBorder(RSC.stdBordermW());
        jPanel9.setBorder(RSC.stdBordermW());
        jPanel12.setBorder(RSC.stdBordermW());
        
        List<String> list = Arrays.asList(addBibFields);
        Vector<String> vec = new Vector<>( list );
        DefaultComboBoxModel addModel=new DefaultComboBoxModel(vec);
        addModel.setSelectedItem(addModel.getElementAt(0));
        jCBAddProperty.setModel(addModel);
        DropTarget dt = (new DropTarget(jHTMLview, DnDConstants.ACTION_COPY_OR_MOVE,this,true,null));
        kit = new HTMLEditorKit();
        jHTMLview.setEditorKit(kit);
        jTPItem.setTabComponentAt(0,new TabLabel("Info",Resources.infoTabIcon,rsc,null,false));
        // Init Linktree
        DefaultTreeCellRenderer renderer3 = new DefaultTreeCellRenderer();
        renderer3.setLeafIcon(RSC.icons.getIcon("arrow_right"));
        renderer3.setClosedIcon(RSC.icons.getIcon("folder"));
        renderer3.setOpenIcon(RSC.icons.getIcon("folder_link"));
        jLFiles1.setModel(new DefaultListModel());
        jLFiles2.setModel(new DefaultListModel());
        jLFiles3.setModel(new DefaultListModel());
        
        jPEdit=new EditorPanel(RSC,true);
        jPEdit.addChangeListener(this);
        
        RSC.adjustComponents(this.getComponents());
        jTABibTeX.setFont(RSC.stdFontMono());

        // TODO: Questionable
        RSC.guiStates.registerDirectlyEnabledComponent("mainFrame", "noLib", new JComponent[] {jMIEditDS1});
        
        celsiusTable=null;
        tabMode=-1000;
    }

    public Item getItem() {
        return((Item)RSC.getCurrentTable().getCurrentlySelectedRow());
    }

    public Person getPerson() {
        return((Person)RSC.getCurrentTable().getCurrentlySelectedRow());
    }
    
    public void updateThumb() {
        if (celsiusTable==null) return;
        if (jPThumb.isVisible()) {
            TableRow currentRow=celsiusTable.getCurrentlySelectedRow();
            if (currentRow!=null) {
                if (currentRow.hasThumbnail()) {
                    String thumbPath = currentRow.getThumbnailPath();
                    if (thumbPath != null) {
                        jBtnResizeThumb.setEnabled(true);
                        jBtnRemoveThumb.setEnabled(true);
                        try {
                            jLIcon.setIcon(new ImageIcon(new URL("file://" + thumbPath)));
                            return;
                        } catch (Exception e) {
                            RSC.outEx(e);
                        }
                    }
                }
            }
            jBtnResizeThumb.setEnabled(false);
            jBtnRemoveThumb.setEnabled(false);
            jLIcon.setIcon(null);
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

        jPMHTML = new javax.swing.JPopupMenu();
        jMIEditDS1 = new javax.swing.JMenuItem();
        jMIAddThumb = new javax.swing.JMenuItem();
        jTPItem = new javax.swing.JTabbedPane();
        jSP3 = new javax.swing.JScrollPane();
        jHTMLview = new javax.swing.JEditorPane();
        jPBibData = new javax.swing.JPanel();
        jScrollPane10 = new javax.swing.JScrollPane();
        jTABibTeX = new javax.swing.JTextArea();
        jPanel8 = new javax.swing.JPanel();
        jBtnApplyBibTeX = new javax.swing.JButton();
        jBtnCreateBibTeX = new javax.swing.JButton();
        jBtnNormalizeBibTeX = new javax.swing.JButton();
        jCBAddProperty = new javax.swing.JComboBox();
        jCBBibPlugins = new javax.swing.JComboBox();
        jPRemarks = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        jBtnApplyRem = new javax.swing.JButton();
        jScrollPane8 = new javax.swing.JScrollPane();
        jTARemarks = new javax.swing.JTextArea();
        jPAttachments = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jLAttachments = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        jBtnView = new javax.swing.JButton();
        jBtnUp = new javax.swing.JButton();
        jBtnDown = new javax.swing.JButton();
        jBtnRename = new javax.swing.JButton();
        jBtnPath = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jBtnAdd = new javax.swing.JButton();
        jPSources = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jScrollPane7 = new javax.swing.JScrollPane();
        jLFiles1 = new javax.swing.JList<>();
        jScrollPane9 = new javax.swing.JScrollPane();
        jLFiles2 = new javax.swing.JList<>();
        jScrollPane13 = new javax.swing.JScrollPane();
        jLFiles3 = new javax.swing.JList<>();
        jPanel9 = new javax.swing.JPanel();
        jBtnChooseSourceFolder = new javax.swing.JButton();
        jBtnShowCited = new javax.swing.JButton();
        jPLinks = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jLLinkedItems = new javax.swing.JList<>();
        jPanel6 = new javax.swing.JPanel();
        jCBLinkType = new javax.swing.JComboBox<>();
        jBtnAdd1 = new javax.swing.JButton();
        jBtnRemove = new javax.swing.JButton();
        jBtnView1 = new javax.swing.JButton();
        jPThumb = new javax.swing.JPanel();
        jPanel11 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLIcon = new javax.swing.JLabel();
        jPanel12 = new javax.swing.JPanel();
        jBtnAddThumb = new javax.swing.JButton();
        jBtnResizeThumb = new javax.swing.JButton();
        jBtnRemoveThumb = new javax.swing.JButton();
        jPItemRaw = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTARaw1 = new javax.swing.JTextArea();
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();

        jPMHTML.addPopupMenuListener(new javax.swing.event.PopupMenuListener() {
            public void popupMenuCanceled(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeInvisible(javax.swing.event.PopupMenuEvent evt) {
            }
            public void popupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {
                jPMHTMLPopupMenuWillBecomeVisible(evt);
            }
        });

        jMIEditDS1.setText("Edit HTML template");
        jMIEditDS1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIEditDS1jMIEditDSActionPerformed(evt);
            }
        });
        jPMHTML.add(jMIEditDS1);

        jMIAddThumb.setText("Add Thumbnail");
        jMIAddThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jMIAddThumbActionPerformed(evt);
            }
        });
        jPMHTML.add(jMIAddThumb);

        setMinimumSize(new java.awt.Dimension(0, 298));
        setPreferredSize(new java.awt.Dimension(300, 298));

        jTPItem.setTabLayoutPolicy(javax.swing.JTabbedPane.SCROLL_TAB_LAYOUT);
        jTPItem.setName(""); // NOI18N
        jTPItem.setPreferredSize(new java.awt.Dimension(395, 627));

        jHTMLview.setEditable(false);
        jHTMLview.setFont(jHTMLview.getFont());
        jHTMLview.setComponentPopupMenu(jPMHTML);
        jHTMLview.addHyperlinkListener(new javax.swing.event.HyperlinkListener() {
            public void hyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {
                jHTMLviewHyperlinkUpdate(evt);
            }
        });
        jHTMLview.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                jHTMLviewKeyReleased(evt);
            }
        });
        jSP3.setViewportView(jHTMLview);

        jTPItem.addTab("Info", jSP3);

        jPBibData.setName("Bibliography"); // NOI18N
        jPBibData.setLayout(new java.awt.BorderLayout());

        jTABibTeX.setColumns(20);
        jTABibTeX.setFont(new java.awt.Font("Monospaced", 0, RSC.guiScale(12))
        );
        jScrollPane10.setViewportView(jTABibTeX);

        jPBibData.add(jScrollPane10, java.awt.BorderLayout.CENTER);

        jPanel8.setName(""); // NOI18N
        jPanel8.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnApplyBibTeX.setText("Apply");
        jBtnApplyBibTeX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnApplyBibTeXActionPerformed(evt);
            }
        });
        jPanel8.add(jBtnApplyBibTeX);

        jBtnCreateBibTeX.setText("Create");
        jBtnCreateBibTeX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCreateBibTeXActionPerformed(evt);
            }
        });
        jPanel8.add(jBtnCreateBibTeX);

        jBtnNormalizeBibTeX.setText("Normalize");
        jBtnNormalizeBibTeX.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnNormalizeBibTeXActionPerformed(evt);
            }
        });
        jPanel8.add(jBtnNormalizeBibTeX);

        jCBAddProperty.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "author", "editor", "publisher", "title", "journal", "volume", "number", "series", "year", "pages", "note", "doi", "eprint", "archiveprefix", "primaryclass", "slaccitation" }));
        jCBAddProperty.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBAddPropertyActionPerformed(evt);
            }
        });
        jPanel8.add(jCBAddProperty);

        jCBBibPlugins.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBBibPluginsActionPerformed(evt);
            }
        });
        jPanel8.add(jCBBibPlugins);

        jPBibData.add(jPanel8, java.awt.BorderLayout.SOUTH);

        jTPItem.addTab("Bibliography1", jPBibData);

        jPRemarks.setName("Remarks"); // NOI18N
        jPRemarks.setLayout(new java.awt.BorderLayout());

        jPanel7.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnApplyRem.setText("Apply");
        jBtnApplyRem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnApplyRemActionPerformed(evt);
            }
        });
        jPanel7.add(jBtnApplyRem);

        jPRemarks.add(jPanel7, java.awt.BorderLayout.SOUTH);

        jTARemarks.setColumns(20);
        jTARemarks.setFont(jTARemarks.getFont());
        jTARemarks.setLineWrap(true);
        jTARemarks.setRows(5);
        jTARemarks.setWrapStyleWord(true);
        jScrollPane8.setViewportView(jTARemarks);

        jPRemarks.add(jScrollPane8, java.awt.BorderLayout.CENTER);

        jTPItem.addTab("Remarks", jPRemarks);

        jPAttachments.setName("Attachments"); // NOI18N
        jPAttachments.setLayout(new java.awt.BorderLayout());

        jLAttachments.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLAttachmentsMouseClicked(evt);
            }
        });
        jLAttachments.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLAttachmentsValueChanged(evt);
            }
        });
        jScrollPane3.setViewportView(jLAttachments);

        jPAttachments.add(jScrollPane3, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new javax.swing.BoxLayout(jPanel3, javax.swing.BoxLayout.LINE_AXIS));

        jBtnView.setText("View");
        jBtnView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnViewActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnView);

        jBtnUp.setText("Move Up");
        jBtnUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnUpActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnUp);

        jBtnDown.setText("Move Down");
        jBtnDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDownActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDown);

        jBtnRename.setText("Rename");
        jBtnRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRenameActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnRename);

        jBtnPath.setText("Change File Path");
        jBtnPath.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnPathActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnPath);

        jBtnDelete.setText("Delete");
        jBtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDeleteActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDelete);

        jBtnAdd.setText("Add");
        jBtnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnAdd);

        jPAttachments.add(jPanel3, java.awt.BorderLayout.SOUTH);

        jTPItem.addTab("Attachments", null, jPAttachments, "Attachments");

        jPSources.setName("Sources"); // NOI18N
        jPSources.setLayout(new java.awt.BorderLayout());

        jPanel10.setLayout(new java.awt.GridLayout(1, 0, 10, 0));

        jLFiles1.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLFiles1MouseClicked(evt);
            }
        });
        jScrollPane7.setViewportView(jLFiles1);

        jPanel10.add(jScrollPane7);

        jLFiles2.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLFiles2MouseClicked(evt);
            }
        });
        jScrollPane9.setViewportView(jLFiles2);

        jPanel10.add(jScrollPane9);

        jLFiles3.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLFiles3MouseClicked(evt);
            }
        });
        jScrollPane13.setViewportView(jLFiles3);

        jPanel10.add(jScrollPane13);

        jPSources.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnChooseSourceFolder.setText("Choose Source folder");
        jBtnChooseSourceFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnChooseSourceFolderActionPerformed(evt);
            }
        });
        jPanel9.add(jBtnChooseSourceFolder);

        jBtnShowCited.setText("Show papers cited in TeX-File");
        jBtnShowCited.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnShowCitedActionPerformed(evt);
            }
        });
        jPanel9.add(jBtnShowCited);

        jPSources.add(jPanel9, java.awt.BorderLayout.SOUTH);

        jTPItem.addTab("Sources", new javax.swing.ImageIcon(getClass().getResource("/celsius/images/star.png")), jPSources, "Sources"); // NOI18N

        jPLinks.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.GridLayout(1, 0));

        jLLinkedItems.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLLinkedItemsMouseClicked(evt);
            }
        });
        jScrollPane4.setViewportView(jLLinkedItems);

        jPanel5.add(jScrollPane4);

        jPLinks.add(jPanel5, java.awt.BorderLayout.CENTER);

        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jCBLinkType.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCBLinkTypeActionPerformed(evt);
            }
        });
        jPanel6.add(jCBLinkType);

        jBtnAdd1.setText("Add last selection");
        jBtnAdd1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAdd1ActionPerformed(evt);
            }
        });
        jPanel6.add(jBtnAdd1);

        jBtnRemove.setText("Remove");
        jBtnRemove.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRemoveActionPerformed(evt);
            }
        });
        jPanel6.add(jBtnRemove);

        jBtnView1.setText("View");
        jBtnView1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnView1ActionPerformed(evt);
            }
        });
        jPanel6.add(jBtnView1);

        jPLinks.add(jPanel6, java.awt.BorderLayout.SOUTH);

        jTPItem.addTab("tab8", jPLinks);

        jPThumb.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPThumb.setName(""); // NOI18N
        jPThumb.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPThumbComponentShown(evt);
            }
        });
        jPThumb.setLayout(new java.awt.BorderLayout());

        jLabel4.setFont(jLabel4.getFont().deriveFont(jLabel4.getFont().getStyle() & ~java.awt.Font.BOLD, jLabel4.getFont().getSize()-1));
        jLabel4.setText("Thumbnail:");

        jScrollPane2.setViewportView(jLIcon);

        javax.swing.GroupLayout jPanel11Layout = new javax.swing.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel11Layout.createSequentialGroup()
                        .addComponent(jLabel4)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 672, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 158, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPThumb.add(jPanel11, java.awt.BorderLayout.CENTER);

        jPanel12.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnAddThumb.setFont(jBtnAddThumb.getFont().deriveFont(jBtnAddThumb.getFont().getStyle() & ~java.awt.Font.BOLD, jBtnAddThumb.getFont().getSize()-1));
        jBtnAddThumb.setText("Add File");
        jBtnAddThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddThumbActionPerformed(evt);
            }
        });
        jPanel12.add(jBtnAddThumb);

        jBtnResizeThumb.setFont(jBtnResizeThumb.getFont().deriveFont(jBtnResizeThumb.getFont().getStyle() & ~java.awt.Font.BOLD, jBtnResizeThumb.getFont().getSize()-1));
        jBtnResizeThumb.setText("Resize to 240x240");
        jBtnResizeThumb.setEnabled(false);
        jBtnResizeThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnResizeThumbActionPerformed(evt);
            }
        });
        jPanel12.add(jBtnResizeThumb);

        jBtnRemoveThumb.setFont(jBtnRemoveThumb.getFont().deriveFont(jBtnRemoveThumb.getFont().getStyle() & ~java.awt.Font.BOLD, jBtnRemoveThumb.getFont().getSize()-1));
        jBtnRemoveThumb.setText("Remove");
        jBtnRemoveThumb.setEnabled(false);
        jBtnRemoveThumb.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRemoveThumbActionPerformed(evt);
            }
        });
        jPanel12.add(jBtnRemoveThumb);

        jPThumb.add(jPanel12, java.awt.BorderLayout.SOUTH);

        jTPItem.addTab("Thumbnail", jPThumb);

        jPItemRaw.setToolTipText("Information associated to the document in raw form");
        jPItemRaw.setName("Internal"); // NOI18N
        jPItemRaw.setLayout(new java.awt.BorderLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
        jPanel1.setLayout(new java.awt.BorderLayout());

        jScrollPane1.setEnabled(false);

        jTARaw1.setColumns(20);
        jTARaw1.setFont(jTARaw1.getFont());
        jTARaw1.setRows(5);
        jScrollPane1.setViewportView(jTARaw1);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel2.setPreferredSize(new java.awt.Dimension(RSC.guiScale(223), RSC.guiScale(17)));

        jLabel1.setFont(jLabel1.getFont().deriveFont(jLabel1.getFont().getStyle() & ~java.awt.Font.BOLD, jLabel1.getFont().getSize()-1));
        jLabel1.setText("View raw data");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap(606, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.add(jPanel2, java.awt.BorderLayout.NORTH);

        jPItemRaw.add(jPanel1, java.awt.BorderLayout.CENTER);

        jTPItem.addTab("Data", jPItemRaw);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTPItem, javax.swing.GroupLayout.DEFAULT_SIZE, 710, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTPItem, javax.swing.GroupLayout.DEFAULT_SIZE, 298, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void jHTMLviewHyperlinkUpdate(javax.swing.event.HyperlinkEvent evt) {//GEN-FIRST:event_jHTMLviewHyperlinkUpdate
        if (evt.getEventType().equals(javax.swing.event.HyperlinkEvent.EventType.ACTIVATED)) {
             String cmd=evt.getDescription();
            if (cmd.charAt(0)=='#') {
                jHTMLview.scrollToReference(cmd.substring(1));
                return;
            }
            if (cmd.equals("http://$$view")) {
                Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
                RSC.viewItem(currentItem);
                return;
            }
            if (cmd.equals("http://$$viewsimilar")) {
                //TODO
                //RSC.showCombined();
                return;
            }
            if (cmd.startsWith("http://$$display-message")) {
                String s1=Parser.cutFrom(cmd, "http://$$display-message:");
                String s2=Parser.cutFrom(s1,":");
                s1=Parser.cutUntil(s1,":");
                RSC.showInformation(s2,s1);
                return;
            }
            if (cmd.startsWith("http://$$view-attachment")) {
                String nmb=Parser.cutFromLast(cmd, "-");
                Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
                RSC.configuration.view(currentItem, Integer.valueOf(nmb));
                return;
            }
            if (cmd.startsWith("http://cid-")) {
                String id=Parser.cutFromLast(cmd, "-");
                String nmb=null;
                if (id.indexOf("-")>-1) {
                    nmb=Parser.cutFrom(id,"-");
                    id=Parser.cutUntil(id,"-");
                }
                RSC.configuration.view((new Item(library,id)),0);
                return;
            }
            if (cmd.startsWith("http://$$links")) {
                String type=Parser.cutFromLast(cmd, "-");
                //updateLinks();
                //TODO
                //RSC.showLinksOfType(type);
                return;
            }
            if (cmd.equals("http://$$journallink")) {
                if (RSC.MF.jTPTabList.getSelectedIndex() < 0) {
                    return;
                }
                Item item = (Item)RSC.getCurrentTable().getCurrentlySelectedRow();
                String cmdln=RSC.getJournalLinkCmd(item);
                if (cmdln.length()>0) {
                    RSC.out("JM>Journal link command: " + cmdln);
                    (new ExecutionShell(cmdln, 0, true)).start();
                } else {
                    RSC.showWarning("No journal link found!", "Warning");
                }
                return;
            }
            if (cmd.startsWith("http://$$person.")) {
                String personID=Parser.cutFrom(cmd,"http://$$person.");
                RSC.MF.goToPerson(personID);
                return;
            } else if (cmd.startsWith("http://$$item.")) {
                String itemID=Parser.cutFrom(cmd,"http://$$item.");
                RSC.MF.goToItem(itemID);
                return;
            }
            RSC.configuration.viewHTML(evt.getURL().toString());
        }
}//GEN-LAST:event_jHTMLviewHyperlinkUpdate

    private void jBtnApplyBibTeXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnApplyBibTeXActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (jTABibTeX.getText().trim().equals("")) {
            currentItem.put("bibtex",null);
            if (currentItem.get("type").equals("Paper")) {
                currentItem.put("type","Preprint");
            }
            updateGUI();
            currentItem.save();
            return;
        }
        BibTeXRecord btr = new BibTeXRecord(jTABibTeX.getText());
        if (btr.parseError != 0) {
            RSC.showWarning("BibTeX entry not consistent: " + BibTeXRecord.status[btr.parseError], "Warning:");
            return;
        }
        jTABibTeX.setText(btr.toString());
        currentItem.put("bibtex", btr.toString());
        currentItem.put("citation-tag", btr.getTag());
        if (btr.type.toLowerCase().equals("book")) {
            currentItem.put("type","Book");
        } else if (btr.type.toLowerCase().equals("phdthesis")) {
            currentItem.put("type","Thesis");
        } else if (currentItem.get("type").equals("Paper") && (btr.get("journal")==null)) {
            currentItem.put("type","Preprint");
        } else if (currentItem.get("type").equals("Preprint") && (btr.get("journal")!=null)) {
            currentItem.put("type","Paper");
        }
        currentItem.save();
        updateGUI();
        if (currentItem.error==6) RSC.showWarning("Error while saving information file.", "Exception:");
}//GEN-LAST:event_jBtnApplyBibTeXActionPerformed

    private void jBtnCreateBibTeXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCreateBibTeXActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        try {
            SWApplyPlugin swAP = new SWApplyPlugin(currentItem.library, RSC, null, RSC.plugins.get("Create BibTeX"), "", currentItem);
            swAP.execute();
            swAP.get();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        jTABibTeX.setText(currentItem.getS("bibtex"));
        jTABibTeX.setCaretPosition(0);
}//GEN-LAST:event_jBtnCreateBibTeXActionPerformed

    private void jBtnNormalizeBibTeXActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnNormalizeBibTeXActionPerformed
        String tmp = BibTeXRecord.normalizeBibTeX(jTABibTeX.getText());
        if (!tmp.startsWith("@")) {
            RSC.showWarning(tmp, "BibTeX Error");
        } else {
            jTABibTeX.setText(tmp);
            jTABibTeX.setCaretPosition(0);
        }
}//GEN-LAST:event_jBtnNormalizeBibTeXActionPerformed

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

    private void jCBBibPluginsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBBibPluginsActionPerformed
        if (!jCBBibPlugins.isShowing()) return;
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (jCBBibPlugins.getSelectedIndex()==0) {
            BibTeXRecord bibtex = new BibTeXRecord(currentItem.get("bibtex"));
            if (bibtex.parseError == 0) {
                jTABibTeX.setText(bibtex.toString());
            } else {
                jTABibTeX.setText(currentItem.get("bibtex"));
                if (bibtex.parseError < 250)
                    RSC.showWarning("BibTeX parsing error: " + BibTeXRecord.status[bibtex.parseError], "Warning:");
            }
            jTABibTeX.setCaretPosition(0);
            jBtnApplyBibTeX.setEnabled(true);
            jBtnCreateBibTeX.setEnabled(true);
            jBtnNormalizeBibTeX.setEnabled(true);
            jCBAddProperty.setEnabled(true);
        } else {
            jTABibTeX.setText(RSC.getBibOutput(currentItem));
            jTABibTeX.setCaretPosition(0);
            jBtnApplyBibTeX.setEnabled(false);
            jBtnCreateBibTeX.setEnabled(false);
            jBtnNormalizeBibTeX.setEnabled(false);
            jCBAddProperty.setEnabled(false);
        }
}//GEN-LAST:event_jCBBibPluginsActionPerformed

    private void jBtnApplyRemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnApplyRemActionPerformed
        if (tabMode == InformationPanel.TabMode_ITEM) {
            Item item=(Item)celsiusTable.getCurrentlySelectedRow();
            item.put("remarks", jTARemarks.getText().trim());
            item.save();
        }
        if (tabMode == InformationPanel.TabMode_PERSON) {
            Person person=(Person)celsiusTable.getCurrentlySelectedRow();
            person.put("remarks", jTARemarks.getText().trim());
            person.save();
        }
        if (tabMode == InformationPanel.TabMode_CATEGORY) {
            Category category=RSC.MF.getSelectedCategory();
            if (category!=null) {
                category.setRemarks(jTARemarks.getText().trim());
                category.save();
            }
        }
        updateGUI();
}//GEN-LAST:event_jBtnApplyRemActionPerformed

    private void jMIEditDS1jMIEditDSActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIEditDS1jMIEditDSActionPerformed
        String tmp = library.getHTMLTemplate(currentTemplate).templateString;
        MultiLineEditor MLE = new MultiLineEditor(RSC, "Edit HTML template", tmp);
        MLE.setVisible(true);
        if (!MLE.cancel) {
            library.setHTMLTemplate(currentTemplate,MLE.text);
            updateGUI();
        }
}//GEN-LAST:event_jMIEditDS1jMIEditDSActionPerformed

    private void jPMHTMLPopupMenuWillBecomeVisible(javax.swing.event.PopupMenuEvent evt) {//GEN-FIRST:event_jPMHTMLPopupMenuWillBecomeVisible
        Clipboard cb=Toolkit.getDefaultToolkit().getSystemClipboard();
        if ((tabMode==0) && cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            jMIAddThumb.setEnabled(true);
        } else {
            jMIAddThumb.setEnabled(false);
        }
    }//GEN-LAST:event_jPMHTMLPopupMenuWillBecomeVisible
    
    private void jMIAddThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMIAddThumbActionPerformed
        Clipboard cb=Toolkit.getDefaultToolkit().getSystemClipboard();
        if ((tabMode==0) && cb.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
            try {
                Item currentItem = (Item) celsiusTable.getCurrentlySelectedRow();
                String target = currentItem.library.baseFolder + "thumbnails" + ToolBox.filesep + currentItem.id + ".jpg";
                Image image = (Image) cb.getData(DataFlavor.imageFlavor);
                FileTools.deleteIfExists(target);
                // convert from no longer supported types.
                BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
                Graphics bg = bufferedImage.getGraphics();
                bg.drawImage(image, 0, 0, null);
                bg.dispose();
                if (ImageIO.write(bufferedImage, "jpeg", new File(target))) {
                    currentItem.save();
                } else {
                    RSC.out("Saving jpge failed!");
                }
            } catch (UnsupportedFlavorException ex) {
                RSC.outEx(ex);
            } catch (IOException ex) {
                RSC.outEx(ex);
            }
        } else {
            RSC.showWarning("Incompatible file type.", "Warning:");
        }
    }//GEN-LAST:event_jMIAddThumbActionPerformed

    private void jBtnAddThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddThumbActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        String filename=RSC.selectFile("Indicate the file containing the thumbnail", "thumbnail", "_ALL", "All files");
        if (filename!=null) {
            try {
                String filetype="."+FileTools.getFileType(filename);
                String oldThumb=currentItem.getThumbnailPath();
                if (oldThumb!=null) {
                    FileTools.deleteIfExists(oldThumb);
                }
                FileTools.moveFile(filename, currentItem.getThumbnailPath());
                currentItem.save();
                updateGUI();
            } catch (IOException ex) {
                RSC.showWarning("Error writing thumbnail: "+ex.toString(), "Warning:");
                RSC.outEx(ex);
            }
        }
}//GEN-LAST:event_jBtnAddThumbActionPerformed

    private void jPThumbComponentShown(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPThumbComponentShown
        updateThumb();
}//GEN-LAST:event_jPThumbComponentShown

    private void jBtnResizeThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnResizeThumbActionPerformed
        TableRow currentRow=celsiusTable.getCurrentlySelectedRow();
        try {
            String target = currentRow.getThumbnailPath();
            BufferedImage bf = ImageIO.read(new File(target));
            //System.out.println(bf.getWidth());
            int w = bf.getWidth();
            int h = bf.getHeight();
            //System.out.println(w);
            //System.out.println(h);
            double rx = (240+0.001) / w;
            double ry = (240+0.001) / h;
            double r = rx;
            if (ry < rx) {
                r = ry;
            }
            BufferedImageOp op = new AffineTransformOp(
              AffineTransform.getScaleInstance(r, r),
              new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                                 RenderingHints.VALUE_INTERPOLATION_BICUBIC));
            BufferedImage img = op.filter(bf,null);
            String targetname=target;
            if (!target.endsWith("png"))
               targetname+=".png";
            ImageIO.write(img, "png", new File(targetname));
            if (!target.equals(targetname))
                FileTools.deleteIfExists(target);
            updateGUI();
        } catch (IOException ex) {
            RSC.outEx(ex);
        }
    }//GEN-LAST:event_jBtnResizeThumbActionPerformed

    private void jBtnRemoveThumbActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRemoveThumbActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        FileTools.deleteIfExists(currentItem.getThumbnailPath());
        currentItem.save();
        updateGUI();
    }//GEN-LAST:event_jBtnRemoveThumbActionPerformed

    private void jLFiles1MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLFiles1MouseClicked
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (evt.getClickCount() == 2) {
            String fn=jLFiles1.getSelectedValue();
            if (fn.length()>0) {
                fn=currentItem.get("source")+ToolBox.filesep+fn;
                RSC.configuration.view("pdf", fn);
            }
        }
    }//GEN-LAST:event_jLFiles1MouseClicked

    private void jLFiles2MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLFiles2MouseClicked
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (evt.getClickCount() == 2) {
            String fn=jLFiles2.getSelectedValue();
            if (fn.length()>0) {
                fn=currentItem.get("source")+ToolBox.filesep+fn;
                RSC.configuration.view("tex", fn);
            }
        }
    }//GEN-LAST:event_jLFiles2MouseClicked

    private void jLFiles3MouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLFiles3MouseClicked
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (evt.getClickCount() == 2) {
            String fn=jLFiles3.getSelectedValue();
            if (fn.length()>0) {
                fn=currentItem.get("source")+ToolBox.filesep+fn;
                RSC.configuration.view("---", fn);
            }
        }
    }//GEN-LAST:event_jLFiles3MouseClicked

    private void jBtnChooseSourceFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnChooseSourceFolderActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        String folder=RSC.selectFolder("Select the source folder for this item","sourcefolders");
        // cancelled?
        if (folder!=null) {
            currentItem.putS("source",folder);
            currentItem.save();
            updateGUI();
        }
    }//GEN-LAST:event_jBtnChooseSourceFolderActionPerformed

    private void jBtnShowCitedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnShowCitedActionPerformed
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        String fn=jLFiles2.getSelectedValue();
        if (fn!=null)  {
            fn=currentItem.get("source")+ToolBox.filesep+fn;
            CelsiusTable IT=RSC.makeNewTabAvailable(CelsiusTable.TABLETYPE_ITEM_SEARCH, "Cited in " + jLFiles2.getSelectedValue(),"search");
            RSC.guiStates.adjustState("mainFrame","itemSelected", false);
            SWShowCited swAP = new SWShowCited(IT,0,fn);
            swAP.execute();
        }
    }//GEN-LAST:event_jBtnShowCitedActionPerformed

    private void jHTMLviewKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jHTMLviewKeyReleased
        if (evt.isControlDown() && evt.getExtendedKeyCode()==67) {
            Clipboard Clp = RSC.MF.getToolkit().getSystemClipboard();
            StringSelection cont = new StringSelection(jHTMLview.getSelectedText());
            Clp.setContents(cont, RSC.MF);
        }
    }//GEN-LAST:event_jHTMLviewKeyReleased

    private void jBtnUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnUpActionPerformed
        int pos=jLAttachments.getSelectedIndex();
        if (pos<1) return;
        Item item=getItem();
        // rearrange attachments internally
        Attachment attachmentMovedDown=item.linkedAttachments.get(pos-1);
        Attachment attachmentMovedUp=item.linkedAttachments.get(pos);
        attachmentMovedDown.order=pos;
        attachmentMovedUp.order=pos-1;
        item.linkedAttachments.remove(pos-1);
        item.linkedAttachments.add(pos,attachmentMovedDown);
        // modify in database
        item.library.executeEX("UPDATE item_attachment_links SET ord="+String.valueOf(pos)+" WHERE item_id="+item.id+" AND attachment_id="+attachmentMovedDown.id+";");
        item.library.executeEX("UPDATE item_attachment_links SET ord="+String.valueOf(pos-1)+" WHERE item_id="+item.id+" AND attachment_id="+attachmentMovedUp.id+";");
        updateGUI();
        jLAttachments.setModel(item.getAttachmentListModel());
        jLAttachments.setSelectedIndex(pos-1);
    }//GEN-LAST:event_jBtnUpActionPerformed

    private void jBtnViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnViewActionPerformed
        RSC.configuration.view(getItem(), jLAttachments.getSelectedIndex());
    }//GEN-LAST:event_jBtnViewActionPerformed

    private void jBtnDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDownActionPerformed
        int pos=jLAttachments.getSelectedIndex();
        if (pos>=jLAttachments.getModel().getSize()-1) return;
        Item item=getItem();
        // rearrange attachments internally
        Attachment attachmentMovedDown=item.linkedAttachments.get(pos);
        Attachment attachmentMovedUp=item.linkedAttachments.get(pos+1);
        attachmentMovedUp.order=pos;
        attachmentMovedDown.order=pos+1;
        item.linkedAttachments.remove(pos+1);
        item.linkedAttachments.add(pos,attachmentMovedUp);
        // modify in database
        item.library.executeEX("UPDATE item_attachment_links SET ord="+String.valueOf(pos+1)+" WHERE item_id="+item.id+" AND attachment_id="+attachmentMovedDown.id+";");
        item.library.executeEX("UPDATE item_attachment_links SET ord="+String.valueOf(pos)+" WHERE item_id="+item.id+" AND attachment_id="+attachmentMovedUp.id+";");
        updateGUI();
        jLAttachments.setModel(item.getAttachmentListModel());
        jLAttachments.setSelectedIndex(pos+1);
    }//GEN-LAST:event_jBtnDownActionPerformed

    private void jBtnRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRenameActionPerformed
        Item item=getItem();
        Attachment attachment=item.linkedAttachments.get(jLAttachments.getSelectedIndex());
        SingleLineEditor SLE=new SingleLineEditor(RSC,"Enter a new name",attachment.get("name"),true);
        SLE.setVisible(true);
        if (!SLE.cancel) {
            attachment.put("name", SLE.text);
            try {
                attachment.save();
            } catch (Exception e) {
                RSC.outEx(e);
            }
            jLAttachments.setModel(item.getAttachmentListModel());
            updateGUI();
        }
    }//GEN-LAST:event_jBtnRenameActionPerformed

    private void jBtnPathActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnPathActionPerformed
        Item item=getItem();
        Attachment attachment=item.linkedAttachments.get(jLAttachments.getSelectedIndex());
        String oldPath=attachment.getFullPath();
        JFileChooser FC = new JFileChooser();
        FC.setDialogTitle("Select the linked file");
        FC.setSelectedFile(new File(oldPath));
        FC.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FC.setDialogType(JFileChooser.OPEN_DIALOG);
        // cancelled?
        if (!(FC.showOpenDialog(RSC.MF) == JFileChooser.CANCEL_OPTION)) {
            attachment.setPath(FC.getSelectedFile().getAbsolutePath());
            try {
                attachment.save();
            } catch(Exception ex) {
                RSC.outEx(ex);
            }
        }
    }//GEN-LAST:event_jBtnPathActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        Item item=getItem();
        int i=RSC.askQuestionYN("Are you sure you want to delete this attachement?", "Warning:");
        if (i==JOptionPane.NO_OPTION) return;
        Attachment attachment=item.linkedAttachments.get(jLAttachments.getSelectedIndex());
        attachment.delete();
        item.library.itemChanged(item.id);
        jLAttachments.setModel(item.getAttachmentListModel());
    }//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jLAttachmentsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLAttachmentsMouseClicked
        if (evt.getClickCount() == 2 && evt.getButton() == MouseEvent.BUTTON1) {
            RSC.configuration.view(getItem(), jLAttachments.getSelectedIndex());
        }
    }//GEN-LAST:event_jLAttachmentsMouseClicked

    private void jLAttachmentsValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLAttachmentsValueChanged
        if (evt.getValueIsAdjusting()) return;
        adjustAttachmentButtons();
    }//GEN-LAST:event_jLAttachmentsValueChanged

    private void jBtnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddActionPerformed
        associateFileToCurrentItem();
    }//GEN-LAST:event_jBtnAddActionPerformed

    private void jLLinkedItemsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLLinkedItemsMouseClicked
        if (evt.getClickCount() == 2) {
            viewSelectedItem();
        }
    }//GEN-LAST:event_jLLinkedItemsMouseClicked

    private void jCBLinkTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCBLinkTypeActionPerformed
        adjustLinkedItemsList();
    }//GEN-LAST:event_jCBLinkTypeActionPerformed

    private void jBtnAdd1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAdd1ActionPerformed
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        if (tableRow.linkedItems.get(jCBLinkType.getSelectedIndex())==null) {
            tableRow.linkedItems.put(jCBLinkType.getSelectedIndex(),new ArrayList<Item>());
        }
        for (Item item : RSC.lastItemSelection.itemList) {
            tableRow.linkedItems.get(jCBLinkType.getSelectedIndex()).add(item);
            tableRow.library.executeEX(addLinkSQL, new String[]{tableRow.id,item.id});
        }
        tableRow.notifyChanged();
        adjustLinkedItemsList();
    }//GEN-LAST:event_jBtnAdd1ActionPerformed

    private void jBtnRemoveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRemoveActionPerformed
        ArrayList<Item> itemsToRemove=new ArrayList<>();
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        for (Integer i : jLLinkedItems.getSelectedIndices()) {
            itemsToRemove.add(tableRow.linkedItems.get(jCBLinkType.getSelectedIndex()).get(i));
        }
        StringBuffer idList=new StringBuffer();
        for (Item item : itemsToRemove) {
            idList.append(",");
            idList.append(item.id);
            tableRow.linkedItems.get(jCBLinkType.getSelectedIndex()).remove(item);
        }
        tableRow.library.executeEX(removeLinkSQL,new String[]{tableRow.id,idList.substring(1)});
        adjustLinkedItemsList();
        tableRow.notifyChanged();
    }//GEN-LAST:event_jBtnRemoveActionPerformed

    private void jBtnView1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnView1ActionPerformed
        viewSelectedItem();
    }//GEN-LAST:event_jBtnView1ActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAdd;
    private javax.swing.JButton jBtnAdd1;
    private javax.swing.JButton jBtnAddThumb;
    private javax.swing.JButton jBtnApplyBibTeX;
    private javax.swing.JButton jBtnApplyRem;
    private javax.swing.JButton jBtnChooseSourceFolder;
    private javax.swing.JButton jBtnCreateBibTeX;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnDown;
    private javax.swing.JButton jBtnNormalizeBibTeX;
    private javax.swing.JButton jBtnPath;
    private javax.swing.JButton jBtnRemove;
    private javax.swing.JButton jBtnRemoveThumb;
    private javax.swing.JButton jBtnRename;
    private javax.swing.JButton jBtnResizeThumb;
    private javax.swing.JButton jBtnShowCited;
    private javax.swing.JButton jBtnUp;
    private javax.swing.JButton jBtnView;
    private javax.swing.JButton jBtnView1;
    private javax.swing.JComboBox jCBAddProperty;
    public javax.swing.JComboBox jCBBibPlugins;
    private javax.swing.JComboBox<String> jCBLinkType;
    private javax.swing.JEditorPane jHTMLview;
    private javax.swing.JList<String> jLAttachments;
    private javax.swing.JList<String> jLFiles1;
    private javax.swing.JList<String> jLFiles2;
    private javax.swing.JList<String> jLFiles3;
    private javax.swing.JLabel jLIcon;
    private javax.swing.JList<String> jLLinkedItems;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JMenuItem jMIAddThumb;
    public javax.swing.JMenuItem jMIEditDS1;
    private javax.swing.JPanel jPAttachments;
    private javax.swing.JPanel jPBibData;
    private javax.swing.JPanel jPItemRaw;
    private javax.swing.JPanel jPLinks;
    private javax.swing.JPopupMenu jPMHTML;
    private javax.swing.JPanel jPRemarks;
    private javax.swing.JPanel jPSources;
    private javax.swing.JPanel jPThumb;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jSP3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane10;
    private javax.swing.JScrollPane jScrollPane13;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane7;
    private javax.swing.JScrollPane jScrollPane8;
    private javax.swing.JScrollPane jScrollPane9;
    private javax.swing.JTextArea jTABibTeX;
    private javax.swing.JTextArea jTARaw1;
    public javax.swing.JTextArea jTARemarks;
    public javax.swing.JTabbedPane jTPItem;
    // End of variables declaration//GEN-END:variables

    public void adjustAttachmentButtons() {
        Component[] buttons=new Component[] {jBtnView,jBtnUp,jBtnDown,jBtnRename,jBtnPath,jBtnDelete};
        for (Component btn : buttons) {
            btn.setEnabled(jLAttachments.getSelectedIndex()>=0);
        }
        jBtnUp.setEnabled(jLAttachments.getSelectedIndex() > 0);
        jBtnDown.setEnabled((jLAttachments.getSelectedIndex() > -1) && (jLAttachments.getSelectedIndex() < getItem().linkedAttachments.size() - 1));        
    }
    
    /*public void updateLinks() {
        if (celsiusTable==null) return;
        if (celsiusTable.getTableType()>=10) return;
        Item currentItem=(Item)celsiusTable.getCurrentlySelectedRow();
        if (tabMode==-1) return;
        if (currentItem==null) return;
        //DefaultTreeModel DTM=library.createLinksTree(currentItem);
        //jTLinks.setModel(DTM);
    }*/
    
    public void addToPanel(String title, String icon, JComponent panel) {
        jTPItem.add(panel);
        jTPItem.setTabComponentAt(jTPItem.getTabCount()-1, new TabLabel(title,icon,RSC,null,false));
    }
    
    public void switchToTabMode(int mode) {
        if (mode==tabMode) return;
        jTPItem.removeAll();
        switch (mode) {
            case InformationPanel.TabMode_ITEM:
                addToPanel("Summary",Resources.infoTabIcon,jSP3);
                if (!library.hideFunctionality.contains("Tab:Bibliography")) addToPanel("Bibliography", Resources.bibliographyTabIcon, jPBibData);
                addToPanel("Remarks",Resources.remarksTabIcon,jPRemarks);
                addToPanel("Attachments",Resources.attachmentsTabIcon,jPAttachments);
                addToPanel("Sources",Resources.sourcesTabIcon,jPSources);
                addToPanel("Links",Resources.linksTabIcon,jPLinks);
                addToPanel("Edit",Resources.editTabIcon,jPEdit);
                if (!library.hideFunctionality.contains("Tab:Thumbnail")) addToPanel("Image", Resources.thumbTabIcon,jPThumb);
                addToPanel("Internal",Resources.internalTabIcon,jPItemRaw);
                break;
            case InformationPanel.TabMode_PERSON:
                addToPanel("Summary",Resources.infoTabIcon,jSP3);
                addToPanel("Remarks",Resources.remarksTabIcon,jPRemarks);
                addToPanel("Edit",Resources.editTabIcon,jPEdit);
                if (!library.hideFunctionality.contains("Tab:Thumbnail")) addToPanel("Image", Resources.thumbTabIcon,jPThumb);
                addToPanel("Internal",Resources.internalTabIcon,jPItemRaw);
                break;
            case InformationPanel.TabMode_CATEGORY:
                addToPanel("Summary",Resources.infoTabIcon,jSP3);
                addToPanel("Remarks",Resources.remarksTabIcon,jPRemarks);
                break;
            default : 
                jTPItem.removeAll();
                jTPItem.add(jSP3);
                jTPItem.setTabComponentAt(0,new TabLabel("Summary",Resources.infoTabIcon,RSC,null,false));
        }
        tabMode=mode;
    }
    
    public void updateCSS() {
        kit = new HTMLEditorKit();
        kit.setStyleSheet(null);
        library.styleSheet=kit.getStyleSheet();
        String[] rules=library.config.get("css-style").split("\n");
        for (String rule : rules) {
            library.styleSheet.addRule(rule);
        }
        jHTMLview.setEditorKit(kit);        
    }
    
    /**
     * Update panel and full GUI according to current situation
     */
    public void updateGUI() {
        if (library!=RSC.getCurrentlySelectedLibrary()) {
            library=RSC.getCurrentlySelectedLibrary();
            updateCSS();
        };
        jHTMLview.setContentType("text/html");
        RSC.plugins.updateExportPlugins();
        if (!RSC.guiStates.getState("mainFrame","librarySelected")) {
            switchToTabMode(InformationPanel.TabMode_EMPTY);
            jHTMLview.setText(RSC.stdHTMLstring);
            jHTMLview.setCaretPosition(0);
            return;
        }
        celsiusTable=RSC.getCurrentTable();
        // library, but no tab TODO: information about library
        if ((celsiusTable==null) || (celsiusTable.getTableType()==CelsiusTable.EMPTY_TABLE)) {
            guiToNoTab();
            return;
        }
        // see how many items are selected
        if (celsiusTable.hasSingleSelection()) {
            switch (celsiusTable.getObjectType()) {
                case CelsiusTable.ITEM_TABLE:
                    guiToSingleItem();
                    break;
                case CelsiusTable.PERSON_TABLE:
                    guiToSinglePerson();
                    break;
                default:
                    guiToSingleItem();
            }
        } else if (celsiusTable.hasMultiSelection()) {
            switch (celsiusTable.getObjectType()) {
                case CelsiusTable.ITEM_TABLE:
                    guiToMultiItem();
                    break;
                case CelsiusTable.PERSON_TABLE:
                    guiToMultiPerson();
                    break;
                default:
                    guiToMultiItem();
            }
        } else {
            switch (celsiusTable.getTableType()) {
                case CelsiusTable.TABLETYPE_ITEMS_IN_CATEGORY:
                    guiToCategory();
                    break;
                case CelsiusTable.TABLETYPE_ITEM_WITH_KEYWORD:
                    guiToKeyword();
                    break;
                case CelsiusTable.TABLETYPE_ITEM_WHEN_ADDED:
                    guiToHistory();
                    break;
                case CelsiusTable.TABLETYPE_ITEM_SEARCH:
                    guiToItemSearchResults();
                    break;
                default:
                    guiToItemSearchResults();
            }
        }
        jHTMLview.setCaretPosition(0);
    }

    private void guiToItemSearchResults() {
        switchToTabMode(InformationPanel.TabMode_ITEM_SEARCH);
        CelsiusTemplate template=library.getHTMLTemplate(5);
        currentTemplate=5;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }
    
    private void guiToNoTab() {
        switchToTabMode(InformationPanel.TabMode_EMPTY);
        CelsiusTemplate template = library.htmlTemplates.get("-1");
        currentTemplate = -1;
        jHTMLview.setText(template.fillIn(RSC.getCurrentlySelectedLibrary().getDataHash()));
        jHTMLview.setCaretPosition(0);
    }
    
    private void guiToCategory() {
        switchToTabMode(InformationPanel.TabMode_CATEGORY);
        CelsiusTemplate template=library.getHTMLTemplate(2);
        currentTemplate=2;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }

    private void guiToKeyword() {
        switchToTabMode(InformationPanel.TabMode_EMPTY);
        CelsiusTemplate template=library.getHTMLTemplate(6);
        currentTemplate=6;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }
    
    private void guiToHistory() {
        switchToTabMode(InformationPanel.TabMode_EMPTY);
        CelsiusTemplate template=library.getHTMLTemplate(8);
        currentTemplate=8;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }
    
    private void guiToMultiItem() {
        switchToTabMode(InformationPanel.TabMode_EMPTY);
        CelsiusTemplate template=library.getHTMLTemplate(3);
        currentTemplate=3;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }

    private void guiToMultiPerson() {
        switchToTabMode(InformationPanel.TabMode_PERSON_SEARCH);
        CelsiusTemplate template=library.getHTMLTemplate(9);
        currentTemplate=9;
        RSC.getCurrentTable().updateStats();
        HashMap<String,String> data=RSC.getCurrentTable().properties;
        jHTMLview.setText(template.fillIn(data));
    }
    
    private void guiToSinglePerson() {
        switchToTabMode(InformationPanel.TabMode_PERSON);
        Person person=(Person)celsiusTable.getCurrentlySelectedRow();
        person.loadLevel(2);
        person.loadCollaborators();
        CelsiusTemplate template=library.getHTMLTemplate(1);
        currentTemplate=1;
        person.put("$$currentitems",library.getNumberOfItemsForPerson(person));
        person.put("$$currentpages",library.getNumberOfPagesForPerson(person));
        jHTMLview.setText(template.fillIn(person,true));
        jTARemarks.setText(person.get("remarks"));
        jTARemarks.setCaretPosition(0);
        jPEdit.setEditable(person);
        jTARaw1.setText(person.getRawData());
        jTARaw1.setCaretPosition(0);
        updatePersonLinks();
        updateThumb();
    }

    private void guiToSingleItem() {
        switchToTabMode(InformationPanel.TabMode_ITEM);
        Item item = (Item)celsiusTable.getCurrentlySelectedRow();
        item.loadLevel(3);
        CelsiusTemplate template=library.getHTMLTemplate(0);
        currentTemplate=0;
        jHTMLview.setText(template.fillIn(item,false));
        jTARaw1.setText(item.getRawData());
        jTARaw1.setCaretPosition(0);
        celsiusTable.updateRow(item);
        jPEdit.setEditable(item);
        jLAttachments.setModel(item.getAttachmentListModel());

        jTARemarks.setText(item.get("remarks"));
        jTARemarks.setCaretPosition(0);
        if (jCBBibPlugins.getSelectedIndex() == 0) {
            BibTeXRecord bibtex = new BibTeXRecord(item.get("bibtex"));
            if (bibtex.parseError == 0) {
                jTABibTeX.setText(bibtex.toString());
            } else {
                jTABibTeX.setText(item.get("bibtex"));
                if (bibtex.parseError < 250) {
                    RSC.showWarning("BibTeX parsing error: " + BibTeXRecord.status[bibtex.parseError], "Warning:");
                }
            }
            jTABibTeX.setCaretPosition(0);
        } else {
            jTABibTeX.setText(RSC.getBibOutput(item));
            jTABibTeX.setCaretPosition(0);
        }
        DefaultListModel listModel = (DefaultListModel) jLFiles1.getModel();
        listModel.clear();
        listModel = (DefaultListModel) jLFiles2.getModel();
        listModel.clear();
        listModel = (DefaultListModel) jLFiles3.getModel();
        listModel.clear();
        if (item.get("source") != null) {
            // Fill File lists with pdf, tex and all
            listModel = new DefaultListModel();
            File folder = new File(item.get("source"));
            if (folder.exists()) {
                File[] listOfFiles;
                listOfFiles = folder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".pdf"));
                Arrays.sort(listOfFiles, (File f1, File f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File listOfFile : listOfFiles) {
                    listModel.addElement(listOfFile.getName());
                }
                jLFiles1.setModel(listModel);
                // tex-files
                listModel = new DefaultListModel();
                folder = new File(item.get("source"));
                listOfFiles = folder.listFiles((File dir, String name1) -> name1.toLowerCase().endsWith(".tex"));
                Arrays.sort(listOfFiles, (File f1, File f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File listOfFile : listOfFiles) {
                    listModel.addElement(listOfFile.getName());
                }
                jLFiles2.setModel(listModel);
                // all files
                listModel = new DefaultListModel();
                folder = new File(item.get("source"));
                listOfFiles = folder.listFiles();
                Arrays.sort(listOfFiles, (File f1, File f2) -> Long.compare(f2.lastModified(), f1.lastModified()));
                for (File listOfFile : listOfFiles) {
                    listModel.addElement(listOfFile.getName());
                }
                jLFiles3.setModel(listModel);
            }
        }
        updateThumb();
        updateItemLinks();
        adjustAttachmentButtons();
    }
    
    public void dragEnter(DropTargetDragEvent dtde) {
        if (!acceptData(dtde.getTransferable()))
            dtde.rejectDrag();
    }

    public void dragOver(DropTargetDragEvent dtde) {
        if (!acceptData(dtde.getTransferable()))
            dtde.rejectDrag();
    }

    public void dropActionChanged(DropTargetDragEvent dtde) {
        System.out.println("Drop action changed");
    }

    public void dragExit(DropTargetEvent dte) {
        System.out.println("Drag exit");
        System.out.println("Drop3:"+dte.toString());
    }

    public void drop(DropTargetDropEvent dtde) {
        dtde.acceptDrop(DnDConstants.ACTION_COPY);
        System.out.println("Drop1");
        if (acceptData(dtde.getTransferable())) {
            System.out.println("Drop2");
            dtde.acceptDrop(dtde.getDropAction());
            try {
                TableRow currentRow = celsiusTable.getCurrentlySelectedRow();
                String target = currentRow.getThumbnailPath();
                if (dtde.isDataFlavorSupported(DataFlavor.imageFlavor)) {
                    System.out.println("Drop3:"+dtde.getTransferable().getTransferData(DataFlavor.imageFlavor).toString());
                    Image image = (Image) dtde.getTransferable().getTransferData(DataFlavor.imageFlavor);
                    FileTools.deleteIfExists(target);
                    // convert from no longer supported types.
                    BufferedImage bufferedImage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
                    Graphics bg = bufferedImage.getGraphics();
                    bg.drawImage(image, 0, 0, null);
                    bg.dispose();
                    if (ImageIO.write(bufferedImage, "jpeg", new File(target))) {
                        currentRow.save();
                    } else {
                        RSC.out("Saving jpeg failed!");
                    }
                } else if (dtde.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    System.out.println("Drop4:"+dtde.getTransferable().getTransferData(DataFlavor.stringFlavor).toString());
                    String path=Parser.cutFrom(dtde.getTransferable().getTransferData(DataFlavor.stringFlavor).toString(),"file://");
                    if ((new File(path)).exists()) {
                        System.out.println("Drop42:copy");
                        FileTools.copyFile(path, target);
                        currentRow.save();
                    }
                }
                updateGUI();
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
        }
    }

    private boolean acceptData(Transferable t) {
        boolean ret;
        try {
            if (t == null) {
                return (false);
            }
            if ((tabMode != InformationPanel.TabMode_ITEM) && (tabMode != InformationPanel.TabMode_PERSON)) {
                return (false);
            }
            ret=true;
        } catch (Exception e) {
            ret = false;
            RSC.outEx(e);
        }
        return (ret);
    }

    @Override
    public void guiEventHappened(String id, String message) {
        updateGUI();
    }
    
    public void associateFileToCurrentItem() {
        if (RSC.guiStates.getState("mainFrame","tabAvailable")) {
            Library library = RSC.getCurrentlySelectedLibrary();
            Item item=(Item)RSC.getCurrentTable().getSelectedRows().get(0);
            String filename = RSC.selectFile("Indicate the file to be associated with the selected record", "associate", "_ALL", "All files");
            if (filename != null) {
                String name = null;
                if (item.linkedAttachments.isEmpty()) {
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
        RSC.MF.updateStatusBar(true);
        updateGUI();        
    }
    
    private void adjustLinkedItemsList() {
        DefaultListModel DLM=new DefaultListModel();
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        ArrayList<Item> linkedItems=tableRow.linkedItems.get(jCBLinkType.getSelectedIndex());
        if ((linkedItems!=null) && (linkedItems.size()>0)) DLM.addAll(linkedItems);
        jLLinkedItems.setModel(DLM);
    }
    
    private void viewSelectedItem() {
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        Item selectedItem=tableRow.linkedItems.get(jCBLinkType.getSelectedIndex()).get(jLLinkedItems.getSelectedIndex());
        RSC.viewItem(selectedItem);
    }
    
    private void updateItemLinks() {
        DefaultComboBoxModel DCBM=new DefaultComboBoxModel();
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        DCBM.addAll(tableRow.library.linkTypes);
        jCBLinkType.setModel(DCBM);
        jCBLinkType.setSelectedIndex(0);
        adjustLinkedItemsList();        
        addLinkSQL = "INSERT INTO item_item_links (item1_id,item2_id) VALUES (?,?)";
        removeLinkSQL = "DELETE FROM item_item_links WHERE item1_id=? AND item2_id IN (?);";
        setPreferredSize(new Dimension(RSC.guiScale(500),RSC.guiScale(600)));
        jBtnAdd1.setEnabled((RSC.lastItemSelection!=null) && (RSC.lastItemSelection.library==tableRow.library));
    }

    private void updatePersonLinks() {
        DefaultComboBoxModel DCBM=new DefaultComboBoxModel();
        TableRow tableRow=celsiusTable.getCurrentlySelectedRow();
        DCBM.addAll(tableRow.library.linkTypes);
        jCBLinkType.setModel(DCBM);
        jCBLinkType.setSelectedIndex(0);
        adjustLinkedItemsList();        
        addLinkSQL = "INSERT INTO person_item_links (person_id,item_id) VALUES (?,?)";
        removeLinkSQL = "DELETE FROM person_item_links WHERE person_id=? AND item_id IN (?);";
        setPreferredSize(new Dimension(RSC.guiScale(500),RSC.guiScale(600)));
        jBtnAdd1.setEnabled((RSC.lastItemSelection!=null) && (RSC.lastItemSelection.library==tableRow.library));
    }
    

}
