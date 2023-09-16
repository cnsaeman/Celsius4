
package celsius.components.tableTabs;

import atlantis.gui.HasManagedStates;
import celsius.components.library.Library;
import celsius.Resources;
import celsius.data.Item;
import celsius.components.library.LibraryChangeListener;
import celsius.data.TableRow;
import atlantis.tools.ExecutionShell;
import atlantis.tools.Parser;
import celsius.gui.MainFrame;
import celsius.tools.ToolBox;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

/**
 *
 * @author cnsaeman
 */
public final class CelsiusTable implements ListSelectionListener, MouseListener, TableColumnModelListener, LibraryChangeListener, KeyListener {
    
    // Types of listed objects 
    public static final int EMPTY_TABLE=-1;
    public static final int ITEM_TABLE=0;
    public static final int PERSON_TABLE=1;
    public static final int ITEM_HISTORY_TABLE=2;
    
    // Table types: anything item related 0..9 anything person related 10..20
    public final static int TABLETYPE_EMPTY=-1;
    public final static int TABLETYPE_ITEM_SEARCH=0;
    public final static int TABLETYPE_PERSON_SEARCH=10;
    public final static int TABLETYPE_ITEMS_IN_CATEGORY=1;
    public final static int TABLETYPE_PERSON_IN_CATEGORY=11;
    public final static int TABLETYPE_ITEM_WITH_KEYWORD=2;
    public final static int TABLETYPE_PERSON_WITH_KEYWORD=12;
    public final static int TABLETYPE_ITEM_WHEN_ADDED=3;
    public final static int TABLETYPE_PERSON_WHEN_ADDED=13;
    public final static int TABLETYPE_ITEMS_OF_PERSON=4;
    public final static int TABLETYPE_ITEMS_OF_PERSONS=5;
    public final static int TABLETYPE_ITEM_HISTORY=20;

    private final Resources RSC;

    public HashMap<String,String> properties;

    public String title;

    public Library library;
    
    public final JTable jtable;
    public final ThumbnailView thumbnailView;
    public MainFrame MF;
    public CelsiusTableModel celsiusTableModel;
    private ArrayList<Integer> sizes;
    
    private int tableType; // see above

    public String header;

    private boolean resizable;

    public int selectedfirst;
    public int selectedlast;

    public int sorted;

    public final ThreadPoolExecutor TPE;
    public final LinkedBlockingQueue<Runnable> LBQ;
    
    public int postID;

    public CelsiusTable(MainFrame mf,Library library,String title,int tableType) {
        super();
        this.library=library;
        library.addLibraryChangeListener(this);
        sorted=-1;
        this.title=title;
        properties=new HashMap<>();
        MF=mf;
        RSC=mf.RSC;
        this.tableType=tableType;
        LBQ=new LinkedBlockingQueue<Runnable>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
        thumbnailView=new ThumbnailView(this);
        resizable=false;
        int objectType=-1;
        if ((tableType>=0) && (tableType<10)) objectType=CelsiusTable.ITEM_TABLE;
        if ((tableType>=10) && (tableType<20)) objectType=CelsiusTable.PERSON_TABLE;
        if (tableType==20) objectType=CelsiusTable.ITEM_HISTORY_TABLE;
        celsiusTableModel=new CelsiusTableModel(RSC,library,objectType);
        jtable=new JTable();
        jtable.setModel(celsiusTableModel);
        jtable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
        jtable.setDragEnabled(true);
        jtable.setTransferHandler(new TableTransferHandler(this));
        jtable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jtable.getSelectionModel().addListSelectionListener(this);
        jtable.addMouseListener(this);
        jtable.addKeyListener(this);
        jtable.getTableHeader().addMouseListener(this);
        jtable.getColumnModel().addColumnModelListener(this);
        jtable.setDefaultRenderer(Object.class, new CellRenderer());
        adjustContextMenu();
        resetTableProperties();
        jtable.setVisible(true);
    }

