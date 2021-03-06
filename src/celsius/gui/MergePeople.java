/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.gui;

import celsius.Resources;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.data.Person;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 *
 * @author cnsaeman
 */
public class MergePeople extends javax.swing.JDialog implements ListSelectionListener {

    private final Resources RSC;
    private final Library library;
    private final LinkedHashMap<String,Person> peopleList;
    private final DefaultTableModel tableModel;
    
    /**
     * Creates new form MergePeople
     * @param rsc
     * @param ids
     */
    public MergePeople(Resources rsc, String ids) {
        super(rsc.MF, true);
        RSC=rsc;
        library=RSC.getCurrentlySelectedLibrary();
        peopleList=new LinkedHashMap<>();
        try {
            ResultSet rs = library.executeResEX("SELECT * FROM persons WHERE id IN (" + ids + ") ORDER BY id;");
            while (rs.next()) {
                Person person=new Person(library,rs);
                peopleList.put(person.id, person);
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        tableModel=new DefaultTableModel();
        ArrayList<String> columnData = new ArrayList<>();
        for (String id : peopleList.keySet()) {
            columnData.add(id);
        }
        columnData.add("");
        columnData.add("merged to:");
        columnData.add(columnData.get(0));
        tableModel.addColumn("id", columnData.toArray());
        for (String column : library.personPropertyKeys) {
            // exclude some columns
            if (!column.equals("search")) {
                columnData = new ArrayList<>();
                for (String id : peopleList.keySet()) {
                    columnData.add(peopleList.get(id).get(column));
                }
                columnData.add("");
                columnData.add("");
                columnData.add(columnData.get(0));
                tableModel.addColumn(column, columnData.toArray());
            }
        }
        
        // find all people
        initComponents();
        jTable.getSelectionModel().addListSelectionListener(this);
        jTable.getColumnModel().getSelectionModel().addListSelectionListener(this);
        jPanel2.setBorder(RSC.stdBorder());
        this.setSize(RSC.guiScale(800), RSC.guiScale(400));
        GUIToolBox.centerDialog(this, RSC.MF);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTable = new javax.swing.JTable();
        jPanel3 = new javax.swing.JPanel();
        jBtnCancel = new javax.swing.JButton();
        jBtnOK = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Merge People");

        jLabel1.setText("Please select the fields you want to keep:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(262, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        getContentPane().add(jPanel1, java.awt.BorderLayout.NORTH);

        jPanel2.setLayout(new java.awt.BorderLayout(5, 5));

        jTable.setModel(tableModel);
        jTable.setCellSelectionEnabled(true);
        jTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jTable);

        jPanel2.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        getContentPane().add(jPanel2, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        jBtnCancel.setText("Cancel");
        jBtnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCancelActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnCancel);

        jBtnOK.setText("OK");
        jBtnOK.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnOKActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnOK);

        getContentPane().add(jPanel3, java.awt.BorderLayout.SOUTH);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCancelActionPerformed
        dispose();
    }//GEN-LAST:event_jBtnCancelActionPerformed

    private void jBtnOKActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnOKActionPerformed
        String topid=(String)tableModel.getValueAt(0,0);
        StringBuilder otherids=new StringBuilder();
        for (String id : peopleList.keySet()) {
            if (!topid.equals(id)) {
                otherids.append(',');
                otherids.append(id);
            }
        }
        String otheridsArray=otherids.substring(1);
        try {
            String itemIDs = "("+library.executeResEX("SELECT GROUP_CONCAT(item_id, ',') AS result FROM item_person_links WHERE person_id IN (" + otheridsArray + ");").getString(1)+")";
            library.executeEX("UPDATE item_person_links SET person_id = " + topid + " WHERE person_id IN (" + otheridsArray+ ");");
            library.executeEX("DELETE FROM persons WHERE id IN (" + otheridsArray + ");");
            Person person = new Person(library, topid);
            for (int i = 1; i < tableModel.getColumnCount(); i++) {
                person.put(tableModel.getColumnName(i), (String) tableModel.getValueAt(tableModel.getRowCount() - 1, i));
            }
            person.save();
            library.personChanged(person.id);
            for (String id : otheridsArray.split(",")) {
                library.personChanged(id);
            }
            
            ResultSet rs=library.executeResEX("SELECT * FROM items WHERE ID in "+itemIDs+";");
            while (rs.next()) {
                Item item=new Item(library,rs);
                // adjust loadLevel since it's whole table
                item.currentLoadLevel=2;
                item.loadLevel(3);
                item.updateShorts();
                library.itemChanged(item.id);
                item.save();
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
        dispose();
    }//GEN-LAST:event_jBtnOKActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnCancel;
    private javax.swing.JButton jBtnOK;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTable;
    // End of variables declaration//GEN-END:variables

    @Override
    public void valueChanged(ListSelectionEvent lse) {
        if (lse.getValueIsAdjusting()) return;
        int r=jTable.getSelectedRow();
        int c=jTable.getSelectedColumn();
        int rowCount=jTable.getRowCount();
        String newValue=(String)tableModel.getValueAt(r, c);
        tableModel.setValueAt(newValue, rowCount-1, c);
    }
}
