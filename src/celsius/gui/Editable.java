/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.gui;

import celsius.data.KeyValueTableModel;
import celsius.data.Library;

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