    public CelsiusTable(CelsiusTable celsiusTable) {
        super();
        sorted=-1;
        title=celsiusTable.title;
        properties=new HashMap<>();
        resizable=false;
        tableType=celsiusTable.tableType;
        LBQ=new LinkedBlockingQueue<>();
        TPE=new ThreadPoolExecutor(5, 5, 500L, TimeUnit.DAYS,LBQ);
        thumbnailView=new ThumbnailView(this);
        RSC=celsiusTable.RSC;
        MF=celsiusTable.MF;
        jtable=new JTable();
        celsiusTableModel.addTableModelListener(thumbnailView);
        celsiusTableModel.tableview=true;
        jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        jtable.setDragEnabled(true);
        jtable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        jtable.getSelectionModel().addListSelectionListener(this);
        jtable.addMouseListener(this);
        jtable.getTableHeader().addMouseListener(this);
        jtable.getColumnModel().addColumnModelListener(this);
        setLibraryAndTableType(celsiusTable.library,celsiusTable.getObjectType());
        CelsiusTableModel tc=celsiusTable.getDTM();
        addRows(tc.tableRows);
        setTableType(celsiusTable.getTableType());
        jtable.setDefaultRenderer(Object.class, new CellRenderer());
        resetTableProperties();
        sizes=new ArrayList<>();
        for (Integer i : celsiusTable.sizes)
            sizes.add(i);
        resizeTable(false);
        jtable.setVisible(true);
    }
    
    /**
     * Get table type: Item/Person/etc.
     * 
     * @return 
     */
    public int getObjectType() {
        return(celsiusTableModel.objectType);
    }

    public void setHeader(String h) {
        header=h;
    }

    private void resetTableProperties() {
        celsiusTableModel.tableview=true;
        setSizes(library.itemTableColumnSizes);
        if (jtable.getColumnCount()<sizes.size()) return;
        jtable.setGridColor(Color.LIGHT_GRAY);
        if (!celsiusTableModel.tableview) thumbnailView.updateView();
    }

    public synchronized void close() {
        LBQ.clear();
        TPE.shutdownNow();
        library.removeLibraryChangeListener(this);
        int i=RSC.celsiusTables.indexOf(this);
        RSC.celsiusTables.remove(i);
        MF.jTPTabList.remove(i);
        if (MF.jTPTabList.getTabCount() == 0) {
            RSC.guiStates.adjustState("mainFrame", "tabAvailable",false);
            RSC.guiStates.adjustState("mainFrame", "itemSelected", false);
            RSC.guiStates.adjustState("mainFrame", "personSelected", false);
}
    }

    public void switchView() {
        if (celsiusTableModel.tableview) switchToThumbnails();
        else switchToTable();
    }

    public void switchToThumbnails() {
        if (!celsiusTableModel.tableview) return;
        int i=MF.jTPTabList.indexOfComponent(jtable.getParent().getParent());
        final JScrollPane scrollpane = new JScrollPane(thumbnailView);
        scrollpane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        MF.jTPTabList.setComponentAt(i,scrollpane);
        thumbnailView.updateView();
        celsiusTableModel.tableview=false;
    }

    public void switchToTable() {
        if (celsiusTableModel.tableview) return;
        int i=MF.jTPTabList.indexOfComponent(thumbnailView.getParent().getParent());
        final JScrollPane scrollpane = new JScrollPane(jtable);
        MF.jTPTabList.setComponentAt(i,scrollpane);
        celsiusTableModel.tableview=true;
    }

    public void setLibraryAndTableType(Library l, int tt) {
        tableType=tt;
        if (library!=l) {
            celsiusTableModel.setLibrary(l);
            library=l;
            setSizes(library.itemTableColumnSizes);
            celsiusTableModel.clearToObjectType(-1);
        }
        celsiusTableModel.tableview=true;
        int objectType=-1;
        if ((tableType>=0) && (tableType<10)) objectType=CelsiusTable.ITEM_TABLE;
        if ((tableType>=10) && (tableType<20)) objectType=CelsiusTable.PERSON_TABLE;
        celsiusTableModel.clearToObjectType(objectType);
        adjustContextMenu();
    }

    public void setTableType(int t) {
        tableType=t;
    }

    public int getTableType() {
        return(tableType);
    }

    public CelsiusTableModel getDTM() {
        return(celsiusTableModel);
    }

    public synchronized void removeRow(TableRow tableRow) {
        removeID(tableRow.id);
    }

    public synchronized void removeID(String id) {
        int i=celsiusTableModel.IDs.indexOf(id);
        if (i>-1) {
            celsiusTableModel.removeRow(i);

        }
    }

    public synchronized void removeAllRows() {
        thumbnailView.clear();
        properties.clear();
        celsiusTableModel.clear();
    }
        
    public void adjustContextMenu() {
        if (tableType<10) {
            jtable.setComponentPopupMenu(MF.jPMItemTable);
        } else if (tableType>=10) {
            jtable.setComponentPopupMenu(MF.jPMPeopleTable);
        } else {
            jtable.setComponentPopupMenu(null);
        }
    }
    
