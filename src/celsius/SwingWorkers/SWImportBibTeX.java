/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package celsius.SwingWorkers;

import celsius.Resources;
import celsius.data.Attachment;
import celsius.data.BibTeXRecord;
import celsius.data.Item;
import celsius.data.Library;
import celsius.gui.AddItems;
import atlantis.tools.Parser;
import celsius.tools.Plugin;
import atlantis.tools.TextFile;
import celsius.tools.ToolBox;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWImportBibTeX extends SwingWorker<Void,Item> {

    Resources RSC;
    boolean plugins;
    public int state;  
    // -1: initialised, 1: running, 0: done
    private final Library library;
    private final AddItems addItems;
    
    private final String filename;
    private final String TI;
    
    public SWImportBibTeX(Resources rsc, AddItems ai, String fn, Library lib) {
        RSC=rsc;
        addItems=ai;
        library=lib;
        filename=fn;
        TI="SWImportBibTeX>";
        state=-1;
    }

    @Override
    protected Void doInBackground() throws Exception {
        String file=TextFile.ReadOutFile(filename);
        // Regexp: first (?m) enables multiline mode.
        String[] entries=file.split("(?m)^\\s*@");
        // iterate over all but first
        boolean first=true;
        for (String entry : entries) {
            if (first) {
                first=false;
            } else {
                BibTeXRecord BTR = new BibTeXRecord("@" + entry.trim());
                Item item = new Item(library);
                if (BTR.parseError == 0) {
                    item.put("bibtex", BTR.toString());
                    for (Plugin plugin : RSC.plugins.listPlugins("import", library)) {
                        SWApplyPlugin swAP = new SWApplyPlugin(library, RSC, null, plugin, "", item);
                        swAP.execute();
                        swAP.get();
                    }
                } 
                if (item.isEmpty("title")) {
                    item.put("title", "Parsing error: " + entry.substring(0, 30));
                }
                publish(item);
            }
        }
        return null;
    }
    
    @Override
    protected void process(List<Item> items) {
        for (Item item : items) {
            addItems.addImportedItem(item);
        }
    }

    
}
