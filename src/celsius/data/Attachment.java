package celsius.data;

import celsius.components.library.Library;
import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import celsius.tools.ToolBox;
import java.io.File;
import java.sql.ResultSet;

/**
 * This class describes a file attachment to an item.
 * 
 * @author cnsaeman
 */
public class Attachment extends TableRow {
    
    public String fileSize;
    public int order;
    public Item parent;
    
    public Attachment(Library lib,Item item,String i) {
        super(lib,"attachments",i,lib.attachmentPropertyKeys);
        parent=item;
        tableHeaders=lib.attachmentPropertyKeys;
    }

    public Attachment(Library lib,Item item) {
        super(lib,"attachments",lib.attachmentPropertyKeys);
        tableHeaders=lib.attachmentPropertyKeys;
        parent=item;
        order=parent.linkedAttachments.size();
    }

    public Attachment(Library lib, Item item, ResultSet rs) {
        super(lib,"attachments",rs,lib.attachmentPropertyKeys);
        tableHeaders=lib.attachmentPropertyKeys;
        parent=item;
    }
    
    @Override
    public void save() throws Exception {
        if (dirtyFields.contains("path")) {
            put("md5",FileTools.md5checksum(parent.getCompletedDir(get("path"))));
        }
        super.save();
    }

    public String getHTML(int type) {
        if (fileSize==null) {
            fileSize=FileTools.fileSize(library.completeDir(getS("path")));
        }
        StringBuilder out=new StringBuilder();
        if (type==1) {
            out.append("<a href=\"http://$$view-attachment-");
            out.append(String.valueOf(order));
            out.append("\">");
        }
        out.append(getS("name"));
        if (type==1) {
            out.append("</a>");
        }
        out.append(" (");
        out.append(getS("filetype"));
        out.append("-file, ");
        out.append(getS("pages"));
        out.append(" pages, ");
        out.append(fileSize);
        out.append(")");
        return(out.toString());
    }
    
    public void attachToParent() {
        order=parent.linkedAttachments.size();
        parent.linkedAttachments.add(this);
    }

    public void saveAttachmentLinkToDatabase() {
        library.executeEX("INSERT INTO item_attachment_links (item_id,attachment_id,ord) VALUES (?,?,?)",new String[]{parent.id,id,String.valueOf(order)});
    }
    
    public String standardFileName() {
        String filetype=getS("filetype");
        parent.put("$$filetype",filetype);
        String standardFileName=Parser.cutProhibitedChars2(parent.library.itemNamingConvention.fillIn(parent,false));
        if (standardFileName.length()>150) {
            if (standardFileName.endsWith("."+filetype)) {
                standardFileName=standardFileName.substring(0,149-filetype.length())+"."+filetype;
            } else standardFileName=standardFileName.substring(0,150);
        }
        standardFileName=Parser.normalizeSpecialCharacters(standardFileName);
        return(standardFileName);
    }
    
    public void setPath(String path) {
        put("path",parent.library.compressFilePath(path));
    }
    
    public String getFullPath() {
        return(parent.getCompletedDir(get("path")));
    }
    
    public boolean isFilePresent() {
        return((new File(getFullPath())).exists());
    }
    
    public String normalizeForSave(String s) {
        s=Parser.replace(s, "\n", " ");
        s=Parser.replace(s, "\t", " ");
        return(s);
    }
    
    /**
     * Moves attachment to standard location, finding a free file name
     * @param save: performs a save on attachment
     * @return 2 : standard folder could not be created, 1 : problem copying, 0 : all good.
     */
    public int moveToStandardLocation(boolean save) {
        if (parent.library.addingMode==0) return(0);
        if (!parent.guaranteeStandardFolder()) {
            library.RSC.guiTools.showWarning("Warning:","Standard item folder could not be created.");
            return (2);
        }
        String newFileName = library.getStandardFolder(this) + ToolBox.FILE_SEPARATOR + standardFileName();
        String newFullPath = normalizeForSave(library.completeDir(newFileName, ""));
        
        // add numbers until a free filename is found
        if ((new File(newFullPath)).exists()) {
            int number = 0;
            String filetype=getS("filetype");
            String stem=Parser.cutUntilLast(newFullPath,filetype);
            while ((new File(newFullPath)).exists()) {
                number++;
                newFullPath = stem+ToolBox.fillLeadingZeros(String.valueOf(number), 3)+"."+filetype;
            }
        }
        try {
            FileTools.moveFile(getFullPath(), newFullPath);
            put("path", library.compressFilePath(newFullPath));
            if (save) save();
        } catch (Exception ex) {
            library.RSC.out("Error moving file to " + newFileName);
            library.RSC.outEx(ex);
            return(1);
        }
        return(0);
    }
    
    public void delete() {
        try {
            parent.linkedAttachments.remove(this);
            parent.library.executeEX("DELETE FROM item_attachment_links WHERE attachment_id=" + id + ";");
            parent.library.executeEX("DELETE FROM attachments WHERE id=" + id + ";");
            FileTools.deleteIfExists(getFullPath());
        } catch (Exception ex) {
            parent.library.RSC.outEx(ex);
        }
        
    }
    
    
}
