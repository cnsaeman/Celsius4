/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * EditLibraryTemplates.java
 *
 * Created on 20.02.2010, 19:01:06
 */

package celsius.components.library;

import atlantis.gui.KeyValueTableModel;
import celsius.components.library.Library;
import celsius.Resources;
import celsius.components.library.LibraryTemplate;
import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import celsius.gui.GUIToolBox;
import celsius.gui.MainFrame;
import atlantis.gui.MultiLineEditor;
import atlantis.gui.SingleLineEditor;
import celsius.gui.TabLabel;
import celsius.components.tableTabs.TableTTRenderer;
import celsius.tools.ToolBox;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import javax.swing.DefaultListModel;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author cnsaeman
 */
public class EditLibraryTemplates extends javax.swing.JDialog implements MouseListener, KeyListener {

    private final MainFrame MF;
    private final Resources RSC;
    private final DefaultListModel DLM;
    private KeyValueTableModel KVTM;
    private LibraryTemplate libraryTemplate;

    /** Creates new form EditLibraryTemplates */
    public EditLibraryTemplates(MainFrame mf, Resources rsc) {
        super(mf, true);
        MF=mf;
        RSC=rsc;
        initComponents();
        DLM=new DefaultListModel();
        for (LibraryTemplate template : RSC.libraryTemplates)
            DLM.addElement(template);
        jLTemplates.setModel(DLM);
        jLTemplates.setSelectedIndex(0);
        jTabConfiguration.addMouseListener(this);
        jTabConfiguration.addKeyListener(this);
        jTabHTMLTemplates.addMouseListener(this);
        jTabHTMLTemplates.addKeyListener(this);
        jTabCreationInstructions.addMouseListener(this);
        jTabCreationInstructions.addKeyListener(this);
        jTabbedPane.setTabComponentAt(0,new TabLabel("Configuration",Resources.editTabIcon,RSC,null,false));        
        jTabbedPane.setTabComponentAt(1,new TabLabel("HTML templates",Resources.editTabIcon,RSC,null,false));        
        jTabbedPane.setTabComponentAt(2,new TabLabel("Creation Instructions",Resources.editTabIcon,RSC,null,false));        
        this.pack();
        this.setSize(RSC.guiScale(600), RSC.guiScale(500));
        goToSelected();
        GUIToolBox.centerDialog(this,mf);
    }

    public void goToSelected() {
        int i=jLTemplates.getSelectedIndex();
        if (i>-1) {
            libraryTemplate = (LibraryTemplate) jLTemplates.getSelectedValue();
            jTabConfiguration.setModel(libraryTemplate.getConfigurationModel());
            jTabHTMLTemplates.setModel(libraryTemplate.getHTMLTemplatesModel());
            jTabHTMLTemplates.getColumnModel().getColumn(0).setMaxWidth(RSC.guiScale(40));
            jTabHTMLTemplates.getColumnModel().getColumn(1).setCellRenderer(new TableTTRenderer(RSC));
            jTabCreationInstructions.setModel(libraryTemplate.getCreationInstructionsModel());
        } else {
            libraryTemplate=null;
            jTabConfiguration.setModel(new DefaultTableModel());
            jTabHTMLTemplates.setModel(new DefaultTableModel());
            jTabCreationInstructions.setModel(new DefaultTableModel());
        }
    }

