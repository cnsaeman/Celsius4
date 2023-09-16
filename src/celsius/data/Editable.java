package celsius.data;

import atlantis.gui.KeyValueTableModel;
import celsius.components.library.Library;

/**
 *
 * @author cnsaeman
 */
public interface Editable {

    public Library getLibrary();
    public KeyValueTableModel getEditModel();
    public void put(String key, String value);
    public String get(String key);
    public boolean containsKey(String key);
    public void save();
    public void updateShorts();
    public void notifyChanged();
    
}
