package celsius3;

import celsius.Resources;
import atlantis.tools.FileTools;
import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import celsius.tools.ToolBox;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.zip.GZIPInputStream;

/**
 *
 * @author cnsaeman
 */
public final class Item3 {

    public final String charFilter = "[\\u0000-\\u001F\\u007F]";
    
    public final Library3 Lib;
    private final ArrayList<String> tags;
    private final ArrayList<String> data;
    boolean loadedaddinfo;
    boolean needsSaving;
    private XMLHandler CD;
    public int pos;
    public int id;

    public int error;

    public String toText;
    public String toSort;

    public Item3(Library3 lib,int p) {
        super();
        Lib=lib;
        tags=lib.IndexTags;
        pos=p;
        data=lib.Index.getElementArray(p);
        id=Integer.valueOf(this.get("id"));
        loadedaddinfo=false;
        needsSaving=false;
        error=0;
        toText=null;
        toSort=null;
    }

    public Item3(Library3 lib,String ids) {
        super();
        Lib=lib;
        tags=lib.IndexTags;
        error=1;
        id=Integer.valueOf(ids);
        pos=Lib.getPosition(id);
        data=lib.Index.getElementArray(pos);
        loadedaddinfo=false;
        needsSaving=false;
        error=0;
    }

    /**
     * returns the keys in the index
     * @return
     */
    public ArrayList<String> totalKeySet() {
        ArrayList<String> ret=new ArrayList<String>();
        if (Lib==null) {
            for (String s : tags)
                ret.add(s);
        } else {
            for (String s : Lib.IndexTags)
                ret.add(s);
            ensureAddInfo();
            for (String s : CD.XMLTags)
                if (!ret.contains(s)) ret.add(s);
        }
        return(ret);
    }

    public String get(String s) {
        String tmp=null;
        int i=tags.indexOf(s);
        if (i>-1) {
            tmp=data.get(i);
        } else {
            if (Lib!=null) {
                ensureAddInfo();
                tmp=CD.get(s);
            }
        }
        return(tmp);
    }

   /**
     * directly get the information from the index, don't try addinfo
     *
     * @param s the key
     * @return the value
     */
    public String getI(String s) {
        String tmp=null;
        int i=tags.indexOf(s);
        if (i>-1) {
            tmp=data.get(i);
        } 
        return(tmp);
    }

    public String getS(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        return(tmp);
    }
    
    public boolean isNotSet(String s) {
        return(!tags.contains(s));
    }

    public boolean isEmpty(String s) {
        if (!tags.contains(s)) return(true);
        return(getS(s).equals(""));
    }

    public String getIconField(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        if (Lib.IconDictionary.containsKey(tmp))
            tmp=Lib.IconDictionary.get(tmp);
        return(tmp);
    }
    
    public String getShortName(String field) {
        String person=getS(field);
        if (person.indexOf(",")==-1) return(person.replaceAll("\\|",", "));
       return(Parser.cutUntilLast(person.replaceAll(", .*?\\|", ", "),",").trim());
    }

    public String completeDir(String s) {
        if ((s==null) || (s.indexOf("::")==-1)) return(s);
        if (s.indexOf("::")>2) return(s);
        String sig=Parser.cutUntil(s,"::");
        s=Parser.cutFrom(s,"::");
        String s2=s;
        if (s2.startsWith(ToolBox.FILE_SEPARATOR)) s2=s2.substring(1);
        if (sig.equals("AI")) {
            if (s2.charAt(0)=='.') s2=s2.substring(1);
            return(Lib.basedir+"information"+ToolBox.FILE_SEPARATOR+get("id")+"."+s2);
        }
        if (sig.equals("LD")) return(Lib.basedir+s2);
        if (sig.equals("BD")) return(Lib.celsiusbasedir+s2);
        return(s);
    }

    public String getCompleteDirS(String k) {
        return(completeDir(getS(k)));
    }

    public void remove(String tag) {
        int i=tags.indexOf(tag);
        if (i==-1) {
            if (Lib==null) return;
        } else {
            tags.remove(i);
            data.remove(i);
        }
        ensureAddInfo();
        if (CD.get(tag)!= null) {
            CD.put(tag, null);
            needsSaving = true;
        }
    }

    public void put(String tag,String value) {
        int i=tags.indexOf(tag);
        //System.out.println("Try Writing: "+tag+"::"+value);
        if (Lib==null) {
            if (i==-1) {
                tags.add(tag);
                data.add(value);
            } else {
                data.set(i, value);
            }
        } else {
            boolean addinfo = true;
            if (i > -1) {
                if ((data.get(i) == null) || !data.get(i).equals(value)) {
                    //System.out.println("Writing to data stream");
                    data.set(i, value);
                } else {
                    addinfo = false;
                }
                if (tag.equals("addinfo") || tag.equals("autoregistered") || tag.equals("registered")) {
                    addinfo = false;
                }
            }
            if (addinfo) {
                ensureAddInfo();
                //System.out.println("T:"+CD.get(tag));
                if ((CD.get(tag) == null) || (!CD.get(tag).equals(value))) {
                    //System.out.println("Writing to CD");
                    CD.put(tag, value);
                    needsSaving = true; 
                }
            }
        }
    }

