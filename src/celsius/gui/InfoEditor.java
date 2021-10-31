/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius.gui;

import celsius.Resources;
import celsius.gui.MLInfoEditor;
import celsius.gui.CBRenderer;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import javax.swing.*;
import javax.swing.table.TableCellEditor;

/**
 *
 * @author cnsaeman
 */
public class InfoEditor extends AbstractCellEditor implements TableCellEditor {

    JComponent component;
    final Resources RSC;
    final HashMap<String,ArrayList<String>> choicefields;
    final List<String> iconTags;
    final HashMap<String,String> IconDictionary;

    public InfoEditor(Resources r,HashMap<String,ArrayList<String>> cf,String[] icf,HashMap<String,String> idict) {
        RSC=r;
        choicefields=cf;
        iconTags=Arrays.asList(icf);
        IconDictionary=idict;
    }

    @Override
    public Object getCellEditorValue() {
        String tmp;
        if (component.getClass().getName().equals("javax.swing.JComboBox")) {
            tmp=(String)((JComboBox)component).getSelectedItem();
        } else {
            if (component.getClass().getName().equals("javax.swing.JTextField")) {
                tmp=(String)((JTextField)component).getText();
            } else {
                tmp=((MLInfoEditor)component).value;
                if (tmp.length()==0) tmp="<unknown>";
            }
        }
        return (tmp);
    }

    @Override
    public Component getTableCellEditorComponent(final JTable table, Object value, boolean isSelected, final int row, final int column) {
        if (column==0) return(null);
        String tag=((String)table.getValueAt(row, 0)).toLowerCase();
        if (!choicefields.containsKey(tag)) {
            String tmp=(String)value;
            if (tmp.length()>100) {
                component=new MLInfoEditor(RSC,tag,tmp);
            } else {
                component=new JTextField();
                if (tmp.equals("<unknown>")) tmp="";
                ((JTextField)component).setText(tmp);
                ((JTextField)component).setBorder(javax.swing.BorderFactory.createEmptyBorder(1, 1, 1, 1));
            }
            return(component);
        } else {
            final JComboBox jCB=new JComboBox();
            component=jCB;
            jCB.addActionListener(new java.awt.event.ActionListener() {
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    table.setValueAt(jCB.getSelectedItem(), row, column);
                    table.removeEditor();
                }
            });
            DefaultComboBoxModel DCBM=new DefaultComboBoxModel();
            for (String ft : choicefields.get(tag)) {
                DCBM.addElement(ft);
            }
            jCB.setModel(DCBM);
            if (iconTags.contains(tag))
                jCB.setRenderer(new CBRenderer(RSC.icons,IconDictionary,RSC));
            jCB.setSelectedItem(value);
            return(component);
        }
    }

}
