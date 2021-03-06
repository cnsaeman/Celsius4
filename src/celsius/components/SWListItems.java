/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components;

import celsius.Resources;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.data.TableRow;
import celsius.components.tableTabs.CelsiusTable;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWListItems extends SwingWorker<Void, TableRow> implements PropertyChangeListener {

    public final CelsiusTable celsiusTable;
    public final Library library; 
    public final Resources RSC;
    public final int postID;
    public int done;
    
    /**
     *  Constructor, read in information
     *  itemtable, postID
     */
    public SWListItems(CelsiusTable it, int pid) {
        super();
        celsiusTable=it;
        library=celsiusTable.library;
        RSC=celsiusTable.library.RSC;
        postID=pid;
        done=0;
        setProgress(0);
        addPropertyChangeListener(this);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                RSC.MF.jPBSearch.setMaximum(100);
                RSC.MF.jPBSearch.setValue(0);
                RSC.MF.setThreadMsg("Searching...");
                RSC.guiStates.adjustState("mainFrame","itemSelected", false);
                RSC.guiStates.adjustState("mainFrame","personSelected", false);
                celsiusTable.removeAllRows();
            }
        });    }
    
    @Override
    protected void process(List<TableRow> tableRows) {
        celsiusTable.addRows(postID, tableRows);
    }
    
    @Override
    protected void done() {
        try {
            if (!isCancelled()) {
                setProgress(100);
                RSC.MF.setThreadMsg("Ready.");
                celsiusTable.resizeTable(true);
                RSC.MF.guiInfoPanel.updateGUI();
                /*System.out.println(">> All complete and fine for "+postID);
                System.out.println(">> Rows in table: "+celsiusTable.celsiusTableModel.getRowCount());*/
            } else {
                //System.out.println(">> Thread cancelled for "+postID);
            }
        } catch (Exception ex) {
            RSC.outEx(ex);
        }
    }

    @Override
    protected Void doInBackground() throws Exception {
        return null;
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        RSC.MF.jPBSearch.setValue(this.getProgress());
    }
    
}
