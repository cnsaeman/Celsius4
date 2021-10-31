/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius.data;

import celsius.tools.ToolBox;
import java.sql.ResultSet;
import java.util.*;
import javax.swing.tree.MutableTreeNode;
import javax.swing.tree.TreeNode;

/**
 *
 * @author cnsaeman
 */
public class StructureNode implements MutableTreeNode {
    
    public final Library library;
    public ArrayList<StructureNode> childNodes;
    public StructureNode parent;

    public int id;
    public Category category;
    
    public static StructureNode readInFromResultSet(Library lib, ResultSet rs) {
        HashMap<Integer,StructureNode> structureNodes=new HashMap<>();
        HashMap<Integer,String> childLists=new HashMap<>();
        StructureNode root=null;
        try {
            while (rs.next()) {
                int id=rs.getInt(1);
                Category category=lib.itemCategories.get(rs.getInt(2));
                StructureNode SN=new StructureNode(lib,category,id);
                if (root==null) root=SN;
                structureNodes.put(id, SN);
                childLists.put(id,rs.getString(3));
            }
            for (Integer id : structureNodes.keySet()) {
                StructureNode parent=structureNodes.get(id);
                String[] children=ToolBox.stringToArray2(childLists.get(id));
                for (String childID : children) {
                    StructureNode child=structureNodes.get(Integer.valueOf(childID));
                    child.parent=parent;
                    parent.childNodes.add(child);
                }
            }
        } catch (Exception ex) {
            lib.RSC.outEx(ex);
        }
        return(root);
    }
    
    public StructureNode(Library lib, Category cat, int i) {
        id=i;
        library=lib;
        category=cat;
        childNodes=new ArrayList<StructureNode>();
        parent=null;
    }

    @Override
    public StructureNode getChildAt(int childIndex) {
        return(childNodes.get(childIndex));
    }

    @Override
    public int getChildCount() {
        return(childNodes.size());
    }

    @Override
    public StructureNode getParent() {
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

    public void add(StructureNode node) {
        if (node!=null) {
            node.parent=this;
            childNodes.add(node);
        }
    }

    public void insert(StructureNode node, int i) {
        if (node!=null) {
            node.parent=this;
            childNodes.add(i,node);
        }
    }

    public void remove(StructureNode node) {
        if (node!=null) {
            node.parent=null;
            childNodes.remove(node);
        }
    }

    public StructureNode getRoot() {
        if (parent==null) return(this);
        else return(parent.getRoot());
    }

    public ArrayList<StructureNode> getPath() {
        if (parent==null) {
            ArrayList<StructureNode> path=new ArrayList<StructureNode>();
            path.add(this);
            return(path);
        }
        ArrayList<StructureNode> path=parent.getPath();
        path.add(this);
        return(path);
    }
    
    @Override
    public String toString() {
        if (category==null) return(library.name);
        return(category.label.trim());
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        if (child!=null) {
            child.setParent(this);
            childNodes.add(index,(StructureNode)child);
        }
    }

    @Override
    public void remove(int index) {
        StructureNode SN=getChildAt(index);
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
        parent=(StructureNode)newParent;
    }
    
    // TODO Fix this
    public StructureNode nextOccurence(String search) {
        return(null);
    }

    public String getChildListString() {
        if (childNodes.size()<1) return("");
        String children="";
        for (StructureNode node : childNodes) {
            children+=","+String.valueOf(node.id);
        }
        return(children.substring(1));
    }

}
