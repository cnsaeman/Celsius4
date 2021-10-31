/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.SwingWorkers;

import celsius.data.BibTeXRecord;
import celsius.data.Item;
import celsius.gui.CelsiusTable;
import java.sql.ResultSet;
import java.util.List;

/**
 *
 * @author cnsaeman
 */
public class SWBibTeXIntegrity extends SWListItems {
    
    public final int batchSize=100;
    
    public SWBibTeXIntegrity(CelsiusTable it, int pid) {
        super(it,pid);
    }
    
    @Override
    public Void doInBackground() {
        try {
            int offset=0;
            while (offset+batchSize<library.numberOfItems) {
                ResultSet rs = library.executeResEX("SELECT id,bibtex from items LIMIT " + String.valueOf(batchSize) + " OFFSET " + String.valueOf(offset) + ";");
                while (rs.next() && (!isCancelled())) {
                    String bibtex = rs.getString(2);
                    if ((bibtex != null) && (!bibtex.isBlank())) {
                        if (!BibTeXRecord.isBibTeXConsist(bibtex)) {
                            Item item = new Item(library, rs.getString(1));
                            publish(item);
                        }
                    }
                }
                setProgress(offset/library.numberOfItems);
                offset+=batchSize;
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return(null);
    }
}
