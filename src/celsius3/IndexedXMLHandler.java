//
// Celsius Library System v2
// (w) by C. Saemann
//
// XMLHandler.java
//
// This class contains the simple XML-engine used by Celsius
//
// typesafe
// 
// checked 15.09.2007
//

package celsius3;

import atlantis.tools.TextFile;
import atlantis.tools.Parser;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Scanner;

/**
 * Class to work with a certain format of XML-files, starting with <XML etc>
 * then <!--<tags> [list of tags] </tags>
 * <type-of-file> <element> <[tag]>[value]</[tag]> ... </element>...</type-of-file><-->
 */

public class IndexedXMLHandler {
  
    public final ArrayList<String> XMLTags;                   // ArrayList of tags
    private final ArrayList<ArrayList<String>> XMLElements;    // ArrayList of XMLelements
    private final ArrayList<Integer> index;
    
    public int position;                          // position in file
    //public int sortedPosition;                    // position in sorted iteration
    //public String lastSorted;                     // last tag which was sorted
    
    public String Comment;
    
    public String source;                         // source file from which it was read in
    public String type;                           // type of file
    public boolean endReached;                    // end of file reached?
    
    public String lastError;

    public IndexedXMLHandler() {
        source="";
        position=0;
        endReached=false;
        XMLTags=new ArrayList<String>();
        XMLElements=new ArrayList<ArrayList<String>>();
        index=new ArrayList<Integer>();
        lastError="";
        Comment="";
    }
    
    /**
     * Constructor with filename
     */
    public IndexedXMLHandler(String s) throws IOException, Exception {
        source=s;
        position=0;
        endReached=false;
        XMLTags=new ArrayList<String>();
        XMLElements=new ArrayList<ArrayList<String>>();
        index=new ArrayList<Integer>();
        lastError="";
        ReadHeader();
        ReadElements();
        Comment="";
    }

    /**
     * Constructor with filename
     */
    public IndexedXMLHandler(String s,ArrayList<String> tags) throws IOException, Exception {
        source=s;
        position=0;
        endReached=false;
        XMLTags=tags;
        XMLElements=new ArrayList<ArrayList<String>>();
        index=new ArrayList<Integer>();
        lastError="";
        //ReadHeader();
        ReadElements();
        Comment="";
    }

    public int checkIntegrity() {
        if (this.index.size()!=this.XMLElements.size()) return(-100);
        for (int i=0;i<index.size();i++) {
            String t=this.get(i,"id");
            int i1=Integer.valueOf(t);
            int i2=index.get(i);
            if (i1!=i2) return(i);
        }
        return(-1);
    }
    
    /**
     * Create an empty XMLhandler file target of type type
     */
    public static void Create(String type,String target) throws IOException {
        TextFile f1=new TextFile(target,false);
        f1.putString("<?xml version=\"1.0\"?>");
        f1.putString("<"+type+">");
        f1.putString("</"+type+">");
        f1.close();
    }
    
    /**
     * reads in the tags of the XMLhandler file
     */
    private void ReadHeader() throws IOException {
        TextFile f1=new TextFile(source);
        String s1;
        StringBuffer s2=new StringBuffer(300);
        
        // Check Header
        s1=f1.getString();
        if (!s1.equals("<?xml version=\"1.0\"?>")) throw(new IOException("XML Header wrong!"));
        s1=f1.getString();
       
        while ((s1.trim().length()==0) && (f1.ready()))
            s1=f1.getString();
        
        // Check for comment and read it
        if (s1.trim().startsWith("<!--")) {
            s1=Parser.cutFrom(s1,"<!--");
            while (s1.indexOf("-->")<0) {
                s2.append(s1);
                s1=f1.getString();
            }
            s2.append(Parser.cutUntil(s1,"-->"));
            Comment=s2.toString();
            while ((s1.trim().length()==0) && (f1.ready()))
                s1=f1.getString();
        }
        if (s1.trim().startsWith("<"))
            type=Parser.cutUntil(Parser.cutFrom(s1,"<"),">");
        else
            type="UndefinedType";
        f1.close();
    }
  
    // alternative method
    private void addElt(ArrayList<String> elt) throws Exception {
        int ind=XMLTags.indexOf("id");
        Integer id=Integer.valueOf(elt.get(ind));
        int insertion = Collections.binarySearch(index,id);
        if (insertion>-1) {
            lastError="Element with that index already existing:\n"+Integer.toString(id);
            throw(new Exception ("Element with that index already existing: "+Integer.toString(id)));
        }
        index.add(-insertion-1,id);
        XMLElements.add(-insertion-1, elt);
    }


