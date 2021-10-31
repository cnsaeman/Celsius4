/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

import celsius.gui.Editable;
import celsius.tools.Parser;
import java.sql.ResultSet;
import java.text.Normalizer;
import java.util.ArrayList;

/**
 *
 * @author cnsaeman
 */
public class Person extends TableRow implements Editable {

    public final Library library;
    public String collaborators;
    public String collaboratorsID;
    
    public Person(Library lib,String id) {
        super(lib,"persons",id,lib.personPropertyKeys);
        library=lib;
        orderedStandardKeys=library.orderedPersonPropertyKeys;
        tableHeaders=library.personPropertyKeys;
    }
    
    public Person(Library lib, ResultSet rs) {
        super(lib,"persons",rs,lib.itemPropertyKeys);
        library=lib;
        orderedStandardKeys=library.orderedPersonPropertyKeys;
        tableHeaders=library.personPropertyKeys;
    }

    public void save() {
        try {
            library.RSC.out("Saving Person");
            updateShorts();
            super.save();
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public void loadCollaborators() {
        try {        
            collaborators = "";
            collaboratorsID = "";
            ResultSet rs=library.dbConnection.prepareStatement("SELECT id, first_name, last_name FROM persons WHERE id IN (SELECT DISTINCT p2.person_id FROM item_person_links p1 INNER JOIN item_person_links p2 ON p2.item_id=p1.item_id AND (p1.person_id<>p2.person_id) WHERE p1.person_id IN ("+id+"));").executeQuery();
            while (rs.next()) {
                collaborators+="|"+rs.getString(3)+", "+rs.getString(2);
                collaboratorsID+="|"+rs.getString(1);
            }
            if (collaborators.length()>1) {
                collaborators=collaborators.substring(1);
                collaboratorsID=collaboratorsID.substring(1);
            } else {
                collaborators="None.";
                collaboratorsID="0";
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }
    
    public String getShortName(String field) {
        String person=getS(field);
        if (person.indexOf(",")==-1) return(person.replaceAll("\\|",", "));
       return(Parser.cutUntilLast(person.replaceAll(", .*?\\|", ", "),",").trim());
    }

    public String getName(int type) {
        if (type==31) {
            return("<a href='http://$$person."+id+"'>"+get("first_name")+" "+get("last_name").trim()+"</a>");
        }
        return(get("first_name")+" "+get("last_name").trim());
    }
    
    public String toText(boolean renew) {
        return(getName(0));
    }
    
    /**
     * This function removes accents etc from names for better search compatibility
     * 
     * @param firstName
     * @param lastName
     * @return 
     */
    public static String toSearch(String firstName, String lastName) {
        return(Normalizer.normalize((firstName+" "+lastName).toLowerCase(), Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", ""));
    }

    @Override
    public Library getLibrary() {
        return(library);
    }

    public ArrayList<String> getEditableFields() {
        ArrayList<String> fields=new ArrayList<>();
        fields=new ArrayList<>();
        for (String field : getFields()) {
            if (!fields.contains(field) && !field.startsWith("$")) fields.add(field);
        }
        fields.remove("last_modifiedTS");
        fields.remove("createdTS");
        fields.remove("remarks");
        fields.remove("id");
        fields.remove("search");
        return(fields);
    }

    public KeyValueTableModel getEditModel() {
        KeyValueTableModel KVTM=new KeyValueTableModel("Tag", "Value");
        ArrayList<String> tags=getEditableFields();
        for (String key : tags) {
            if (!KVTM.keys.contains(key)) {
                String t = null;
                t = get(key);
                if (t == null) {
                    t = "<unknown>";
                }
                KVTM.addRow(key, t);
            }
        }
        return(KVTM);
    }

    @Override
    public boolean containsKey(String key) {
        return(properties.keySet().contains(key));
    }

    @Override
    public void updateShorts() {
        // update short search string
        String newShortSearch = "";
        for (String searchtag : library.personSearchFields) {
            newShortSearch += " " + getS(searchtag);
        }
        put("search", Parser.normalizeForSearch(newShortSearch));
    }

    @Override
    public void notifyChanged() {
        library.personChanged(id);        
    }
    
    public String getExtended(String tag) {
        int i = tag.indexOf("&");
        if (i > -1) {
            char tp = tag.charAt(i + 1);
            tag = tag.substring(0, i);
            switch (tp) {
                default:
                    return (getS(tag));
            }
        }
        return (getS(tag));
    }
    

}