    public TableRow getRow(int row) {
        return(celsiusTableModel.tableRows.get(row));
    }
    
    public TableRow getCurrentlySelectedRow() {
        int i=jtable.getSelectedRow();
        if (i==-1) return(null);
        return(getRow(i));
    }

    public int getSelectedRow() {
        return(jtable.getSelectedRow());
    }
    
    public boolean hasSingleSelection() {
        return(jtable.getSelectedRowCount()==1);
    }

    public boolean hasMultiSelection() {
        return(jtable.getSelectedRowCount()>1);
    }

    public ArrayList<TableRow> getSelectedRows() {
        ArrayList<TableRow> out=new ArrayList<>();
        int[] selRows=jtable.getSelectedRows();
        /*for (int i1 = selRows.length - 1; i1 > -1; i1--)
            out.add(DTM.Items.get(selRows[i1]));*/
         for (int i1 = 0; i1<selRows.length; i1++)
            out.add(celsiusTableModel.tableRows.get(selRows[i1]));
        return(out);
    }
    
    public String getSelectedIDsString() {
        StringBuilder ids=new StringBuilder();
        int[] selRows=jtable.getSelectedRows();
        for (int i1 = 0; i1 < selRows.length; i1++) {
            ids.append(',');
            ids.append(celsiusTableModel.IDs.get(selRows[i1]));
        }
        ids.deleteCharAt(0);
        return(ids.toString());
    }

    public void updateStats() {
        if (getObjectType()==CelsiusTable.ITEM_TABLE) {
            properties.put("selecteditems", String.valueOf(jtable.getSelectedRowCount()));
            properties.put("currentitems", String.valueOf(celsiusTableModel.tableRows.size()));
            ArrayList<TableRow> rows = celsiusTableModel.tableRows;
            if (jtable.getSelectedRowCount() > 0) {
                rows = getSelectedRows();
            }
            properties.put("currentpages", String.valueOf(library.getPagesForItems(rows)));
            properties.put("currentduration", ToolBox.formatSeconds(0));
        } else if (getObjectType()==CelsiusTable.PERSON_TABLE) {
            properties.put("selectedpersons", String.valueOf(jtable.getSelectedRowCount()));
            properties.put("currentpersons", String.valueOf(celsiusTableModel.tableRows.size()));
            ArrayList<TableRow> rows = celsiusTableModel.tableRows;
            if (jtable.getSelectedRowCount() > 0) {
                rows = getSelectedRows();
            }
            properties.put("itemsforpersons", String.valueOf(library.getNumberOfItemsForPeople(rows)));
        }
    }

