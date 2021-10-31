/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

import java.sql.ResultSet;
import java.text.Normalizer;

/**
 *
 * @author cnsaeman
 */
public class Person extends TableRow {

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
    
    Library library;
    public String items;
    public String pages;
    public String collaborators;
    public String collaboratorsID;
    
    public Person(Library lib,String id) {
        super(lib,"persons",String.valueOf(id),lib.personPropertyKeys);
        library=lib;
        tableHeaders=library.personPropertyKeys;
        readIn();        
        items="0";
        try {
            ResultSet rs=lib.dbConnection.prepareStatement("SELECT SUM(items.pages),COUNT(*) FROM item_person_links INNER JOIN items on item_id=items.id where person_id="+id+";").executeQuery();
            if (rs.next()) {
                pages=rs.getString(1);
                items=rs.getString(2);
            }
            collaborators="";
            collaboratorsID="";
            rs=lib.dbConnection.prepareStatement("SELECT id, first_name, last_name FROM persons WHERE id IN (SELECT DISTINCT p2.person_id FROM item_person_links p1 INNER JOIN item_person_links p2 ON p2.item_id=p1.item_id AND (p1.person_id<>p2.person_id) WHERE p1.person_id IN ("+id+"));").executeQuery();
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
    
    public void save() {
        try {
            library.RSC.out("Saving Person");
            super.save();
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }

}