    public void putS(String key,String value) {
        if (value.trim().length()!=0) put(key,value);
    }

    // Force writing to AddInfo, even if tag, value matches in the index.
    public void putF(String tag,String value) {
        int i=tags.indexOf(tag);
        //System.out.println("Try Writing: "+tag+"::"+value);
        if (Lib==null) {
            if (i==-1) {
                tags.add(tag);
                data.add(value);
            } else {
                data.set(i, value);
            }
        } else {
            boolean addinfo=true;
            if (i>-1) {
                if ((data.get(i)==null) || !data.get(i).equals(value)) {
                    //System.out.println("Writing to data stream");
                    data.set(i, value);
                }
                Lib.setChanged(true);
                if (tag.equals("addinfo") || tag.equals("autoregistered") || tag.equals("registered")) addinfo=false;
            }
            if (addinfo) {
                ensureAddInfo();
                if ((CD.get(tag)==null) || (!CD.get(tag).equals(value))) {
                    //System.out.println("Writing to CD");
                    CD.put(tag, value);
                    needsSaving=true;
                }
            }
        }
    }

    public void save() {
        if (Lib==null) return;
        toText=null;
        toSort=null;
        //System.out.println("Try saving...");
        if (needsSaving) try {
            CD.writeBack();
            //System.out.println("Saving addinfo");
            //System.out.println(CD.XMLTags);
            //System.out.println(CD.XMLElements);
        } catch (IOException ex) {
            Lib.RSC.outEx(ex);
            error=6;
        }
    }

    public void ensureAddInfo() {
        if (Lib==null) return;
        try {
            if (!loadedaddinfo) {
                if ((tags.indexOf("addinfo")==-1) || (get("addinfo")==null)) {
                    put("addinfo","AI::xml");
                    CD=newAddInfo();
                    CD.addEmptyElement();                    
                } else {
                    CD = new XMLHandler(getCompleteDirS("addinfo"));
                }
                loadedaddinfo=true;
            }
        } catch (Exception ex) {
            CD=new XMLHandler();
            Lib.RSC.outEx(ex);
            error=5;
        }
    }

    public ArrayList<String> getAITags() {
        ensureAddInfo();
        return(CD.XMLTags);
    }

    public int getPages() {
        int i;
        if ((Lib!=null) && (Lib.IndexTags.indexOf("pages")==-1)) return(0);
        try {
            i=Integer.valueOf(get("pages"));
        } catch (Exception e) { i=0; }
        return(i);
    }

    public double getDuration() {
        double d;
        if ((Lib!=null) && (Lib.IndexTags.indexOf("duration")==-1)) return(0);
        try {
            d=Integer.valueOf(get("duration"));
        } catch (Exception e) { d=0; }
        return(d);
    }

    public MProperties getMProperties(boolean loadAddData) {
        MProperties m=new MProperties();
        for (String tag : tags) {
            m.put(tag, get(tag));
        }
        if (loadAddData) {
            if (Lib != null) {
                ensureAddInfo();
                for (String tag : CD.XMLTags) {
                    m.put(tag, CD.get(tag));
                }
            }
        }
        if (get("location")!=null) {
            if (Lib!=null)
                m.put("fullpath", getCompleteDirS("location"));
            else
                m.put("fullpath", get("location"));
        }
        return(m);
    }

    public String getRawData(int i) {
        String tmp="";
        String s2;
        if (i==1) {
            for(String tag : tags) {
                s2 = get(tag);
                if ((s2!=null) && !(s2.indexOf("\n")>-1))
                    tmp += tag.replaceAll("\\p{C}", "?") + ": " + s2.replaceAll("\\p{C}", "?") + ToolBox.LINE_SEPERATOR;
            }
        }
        if (i==2) {
            ensureAddInfo();
            for (String tag : CD.XMLTags) {
                s2 = CD.get(tag);
                if ((s2!=null) && !(s2.indexOf("\n")>-1))
                    tmp += tag.replaceAll(charFilter, "?") + ": " + s2.replaceAll(charFilter, "?") + ToolBox.LINE_SEPERATOR;
            }
        }
        return(tmp);
    }

    private XMLHandler newAddInfo() throws IOException {
        XMLHandler.Create("celsiusv2.1.addinfofile",completeDir("AI::xml"));
        return(new XMLHandler(completeDir("AI::xml")));
    }

