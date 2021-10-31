package celsius.data;

import celsius.Resources;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import celsius.tools.ToolBox;
import java.io.File;
import java.io.IOException;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author cnsaeman
 */
public final class Item extends TableRow {

    public final String charFilter = "[\\u0000-\\u001F\\u007F]";
    
    public final Library library;

    public int error;

    public String toText;
    public String toSort;

    public Item(Library lib,int i) {
        super(lib,"items",String.valueOf(i),lib.itemPropertyKeys);
        library=lib;
        tableHeaders=library.itemPropertyKeys;
        error=0;
        toText=null;
        toSort=null;
    }

    public Item(Library lib,String i) {
        super(lib,"items",i,lib.itemPropertyKeys);
        library=lib;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }

    public Item(Library lib) {
        super(lib,"items",lib.itemPropertyKeys);
        library=lib;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }


    public Item() {
        super("items",null);
        library=null;
        error=0;
    }

    public Item(Library lib, ResultSet rs) {
        super(lib,"items",rs,lib.itemPropertyKeys);
        library=lib;
        tableHeaders=library.itemPropertyKeys;
        error=0;
    }

    public String getIconField(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        if (library.IconDictionary.containsKey(tmp))
            tmp=library.IconDictionary.get(tmp);
        return(tmp);
    }

    public String getExtended(String tag) {
        int i = tag.indexOf("&");
        if (i > -1) {
            if (tag.equals("distance&")) {
                String lat=getS("lat");
                String lon=getS("lon");
                if ((lat.length()<2) || (lon.length()<2)) return("NA");
                return(ToolBox.getDistance(library.RSC.mylat, library.RSC.mylon, Double.valueOf(lat), Double.valueOf(lon)));
            }
            if (tag.equals("heading&")) {
                String lat=getS("lat");
                String lon=getS("lon");
                if ((lat.length()<2) || (lon.length()<2)) return("NA");
                return(ToolBox.getBearing(library.RSC.mylat, library.RSC.mylon, Double.valueOf(lat), Double.valueOf(lon)));
            }
            char tp = tag.charAt(i + 1);
            tag = tag.substring(0, i);
            switch (tp) {
                case '1':
                    return (getShortNames(tag));
                case '2':
                    return (getBibTeXNames(tag));
                case '3':
                    return (getNames3(tag));
                case '4':
                    return (getNames4(tag));
                case '5': {
                    String out="<html><b><tt>";
                    String lf=getS(tag);
                    for (int j=0;j<lf.length();j++) {
                        switch (lf.charAt(j)) {
                            case 'F': {
                                out += "<FONT COLOR=\"#83cd53\">F</FONT>";
                                break;
                                }
                            case 'X': {
                                out += "<FONT COLOR=\"#fe5919\">X</FONT>";
                                break;
                                }
                            default : out+="<FONT COLOR=GRAY>.</FONT>";
                        }
                    }
                    return(out+"</tt></b></html>");
                }
                default:
                    return (getS(tag));
            }
        }
        return (getS(tag));
    }

    public String getCompleteDirS(String k) {
        return(library.completeDir(getS(k)));
    }

    public void putS(String key,String value) {
        if ((value!=null) && (value.trim().length()!=0)) put(key,value);
    }