    /**
     * Add an item to the table
     */
    public synchronized void addRows(ResultSet rs) {
        ArrayList<TableRow> tableRows=new ArrayList<>();
        try {
            while (rs.next()) {
                tableRows.add(new Item(library, rs));
            }
            addRows(tableRows);
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    /**
     * Add an item to the table
     */
    public synchronized void addRows(List<TableRow> tableRows) {
        sorted=-1;
        celsiusTableModel.addRows(tableRows);
    }

    /**
     * Add an item to the table
     */
    public synchronized void addRows(int pid, List<TableRow> tableRows) {
        //System.out.println(">> Received rows for pid "+pid+", my pid is "+postID);
        if (pid!=postID) return;
        celsiusTableModel.addRows(tableRows);
    }
    
    /**
     * Add an item to the table
     */
    public synchronized void addRow(TableRow tableRow) {
        sorted=-1;
        if (celsiusTableModel.IDs.indexOf(tableRow.id)==-1) {
            celsiusTableModel.addRow(tableRow);
            celsiusTableModel.updateStats();
        }
    }

    public synchronized void updateRow(TableRow tableRow) {
        celsiusTableModel.updateRow(tableRow);
        int rowNumber=jtable.getSelectedRow();
        if (rowNumber>-1) {
            if (tableRow.id.equals(celsiusTableModel.tableRows.get(rowNumber).id)) {
                RSC.guiInformationPanel.updateGUI();
            }
        }
    }

    public synchronized void updateRow(String id) {
        celsiusTableModel.updateRow(id);
        int rowNumber=jtable.getSelectedRow();
        if (rowNumber>-1) {
            if (id.equals(celsiusTableModel.tableRows.get(rowNumber).id)) {
                RSC.guiInformationPanel.updateGUI();
            }
        }
    }

    public synchronized void updateAll() {
        celsiusTableModel.updateAll();
    }

    public void sortItems(int col, boolean force) {
        if (force) sorted=col;
        celsiusTableModel.sortItems(col,sorted);
        if (col==sorted) sorted=-1;
        else sorted=col;
    }

    /**
     * Autoformat Table
     */
    public void resizeTable(boolean updateView) {
        synchronized (jtable) {
            if (jtable.getColumnCount() < sizes.size()) {
                return;
            }
            resizable = false;
            int fixedwidth=0;
            int totalwidth=0;
            for (Integer w : sizes) {
                if (w<0) {
                    totalwidth-=w;
                    fixedwidth-=w;
                } else {
                    totalwidth+=w;
                }
            }
            TableColumn column;
            int width = jtable.getWidth();

            double ratio = ((double) width-fixedwidth) / ((double) totalwidth-fixedwidth);
            if (ratio<0) ratio=-ratio;
            int i = 0;
            for (Integer size : sizes) {
                column = jtable.getColumnModel().getColumn(i);
                if (size<0) {
                    column.setPreferredWidth(-size);
                    column.setMaxWidth(-size);
                    column.setMinWidth(-size);
                    column.setResizable(false);
                } else {
                    column.setResizable(true);
                    column.setPreferredWidth((int) (size * ratio));
                }
                i++;
            }
            jtable.setRowHeight(RSC.guiTools.guiScale(24));
            if (updateView) {
                if (!celsiusTableModel.tableview) thumbnailView.updateView(); //#### test to exclude
                MF.guiInfoPanel.updateGUI();
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) {
            return;
        }
        MF.switchToLibrary(library);
        final ListSelectionModel lsm = (ListSelectionModel) e.getSource();
        if (getObjectType() == 0) {
            RSC.guiStates.adjustState("mainFrame", "itemSelected", !lsm.isSelectionEmpty());
            RSC.guiStates.adjustState("mainFrame", "personSelected", false);
        } else {
            RSC.guiStates.adjustState("mainFrame", "itemSelected", false);
            RSC.guiStates.adjustState("mainFrame", "personSelected", !lsm.isSelectionEmpty());
        }
        if (!lsm.isSelectionEmpty()) {
            // TODO this is not correct
            String ft=getRow(getSelectedRow()).get("filetype");
            /* TODO secondary viewers MF.jMActions.removeAll();
            MF.jMActions.setEnabled(false);
            if (ft!=null) {
                String secondary=RSC.configuration.getSecondaryViewers(ft);
                if (secondary!=null) {
                    final String[] viewers=ToolBox.stringToArray(secondary);
                    for (int i=0; i<viewers.length;i++) {
                        MF.jMActions.setEnabled(true);
                        final String actionName=Parser.cutUntil(viewers[i], ":");
                        final CelsiusTable IT=this;
                        JMenuItem jMI=new JMenuItem(actionName);
                        jMI.addActionListener(new java.awt.event.ActionListener() {
                            @Override
                            public void actionPerformed(java.awt.event.ActionEvent evt) {
                                performItemTableAction(actionName, IT);
                            }
                        });
                        MF.jMActions.add(jMI);
                    }
                }
            }*/

            updateStats();
            MF.guiInfoPanel.updateGUI();
        } else {
            jtable.setComponentPopupMenu(null);
        }
    }

    @Override
    public synchronized void mouseClicked(MouseEvent e) {
        if ((e.getSource().getClass() == JLabel.class) || (e.getSource().getClass() == JTextField.class)) {
            thumbnailView.requestFocus();
            String name=((JComponent)e.getSource()).getName();
            int selectedRow = celsiusTableModel.IDs.indexOf(((JComponent) e.getSource()).getName());
            selectedfirst=selectedRow;
            selectedlast=selectedRow;
            thumbnailView.adjustSelection();
            jtable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            if (e.getClickCount() == 2) {
                MF.guiInfoPanel.updateGUI();
                RSC.viewCurrentlySelectedObject();
            }
            if (e.getClickCount() == 1) {
                MF.guiInfoPanel.updateGUI();
            }
        }
        if ((e.getSource() == jtable) && (e.getClickCount() == 2)) {
            MF.guiInfoPanel.updateGUI();
            RSC.viewCurrentlySelectedObject();
        }
        if ((e.getSource() == jtable.getTableHeader())) {
            if (e.getButton()==MouseEvent.BUTTON1) {
                sortItems(jtable.getTableHeader().columnAtPoint(e.getPoint()),false);
                resizeTable(false);
            } else {
                int c=this.jtable.getTableHeader().columnAtPoint(e.getPoint());
                TableColumn[] cols=new TableColumn[jtable.getColumnCount()];
                for (int i=0;i<cols.length;i++) {
                    cols[i]=jtable.getColumnModel().getColumn(i);
                }
                (new TableHeaderPopUp(RSC,library,c,cols,sizes)).show(jtable.getTableHeader(), e.getX(), e.getY());
            }
        }
    }

    @Override
    public void mousePressed(MouseEvent e) {
        resizable=true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        resizable=false;
    }

    @Override
    public void mouseEntered(MouseEvent e) {
    }

    @Override
    public void mouseExited(MouseEvent e) {
    }

    @Override
    public void columnAdded(TableColumnModelEvent e) {
    }

    @Override
    public void columnRemoved(TableColumnModelEvent e) {
    }

    @Override
    public void columnMoved(TableColumnModelEvent e) {
    }

    @Override
    public synchronized void columnMarginChanged(ChangeEvent e) {
        if (resizable) {
            sizes=new ArrayList<>();
            for (int i=0;i<jtable.getColumnCount();i++) {
                int w=jtable.getColumnModel().getColumn(i).getWidth();
                if (library.itemTableColumnSizes.get(i)<0) w=-w;
                sizes.add(w);
            }
        }
    }

    @Override
    public void columnSelectionChanged(ListSelectionEvent e) {
    }

    public synchronized void setSizes(ArrayList<Integer> prefsizes) {
        sizes=new ArrayList<>();
        if (this.tableType==CelsiusTable.TABLETYPE_ITEM_HISTORY) sizes.add(RSC.guiTools.guiScale(120));
        for (Integer i : prefsizes) sizes.add(i);
    }

    /**
     * Reload the current item table
     */
    public synchronized void refresh() {
        for (TableRow tableRow : celsiusTableModel.tableRows) {
            if (tableRow.currentLoadLevel>1) tableRow.reloadfullInformation();
        }
    }

    @Override
    public void libraryElementChanged(String type,String id) {
        if (type.equals("item") && (getObjectType()==CelsiusTable.ITEM_TABLE)) updateRow(id);
        if (type.equals("person") && (getObjectType()==CelsiusTable.PERSON_TABLE)) updateRow(id);
    }
    
    public void performItemTableAction(String aname, CelsiusTable table) {
        RSC.out("MAIN>Action performed: " + aname);
        String currentType="nothing";
        String cmdln=null;
        for (TableRow tableRow : table.getSelectedRows()) {
            if (tableRow.get("filetype")!=null) {
                if (!currentType.equals(tableRow.get("filetype"))) {
                    currentType=tableRow.get("filetype");
                    String secondary=RSC.configuration.getSecondaryViewers(currentType);
                    String[] commands=ToolBox.stringToArray(secondary);
                    cmdln=null;
                    for (int i=0;i<commands.length;i++) {
                        if (commands[i].startsWith(aname+":")) {
                            cmdln=Parser.cutFrom(commands[i],":")+" ";
                            break;
                        }
                    }
                }
                if (cmdln!=null) {
                    String actcmdln = cmdln.replace("%from%", tableRow.getCompletedDirKey("location"));
                    RSC.out("JM>Action command: " + actcmdln);
                    (new ExecutionShell(actcmdln, 0, true)).start();
                }
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {
        if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            e.consume();
        }        
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            e.consume();
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if( e.getKeyCode() == KeyEvent.VK_ENTER ) {
            RSC.viewCurrentlySelectedObject();
            e.consume();
        }
    }

    void removeSelectedFromTable() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public class CellRenderer extends DefaultTableCellRenderer {

        public CellRenderer() {
            super();
        }

        @Override
        public void setValue(Object value) {
            if (ImageIcon.class.isInstance(value)) {
                setIcon((Icon)((ImageIcon)value));
                setText("");
            } else {
                this.setBorder(BorderFactory.createEmptyBorder(0, 5, 0, 5));
                setIcon(null);
                setText((String)value);
            }
        }

    }
    
    public void selectFirst() {
        if (jtable.getRowCount()>0) {
            jtable.getSelectionModel().setSelectionInterval(0,0);
            jtable.requestFocus();
        }
    }
    
}
