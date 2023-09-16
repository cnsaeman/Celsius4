package celsius.components.categories;

import celsius.components.library.Library;
import celsius.tools.ToolBox;
import java.sql.PreparedStatement;
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

    public String id;
    public Category category;
    
    public StructureNode(Library library, Category category, String id) {
        this.id=id;
        this.library=library;
        this.category=category;
        childNodes=new ArrayList<>();
        parent=null;
    }

    public StructureNode(StructureNode node) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public static StructureNode readInFromResultSet(Library library, ResultSet rs) {
        HashMap<String,StructureNode> structureNodes=new HashMap<>();
        HashMap<String,String> childLists=new HashMap<>();
        StructureNode root=null;
        try {
            while (rs.next()) {
                String id=rs.getString(1);
                Category category=library.itemCategories.get(rs.getInt(2));
                StructureNode SN=new StructureNode(library,category,id);
                if (root==null) root=SN;
                structureNodes.put(id, SN);
                childLists.put(id,rs.getString(3));
            }
            for (String id : structureNodes.keySet()) {
                StructureNode parent=structureNodes.get(id);
                String[] children=ToolBox.stringToArray2(childLists.get(id));
                for (String childID : children) {
                    StructureNode child=structureNodes.get(childID);
                    if (child!=null) {
                        child.parent=parent;
                        parent.childNodes.add(child);
                    }
                }
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
        return(root);
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
    
    /**
     * Determines if this structure node has the ancestor "ancestor" and returns the result
     * 
     * @param ancestor
     * @return 
     */
    public boolean hasAncestor(StructureNode ancestor) {
        if (this.parent==null) return(false);
        return((this.parent==ancestor) || (parent.hasAncestor(ancestor)));
    }

    @Override
    public Enumeration children() {
        return(Collections.enumeration(childNodes));
    }

    public StructureNode getRoot() {
        if (parent==null) return(this);
        else return(parent.getRoot());
    }

    public ArrayList<StructureNode> getPath() {
        if (parent==null) {
            ArrayList<StructureNode> path=new ArrayList<>();
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
        return(category.getS("label").trim());
    }

    @Override
    public void insert(MutableTreeNode child, int index) {
        System.out.println("SN:insert1");
        if (child!=null) insert((StructureNode)child,index);
    }

    public void insert(StructureNode node, int index) {
        System.out.println("SN:insert2");
        if (node!=null) {
            try {
                // Fix parent information
                node.parent=this;
                node.writeParentToDatabase();
                // fix children 
                if (!childNodes.contains(node)) childNodes.add(index,node);
                writeChildrenToDatabase();
            } catch (Exception ex) {
                library.RSC.outEx(ex);
            }
        }
    }

    @Override
    public void remove(int index) {
        System.out.println("SN:remove1");
        StructureNode SN=getChildAt(index);
        remove(SN);
    }

    public void remove(StructureNode node) {
        System.out.println("SN:remove2");
        if (node!=null) {
            node.parent=null;
            childNodes.remove(node);
            writeChildrenToDatabase();
        }
    }

    @Override
    public void remove(MutableTreeNode node) {
        System.out.println("SN:remove3");
        if (node!=null) remove((StructureNode)node);
    }

    @Override
    public void removeFromParent() {
        System.out.println("SN:removeFromParent");
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setParent(MutableTreeNode newParent) {
        System.out.println("SN:setParent");
        parent=(StructureNode)newParent;
    }

    @Override
    public void setUserObject(Object object) {
        String newLabel=(String)object;
        if (category.getS("label").equals(newLabel)) return;
        category.put("label",newLabel);
        try {
            category.save();
        } catch (Exception ex) {
            category.library.RSC.outEx(ex);
        }
    }
    
    public StructureNode nextOccurence(String search) {
        if (category.getS("label").toLowerCase().contains(search)) return(this);
        for (StructureNode node : childNodes) {
            StructureNode found=node.nextOccurence(search);
            if (found!=null) {
                return(found);
            }
        }
        if (parent!=null) return(parent.nextOccurence(search,parent.getIndex(this)));
        return(null);
    }
    
    /** 
     * Find next occurrence of search string in category tree, starting from this node and children at position
     * 
     * @param search
     * @param position
     * @return 
     */
    public StructureNode nextOccurence(String search, int position) {
        for(int i=position+1;i<childNodes.size();i++) {
            StructureNode found=childNodes.get(i).nextOccurence(search);
            if (found!=null) {
                return(found);
            }
        }
        if (parent!=null) return(parent.nextOccurence(search,parent.getIndex(this)));
        return(null);
    }
    
    public StructureNode next() {
        if (!childNodes.isEmpty()) return(childNodes.get(0));
        return(parent.next(parent.getIndex(this)));
    }
    
    public StructureNode next(int position) {
        if (childNodes.size()>position+1) return(childNodes.get(position+1));
        return(parent.next(parent.getIndex(this)));
    }

    public String getChildListString() {
        if (childNodes.size()<1) return("");
        StringBuilder childrenList=new StringBuilder();
        for (StructureNode node : childNodes) {
            childrenList.append(",").append(String.valueOf(node.id));
        }
        return(childrenList.substring(1));
    }
    
    public void writeParentToDatabase() {
        try {
            if (id == null) {
                // new node
                PreparedStatement statement = library.dbConnection.prepareStatement("INSERT INTO category_tree (category,parent) VALUES (?,?);");
                statement.setString(1, category.id);
                statement.setString(2, parent.id);
                statement.execute();
                ResultSet rs = statement.getGeneratedKeys();
                rs.next();
                String cid = rs.getString(1);
                id = cid;
            } else {
                // write parent id 
                PreparedStatement statement = library.dbConnection.prepareStatement("UPDATE category_tree SET parent=? WHERE id = ?;");
                statement.setString(1, parent.id);
                statement.setString(2, id);
                statement.execute();
            }
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
    }
    
    private void writeChildrenToDatabase() {
        try {
            PreparedStatement statement=library.dbConnection.prepareStatement("UPDATE category_tree SET children = ? where id = ?;");
            statement.setString(1,getChildListString());
            statement.setString(2,id);
            statement.execute();
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
    }
    
    /**
     * Removes this node from the database
     */
    public void destroy() {
        try {
            PreparedStatement statement=library.dbConnection.prepareStatement("DELETE FROM category_tree WHERE id = ?;");
            statement.setString(1,id);
            statement.execute();
        } catch (Exception e) {
            library.RSC.outEx(e);
        }
    }

}
