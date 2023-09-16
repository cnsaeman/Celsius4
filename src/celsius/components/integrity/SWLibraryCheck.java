package celsius.components.integrity;

import celsius.Resources;
import celsius.components.library.Library;
import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import java.io.File;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import javax.swing.ProgressMonitor;
import javax.swing.SwingWorker;

/**
 * A swing worker that performs consistency checks on the library data and corrects information, if necessary.
 * 
 * @author cnsaeman
 */
public class SWLibraryCheck extends SwingWorker<Void,Void> {

    private final Resources RSC;
    private final ProgressMonitor progressMonitor;
    private final StringBuffer out;
    
    public SWLibraryCheck(Resources RSC,ProgressMonitor progressMonitor) {
        this.RSC=RSC;
        this.progressMonitor=progressMonitor;
        out=new StringBuffer();
    }
    
    @Override
    protected Void doInBackground() throws Exception {
        Library library=RSC.getCurrentlySelectedLibrary();
        String itemFolder=library.completeDir(library.config.get("item-folder"));
        File f = new File(itemFolder);
        ArrayList<String> fileNames = new ArrayList<String>(Arrays.asList(f.list()));
        ResultSet rs;
        StringBuilder ids;
        
        // checks on attachments
        out.append("Verifying all attachments...\n");
        
        // check that in category links, the linked items exist. If not, delete the links.
        rs=library.executeResEX("SELECT item_category_links.item_id FROM item_category_links LEFT JOIN items on item_category_links.item_id=items.id WHERE items.id IS NULL;");
        ids=new StringBuilder();
        while (rs.next()) {
            ids.append(',');
            ids.append(rs.getString(1));
        }
        if (ids.length()>1) {
            out.append("Some non-existing items were linked to categories. These links have been removed.\n");
            library.executeEX("DELETE FROM item_category_links WHERE item_id in ("+ids.substring(1)+");");
        }
        
        // check attachments that are not linked to an item
        rs=library.executeResEX("SELECT id,path FROM attachments LEFT JOIN item_attachment_links on attachments.id=item_attachment_links.attachment_id WHERE item_attachment_links.item_id IS NULL;");
        ids=new StringBuilder();
        ArrayList<String> paths=new ArrayList<>();
        while (rs.next()) {
            ids.append(',');
            ids.append(rs.getString(1));
            paths.add(rs.getString(2));
        }
        if (!paths.isEmpty()) {
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
                out.append("Attachment with path ").append(path).append(" not found. Removed from library.\n");
            }
        }
        out.append("Verifying attachments completed\n---\n");
        if (!fileNames.isEmpty()) {
            out.append("The following files were not linked in attachments and moved to the base folder of the library:\n");
            for (String fn : fileNames) {
                out.append(fn).append("\n");
                FileTools.moveFile(itemFolder+"/"+fn,library.basefolder+"/"+fn);
            }
            out.append("---\n");
        }
        return(null);
    }
    
    @Override
    protected void done() {
        try {
            RSC.guiTools.showLongInformation("Result for Library Check", out.toString());
        } catch (Exception ignore) {
        }
    }
    
}
