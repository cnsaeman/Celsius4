/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.categories;

import celsius.components.library.Library;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

/**
 *
 * @author cnsaeman
 */
public class Category extends HashMap<String,String> {
    
    public final Library library;
    public String id;
    public String label;
    public String remarks;
    public long created;
    
    /**
     * Create category 
     * 
     * @param library
     * @param id
     * @param label
     */
    public Category(Library library, String id,String label) {
        this.library=library;
        this.id=id;
        this.label=label;
    }

    public Category(Library library, String id) {
        this.library=library;
        this.id=id;
        ResultSet rs=library.executeResEX("SELECT * FROM item_categories WHERE id=?;",new String[]{id});
        try {
            rs.next();
            this.label=rs.getString(2);
            this.remarks=rs.getString(3);
        } catch (Exception ex) {
            this.label="ERROR";
            this.remarks="ERROR";
        }
    }

    
    /**
     * Create category from resultset
     * 
     * @param rs
     * @throws SQLException 
     */
    public Category(Library library,ResultSet rs) throws SQLException {
        super();
        this.library=library;
        id=rs.getString(1);
        label=rs.getString(2);
        remarks=rs.getString(3);
    }

    public void setRemarks(String rem) {
        // check if update necessary?
        if (rem.equals(remarks)) return;
        // in memory rename
        remarks=rem;
        // save in database
        try {
            library.executeEX("UPDATE item_categories SET remarks=? where id=?;", new String[] {remarks,id});
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    /**
     *  Saves or updates a category
     *  just updates label and remarks data, nothing else
     */
    public void save() {
        if (id==null) {
            id=library.executeInsertEX("INSERT INTO item_categories (label,remarks) VALUES (?,?);", new String[] {label,remarks});
        } else {
            library.executeEX("UPDATE item_categories SET remarks=?, label=? WHERE id=?;", new String[] {remarks,label,id});
        }
    }
    
}
