/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.integrity;

import celsius.components.SWListItems;
import celsius.components.bibliography.BibTeXRecord;
import celsius.data.Item;
import celsius.components.tableTabs.CelsiusTable;
import java.sql.ResultSet;
import java.util.List;

/**
 *
 * @author cnsaeman
 */
public class SWBibTeXIntegrity extends SWListItems {
    
    public final int batchSize=1000;
    
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
                        if (!BibTeXRecord.isBibTeXConsistent(bibtex)) {
                            Item item = new Item(library, rs.getString(1));
                            publish(item);
                        }
                    }
                }
                setProgress((100*offset)/library.numberOfItems);
                offset+=batchSize;
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }
        return(null);
    }
}
