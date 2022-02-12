/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.SwingWorkers;

import celsius.Resources;
import celsius.data.Attachment;
import celsius.data.Item;
import celsius.data.Library;
import celsius.gui.AddItems;
import atlantis.tools.Parser;
import celsius.tools.Plugin;
import celsius.tools.ToolBox;
import java.io.File;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWGetDetails extends SwingWorker<Void,Void> {
    
    Resources RSC;
    Item item;
    Attachment attachment;
    boolean plugins;
    public int state;  
    private final Library library;
    private final AddItems addItems;
    // -1: initialised, 1: running, 0: done
    
    private final String TI;
    
    public SWGetDetails(Resources RSC, Item item, boolean plugins, AddItems ai) {
        this.RSC=RSC;
        this.item=item;        
        this.plugins=plugins;
        attachment=null;
        if (item.linkedAttachments.size()>0) attachment=item.linkedAttachments.get(0);
        state=-1;
        library=RSC.getCurrentlySelectedLibrary();
        TI="SWGetDetails>";
        addItems=ai;
    }

    @Override
    protected Void doInBackground() throws Exception {
        try {
            RSC.out(TI+"SWGetDetails for "+item.toText(false));
            createPlainText();
            if (plugins) {
                for(Plugin plugin : RSC.plugins.listPlugins("auto-items", library)) {
                    // TODO: add paramters?
                    SWApplyPlugin swAP=new SWApplyPlugin(library, RSC, null,plugin,"",item);
                    swAP.execute();
                    swAP.get(); 
                }
                RSC.out(TI+"All Plugins applied");
            }
            if (isCancelled()) RSC.out(TI+"Interrupted!");
        } catch (Exception e) {       
            RSC.outEx(e);
        }
        RSC.out(TI+"finished.");
        state=0;
        // update information
        java.awt.EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                addItems.updateItemInformation();
                }
        });
        return null;
    }
    
    @Override
    public String toString() {
        if (item==null) {
            return("Uninitialised SWGetInfo");
        } else {
            return("SWGetInfo.java\nFileName:"+item.getS("new-path")+"\nInformation:"+item.toString());
        }
    }    
    
    // Complete plaintext information and read number of pages
    private synchronized void createPlainText() {
        try {
            // plaintxt already existing? Otherwise create it
            String filePath=attachment.get("path");
            String fileNameText=Parser.cutUntilLast(filePath,ToolBox.filesep)+ToolBox.filesep+"."+Parser.cutFromLast(filePath,ToolBox.filesep);
            attachment.put("$plaintext",fileNameText+".txt.gz");
            if (!(new File(attachment.get("$plaintext"))).exists()) {
                RSC.out(TI + "Getting Plain Txt :: " + filePath);
                RSC.configuration.extractText(TI, filePath, fileNameText + ".txt");
            }
            if ((new File(attachment.get("$plaintext"))).exists()) {
                if (attachment.isEmpty("pages")) {
                    RSC.out(TI + "Reading Number of Pages :: " + filePath);
                    attachment.put("pages", Integer.toString(ToolBox.readNumberOfPagesOf(RSC, TI, filePath, item.get("attachment-plaintext-0"))));
                }
            } else {
                attachment.put("$plaintext",null);
            }
        } catch (Exception e) {
            RSC.outEx(e);
            RSC.out(TI + "Error creating or reading plaintxt:" + e.toString());
        }
    }
    
}
