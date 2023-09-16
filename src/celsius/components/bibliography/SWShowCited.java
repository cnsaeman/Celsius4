/*
 * Perhaps batch size, optimize
 */
package celsius.components.bibliography;

import celsius.components.SWListItems;
import celsius.data.Item;
import celsius.components.tableTabs.CelsiusTable;
import atlantis.tools.TextFile;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author cnsaeman
 */
public class SWShowCited  extends SWListItems {
    
    public final int batchSize=100;
    public final String filename;
    public final ArrayList<String> citationTags;
    
    public SWShowCited(CelsiusTable it, int pid, String fn) {
        super(it,pid);
        filename=fn;
        citationTags=new ArrayList<>();
    }
    
    @Override
    public Void doInBackground() {
        try {
            int offset=0;
            getCitationTags();
            for (String tag : citationTags) {
                ResultSet rs = library.executeResEX("SELECT "+library.itemTableSQLTags+" FROM items WHERE `citation-tag` = ?;",tag);
                while (rs.next() && (!isCancelled())) {
                    Item item = new Item(library, rs);
                    publish(item);
                }
                
            }
        } catch (Exception e) {
            RSC.outEx(e);
        }
        // set search bar status to determinate
        return(null);
    }
    
    public void examine(String contents,String patternString) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher=pattern.matcher(contents);
        while (matcher.find()) {
            String[] tags=matcher.group(1).split(",");
            for (String tag : tags) {
                tag=tag.trim();
                if (!citationTags.contains(tag)) citationTags.add(tag);
            }
        }
    }
    
    /**
     * Get all citation tags from a latex-file
     */
    public void getCitationTags() {
        String contents=TextFile.readOutFile(filename);
        examine(contents,"\\\\cite\\{([^\\}]+)\\}");
        examine(contents,"\\\\cite\\[[^\\]]+\\]\\{([^\\}]+)\\}");
        examine(contents,"\\\\bibitem\\{([^\\}]+)\\}");
    }
    
    
}