    public void updateTable() {
        /*goToSelected();
        KVTM=new KeyValueTableModel("Property","Value");
        int j=jLTemplates.getSelectedIndex();
        for (String t : Library.LibraryFields) {
            KVTM.addRow(Parser.lowerEndOfWords(t), xml.get(t));
        }
        jTLibTemplates.setModel(KVTM);
        jTLibTemplates.getColumnModel().getColumn(0).setPreferredWidth(150);
        jTLibTemplates.getColumnModel().getColumn(0).setMaxWidth(150);*/
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jBtnAdd = new javax.swing.JButton();
        jBtnRename = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jLTemplates = new javax.swing.JList();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jBtnDone = new javax.swing.JButton();
        jTabbedPane = new javax.swing.JTabbedPane();
        jPanel7 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTabConfiguration = new javax.swing.JTable();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane4 = new javax.swing.JScrollPane();
        jTabHTMLTemplates = new javax.swing.JTable();
        jPanel9 = new javax.swing.JPanel();
        jScrollPane5 = new javax.swing.JScrollPane();
        jTabCreationInstructions = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Edit Library Templates");

        jPanel1.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnAdd.setText("Add");
        jBtnAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAddActionPerformed(evt);
            }
        });
        jPanel5.add(jBtnAdd);

        jBtnRename.setText("Rename");
        jBtnRename.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnRenameActionPerformed(evt);
            }
        });
        jPanel5.add(jBtnRename);

        jBtnDelete.setText("Delete");
        jBtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDeleteActionPerformed(evt);
            }
        });
        jPanel5.add(jBtnDelete);

        jPanel1.add(jPanel5, java.awt.BorderLayout.PAGE_END);

        jPanel6.setRequestFocusEnabled(false);
        jPanel6.setLayout(new java.awt.GridLayout(1, 0));

        jLTemplates.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jLTemplates.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jLTemplatesValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jLTemplates);

        jPanel6.add(jScrollPane1);

        jPanel1.add(jPanel6, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel1, java.awt.BorderLayout.WEST);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jBtnDone.setText("Done");
        jBtnDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDoneActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDone);

        jPanel2.add(jPanel3, java.awt.BorderLayout.SOUTH);

        jPanel7.setLayout(new java.awt.GridLayout(1, 0));

        jTabConfiguration.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane3.setViewportView(jTabConfiguration);

        jPanel7.add(jScrollPane3);

        jTabbedPane.addTab("Configuration", jPanel7);

        jPanel8.setLayout(new java.awt.GridLayout(1, 0));

        jTabHTMLTemplates.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane4.setViewportView(jTabHTMLTemplates);

        jPanel8.add(jScrollPane4);

        jTabbedPane.addTab("HTML Templates", jPanel8);

        jPanel9.setLayout(new java.awt.GridLayout(1, 0));

        jTabCreationInstructions.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane5.setViewportView(jTabCreationInstructions);

        jPanel9.add(jScrollPane5);

        jTabbedPane.addTab("Database setup", jPanel9);

        jPanel2.add(jTabbedPane, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * TODO check functionality and rewrite
     * 
     * @param evt 
     */
    private void jBtnAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAddActionPerformed
        Library library = RSC.getCurrentlySelectedLibrary();
        if (library != null) {
            int i = RSC.askQuestionOC("This will create a new library template from the currently active library.", "Add a new library template");
            if (i == 0) {
                LibraryTemplate libraryTemplate=new LibraryTemplate(RSC,library);
                DLM.addElement(libraryTemplate);
                RSC.libraryTemplates.add(libraryTemplate);
            }
        } else {
            RSC.showWarning("There is currently no library open.", "Cancelled...");
        }
    }//GEN-LAST:event_jBtnAddActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        int i=RSC.askQuestionYN("Do you really want to delete this library template?", "Please confirm:");
        if (i==0) {
            LibraryTemplate libraryTemplate=(LibraryTemplate)jLTemplates.getSelectedValue();
            DLM.removeElement(libraryTemplate);
            RSC.libraryTemplates.remove(libraryTemplate);
            FileTools.deleteIfExists(libraryTemplate.fileName);
        }
    }//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jBtnDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDoneActionPerformed
        setVisible(false);
        dispose();
    }//GEN-LAST:event_jBtnDoneActionPerformed

    private void jLTemplatesValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jLTemplatesValueChanged
        if (jLTemplates.getSelectedIndex()<0) {
            jBtnRename.setEnabled(false);
            jBtnDelete.setEnabled(false);
        } else {
            jBtnRename.setEnabled(true);
            jBtnDelete.setEnabled(true);
        }
        goToSelected();
    }//GEN-LAST:event_jLTemplatesValueChanged

    private void jBtnRenameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnRenameActionPerformed
        String name=libraryTemplate.name;
        SingleLineEditor SLE = new SingleLineEditor(RSC, "Rename template", name,true);
        SLE.setVisible(true);
        if (!SLE.cancelled && (!SLE.text.equals(name))) {
            libraryTemplate.rename(SLE.text);
            // update UI
            int i=DLM.indexOf(libraryTemplate);
            DLM.setElementAt(libraryTemplate, i);
        }
        
    }//GEN-LAST:event_jBtnRenameActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAdd;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnDone;
    private javax.swing.JButton jBtnRename;
    private javax.swing.JList jLTemplates;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JScrollPane jScrollPane4;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JTable jTabConfiguration;
    private javax.swing.JTable jTabCreationInstructions;
    private javax.swing.JTable jTabHTMLTemplates;
    private javax.swing.JTabbedPane jTabbedPane;
    // End of variables declaration//GEN-END:variables

    @Override
    public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() == 2) {
            editLine(e.getSource());
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        e.consume();
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        e.consume();
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        e.consume();
    }

    @Override
    public void mouseExited(MouseEvent e) {
        e.consume();
    }

    @Override
    public void keyTyped(KeyEvent e) {
        e.consume();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            editLine(e.getSource());
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        e.consume();
    }
    
    public void editLine(Object source) {
        if (source==jTabConfiguration) {
           String key=(String)jTabConfiguration.getModel().getValueAt(jTabConfiguration.getSelectedRow(),0);
            MultiLineEditor MLE = new MultiLineEditor(RSC, "Edit configuration string", libraryTemplate.configuration.get(key));
            MLE.setVisible(true);
            if (!MLE.cancelled) {
                libraryTemplate.setConfiguration(key,MLE.text);
                jTabConfiguration.setModel(libraryTemplate.getConfigurationModel());
            }
        } else if (source==jTabHTMLTemplates) {
           String key=(String)jTabHTMLTemplates.getModel().getValueAt(jTabHTMLTemplates.getSelectedRow(),0);
            MultiLineEditor MLE = new MultiLineEditor(RSC, "Edit HTML Template", libraryTemplate.htmlTemplates.get(key));
            MLE.setVisible(true);
            if (!MLE.cancelled) {
                libraryTemplate.setHTMLTemplate(key,MLE.text);
                jTabHTMLTemplates.setModel(libraryTemplate.getHTMLTemplatesModel());
                jTabHTMLTemplates.getColumnModel().getColumn(0).setMaxWidth(RSC.guiScale(40));
                jTabHTMLTemplates.getColumnModel().getColumn(1).setCellRenderer(new TableTTRenderer(RSC));
            }
        } else if (source==jTabCreationInstructions) {
            int row=jTabCreationInstructions.getSelectedRow();
            String value=(String)jTabCreationInstructions.getModel().getValueAt(row,0);
            MultiLineEditor MLE = new MultiLineEditor(RSC, "Edit SQLite Instructions", libraryTemplate.creationInstructions.get(row));
            MLE.setVisible(true);
            if (!MLE.cancelled) {
                libraryTemplate.setCreationInstruction(row,MLE.text);
                jTabCreationInstructions.setModel(libraryTemplate.getCreationInstructionsModel());
            }
        }
    }

}
