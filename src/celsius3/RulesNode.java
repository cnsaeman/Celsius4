/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius3;

import java.util.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author cnsaeman
 */
public class RulesNode implements MutableTreeNode {

    public ArrayList<RulesNode> childNodes;
    private RulesNode parent;

    private String label;
    private HashMap<String,String> data;

    public String representation;
    public int regid;

    public RulesNode() {
        childNodes=new ArrayList<RulesNode>();
        parent=null;

        label="";
        data=new HashMap<String,String>();

        representation="$full";
    }

    public RulesNode(String l) {
        childNodes=new ArrayList<RulesNode>();
        parent=null;

        label=l;
        data=new HashMap<String,String>();
        representation="$full";
    }

    public RulesNode(HashMap<String,String> d) {
        childNodes = new ArrayList<RulesNode>();
        parent = null;

        label = new String("");
        data = d;
        representation="$full";
    }

    public RulesNode(String l,HashMap<String,String> d) {
        childNodes = new ArrayList<RulesNode>();
        parent = null;

        label = l;
        data = d;
        representation="$full";
    }

    @Override
    public RulesNode getChildAt(int childIndex) {
        return(childNodes.get(childIndex));
    }

    @Override
    public int getChildCount() {
        return(childNodes.size());
    }

    @Override
    public RulesNode getParent() {
        return(parent);
    }

    @Override
    public int getIndex(TreeNode node) {
        return(childNodes.indexOf(node));
    }

    @Override
    public boolean getAllowsChildren() {
        return(true);
    }

    @Override
    public boolean isLeaf() {
        return(childNodes.isEmpty());
    }

    public boolean isRoot() {
        return(parent==null);
    }

    @Override
    public Enumeration children() {
        return(Collections.enumeration(childNodes));
    }

    public void add(RulesNode node) {
        if (node!=null) {
            node.parent=this;
            childNodes.add(node);
        }
    }

    public void insert(RulesNode node, int i) {
        if (node!=null) {
            node.parent=this;
            childNodes.add(i,node);
        }
    }

    public void remove(RulesNode node) {
        if (node!=null) {
            node.parent=null;
            childNodes.remove(node);
        }
    }

    @Override
    public String toString() {
        String tmp="";
        if (representation.equals("$full")) {
            tmp+=getLabel()+" ";
            for (String key : data.keySet()) {
                tmp+=key+"=\""+data.get(key)+"\" ";
            }
        } else {
            if (representation.indexOf("/name/")>-1)
                tmp+=getLabel()+" ";
            else
            for (String key : data.keySet()) {
                if (representation.indexOf("/"+key+"/")>-1) tmp+=data.get(key)+" ";
            }
        }
        return(tmp.trim());
    }

    public String getLabel() {
        return(label);
    }

    public void setLabel(String s) {
        label=s;
    }

    public void writeData(String key,String value) {
        data.put(key, value);
    }


    public String get(String key) {
        return(data.get(key));
    }

    public Set<String> getDataKeys() {
        return(data.keySet());
    }

    public HashMap<String,String> getData() {
        return(data);
    }

    public void setData(HashMap<String,String> d) {
        data=d;
    }

    public RulesNode cloneCompletely() {
        RulesNode clone=new RulesNode();
        clone.representation=representation;
        clone.regid=regid;
        clone.label=label;
        for (String key : data.keySet()) {
            clone.writeData(key, data.get(key));
        }
        for (RulesNode subnode : childNodes) {
            clone.add(subnode.cloneCompletely());
        }
        return(clone);
    }

    public RulesNode getRoot() {
        if (parent==null) return(this);
        else return(parent.getRoot());
    }

    public ArrayList<RulesNode> getPath() {
        if (parent==null) {
            ArrayList<RulesNode> path=new ArrayList<RulesNode>();
            path.add(this);
            return(path);
        }
        ArrayList<RulesNode> path=parent.getPath();
        path.add(this);
        return(path);
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        if (child!=null) {
            child.setParent(this);
            childNodes.add(index,(RulesNode)child);
        }
    }

    @Override
    public void remove(int index) {
        RulesNode SN=getChildAt(index);
        remove(SN);
    }

    @Override
    public void remove(MutableTreeNode node) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setUserObject(Object object) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void removeFromParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        parent=(RulesNode)newParent;
    }
    
    // TODO Fix this
    public RulesNode nextOccurence(String search) {
        return(null);
    }

}
