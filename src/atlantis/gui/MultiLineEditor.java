package atlantis.gui;

import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.KeyStroke;

/**
 *
 * @author  cnsaeman
 */
public class MultiLineEditor extends javax.swing.JDialog {
    
    public String text;
    public boolean cancelled;
    
    private final AtlantisTextArea jText;
    
    /** Creates new form DialogMultiLineEditor */
    public MultiLineEditor(GuiTools guiTools, String title, String text) {
        super(guiTools.MF, true);
        setIconImage(guiTools.appIcon);
        this.setTitle(title);
        initComponents();
        jText = new AtlantisTextArea(guiTools.guiScale(12));
        jText.setColumns(20);
        jText.setRows(5);
        jScrollPane1.setViewportView(jText);
        jText.getActionMap().put("Submit", new AbstractAction("Submit") {
            public void actionPerformed(ActionEvent evt) {
                submit();
            }
        });
        jText.getActionMap().put("Cancel", new AbstractAction("Cancel") {
            public void actionPerformed(ActionEvent evt) {
                cancel();
            }
        });
        jText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE,0), "Cancel");
        jText.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER,InputEvent.CTRL_DOWN_MASK), "Submit");

        this.text=text;
        jText.setText(text);
        jText.setCaretPosition(0);
        this.setSize(guiTools.guiScale(400), guiTools.guiScale(300));
        guiTools.centerDialog(this);
    }

    public void setLineWrapping(boolean b) {
        jText.setLineWrap(b);
        if (b) jText.setWrapStyleWord(true);
    }
    
    public void submit() {
        text=jText.getText();
        cancelled=false;
        setVisible(false);        
    }
    
    public void cancel() {
        cancelled=true;
        setVisible(false);        
    }
    
    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jBtnCancel = new javax.swing.JButton();
        jBtnApply = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jBtnCancel.setText("Cancel");
        jBtnCancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnCancelActionPerformed(evt);
            }
        });

        jBtnApply.setText("Apply");
        jBtnApply.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jBtnApplyActionPerformed(evt);
            }
        });

        jScrollPane1.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jScrollPane1KeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(0, 416, Short.MAX_VALUE)
                        .addComponent(jBtnApply)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jBtnCancel))
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jBtnCancel)
                    .addComponent(jBtnApply))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void jBtnCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnCancelActionPerformed
        cancel();
    }//GEN-LAST:event_jBtnCancelActionPerformed

    private void jBtnApplyActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jBtnApplyActionPerformed
        submit();
    }//GEN-LAST:event_jBtnApplyActionPerformed

    private void jScrollPane1KeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jScrollPane1KeyPressed
        // TODO add your handling code here:
    }//GEN-LAST:event_jScrollPane1KeyPressed
    
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jBtnApply;
    private javax.swing.JButton jBtnCancel;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
    
}
