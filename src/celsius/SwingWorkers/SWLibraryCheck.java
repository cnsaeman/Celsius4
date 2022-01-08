/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.SwingWorkers;

import celsius.Resources;
import celsius.data.Library;
import celsius.tools.FileTools;
import atlantis.tools.Parser;
import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWLibraryCheck extends SwingWorker<Void,Void> {

    private final Resources RSC;
    private final ProgressMonitor PM;
    private final StringBuffer out;
    
    public SWLibraryCheck(Resources rsc,ProgressMonitor pm) {
        RSC=rsc;
        PM=pm;
        out=new StringBuffer();
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        Library library=RSC.getCurrentlySelectedLibrary();
        String itemFolder=library.completeDir(library.config.get("item-folder"));
        File f = new File(itemFolder);
        ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(f.list()));
        
        // checks on attachments
        out.append("Verifying all attachments...\n");
        
        // check attachments that are not linked to an item
        ResultSet rs=library.executeResEX("SELECT id,path FROM attachments LEFT JOIN item_attachment_links on attachments.id=item_attachment_links.attachment_id WHERE item_attachment_links.item_id IS NULL;");
        StringBuilder ids=new StringBuilder();
        ArrayList<String> paths=new ArrayList<>();
        while (rs.next()) {
            ids.append(',');
            ids.append(rs.getString(1));
            paths.add(rs.getString(2));
        }
        if (paths.size()>0) {
            out.append("Some attachments were not linked to items. The ones removed from the database pointed to the following files:\n");
            for (String path : paths) {
                out.append(path);
            }
            library.executeEX("DELETE FROM attachments WHERE ID in ("+ids.substring(1)+");");
        }
        
        // check that the files for attachments exist. Otherwise, delete.
        rs=library.executeResEX("SELECT id, path FROM attachments;");
        int standardAnswer=-1;
        while (rs.next()) {
            String path=library.completeDir(rs.getString(2));
            if (path.startsWith(itemFolder)) {
                fileNames.remove(Parser.cutFrom(path,itemFolder+"/"));
            }
            if (!(new File(path)).exists()) {
                String id=rs.getString(1);
                library.executeEX("DELETE FROM attachments WHERE id="+id+";");
                library.executeEX("DELETE FROM item_attachment_links WHERE attachment_id="+id+";");
                out.append("Attachment with path "+path+" not found. Removed from library.\n");
            }
        }
        out.append("Verifying attachments completed\n---\n");
        if (fileNames.size()>0) {
            out.append("The following files were not linked in attachments and moved to the base folder of the library:\n");
            for (String fn : fileNames) {
                out.append(fn+"\n");
                FileTools.moveFile(itemFolder+"/"+fn,library.baseFolder+"/"+fn);
            }
            out.append("---\n");
        }
        return(null);
    }
    
    @Override
    protected void done() {
        try {
            RSC.showLongInformation("Result for Library Check", out.toString());
        } catch (Exception ignore) {
        }
    }
    
}
