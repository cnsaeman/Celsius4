/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.gui;

import celsius.Resources;
import celsius.data.PeopleListModelDetailed;
import celsius.data.Person;
import atlantis.tools.Parser;
import java.util.ArrayList;

/**
 *
 * @author cnsaeman
 */
public class PeopleEditor extends javax.swing.JDialog implements GenericCelsiusEventListener {

    public final Resources RSC;
    public final PeopleSelector peopleSelector;
    public final PeopleListModelDetailed peopleListModelDetailed;
    
    public boolean cancelled;
    public boolean modified;
    
    /**
     * Creates new form PeopleEditor
     * 
     * @param RSC
     * @param peopleListModelDetailed
     */
    public PeopleEditor(Resources RSC, PeopleListModelDetailed peopleListModelDetailed) {
        super(RSC.MF, true);
        this.setTitle("Edit person field");
        this.RSC=RSC;
        this.peopleListModelDetailed=peopleListModelDetailed;
        initComponents();
        jList.setModel(this.peopleListModelDetailed);
        jPanel2.setBorder(RSC.stdBorder());
        jPanel3.setBorder(RSC.stdBorder());
        peopleSelector=new PeopleSelector(RSC);
        peopleSelector.addEventListener(this);
        jPanel4.add(peopleSelector);
        cancelled=true;
        modified=false;
        this.pack();
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
        jScrollPane1 = new javax.swing.JScrollPane();
        jList = new javax.swing.JList<>();
        jPanel3 = new javax.swing.JPanel();
        jBtnUp = new javax.swing.JButton();
        jBtnDown = new javax.swing.JButton();
        jBtnDelete = new javax.swing.JButton();
        jBtnCancel = new javax.swing.JButton();
        jBtnDone = new javax.swing.JButton();
        jPanel2 = new javax.swing.JPanel();
        jPanel6 = new javax.swing.JPanel();
        jBtnAdd2 = new javax.swing.JButton();
        jPanel7 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jPanel4 = new javax.swing.JPanel();
        jPanel8 = new javax.swing.JPanel();
        jPanel9 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jPanel10 = new javax.swing.JPanel();
        jTFPeople = new javax.swing.JTextField();
        jPanel11 = new javax.swing.JPanel();
        jBtnAdd1 = new javax.swing.JButton();
        jBtnNormalize = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new java.awt.GridLayout(1, 0));

        jPanel1.setLayout(new java.awt.BorderLayout());

