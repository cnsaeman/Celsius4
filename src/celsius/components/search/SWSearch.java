/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.search;

import celsius.components.SWListItems;
import celsius.data.Item;
import celsius.data.Person;
import celsius.data.TableRow;
import celsius.components.tableTabs.CelsiusTable;
import celsius.tools.ToolBox;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 *
 * @author cnsaeman
 */
public class SWSearch extends SWListItems {

    private final int batchSize=100;

    private final String fullSearchString;
    private final String[] search;        // string to search for
    private final int mode;
    private String sqlTags;
    private String sqlTable;
    private String sqlOrderBy;
    private String sqlColumn;
    
  
    /**
     *  Constructor, read in information
     *  Mother, documenttablemodel, search string, class
     */
    public SWSearch(CelsiusTable it, String sstring, int m, int pid) {
        super(it,pid);
        //System.out.println(">> Started search with pid "+pid);
        mode=m;
        fullSearchString=sstring;
        search=sstring.toLowerCase().split(" ");
        sqlTags = library.itemTableSQLTags;
        sqlTable = "items";
        sqlOrderBy = "ORDER BY " + library.config.get("item-autosortcolumn");
        sqlColumn="search";
        if (mode==2) {
            sqlTags=library.personTableSQLTags;
            sqlTable="persons";
            sqlOrderBy="ORDER BY "+library.config.get("person-autosortcolumn");
            sqlColumn="search";
        } 
    }
    
    @Override
    public Void doInBackground() {
        //RSC.out("SCM>Celsius Library::Search module");
        //RSC.out("SCM>" + ToolBox.getCurrentDate());
        
        switch(mode) {
                case 0 : 
                    searchIndex();
                    break;
                case 1 :
                    searchDeep();
                    break;
                case 2 : 
                    searchIndex();
                    break;
                default :
                    searchDeep();
                    break;
        }

        RSC.out("SCM>finished:" + ToolBox.getCurrentDate());
        return(null);
    }
    
    private void searchIndex() {
        done = 0;
        int last_id=-1;
        int count=batchSize;
        int lastID=0;
        //System.out.println("--");
        //System.out.println("Starting search with string "+search[0]);
        while (!isCancelled() && count==batchSize) {
            StringBuilder sql=new StringBuilder();
            sql.append("SELECT ").append(sqlTags).append(" from ").append(sqlTable).append(" WHERE (").append(sqlColumn).append(" LIKE ?)");
            RSC.out(sql.toString());
            for (int i = 1; i < search.length; i++) {
                sql.append(" AND (").append(sqlColumn).append(" LIKE ?)");
            }
            sql.append("AND id>? COLLATE NOCASE ORDER BY id LIMIT ").append(String.valueOf(batchSize)).append(";");
            count=0;
            try {
                PreparedStatement statement= library.dbConnection.prepareStatement(sql.toString());
                statement.setString(1,"%"+search[0]+"%");
                for (int i=1;i<search.length;i++) {
                    statement.setString(i+1,"%"+search[i]+"%");
                }
                statement.setInt(search.length+1, lastID);
                ResultSet rs = statement.executeQuery();
                while (rs.next() && !isCancelled()) {
                    TableRow tableRow;
                    if (mode==2) {
                        tableRow = new Person(library, rs);
                    } else {
                        tableRow = new Item(library, rs);
                    }
                    //System.out.println(">> Publishing id: "+tableRow.id+" to table "+postID);
                    publish(tableRow);
                    lastID = rs.getInt(1);
                    count++;
                }
            } catch (SQLException ex) {
                RSC.outEx(ex);
            }
            done += batchSize;
            setProgress(done);
        }
        //System.out.println("Out of the outer loop, cancelled:" + String.valueOf(isCancelled()));
    }

    private void searchDeep() {
        done = 0;
        int last_id=-1;
        int count=batchSize;
        int lastID=0;
        
        while (!isCancelled() && count==batchSize) {
            String sql="SELECT rowid from search WHERE text MATCH ? LIMIT "+String.valueOf(batchSize)+" OFFSET "+String.valueOf(done)+";";
            count=0;
            try {
                RSC.out("Preparing statement");
                PreparedStatement statement=library.searchDBConnection.prepareStatement(sql);
                RSC.out("PS1");
                statement.setString(1,fullSearchString);
                RSC.out("PS2");
                ResultSet rs = statement.executeQuery();
                StringBuffer ids=new StringBuffer();
                while (rs.next() && !isCancelled()) {
                    ids.append(',');
                    ids.append(rs.getString(1));
                }
                ids.deleteCharAt(0);
                //RSC.out("IDs found: "+ids.toString());
                sql="SELECT "+library.itemTableSQLTags+" FROM item_attachment_links LEFT JOIN items ON item_attachment_links.item_id=items.id WHERE attachment_id IN ("+ids.toString()+") GROUP BY items.id;";
                RSC.out(sql);
                rs=library.executeResEX(sql);
                while (rs.next() && !isCancelled()) {
                    Item item = new Item(library, rs);
                    publish(item);
                    count++;
                }
            } catch (SQLException ex) {
                RSC.outEx(ex);
            }
            done += batchSize;
            setProgress(done);
        }
    }
    
}
