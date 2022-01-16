/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package atlantis.tools;

import java.util.ArrayList;

/**
 *
 * @author cnsaeman
 */
public class JSONParser {

    private int pos;
    private int tmppos;
    private int upperBound;
    private int lowerBound;
    private final String content;
    private final ArrayList<Integer> markers;
    
    public JSONParser(String c) {
        content=c;
        pos=0;
        upperBound=content.length();
        lowerBound=0;
        markers=new ArrayList<>();
    }

    public boolean moveToNextTag(String tag) {
        int i=content.indexOf("\""+tag+"\"",pos+1);
        if ((i>-1) && (i<upperBound)) {
           pos=i;
           return(true);
        } else {
           return(false);
        }
    }
    
    public void pushPosition() {
        markers.add(pos);
    }
    
    public void pullPosition() {
        pos=markers.get(markers.size()-1);
        markers.remove(markers.size()-1);
    }
    
    public String extractStringFromTag(String tag) {
        int i=content.indexOf("\""+tag+"\":\"");
        if ((i>-1) && (i<upperBound)) {
           pos=i+4+tag.length();
           i=content.indexOf("\"",pos);
           if (i==-1) return(null);
           return(content.substring(pos, i));
        } else {
           return(null);
        }
    }

    public String extractStringFromNextTag(String tag) {
        int i=content.indexOf("\""+tag+"\":\"",pos);
        if ((i>-1) && (i<upperBound)) {
           pos=i+4+tag.length();
           i=content.indexOf("\"",pos);
           if (i==-1) return(null);
           return(content.substring(pos, i));
        } else {
           return(null);
        }
    }

    public Boolean extractBooleanFromNextTag(String tag) {
        int i=content.indexOf("\""+tag+"\":",pos);
        if ((i>-1) && (i<upperBound)) {
           pos=i+3+tag.length();
           String sc=content.substring(pos,pos+10);
           return(content.charAt(pos)=='t');
        } else {
           return(null);
        }
    }
    
    public Integer extractIntFromNextTag(String tag) {
        int i=content.indexOf("\""+tag+"\":",pos);
        try {
            if ((i > -1) && (i < upperBound)) {
                pos = i + 3 + tag.length();
                i = content.indexOf(",", pos);
                return (Integer.valueOf(content.substring(pos, i)));
            } else {
                return (null);
            }
        } catch (NumberFormatException e) {
            return(null);
        }
    }
    
    public boolean moveToFirstTag(String tag) {
        int i=content.indexOf("\""+tag+"\"",lowerBound);
        if ((i>-1) && (i<upperBound)) {
           pos=i;
           return(true);
        } else {
           return(false);
        }
    }
    
    // find the matching closing bracket recursively
    public void closeBracket(char bc) {
        StringBuffer toWorkThrough=new StringBuffer(); // buffer of brackets that need to be closed
        toWorkThrough.append(bc);
        char pc=' ';
        char cc=' ';
        while (tmppos<content.length()-1) {
            tmppos++;
            pc=cc;
            cc=content.charAt(tmppos);
            if ((cc == bc) && (pc != '\\')) {
                toWorkThrough.deleteCharAt(toWorkThrough.length()-1);
                if (toWorkThrough.length()>0) {
                    bc=toWorkThrough.charAt(toWorkThrough.length()-1);
                } else {
                    return;
                }
            } else if ((cc == '"') && (pc != '\\')) {
                toWorkThrough.append('"');
                bc='"';
            } else if ((cc == '[') && (pc != '\\')) {
                toWorkThrough.append(']');
                bc=']';
            } else if ((cc == '{') && (pc != '\\')) {
                toWorkThrough.append('}');
                bc='}';
            }
        }
    }
        
    public ArrayList<JSONParser> extractArray() {
        ArrayList<JSONParser> out=new ArrayList<>();
        pos=content.indexOf('[',pos);
        tmppos=pos;
        closeBracket(']');
        int maxpos=tmppos;
        tmppos=pos;
        int lastpos=pos;
        while ((tmppos>-1) && (tmppos<maxpos)) {
            closeBracket(',');
            System.out.println(content.substring(lastpos+1,tmppos-1));
            out.add(new JSONParser(content.substring(lastpos+1,tmppos-1)));
            lastpos=tmppos;
        }
        return(out);
    }

    // make more sophisticated, recursive
    public void restrictLevel() {
        int j=content.indexOf(':',pos)+1;
        lowerBound=j;
        tmppos=j;
        if (content.charAt(j)=='{') {
            closeBracket('}');
        } else if (content.charAt(j)=='[') {
            closeBracket(']');
        }
        upperBound=tmppos;
    }

    public void releaseLevel() {
        lowerBound=0;
        upperBound=content.length();
    }
    
    public void moveToLowerBound() {
        pos=lowerBound;
    }
    
    public void fixLowerBound() {
        lowerBound=pos;
    }

    public void fixUpperBound() {
        upperBound=pos;
    }
    
}
