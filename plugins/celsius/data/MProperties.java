//
// Celsius Library System
// (w) by C. Saemann
//
// MProperties.java
//
// This class contains the thread for searching...
//
// typesafe
//
// checked: 11/2009
//


package celsius.data;

import celsius.data.BibTeXRecord;
import celsius.tools.Parser;
import celsius.tools.ToolBox;
import java.util.HashMap;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author cnsaeman
 */
public final class MProperties extends HashMap<String,String> {
    
    public boolean changed;

    public MProperties() {
        super();
        changed=false;
    }

    public MProperties(BibTeXRecord btr) {
        super();
        changed=false;
        for (String k : btr.keySet()) {
            put(k,btr.get(k));
        }
    }

    @Override
    public String put(String key,String value) {
        if (value==null) return(null);
        if ((super.get(key)==null) || ((super.get(key)!=null) && (!value.equals(super.get(key))))) {
            if (!key.startsWith("##") && !key.startsWith("$$")) changed=true;
            return(super.put(key, value));
        }
        return(value);
    }

    public void putNT(String key,String value) {
        if (value.length()!=0) super.put(key,value);
    }

    public void reset() {
        changed=false;
    }

    public boolean isNotSet(String s) {
        return(!this.containsKey(s));
    }

    public boolean isEmpty(String s) {
        if (!this.containsKey(s)) return(true);
        return(getS(s).equals(""));
    }

    public String getS(String k) {
        String ret=get(k);
        if (ret==null) return("");
        return(ret);
    }

           
}
