/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.tableTabs;

import celsius.components.library.Library;
import celsius.Resources;
import celsius.components.tableTabs.CelsiusTable;
import atlantis.tools.Parser;
import celsius.data.Item;
import celsius.data.Person;
import celsius.data.TableRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author cnsaeman
 */
public class CelsiusTableModel extends AbstractTableModel {
    
    public static final int CELSIUS_TABLE_ITEM_TYPE=0;
    public static final int CELSIUS_TABLE_PERSON_TYPE=1;

    private final Resources RSC;
    public int objectType; // 0 : items, 1 : persons, see above
    public final ArrayList<String> columns;
    public final ArrayList<String> headers;
    public final ArrayList<String> IDs;
    public final ArrayList<TableRow> tableRows;
    public Library library;
    public List<String> iconFields;
    public int currentPages;
    public int currentDuration;
    public boolean tableview;

    /**
     * Constructor
     */
    public CelsiusTableModel(Resources rsc,Library lib,int tt) {
        super();
        RSC = rsc;
        setLibrary(lib);
        columns = new ArrayList<>();
        headers = new ArrayList<>();
        IDs = new ArrayList<>();
        tableRows = new ArrayList<>();
        objectType=-2; // undefined
        clearToObjectType(tt);
    }
    
    public void setLibrary(Library lib) {
        library = lib;
        if (lib!=null) {
            iconFields = Arrays.asList(library.iconFields);
        } else {
            iconFields = new ArrayList<>();
        }
    }

    public void clear() {
        int numberOfRows = tableRows.size();
        currentPages = 0;
        currentDuration = 0;
        IDs.clear();
        tableRows.clear();
        if (numberOfRows>0) fireTableRowsDeleted(0, numberOfRows - 1);
    }
    
    public void clearToObjectType(int ot) {
        if (objectType==ot) {
            clear();
        } else {
            objectType = ot;
            int numberOfRows = tableRows.size();
            headers.clear();
            columns.clear();
            if (objectType == CelsiusTable.EMPTY_TABLE) {
                headers.add("Empty table");
                columns.add(null);
            } else if (objectType == CelsiusTable.ITEM_TABLE) {
                if (library.itemTableHeaders.isEmpty()) {
                    for (String th : library.itemTableTags) {
                        headers.add(Parser.lowerEndOfWords2(Parser.cutUntil(th, "&")));
                        columns.add(th);
                    }
                } else {
                    columns.addAll(library.itemTableTags);
                    headers.addAll(library.itemTableHeaders);
                }
            } else if (objectType == CelsiusTable.PERSON_TABLE) {
                if (library.personTableHeaders.isEmpty()) {
                    for (String th : library.personTableTags) {
                        headers.add(Parser.lowerEndOfWords2(Parser.cutUntil(th, "&")));
                        columns.add(th);
                    }
                } else {
                    columns.addAll(library.personTableTags);
                    headers.addAll(library.personTableHeaders);
                }
            }
            currentPages = 0;
            currentDuration = 0;
            IDs.clear();
            tableRows.clear();
            if (numberOfRows > 0) {
                fireTableRowsDeleted(0, numberOfRows - 1);
            }
        }
    }
    
    @Override
    public synchronized Object getValueAt(int row, int column) {
        if ((row < getRowCount()) && (column < getColumnCount())) {
            TableRow tableRow = tableRows.get(row);
            String tag = columns.get(column);
            if (tag==null) return("NULL");
            if (iconFields.contains(tag)) {
                return (RSC.icons.getIcon(tableRow.getIconField(tag)));
            }
            return (tableRow.getExtended(tag));
        }
        return ("ERROR");
    }

    public synchronized void removeRow(int i) {
        IDs.remove(i);
        tableRows.remove(i);
        fireTableRowsDeleted(i, i);
    }

    /**
     * render the whole table non-editable
     */
    @Override
    public boolean isCellEditable(int row, int col) {
        return (false);
    }
    
    public synchronized void sortItems(final int i, int sorted) {
        final boolean invertSort = (i == sorted);
        final ArrayList<TableRow> tmp = new ArrayList<>();
        for (TableRow tableRow : tableRows) {
            tmp.add(tableRow);
        }
        if (tmp.isEmpty()) {
            return;
        }
        int type = 0;
        if (i == 1000) {
            Collections.sort(tmp, library.getComparator(null, invertSort, type));
        } else {
            if (library.itemTableColumnTypes.get(i).startsWith("unit")) {
                type = 1;
            }
            Collections.sort(tmp, library.getComparator(columns.get(i), invertSort, type));
        }
        tableRows.clear();
        tableRows.addAll(tmp);
        IDs.clear();
        for (TableRow tableRow : tableRows) {
            IDs.add(tableRow.id);
        }
        fireTableRowsUpdated(0, tableRows.size() - 1);
    }

    @Override
    public String getColumnName(int i) {
        if (headers.size() <= i) {
            return ("");
        }
        return (headers.get(i));
    }

    public void addRow(TableRow tableRow) {
        tableRows.add(tableRow);
        IDs.add(tableRow.id);
        //if (!tableview) TNV.addItem(getRowCount()-1);
        fireTableRowsInserted(tableRows.size() - 1, tableRows.size() - 1);
    }

    public void updateStats() {
        if (objectType==0) {
            currentPages = library.getPagesForItems(tableRows);
        }
    }

    @Override
    public int getRowCount() {
        return (tableRows.size());
    }

    @Override
    public int getColumnCount() {
        return (columns.size());
    }
    
    public void updateRow(String id) {
        int pos=IDs.indexOf(id);
        if (pos>-1) {
            if (objectType==CelsiusTable.ITEM_TABLE) {
                Item item=new Item(library,id);
                if (item.currentLoadLevel>-1) {
                    updateRow(item);
                } else {
                    IDs.remove(pos);
                    tableRows.remove(pos);
                    fireTableRowsDeleted(pos, pos);
                }
            } else if (objectType==CelsiusTable.PERSON_TABLE) {
                Person person=new Person(library,id);
                if (person.currentLoadLevel>-1) {
                    updateRow(person);
                } else {
                    IDs.remove(pos);
                    tableRows.remove(pos);
                    fireTableRowsDeleted(pos, pos);
                }
            }
        }
    }

    public void updateRow(TableRow tableRow) {
        int row = IDs.indexOf(tableRow.id);
        tableRows.set(row, tableRow);
        fireTableRowsUpdated(row, row);
    }

    public void updateAll() {
        for (int r = 0; r < IDs.size(); r++) {
            tableRows.set(r, new Item(library, IDs.get(r)));
        }
        fireTableRowsUpdated(0, IDs.size() - 1);
    }

    public void addRows(List<TableRow> newRows) {
        int startRow = tableRows.size() - 1;
        for (TableRow tableRow : newRows) {
            tableRows.add(tableRow);
            IDs.add(tableRow.id);
            //if (!tableview) TNV.addItem(getRowCount()-1);
        }
        int endRow = tableRows.size() - 1;
        fireTableRowsInserted(startRow, endRow);
    }

}
