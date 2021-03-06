/*
 * Class acting as an icon wallet.
 */

package atlantis.gui;

import java.util.ArrayList;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

public class KeyValueTableModel implements TableModel {

    public final ArrayList<String> keys;
    public final ArrayList<String> values;
    private final ArrayList<TableModelListener> Listeners;
    private final String ColName1;
    private final String ColName2;

    public KeyValueTableModel(String n1, String n2) {
        keys=new ArrayList();
        values=new ArrayList();
        Listeners=new ArrayList<>();
        ColName1=n1; ColName2=n2;
    }

    public void clear() {
        keys.clear();
        values.clear();
    }

    @Override
    public int getRowCount() {
        return(keys.size());
    }

    @Override
    public int getColumnCount() {
        return(2);
    }

    @Override
    public String getColumnName(int columnIndex) {
        if (columnIndex==0) return(ColName1);
        return(ColName2);
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return String.class;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return(false);
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        if (columnIndex==0) return(keys.get(rowIndex));
        return(values.get(rowIndex));
    }
    
    public void put(String key, String value) {
        int pos=keys.indexOf(key);
        values.remove(pos);
        values.add(pos, value);
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (keys.size()<=rowIndex) {
            for (int i=keys.size();i<=rowIndex;i++) {
                keys.add(null);
                values.add(null);
            }
        }
        if (columnIndex==0) keys.set(rowIndex, (String)aValue);
        if (columnIndex==1) values.set(rowIndex, (String)aValue);
        TableModelEvent e = new TableModelEvent( this, rowIndex, rowIndex,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.UPDATE );
        for(TableModelListener TML : Listeners)
            TML.tableChanged(e);

    }

    @Override
    public void addTableModelListener(TableModelListener l) {
        Listeners.add(l);
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
        Listeners.remove(l);
    }

    public void removeRow(int i) {
        keys.remove(i);
        values.remove(i);
        TableModelEvent e = new TableModelEvent( this, i, i,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.DELETE);
        for(TableModelListener TML : Listeners)
            TML.tableChanged(e);
    }

    public void addRow(String key, String value) {
        keys.add(key);
        values.add(value);
        TableModelEvent e = new TableModelEvent( this, keys.size()-1, keys.size()-1,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
        for(TableModelListener TML : Listeners)
            TML.tableChanged(e);
    }
    
    public void set(String key, String value) {
        int i=keys.indexOf(key);
        if (i==-1) {
            addRow(key,value);
        } else {
            values.set(i, value);
            TableModelEvent e = new TableModelEvent( this, i, i,
                TableModelEvent.ALL_COLUMNS, TableModelEvent.INSERT);
            for(TableModelListener TML : Listeners)
                TML.tableChanged(e);
        }
    }
    

}