    /**
     * reads in all elements (alternative method)
     */
    private void ALTReadElements() throws IOException, Exception {
        long start = System.currentTimeMillis();
        System.out.println("Reading file... "+source+".bin");
        FileInputStream fis = new FileInputStream(source+".bin");
        BufferedInputStream bis= new BufferedInputStream(fis);
        DataInputStream dis = new DataInputStream(bis);
        String rl;
        ArrayList<String> elt=new ArrayList<String>();
        int id=-1233;
        while(dis.available()>0) {
            rl=dis.readUTF();
            if (rl.equals("<element>")) {
                if (!elt.isEmpty()) addElt(elt);
                elt=new ArrayList<String>();
                for (int i=0;i<XMLTags.size();i++) elt.add(null);
            } else {
                String tag=rl;
                String value=dis.readUTF();
                int ind=XMLTags.indexOf(tag);
                if (ind==-1) {
                    XMLTags.add(tag);
                    ind=XMLTags.size()-1;
                    elt.add(null);
                }
                elt.set(ind,value);
            }
        }
        dis.close();
        fis.close();
        long finish = System.currentTimeMillis();
        System.out.println("Time: "+String.valueOf(finish-start));
    }
    
    
    
    /**
     * reads in all elements
     */
    private void ReadElements() throws IOException, Exception {
       // long start = System.currentTimeMillis();
       // System.out.println("Reading file... "+source+".bin");
       Scanner scanner = new Scanner(new File(source));
       scanner.useDelimiter("<element>");
       if (scanner.hasNext()) scanner.next();
       while (scanner.hasNext()) {
         String s=scanner.next();
         try {
            ParseElement(s);
         } catch (Exception e) {
             lastError="Error reading element:\n"+s;
         }
       }
       scanner.close();
        // long finish = System.currentTimeMillis();
        // System.out.println("Time: "+String.valueOf(finish-start));
    }
    
    /**
     * parses an element, rewritten
     */
    private void ParseElement(String s) throws Exception {
        String t1,t3;
        ArrayList<String> elt=new ArrayList<String>();
        for (int i=0;i<XMLTags.size();i++) elt.add(null);
        int i,j,k,l;
        Integer id=-1;
        i=s.indexOf("<");
        while (i>-1) {
            // Cut tag
            j=s.indexOf(">",i);
            t1=s.substring(i+1,j);
            if ((t1.length()==0) || (t1.charAt(0)=='/')) {
                l=j;
            } else {
                int ind=XMLTags.indexOf(t1);
                if (ind==-1) {
                    XMLTags.add(t1);
                    ind=XMLTags.size()-1;
                    elt.add(null);
                }
                // Cut value
                k=s.indexOf("</"+t1+">",j);
                l=s.indexOf(">",k+3);
                t3=s.substring(j+1,k).trim();
                if (t1.equals("id")) {
                    //if (t3.length()==7) t3="1"+t3.substring(1); // to correct old index distinction by a leading 0
                    id=Integer.valueOf(t3);
                }
                elt.set(ind,t3);
            }
            i=s.indexOf("<",l);
        }
        if (id==-1) throw(new Exception("Element without an id in file!+\n"+s));
        int insertion = Collections.binarySearch(index,id);
        if (insertion>-1) {
            lastError="Element with that index already existing:\n"+s;
            throw(new Exception ("Element with that index already existing: "+Integer.toString(id)));
        }
        index.add(-insertion-1,id);
        XMLElements.add(-insertion-1, elt);
    }

    public int getPosition(String id) {
        return(getPosition((int)Integer.valueOf(id)));
    }

    public int getPosition(int id) {
        return(Collections.binarySearch(index,new Integer(id)));
    }
    
    /**
     * return this element as a ArrayList
     */
    public ArrayList<String> thisElement() {
        return((ArrayList<String>)XMLElements.get(position));
    }
    
    /**
     * go to next Element in order, endReached is true, if arrived at end of data
     */
    public void nextElement() {
        position++;
        if (position>=XMLElements.size()) {
            position=XMLElements.size()-1;
            endReached=true;
        }
    }
    
    /**
     * go to previous Element in order, endReached is true, if arrived at beginning of data
     */
    public void previousElement() {
        position--;
        if (position<0) {
            position=0;
            endReached=true;
        }
    }
    
    /**
     * set tag to value, check for tag
     */
    public void put(int pos,String tag, String value) {
        if (XMLElements.isEmpty()) addEmptyElement();
        int ind=XMLTags.indexOf(tag);
        if (ind==-1) {
            XMLTags.add(tag); 
            XMLElements.get(pos).add(null);
            ind=XMLTags.size()-1;
        }
        if (XMLElements.size()>pos) {
            int s=XMLElements.get(pos).size();
            if (s<=ind) {
                for(int i=s;i<=ind;i++)
                    XMLElements.get(pos).add(null);
            }
            (XMLElements.get(pos)).set(ind,value);
        }
    }
    
    /**
     * set tag tag of the current element to value
     * length safe check for tag
     */
    public void put(String tag, String value) {
        put(position,tag,value);
    }

    public String getDataElement(int pos,int ind) {
        if ((XMLElements.size()>pos) && (ind>-1) && (XMLElements.get(pos).size()>ind))
            return((XMLElements.get(pos)).get(ind));
        else return(null);
    }

    public String getDataElement(int ind) {
        return(getDataElement(position,ind));
    }
    
    /**
     * get value for tag s of element at position pos
     */
    public String get(int pos,String tag) {
        int ind=XMLTags.indexOf(tag);
        return(getDataElement(pos,ind));
    }
    