    public void save() {
        try {
            library.RSC.out("Saving Item");
            super.save();
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }

    public int getPages() {
        int i;
        if ((library!=null) && (library.indexFields.indexOf("pages")==-1)) return(0);
        try {
            i=Integer.valueOf(get("pages"));
        } catch (Exception e) { i=0; }
        return(i);
    }
    
    public double getDuration() {
        double d;
        if ((library!=null) && (library.indexFields.indexOf("duration")==-1)) return(0);
        try {
            d=Integer.valueOf(get("duration"));
        } catch (Exception e) { d=0; }
        return(d);
    }

    public MProperties getMProperties(boolean loadAddData) {
        MProperties m=new MProperties();
        m.put("id",String.valueOf(id));
        for (String field : getFields()) {
            m.put(field, get(field));
        }
        if (loadAddData) {
            if (library != null) {
                ensureFullData();
            }
        }
        if (get("location")!=null) {
            if (library!=null)
                m.put("fullpath", getCompleteDirS("location"));
            else
                m.put("fullpath", get("location"));
        }
        return(m);
    }

    public String getRawData() {
        String tmp="";
        String s2;
        for (String field : this.getEditableFields()) {
            s2 = get(field);
            if ((s2!=null) && !(s2.indexOf("\n")>-1))
                tmp += field.replaceAll("\\p{C}", "?") + ": " + s2.replaceAll("\\p{C}", "?") + ToolBox.linesep;
        }
        return(tmp);
    }
    
    public ArrayList<String> getEditableFields() {
        ArrayList<String> fields=new ArrayList<>();
        for (String field : getFields()) {
            if (!field.startsWith("attachment-")) fields.add(field);
        }
        for (String field : library.listOf("standardfields"))
            if (!fields.contains(field)) fields.add(field);
        for (String person : library.peopleFields) {
            fields.remove("short_"+person);
            fields.remove(person+"_ids");
        }
        fields.remove("short_search");
        fields.remove("last_modified");
        fields.remove("id");
        return(fields);
    }

    // TODO
    public void associateWithFile(Resources RSC, String path, String name) throws IOException {
        
        /* String loc,ft,pt,pg,rem,nmb;
        String filetype=RSC.Configuration.getFileType(path);
        String filename=standardFileName(null);
        if (get("location")==null) {
            loc="location"; ft="filetype";pt="plaintxt";pg="pages";rem=null;nmb=null;
        } else {
            ensureFullData();
            /*nmb=getFreeAltVerNo();//TODO correct
            loc="altversion-location-"+nmb;
            pt="altversion-plaintxt-"+nmb;
            rem="altversion-label-"+nmb;
            ft="altversion-filetype-"+nmb;
            pg="altversion-pages-"+nmb;
            filename+="."+nmb;
            if (name==null) name="Alt version "+nmb;
        }
        guaranteeStandardFolder();
        filename=getStandardFolder()+toolbox.filesep+filename+"."+filetype;
        if (!TextFile.moveFile(path, completeDir(filename))) throw new IOException("Couldn't move file!");
        String txttarget="AI::";
        if (nmb!=null) txttarget+=nmb+".";
        txttarget+="txt";
        RSC.Configuration.ExtractText("LIBAF>",completeDir(filename),completeDir(txttarget));
        txttarget+=".gz";
        if ((new File(completeDir(txttarget))).exists()) {
            put(pt,txttarget);
            String pages=Integer.toString(toolbox.ReadNumberOfPagesOf(RSC.Msg1,"LIBAF>",completeDir(filename),completeDir(txttarget)));
            put(pg,pages);
        }
        put(ft,filetype);
        put(loc,filename);
        if (rem!=null) put (rem,name);
        save();
        library.setChanged(true);*/
    }

    public void shiftReplaceWithFile(Resources RSC, String path) throws IOException {
        /* String loc,ft,pt,pg,rem,nmb;
        String filetype=RSC.Configuration.getFileType(path);
        String filename=Parser.CutTillLast(get("location"),get("filetype"));
        String target=filename;
        int i=0;
        ensureFullData();
        nmb=getFreeAltVerNo();
        target+=nmb+"."+get("filetype");
        TextFile.moveFile(getCompleteDirS("location"), completeDir(target));
        if (get("plaintxt")!=null) {
            TextFile.moveFile(completeDir(get("plaintxt")), completeDir("AI::"+nmb+".txt.gz"));
            put("altversion-plaintxt-"+nmb,"AI::"+nmb+".txt.gz");
            put("altversion-pages-"+nmb,get("pages"));
        }
        put("altversion-location-"+nmb,target);
        put("altversion-filetype-"+nmb,get("filetype"));
        put("altversion-label-"+nmb,"Alt version "+nmb);
        filename=filename+"."+filetype;
        if (!TextFile.moveFile(path, completeDir(filename))) throw new IOException("Couldn't move file!");
        String txttarget="AI::";
        txttarget+=".txt";
        RSC.Configuration.ExtractText("LIBAF>",completeDir(filename),completeDir(txttarget));
        txttarget+=".gz";
        if ((new File(completeDir(txttarget))).exists()) {
            String pages=Integer.toString(toolbox.ReadNumberOfPagesOf(RSC.Msg1,"LIBAF>",completeDir(filename),completeDir(txttarget)));
            put("plaintxt",txttarget);
            put("pages",pages);
        }
        put("filetype",filetype);
        put("location",filename);
        save();
        library.setChanged(true);*/
    }

    public void reloadAI() {
        loadedFullData=false;
        ensureFullData();
    }

    public void deleteFiles() {
        if (library!=null) {
            TextFile.Delete(getCompleteDirS("location"));
            TextFile.Delete(getCompleteDirS("plaintxt"));
        } else {
            TextFile.Delete(get("location"));
            TextFile.Delete(get("plaintxt"));
        }
    }

    // TODO remove from attributes
    public void reduceToDocRef() {
        deleteFiles();
        /*for (String tag : CD.XMLTags) {
            if (tag.startsWith("altversion-location"))
                TextFile.Delete(getCompleteDirS("location"));
            if (tag.startsWith("altversion-plaintxt"))
                TextFile.Delete(getCompleteDirS("plaintxt"));
            if (tag.startsWith("altversion")) CD.put(tag, null);
        }*/
        put("location",null);
        put("filetype",null);
        put("plaintxt",null);
        put("pages","0");
        save();
    }

    public void removeAltVersion(String nmb) {
        /*TextFile.Delete(completeDir(get("altversion-location-"+nmb)));
        TextFile.Delete(completeDir(get("altversion-plaintxt-"+nmb)));
        put("altversion-location-"+nmb,null);
        put("altversion-filetype-"+nmb,null);
        put("altversion-plaintxt-"+nmb,null);
        put("altversion-pages-"+nmb,null);
        put("altversion-label-"+nmb,null);
        save();*/
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
            TextFile.swapFile(getCompleteDirS("location"), getCompleteDirS("altversion-location-"+nmb));
            String ft1=get("filetype");
            String ft2=get("altversion-filetype-"+nmb);
            if (!ft1.equals(ft2)) {
                String n=Parser.cutUntilLast(getCompleteDirS("location"),ft2)+ft1;
                (new File(getCompleteDirS("location"))).renameTo(new File(n));
                put("location",library.compressDir(n));
                n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb),ft1)+ft2;
                (new File(getCompleteDirS("altversion-location-"+nmb))).renameTo(new File(n));
                put("altversion-location-"+nmb,library.compressDir(n));
            }
            if ((get("plaintxt")!=null) && ((new File(getCompleteDirS("plaintxt"))).exists())) TextFile.swapFile(getCompleteDirS("plaintxt"), getCompleteDirS("altversion-plaintxt-"+nmb));
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
            TextFile.swapFile(getCompleteDirS("altversion-location-"+nmb1), getCompleteDirS("altversion-location-"+nmb2));
            String ft1=get("altversion-filetype-"+nmb1);
            String ft2=get("altversion-filetype-"+nmb2);
            if (!ft1.equals(ft2)) {
                String n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb1),ft2)+ft1;
                (new File(getCompleteDirS("altversion-location-"+nmb1))).renameTo(new File(n));
                put("altversion-location-"+nmb1,library.compressDir(n));
                n=Parser.cutUntilLast(getCompleteDirS("altversion-location-"+nmb2),ft1)+ft2;
                (new File(getCompleteDirS("altversion-location-"+nmb2))).renameTo(new File(n));
                put("altversion-location-"+nmb2,library.compressDir(n));
            }
            TextFile.swapFile(getCompleteDirS("altversion-plaintxt-"+nmb1), getCompleteDirS("altversion-plaintxt-"+nmb2));
        } catch (Exception e) {
            e.printStackTrace();
        }
        save();
    }

    public ArrayList<Item> getLinksOfType(String type) {
        if ((type!=null) && (type.trim().equals(""))) type=null;
        ArrayList<Item> items = new ArrayList<Item>();
        for (String key : library.Links.keySet()) {
            if ((type.equals("Available Links")) || (type.equals(key))) {
                //System.out.println("indeed!");
                ArrayList<String> ids=library.Links.get(key);
                for (String id : ids) {
                    if (!id.equals("?")) items.add(new Item(library,id));
                }
            }
        }
        Collections.sort(items, new CompareItems());
        return(items);
    }

    public ArrayList<Item> getCombined() {
        ArrayList<Item> items = new ArrayList<Item>();
        String combine=get("combine");
        if (combine==null) return(items);
        String[] test=combine.split("\\|");
        String[] tag=new String[test.length];
        String[] cond=new String[test.length];
        for (int i=0;i<test.length;i++) {
            tag[i]=Parser.cutUntil(test[i], ":");
            cond[i]=Parser.cutFrom(test[i], ":");
        }
        for (Item item : library) {
            boolean ok=true;
            if (item.id==id) ok=false;
            for (int i=0;(i<test.length) && ok;i++) {
                String a=item.get(tag[i]);
                if (a==null) ok=false;
                else {
                    if (cond[i].charAt(cond[i].length()-1)=='*') {
                        ok=item.get(tag[i]).startsWith(cond[i].substring(0,cond[i].length()-1));
                    } else {
                        ok=item.get(tag[i]).equals(cond[i]);
                    }
                }
            }
            if (ok) items.add(item);
        }
        Collections.sort(items, new CompareItems());
        return(items);
    }

    class CompareItems implements Comparator<Item> {

        public CompareItems() {
        }

        @Override
        public int compare(final Item A, final Item B) {
            return (A.toSort().compareTo(B.toSort()));
        }

        public boolean equals() {
            return (false);
        }
    }

    public boolean hasAttribute(String attribute) {
        if (isEmpty("attributes")) return(false);
        return (Parser.EnumContains(get("attributes"), attribute));
    }

    public void setAttribute(String attribute) {
        if (!hasAttribute(attribute)) {
            String attributes=getS("attributes");
            attributes+="|"+attribute;
            if (attributes.startsWith("|")) attributes=attributes.substring(1);
            put("attributes",attributes);
        }
    }

    public String toText() {
        if (toText!=null) return(toText);
        return(toText(library));
    }

    public String toSort() {
        if (toSort!=null) return(toSort);
        return(toSort(library));
    }

    public String toText(Library Library) {
        if (toText!=null) return(toText);
        if (Library==null) return("ID:"+get("id"));
        toText=Library.itemRepresentation.fillIn(this);
        return(toText);
    }

    public String toSort(Library Library) {
        if (toSort!=null) return(toSort);
        if (Library==null)
            return(get("location"));
        toSort=Library.itemSortRepresentation.fillIn(this);
        return(toSort);
    }

    public void addLink(String tag, String value) {
        String s=get("links");
        if (s==null) s="";
        if (!s.equals("")) s+="|";
        s+=tag+":"+value;
        put("links",s);
    }
        
}