    public boolean isSynchronous(String t) {
        ensureAddInfo();
        int p=tags.indexOf(t);
        if (p>-1) {
            if ((data.get(p)==null) && (CD.get(t)==null)) return(true);
            if ((data.get(p)==null) ^ (CD.get(t)==null)) return(false);
            return(data.get(p).equals(CD.get(t)));
        }
        return(true);
    }

    /**
     * Checks the integrity of the current Item
     * @return 0: everything ok
     *         1: addinfo missing
     */
    public int checkIntegrity() {
        ensureAddInfo();
        if (error>0) return(1);
        
        return(0);
    }

    public String getFromCD(String t) {
        ensureAddInfo();
        return(CD.get(t));
    }

    public String getFreeAltVerNo() {
        int top=-1;
        int k;
        String isn,nmb;
        for (String key : this.CD.XMLTags) {
            if ((key!=null) && (key.startsWith("altversion-location-"))) {
                isn=Parser.cutFromLast(key,"-");
                k=ToolBox.intvalue(isn);
                if (k>top) top=k;
            }
        }
        nmb="00"+String.valueOf(top+1);
        nmb=nmb.substring(nmb.length()-3);
        return(nmb);
    }

    public boolean guaranteeStandardFolder() {
        return(guaranteeStandardFolder(Lib));
    }

    public boolean guaranteeStandardFolder(Library3 lib) {
        String folder=lib.completeDir(getStandardFolder(lib),"");
        if (folder==null) return(true);
        if (!(new File(folder)).exists())
            return((new File(folder)).mkdir());
        return(true);
    }

    public String getStandardFolder() {
        return(getStandardFolder(Lib));
    }

    public String getStandardFolder(Library3 lib) {
        String ifolder=lib.MainFile.get("item-folder");
        if ((ifolder==null) || (ifolder.equals(""))) ifolder="LD::items";
        String folder=fillOut(true,ifolder);
        folder=Parser.replace(folder, "\\\\", "\\");
        return(folder);
    }

    public String getAIFile(String end) {
        String tmp="AI::";
        while ((new File(completeDir(tmp+end))).exists()) tmp+="x";
        return(tmp+end);
    }

    
    public void reloadAI() {
        loadedaddinfo=false;
        ensureAddInfo();
    }

    public void deleteFiles() {
        if (Lib!=null) {
            FileTools.deleteIfExists(getCompleteDirS("location"));
            FileTools.deleteIfExists(getCompleteDirS("plaintxt"));
        } else {
            FileTools.deleteIfExists(get("location"));
            FileTools.deleteIfExists(get("plaintxt"));
        }
    }

    public void reduceToDocRef() {
        deleteFiles();
        for (String tag : CD.XMLTags) {
            if (tag.startsWith("altversion-location"))
                FileTools.deleteIfExists(getCompleteDirS("location"));
            if (tag.startsWith("altversion-plaintxt"))
                FileTools.deleteIfExists(getCompleteDirS("plaintxt"));
            if (tag.startsWith("altversion")) CD.put(tag, null);
        }
        put("location",null);
        put("filetype",null);
        put("plaintxt",null);
        put("pages","0");
        save();
        Lib.setChanged(true);
    }

    public void removeFromLib(boolean files) {
        if (files)
            deleteFiles();
        else {
            FileTools.deleteIfExists(getCompleteDirS("plaintxt"));
            put("location",null);
            Lib.setChanged(true);
            for (String tag : getAITags()) {
                if (tag.startsWith("altversion-location-"))
                    put (tag,null);
            }
        }
        String gtag;
        for (String tag : getAITags()) {
            gtag = get(tag);
            if ((gtag != null) && (gtag.startsWith("LD::"))) {
                FileTools.deleteIfExists(completeDir(gtag));
            }
        }
        FileTools.deleteIfExists(completeDir(get("addinfo")));
        int p=Lib.getPosition(id);
        if (p>-1) {
            if (Integer.valueOf(Lib.Index.get(p,"id"))!=id) {
                //Display Error
            } else {
                Lib.Index.deleteElement(p);
                Lib.setChanged(true);
            }
        }        
    }

    public void removeAltVersion(String nmb) {
        FileTools.deleteIfExists(completeDir(get("altversion-location-"+nmb)));
        FileTools.deleteIfExists(completeDir(get("altversion-plaintxt-"+nmb)));
        put("altversion-location-"+nmb,null);
        put("altversion-filetype-"+nmb,null);
        put("altversion-plaintxt-"+nmb,null);
        put("altversion-pages-"+nmb,null);
        put("altversion-label-"+nmb,null);
        save();
    }

