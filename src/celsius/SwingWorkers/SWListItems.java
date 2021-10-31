/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.SwingWorkers;

import celsius.Resources;
import celsius.data.Item;
import celsius.data.Library;
import celsius.data.TableRow;
import celsius.gui.CelsiusTable;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWListItems extends SwingWorker<Void, TableRow> {

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
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                RSC.MF.jPBSearch.setMaximum(library.numberOfItems);
                RSC.MF.jPBSearch.setValue(0);
                RSC.MF.setThreadMsg("Searching...");
                RSC.guiStates.adjustState("mainFrame","itemSelected", false);
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
    
}
