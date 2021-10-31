/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius3;

import celsius3.Item3;
import java.util.Iterator;

/**
 *
 * @author cnsaeman
 */
public class LibraryIterator3 implements Iterator<Item3> {

    int pos;
    int max;
    Library3 Lib;

    public LibraryIterator3 (Library3 lib) {
        pos=-1;
        Lib=lib;
        max=lib.getSize()-1;
    }

    public boolean hasNext() {
        return(pos<max);
    }

    public Item3 next() {
        pos++;
        return(new Item3(Lib,pos));
    }

    public void remove() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