        jList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                jListValueChanged(evt);
            }
        });
        jScrollPane1.setViewportView(jList);

        jPanel1.add(jScrollPane1, java.awt.BorderLayout.CENTER);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnUp.setText("Move up");
        jBtnUp.setEnabled(false);
        jBtnUp.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnUpActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnUp);

        jBtnDown.setText("Move down");
        jBtnDown.setEnabled(false);
        jBtnDown.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDownActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDown);

        jBtnDelete.setText("Remove");
        jBtnDelete.setEnabled(false);
        jBtnDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDeleteActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDelete);

        jBtnCancel.setText("Cancel");
        jBtnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCancelActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnCancel);

        jBtnDone.setText("Done");
        jBtnDone.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnDoneActionPerformed(evt);
            }
        });
        jPanel3.add(jBtnDone);

        jPanel1.add(jPanel3, java.awt.BorderLayout.SOUTH);

        getContentPane().add(jPanel1);

        jPanel2.setLayout(new java.awt.BorderLayout());

        jPanel6.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnAdd2.setText("Add to List");
        jBtnAdd2.setEnabled(false);
        jBtnAdd2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAdd2ActionPerformed(evt);
            }
        });
        jPanel6.add(jBtnAdd2);

        jPanel2.add(jPanel6, java.awt.BorderLayout.SOUTH);

        jPanel7.setLayout(new java.awt.BorderLayout());

        jPanel5.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel1.setText("Look for people:");
        jPanel5.add(jLabel1);

        jPanel7.add(jPanel5, java.awt.BorderLayout.NORTH);

        jPanel4.setLayout(new java.awt.GridLayout(1, 0));
        jPanel7.add(jPanel4, java.awt.BorderLayout.CENTER);

        jPanel2.add(jPanel7, java.awt.BorderLayout.CENTER);

        jPanel8.setLayout(new java.awt.BorderLayout());

        jPanel9.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jLabel2.setText("Add person(s) by name (Last name, first name, separate by | or and)");
        jPanel9.add(jLabel2);

        jPanel8.add(jPanel9, java.awt.BorderLayout.NORTH);

        jPanel10.setLayout(new java.awt.GridLayout());
        jPanel10.add(jTFPeople);

        jPanel8.add(jPanel10, java.awt.BorderLayout.CENTER);

        jPanel11.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

        jBtnAdd1.setText("Add to List");
        jBtnAdd1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnAdd1ActionPerformed(evt);
            }
        });
        jPanel11.add(jBtnAdd1);

        jBtnNormalize.setText("Normalize");
        jBtnNormalize.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnNormalizeActionPerformed(evt);
            }
        });
        jPanel11.add(jBtnNormalize);

        jPanel8.add(jPanel11, java.awt.BorderLayout.SOUTH);

        jPanel2.add(jPanel8, java.awt.BorderLayout.NORTH);

        getContentPane().add(jPanel2);

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCancelActionPerformed
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jBtnCancelActionPerformed

    private void jBtnDoneActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDoneActionPerformed
        cancelled=false;
        this.setVisible(false);
        this.dispose();
    }//GEN-LAST:event_jBtnDoneActionPerformed

    private void jListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_jListValueChanged
        jBtnUp.setEnabled(jList.getSelectedIndex()>0);
        jBtnDown.setEnabled((jList.getSelectedIndex()>-1) && (jList.getSelectedIndex()<jList.getModel().getSize()-1));
        jBtnDelete.setEnabled(jList.getSelectedIndex()>-1);
    }//GEN-LAST:event_jListValueChanged

    private void jBtnAdd2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAdd2ActionPerformed
        String id=peopleSelector.getSelectedPersonIDs();
        Person person=new Person(RSC.getCurrentlySelectedLibrary(),id);
        peopleListModelDetailed.add(person);
        modified=true;
    }//GEN-LAST:event_jBtnAdd2ActionPerformed

    private void jBtnUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnUpActionPerformed
        int p=jList.getSelectedIndex();
        peopleListModelDetailed.swap(p-1,p);
        jList.setSelectedIndex(p-1);        
        modified=true;
    }//GEN-LAST:event_jBtnUpActionPerformed

    private void jBtnDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDownActionPerformed
        int p=jList.getSelectedIndex();
        peopleListModelDetailed.swap(p,p+1);
        jList.setSelectedIndex(p+1);
        modified=true;
    }//GEN-LAST:event_jBtnDownActionPerformed

    private void jBtnDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnDeleteActionPerformed
        int p=jList.getSelectedIndex();
        peopleListModelDetailed.remove(p);
        modified=true;
    }//GEN-LAST:event_jBtnDeleteActionPerformed

    private void jBtnAdd1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnAdd1ActionPerformed
        String peopleString=Parser.replace(jTFPeople.getText().trim()," And "," and ");
        String[] people;
        if (peopleString.contains(" and ")) {
            people=peopleString.split(" and ");
        } else {
            people=peopleString.split("\\|");
        }
        ArrayList<Person> peopleToBeAdded=new ArrayList<Person>();
        for (String person : people) {
            try {
                peopleToBeAdded.add(RSC.getCurrentlySelectedLibrary().findOrCreatePerson(person));
                modified=true;
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
        }
        peopleListModelDetailed.add(peopleToBeAdded);
    }//GEN-LAST:event_jBtnAdd1ActionPerformed

    private void jBtnNormalizeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnNormalizeActionPerformed
        jTFPeople.setText(Parser.lowerEndOfWords(jTFPeople.getText()));
    }//GEN-LAST:event_jBtnNormalizeActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnAdd1;
    private javax.swing.JButton jBtnAdd2;
    private javax.swing.JButton jBtnCancel;
    private javax.swing.JButton jBtnDelete;
    private javax.swing.JButton jBtnDone;
    private javax.swing.JButton jBtnDown;
    private javax.swing.JButton jBtnNormalize;
    private javax.swing.JButton jBtnUp;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JList<String> jList;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTFPeople;
    // End of variables declaration//GEN-END:variables

    @Override
    public void genericEventOccured(GenericCelsiusEvent e) {
        if (peopleSelector.SHOW_ITEMS==e.type) {
            String ids=peopleSelector.getSelectedPersonIDs();
            jBtnAdd2.setEnabled((ids!=null) && (ids.length()>0) && (!ids.contains(",")));
        } else if (e.type==peopleSelector.NAME_SELECTED) {
            String ids=peopleSelector.getSelectedPersonIDs();
            if ((ids!=null) && (ids.length()>0) && (!ids.contains(","))) {
                String id = peopleSelector.getSelectedPersonIDs();
                Person person = new Person(RSC.getCurrentlySelectedLibrary(), id);
                peopleListModelDetailed.add(person);
                modified = true;
            }
        }
    }
}
