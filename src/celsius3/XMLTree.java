//
// Celsius Library System
// (w) by C. Saemann
//
// XMLTree.java
//
// This class contains a second XML engine used by Celsius
//
// not entirely typesafe
//
// checked 16.09.2007, optimization?
//

package celsius3;

import celsius.tools.Parser;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import javax.swing.tree.TreeNode;


public class XMLTree {
    
    public RulesNode Root;
    public RulesNode Node;
    public String representation;
    public String name;
    public String source;
    public HashMap<String,String> XMLattribs;
    private boolean foundnode;
    private TextFile f1;
    private String tmp; // required for reading in
    public int regid;
    
    public void DeleteNode() {
        RulesNode TN=Node;
        goTo(Node.getParent());
        Node.remove(TN);
    }
    
    public void write(String tag,String value) {
        Node.writeData(tag, value);
    }
    
    public RulesNode CreateNewNode(String l,HashMap<String,String> data) {
        RulesNode TN=new RulesNode(l,data);
        regNode(TN);
        TN.representation=representation;
        Node.add(TN);
        return(TN);
    }
    
    public void goTo(RulesNode D) {
        Node=D;
        name=D.getLabel();
        XMLattribs=Node.getData();
    }

    public boolean goTo(int id) {
        if (Node.regid==id) return(true);
        boolean found=false;
        for (Enumeration e=Node.children();e.hasMoreElements() && (!found);) {
            goTo((RulesNode)e.nextElement());
            found=goTo(id);
        }
        return(found);
    }
    
    private RulesNode LookThrough(String src,RulesNode DMTN, RulesNode DMTNaf) {
        if (DMTN.toString().toLowerCase().indexOf(src)>-1) {
            if (foundnode) return(DMTN);
            if (DMTN==DMTNaf) foundnode=true;
        }
        RulesNode dmtn;
        for (Enumeration e=DMTN.children();e.hasMoreElements();) {
            dmtn=LookThrough(src,(RulesNode)e.nextElement(),DMTNaf);
            if (!(dmtn==null)) return(dmtn);
        }
        return(null);
    }

    public RulesNode nextOccurence(String src,RulesNode dmtnstart) {
        if (src.length()!=0) {
            foundnode=false;
            if (dmtnstart==null) { dmtnstart=this.Root; foundnode=true; }
            RulesNode dmtn=LookThrough(src.toLowerCase(),this.Root,dmtnstart);
            if ((dmtn==null) && (foundnode))
                dmtn=LookThrough(src.toLowerCase(),this.Root,null);
            return(dmtn);
        }
        return(null);
    }

    public boolean isRoot() {
        return(Node==Root);
    }
    
    public void goTo(TreeNode D) {
        goTo((RulesNode)D);
    }
    
    
    public String get(String s) {
        if (XMLattribs.containsKey(s))
            return(XMLattribs.get(s));
        else
            return("");
    }
    
    private String getTag() throws IOException {
        String tmp="";
        while (!tmp.trim().startsWith("<"))
            tmp=f1.getString();
        return(tmp);
    }

    private void regNode(RulesNode SN) {
        SN.regid=regid;
        regid++;
    }
    
    private void ReadElement() throws IOException {
        String t1,t2,t3;
        HashMap<String,String> data=new HashMap<String,String>();
        boolean isshort=false;
        t1=Parser.cutFrom(tmp,"<");
        int l=0;
        int k=0;
        for (int i=0;i<t1.length();i++) {
            if (t1.charAt(i)=='\"') l=1-l;
            if (t1.charAt(i)=='\'') k=1-l;
            if ((l==0) && (k==0) && t1.charAt(i)=='>') {
                t1=t1.substring(0,i);
                break;
            }
        }
        if (t1.endsWith("/")) {
            isshort=true; t1=t1.substring(0,t1.length()-1);
        }
        t2=Parser.cutUntil(t1," ");
        t1=Parser.cutFrom(t1," ");
        Node.setLabel(t2);
        t1=t1.trim();
        while(t1.length()>0) {
            t2=Parser.cutUntil(t1,"=");
            t3=Parser.cutFrom(t1,"\"");
            t3=Parser.cutUntil(t3,"\"");
            t1=Parser.cutFrom(t1,"\"");
            t1=Parser.cutFrom(t1,"\"");
            data.put(t2,t3);
            t1=t1.trim();
        }
        if (!isshort) {
            tmp=getTag();
            while (!tmp.trim().startsWith("</")) {
                RulesNode Node2=new RulesNode();
                regNode(Node2);
                Node2.representation=representation;
                Node.add(Node2);
                Node=Node2;
                ReadElement();
                Node=Node.getParent();
                tmp=getTag();
            }
        }
        Node.setData(data);
    }
    
    public XMLTree(String s) throws IOException {
        source=s;
        regid=0;
        f1=new TextFile(s);
        tmp=f1.getString();
        if (!tmp.equals("<?xml version='1.0'?>")) throw(new IOException("XML-Header Error"));
        Node=new RulesNode();
        regNode(Node);
        tmp=getTag();
        representation="";
        Node.representation="";
        ReadElement();
        f1.close();
        Root=Node;
    }
    
    public XMLTree(String s,String rep) throws IOException {
        source=s;
        regid=0;
        f1=new TextFile(s);
        tmp=f1.getString();
        if (!tmp.equals("<?xml version='1.0'?>")) throw(new IOException("XML-Header Error"));
        Node=new RulesNode();
        regNode(Node);
        tmp=getTag();
        representation=rep;
        Node.representation=rep;
        ReadElement();
        f1.close();
        Root=Node;
    }

    private void ref(String key, String Told, String Tnew, RulesNode SN) {
        goTo(SN);
        if (SN.getDataKeys().contains("Told")) {
            write(key, get(key).replace(Told, Tnew));
        }
        for (RulesNode subnode : SN.childNodes) {
            ref(key,Told,Tnew,subnode);
        }
    }

    public void refactor(String key, String Told, String Tnew) {
        RulesNode SN=Node;
        ref(key,Told,Tnew,Root);
        goTo(Node);
    }
    
    public XMLTree(XMLTree tree) {
        representation=tree.representation;
        name=tree.name;
        Root=tree.Root.cloneCompletely();
        Node=Root;
    }
    
    private void WriteElement(String indent) throws IOException {
        f1.putStringO(indent+"<"+Node.getLabel());
        for (String key : Node.getDataKeys())
            f1.putStringO(" "+key+"=\""+Node.get(key)+"\"");
        if (!Node.isLeaf()) {
            f1.putString(">");
            for (int i=0;i<Node.getChildCount();i++) {
                Node=Node.getChildAt(i);
                WriteElement(indent+"    ");
                Node=Node.getParent();
            }
            f1.putString(indent+"</"+Node.getLabel()+">");
        } else {
            f1.putString("/>");
        }
    }
    
    public void writeTo(String s) throws IOException {
        f1=new TextFile(s,false);
        f1.putString("<?xml version='1.0'?>");
        Node=Root;
        WriteElement("");
        f1.close();
    }
    
    public void writeBack() throws IOException {
        writeTo(source);
    }    
    
}