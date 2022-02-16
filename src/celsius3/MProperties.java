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


package celsius3;

import celsius.components.bibliography.BibTeXRecord;
import atlantis.tools.FileTools;
import atlantis.tools.TextFile;
import celsius.tools.ToolBox;
import static celsius.tools.ToolBox.getFirstPage;
import java.util.ArrayList;
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
    public ArrayList<String> files;

    public MProperties() {
        super();
        changed=false;
        files=new ArrayList<String>();
    }

    public MProperties(BibTeXRecord btr) {
        super();
        changed=false;
        for (String k : btr.keySet()) {
            put(k,btr.get(k));
        }
        files=new ArrayList<String>();
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
    
    public void deleteFiles() {
        for (String fkey:files) {
            String fn=get(fkey);
            FileTools.deleteIfExists(fn);
        }
    }
    
    public String getFirstPage() {
        if (files.isEmpty()) return("");
        String fkey=files.get(0);
        String fn=get(fkey);
        if (fn==null) return("");
        return(ToolBox.getFirstPage(fn));
    } 
    
    public String toString() {
        String out="";
        for (String key : keySet()) {
            out+="\n"+key+"::"+get(key);
        }
        return(out);
    }
           
}