    public void swapWithMain(String nmb) {
        String tmp=get("pages");
        put("pages",get("altversion-pages-"+nmb));
        put("altversion-pages-"+nmb,tmp);
        tmp=get("filetype");
        put("filetype",get("altversion-filetype-"+nmb));
        put("altversion-filetype-"+nmb,tmp);
        put("altversion-label-"+nmb,"former main version");
        try {
            FileTools.swapFile(getCompleteDirS("location"), getCompleteDirS("altversion-location-"+nmb));
            String ft1=get("filetype");
            String ft2=get("altversion-filetype-"+nmb);
            if (!ft1.equals(ft2)) {
                String n=Parser.cutUntilLast(getCompleteDirS("location"),ft2)+ft1;
                (new File(getCompleteDirS("location"))).renameTo(new File(n));
                put("location",Lib.compressDir(n));
                n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb),ft1)+ft2;
                (new File(getCompleteDirS("altversion-location-"+nmb))).renameTo(new File(n));
                put("altversion-location-"+nmb,Lib.compressDir(n));
            }
            if ((get("plaintxt")!=null) && ((new File(getCompleteDirS("plaintxt"))).exists())) FileTools.swapFile(getCompleteDirS("plaintxt"), getCompleteDirS("altversion-plaintxt-"+nmb));
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public void swapAltVersions(String nmb1, String nmb2) {
        String tmp=get("altversion-pages-"+nmb1);
        put("altversion-pages-"+nmb1,get("altversion-pages-"+nmb2));
        put("altversion-pages-"+nmb2,tmp);
        tmp=get("altversion-filetype-"+nmb1);
        put("altversion-filetype-"+nmb1,get("altversion-filetype-"+nmb2));
        put("altversion-filetype-"+nmb2,tmp);
        tmp=get("altversion-label-"+nmb1);
        put("altversion-label-"+nmb1,get("altversion-label-"+nmb2));
        put("altversion-label-"+nmb2,tmp);
        try {
            FileTools.swapFile(getCompleteDirS("altversion-location-"+nmb1), getCompleteDirS("altversion-location-"+nmb2));
            String ft1=get("altversion-filetype-"+nmb1);
            String ft2=get("altversion-filetype-"+nmb2);
            if (!ft1.equals(ft2)) {
                String n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb1),ft2)+ft1;
                (new File(getCompleteDirS("altversion-location-"+nmb1))).renameTo(new File(n));
                put("altversion-location-"+nmb1,Lib.compressDir(n));
                n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb2),ft1)+ft2;
                (new File(getCompleteDirS("altversion-location-"+nmb2))).renameTo(new File(n));
                put("altversion-location-"+nmb2,Lib.compressDir(n));
            }
            FileTools.swapFile(getCompleteDirS("altversion-plaintxt-"+nmb1), getCompleteDirS("altversion-plaintxt-"+nmb2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public void restoreAI() throws IOException {
        CD=newAddInfo();
        put("addinfo", "AI::xml");
        for (String k : tags) 
            if (!k.equals("addinfo")) CD.put(k, get(k));
        CD.writeBack();
    }

    public boolean hasAttribute(String attribute) {
        if (isEmpty("attributes")) return(false);
        return (Parser.listContains(get("attributes"), attribute));
    }

    public void setAttribute(String attribute) {
        if (!hasAttribute(attribute)) {
            String attributes=getS("attributes");
            attributes+="|"+attribute;
            if (attributes.startsWith("|")) attributes=attributes.substring(1);
            put("attributes",attributes);
        }
    }

    public void addLink(String tag, String value) {
        String s=get("links");
        if (s==null) s="";
        if (!s.equals("")) s+="|";
        s+=tag+":"+value;
        put("links",s);
    }

    public String fillOut(boolean addInfo, String s) {
        MProperties mProp=this.getMProperties(true);
        for (String field : mProp.keySet()) {
            s=Parser.replace(s, "#"+field+"#", getS(field));
            int i=s.indexOf("#"+field+"&");
            if (i>-1) {
                /*s=Parser.Substitute(s, "#"+field+"&1#", getShortNames(field));
                s=Parser.Substitute(s, "#"+field+"&2#", toolbox.ToBibTeXAuthors(get(field)));
                s=Parser.Substitute(s, "#"+field+"&3#", toolbox.Authors3FromCelAuthors(get(field)));
                s=Parser.Substitute(s, "#"+field+"&4#", toolbox.Authors4FromCelAuthors(get(field)));*/
            }
        }
        String res;
        int i,j;
        i=0;
        while (s.indexOf('#',i)>-1) {
            i=s.indexOf('#',i);
            j=s.indexOf('#',i+1);
            if ((j>-1) && (j-i<20)) {
                res="";
                if (i>0) res=s.substring(0,i-1);
                if (j+1<s.length()) res+=s.substring(j+1);
                s=res;
            } else i++;
        }
        return(s);
    }

}
