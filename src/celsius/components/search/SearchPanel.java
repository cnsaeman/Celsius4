/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.search;

import celsius.Resources;
import celsius.components.search.SWSearch;
import celsius.components.library.Library;
import celsius.components.tableTabs.CelsiusTable;
import celsius.gui.MainFrame;
import java.awt.event.KeyEvent;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 *
 * @author cnsaeman
 */
public class SearchPanel extends javax.swing.JPanel implements DocumentListener {
    
    private final MainFrame MF;
    private final Resources RSC;
    
    public SWSearch swSearch;
    
    public int postID;
    
    public SearchPanel() {
        MF=null;
        RSC=null;
        postID=0;
    }

    /**
     * Creates new form SearchPanel
     */
    public SearchPanel(MainFrame mf) {
        MF=mf;
        RSC=MF.RSC;
        initComponents();
        RSC.adjustComponents(this.getComponents());
        this.setBorder(RSC.stdBordermS());
        jTFMainSearch.getDocument().addDocumentListener(this);
        jPanel22.setBorder(RSC.stdBorder());
        postID=0;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        searchBtnGrp = new javax.swing.ButtonGroup();
        jTFMainSearch = new celsius.gui.jExtTextField();
        jPanel22 = new javax.swing.JPanel();
        jCBSearchMode = new javax.swing.JComboBox<>();

        setLayout(new javax.swing.BoxLayout(this, javax.swing.BoxLayout.Y_AXIS));

        jTFMainSearch.setDefaultText("Enter a search string (ctrl+f for items, ctrl+p for people)");
        jTFMainSearch.setFont(new java.awt.Font("Arial", 0, RSC.guiScale(20)));
        jTFMainSearch.setHorizontalAlignment(0);
        jTFMainSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTFMainSearchKeyPressed(evt);
            }
        });
        add(jTFMainSearch);

        jPanel22.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 0, 0));
        jPanel22.setMaximumSize(null);
        jPanel22.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 0, 5));

        jCBSearchMode.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item index", "Plain Text", "Person" }));
        jPanel22.add(jCBSearchMode);

        add(jPanel22);
    }// </editor-fold>//GEN-END:initComponents

    private void jTFMainSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTFMainSearchKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            if (jCBSearchMode.getSelectedIndex()==1) {
                performMainSearch();
            } else {
                RSC.getCurrentTable().selectFirst();
                evt.consume();
            }
        }
    }//GEN-LAST:event_jTFMainSearchKeyPressed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JComboBox<String> jCBSearchMode;
    private javax.swing.JPanel jPanel22;
    public celsius.gui.jExtTextField jTFMainSearch;
    private javax.swing.ButtonGroup searchBtnGrp;
    // End of variables declaration//GEN-END:variables

    public void focus() {
        jTFMainSearch.requestFocus();
        jTFMainSearch.selectAll();
    }
    
    public void focus(int mode) {
        jCBSearchMode.setSelectedIndex(mode);
        focus();
    }

    @Override
    public void insertUpdate(DocumentEvent de) {
        if (searchImmediately()) performMainSearch();
    }

    @Override
    public void removeUpdate(DocumentEvent de) {
        if (searchImmediately()) performMainSearch();
    }

    @Override
    public void changedUpdate(DocumentEvent de) {
        if (searchImmediately()) performMainSearch();
    }
    
    public boolean searchImmediately() {
        return(jCBSearchMode.getSelectedIndex()!=1);
    }
    
    public void startSearch(String srch,int mode) {
        RSC.MF.clearCategorySelection();
        CelsiusTable cels=RSC.getCurrentTable();
        int tableType=CelsiusTable.TABLETYPE_ITEM_SEARCH;
        if (mode==2) tableType=CelsiusTable.TABLETYPE_PERSON_SEARCH;
        CelsiusTable celsiusTable=RSC.guaranteeTableAvailable(tableType,srch,"search");
        postID++;
        celsiusTable.postID=postID;
        System.out.println(">> Creating table with postID "+postID);
        System.out.println("object type "+celsiusTable.getObjectType());
        MF.setThreadMsg("Searching...");
        RSC.guiStates.adjustState("mainFrame","itemSelected", false);
        //celsiusTable.resizeTable(true);
        swSearch=new SWSearch(celsiusTable,srch,mode,postID);
        swSearch.execute();
    }

    public void stopSearch() {
        if (swSearch!=null) {
            swSearch.cancel(true);
            swSearch=null;
        }
    }
    
    public void performMainSearch() {
        stopSearch();
        String srch = jTFMainSearch.getText().trim();
        if (srch.equals("")) return;
        MF.searchState = 0;
        if (RSC.getCurrentTable()!=null)
            if (!RSC.getCurrentTable().celsiusTableModel.tableview) return;
        if ((srch.length() > 0) && (!srch.equals(jTFMainSearch.getDefaultText()))) {
            RSC.guiStates.adjustState("mainFrame","itemSelected", false);
            RSC.guiStates.adjustState("mainFrame","personSelected", false);
            startSearch(srch,jCBSearchMode.getSelectedIndex());
        }
    }
    
    public void setEnabled(boolean state) {
        jTFMainSearch.setEnabled(state);
        jCBSearchMode.setEnabled(state);
    }
        
}