    /**
     * get value for tag s of current element
     */
    public String get(String tag) {
        return(get(position,tag));
    }

    /**
     * create empty element, go to this element
     */
    public int addEmptyElement() {
        int id=0;
        int insertion=Collections.binarySearch(index,id);
        while ((insertion=Collections.binarySearch(index,id))>-1) id=(int)(Math.random()*100000);
        ArrayList<String> elt=new ArrayList<String>();
        for (int i=0;i<XMLTags.size();i++) elt.add(null);
        XMLElements.add(-insertion-1, elt);
        index.add(-insertion-1,id);
        position=-insertion-1;
        return(id);
    }
    
    /**
     * create empty element with one tag already defined, go to this element and sort, if lastsorted is something
     */
    public int addEmptyElement(String tag,String entry) {
        int id=addEmptyElement();
        put(tag,entry);
        return(id);
    }

    /**
     * deletes all elements
     */
    public void clear() {
        position=0;
        endReached=true;
        XMLElements.clear();
    }
    
    /**
     * removes the current element from the XMLhandler
     */
    public void deleteCurrentElement() {
        XMLElements.remove(position);
        index.remove(position);

        if (position>=XMLElements.size()) {
            endReached=true;
            position=XMLElements.size()-1;
        }
    }
    
    /**
     * removes the element at position pos from the XMLhandler
     */
    public void deleteElement(int pos) {
        XMLElements.remove(pos);
        index.remove(pos);

        if (position>=pos) {
            position--;
            if (position>=XMLElements.size()) {
                endReached=true;
                position=XMLElements.size()-1;
            }
        }
    }
    
    
    /**
     * go to first element
     */
    public void toFirstElement() {
        position=0;
        endReached=false;
        if (XMLElements.isEmpty()) endReached=true;
    }
    
    /**
     * go to last element
     */
    public void toLastElement() {
        position=XMLElements.size()-1;
        endReached=false;
        if (XMLElements.size()==-1) endReached=true;
    }
        
    /**
     * goes to the first element, whos key is associated with value
     * returns true, if such an element was found, false otherwise
     * (if the current element matches, this routine doesn't move
     */
    public boolean goToFirst(String key, String value) {
        if (get(key)==null) return(false);
        if (get(key).equals(value)) return(true);
        toFirstElement();
        while (!endReached && (!get(key).equals(value)))
              nextElement();
        return(get(key).equals(value));        
    }
    
    /**
     * Remove empty entries and replace them by ""
     */
    public void CompressEmpty() {
        for (ArrayList<String> elt : XMLElements) {
            for (int i=0;i<XMLTags.size();i++) {
                String s2=elt.get(i);
                if (s2.trim().length()==0) elt.set(i, "");
            }
        }
    }
    
    /**
     * Save the current XMLhandler content to file with name s
     */
    public void ALTwriteTo(String s) throws IOException {
        System.out.println("Writing file... "+s+".bin");
        FileOutputStream fos = new FileOutputStream(s+".bin");
        DataOutputStream dos = new DataOutputStream(fos);
        for (ArrayList<String> elt : XMLElements) {
            dos.writeUTF("<element>");
            for (int i=0;(i<XMLTags.size()) && (i<elt.size());i++) {
                String s2=elt.get(i);
                if (s2!=null) {
                    dos.writeUTF(XMLTags.get(i));
                    dos.writeUTF(s2);
                }
            }
        }
        dos.flush();
        dos.close();
        fos.close();
    }

    
    /**
     * Save the current XMLhandler content to file with name s
     */
    public void writeTo(String s) throws IOException {
        TextFile f1=new TextFile(s,false);
        String s1;
        String s2;
        f1.putString("<?xml version=\"1.0\"?>");
        if (Comment.length()>0) f1.putString("<!--"+Comment+"-->");
        f1.putString("<"+type+">");
        for (ArrayList<String> elt : XMLElements) {
            f1.putString("<element>");
            for (int i=0;(i<XMLTags.size()) && (i<elt.size());i++) {
                s1=XMLTags.get(i);
                s2=elt.get(i);
                if (s2!=null)
                    f1.putString("  <"+s1+">"+s2+"</"+s1+">");
            }
            f1.putString("</element>");
        }
        f1.putString("</"+type+">");
        f1.close();
    }
    
    /**
     * Save the current XMLhandler content to the file it came from
     */
    public void writeBack() throws IOException {
        writeTo(source);
    }

    public boolean isNotSet(String s) {
        return(!(XMLTags.contains(s) && (get(s)!=null)));
    }

    public boolean isKeySet(String s) {
        return(!isNotSet(s));
    }

    public boolean isEmpty(String s) {
        if (isNotSet(s)) return(true);
        return(getS(s).equals(""));
    }

    public String getS(String key) {
        String tmp=get(key);
        if (tmp==null) tmp="";
        return(tmp);
    }

    public ArrayList<String> getElementArray(int p) {
        while (XMLElements.get(p).size()<XMLTags.size())
            XMLElements.get(p).add(null);
        return(XMLElements.get(p));
    }

    public int getSize() {
        return(XMLElements.size());
    }

}
