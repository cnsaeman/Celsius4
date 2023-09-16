package celsius.components.addItems;

import celsius.components.plugins.SWApplyPlugin;
import celsius.Resources;
import celsius.components.bibliography.BibTeXRecord;
import celsius.data.Item;
import celsius.components.library.Library;
import celsius.components.plugins.Plugin;
import atlantis.tools.TextFile;
import java.util.List;
import javax.swing.SwingWorker;

/**
 *
 * @author cnsaeman
 */
public class SWImportBibTeX extends SwingWorker<Void,Item> {

    Resources RSC;
    public int state;  
    // -1: initialised, 1: running, 0: done
    private final Library library;
    private final AddItems addItems;
    
    private final String filename;
    private final String TI;
    
    public SWImportBibTeX(Resources RSC, AddItems addItems, String filename, Library library) {
        this.RSC=RSC;
        this.addItems=addItems;
        this.library=library;
        this.filename=filename;
        TI="SWImportBibTeX>";
        state=-1;
    }

    @Override
    protected Void doInBackground() throws Exception {
        String file=TextFile.readOutFile(filename);
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
