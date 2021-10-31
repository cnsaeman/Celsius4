/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.data;

import celsius.tools.Parser;
import celsius.tools.ToolBox;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 *
 * @author cnsaeman
 */
public class TableRow {
    
    public Library library;
    public String table; 
    public String id;
    public String lastError;
    public HashMap<String,String> properties;
    public boolean needsSaving;
    public boolean loadedFullData;
    public ArrayList<String> dirtyFields; // list of keys that need saving
    public final HashSet<String> propertyKeys;
    public HashSet<String> tableHeaders;
    
    public TableRow(Library lib, String tab, String i, HashSet<String> pF) {
        library=lib;
        table=tab;
        id=i;
        lastError=null;
        readIn();
        needsSaving=false;
        propertyKeys=pF;
    }

    public TableRow(Library lib, String tab, ResultSet rs, HashSet<String> pF) {
        library=lib;
        table=tab;
        lastError=null;
        try {
            readIn(rs);
            loadedFullData=false;
        } catch (Exception e) {
            e.printStackTrace();
            lastError="E2:"+e.toString();
        }
        needsSaving=false;
        propertyKeys=pF;
    }

    public TableRow(Library lib, String tab, HashSet<String> pF) {
        library=lib;
        table=tab;
        lastError=null;
        properties=new HashMap<>();
        loadedFullData=false;
        needsSaving=false;
        dirtyFields=new ArrayList<>();
        propertyKeys=pF;
    }
    
    public TableRow(String tab, HashSet<String> pF) {
        library=null;
        table=tab;
        lastError=null;
        properties=new HashMap<>();
        needsSaving=false;
        loadedFullData=false;
        dirtyFields=new ArrayList<>();
        propertyKeys=pF;
    }
    
    public void readIn(ResultSet rs) throws SQLException, IOException, ClassNotFoundException {
        properties=new HashMap<>();
        dirtyFields=new ArrayList<>();
        int pos=rs.getMetaData().getColumnCount();
        for (int i=0;i<pos;i++) {
            String cn=rs.getMetaData().getColumnName(i+1);
            if (cn.equals("id")) id=rs.getString(i+1);
            if (cn.equals("attributes")) {
                HashMap<String,String> attributes=setAttributes(rs.getBytes(i+1));
                for (String key : attributes.keySet()) {
                    properties.put(key,attributes.get(key));
                }
            } else {
                properties.put(cn,rs.getString(i+1));
            }
        }
        dirtyFields.clear();
        needsSaving=false;
    }
    
    public void readIn() {
        try {
            ResultSet rs=library.dbConnection.prepareStatement("SELECT * FROM "+table+" where id = "+id+" LIMIT 1;").executeQuery();
            if (rs.next()) {
                readIn(rs);
                loadedFullData=true;
            } else {
                lastError="E1:Not found";
            }
        } catch (Exception e) {
            e.printStackTrace();
            lastError="E2:"+e.toString();
        }
    }
    
    public void ensureFullData() {
        if (!loadedFullData) {
            if (id==null || id.equals("")) {
                loadedFullData=true;
                return;
            }
            readIn();
            
            // read in people
            int linkType=0;
            for (String peopleField : library.peopleFields) {
                String out1="";
                String out2="";
                String sql="SELECT id, last_name, first_name FROM item_person_links INNER JOIN persons on persons.id=person_id  WHERE item_id="+id+" AND link_type="+String.valueOf(linkType)+" ORDER BY ord ASC";
                try {
                    ResultSet rs=library.executeResEX(sql);
                    while (rs.next()) {
                        out1+="|"+rs.getString(2);
                        String firstName=rs.getString(3);
                        if ((firstName!=null) && (!firstName.isBlank())) out1+=", "+rs.getString(3);
                        out2+="|"+rs.getString(1);
                    }
                } catch(Exception e) {
                    library.RSC.outEx(e);
                }
                properties.put(peopleField,out1.substring(1));
                properties.put(peopleField+"_ids",out2.substring(1));
                linkType++;
            }
            
            // read in keywords
            String out="";
            String sql="SELECT label FROM item_keyword_links INNER JOIN keywords on keywords.id=keyword_id  WHERE item_id="+id+" ORDER BY label ASC";
            try {
                ResultSet rs=library.dbConnection.prepareStatement(sql).executeQuery();
                while (rs.next()) {
                    out+="|"+rs.getString(1);
                }
            } catch(Exception e) {
                library.RSC.outEx(e);
            }
            if (!out.isBlank()) properties.put("keywords",out.substring(1));
            
            // read in attachments
            out="";
            sql="SELECT name, filetype, path, source, plaintext FROM item_attachment_links INNER JOIN attachments on attachments.id=attachment_id  WHERE item_id="+id+" ORDER BY ord ASC;";
            int pos=0;
            try {
                ResultSet rs=library.dbConnection.prepareStatement(sql).executeQuery();
                while (rs.next()) {
                    String label=rs.getString(1);
                    if (label.equals("location")) label=rs.getString(2)+"-file";
                    properties.put("attachment-"+String.valueOf(pos)+"-label",label);
                    properties.put("attachment-"+String.valueOf(pos)+"-filetype",rs.getString(2));
                    properties.put("attachment-"+String.valueOf(pos)+"-path",rs.getString(3));
                    properties.put("attachment-"+String.valueOf(pos)+"-source",rs.getString(4));
                    properties.put("attachment-"+String.valueOf(pos)+"-plaintext",rs.getString(5));
                    pos++;
                }
            } catch(Exception e) {
                library.RSC.outEx(e);
            }
            properties.put("attachment-count",String.valueOf(pos));
            loadedFullData=true;
        }
    }
    
    
    public String get(String key) {
        if (propertyKeys.contains(key)) {
            return(properties.get(key));
        }
        return(null);
    }
    
    public String getS(String s) {
        String tmp=get(s);
        if (tmp==null) tmp="";
        return(tmp);
    }
    
    public void put(String key, String value) {
        if (value==null) {
            remove(key);
        } else {
            if (!value.equals(properties.get(key))) {
                properties.put(key, value);
                needsSaving = true;
                if (!dirtyFields.contains(key)) {
                    dirtyFields.add(key);
                }
            }
        }
    }
    
    public void remove(String key) {
        if (properties.containsKey(key)) {
            if (properties.get(key)!=null) {
                properties.put(key, null);
                if (!dirtyFields.contains(key)) dirtyFields.add(key);
                needsSaving=true;
            }
        }
    }

    public boolean isNotSet(String s) {
        return(!properties.containsKey(s));
    }

    public boolean isEmpty(String s) {
        if (isNotSet(s)) return(true);
        return(getS(s).equals(""));
    }
    
    public ArrayList<String> getFields() {
        ArrayList<String> out=new ArrayList<>();
        out.addAll(properties.keySet());
        return(out);
    }
    
    /**
     * Write an item to the library
     * 
     * @throws Exception 
     */
    public void save() throws Exception {
        System.out.println("Saving tablerow...");
        if (this.needsSaving) {
            if ((library.dbConnection!=null) && (table!=null) && (!table.isBlank())) {
                // First step: save ordinary data
                if (id==null) {
                    String fieldsList="";
                    String qmarks="";
                    boolean saveAttributes=false;
                    for (String field : dirtyFields) {
                        if (!library.linkedField(field)) {
                            if (!tableHeaders.contains(field)) {
                                saveAttributes=true;
                            } else {
                                fieldsList += "," + field;
                                qmarks += ",?";
                            }
                        }
                    }
                    if (saveAttributes) {
                        fieldsList += ",attributes";
                        qmarks += ",?";
                    }
                    fieldsList+=",last_modified";
                    qmarks+=",?";
                    String sql="INSERT INTO "+table+" ("+fieldsList.substring(1)+") VALUES ("+qmarks.substring(1)+");";
                    System.out.println("Writing new Using SQL: "+sql);
                    PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                    int i=1;
                    for (String field : dirtyFields) {
                        if (!library.linkedField(field)) {
                            if (!tableHeaders.contains(field)) {
                                pstmt.setString(i, properties.get(field));
                                i++;
                            }
                        }
                    }
                    if (saveAttributes) {
                        pstmt.setBytes(i, getAttributesBytes());
                        i++;
                    }
                    Long l=ToolBox.now();
                    pstmt.setLong(i, l);
                    properties.put("last_modified", Long.toString(l));
                    pstmt.execute();
                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                    id=String.valueOf(generatedKeys.getLong(1));
                    System.out.println("Obtained id: "+id);
                } else {
                    String fieldsList="";
                    for (String field : dirtyFields) {
                        if (!library.linkedField(field)) {
                            fieldsList+=", "+field+" = ?";
                        }
                    }
                    fieldsList+=", last_modified = ?";
                    String sql="UPDATE "+table+" SET "+fieldsList.substring(1)+" WHERE id="+id;
                    System.out.println("Using SQL: "+sql);
                    PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                    int i=1;
                    for (String field : dirtyFields) {
                        if (!library.linkedField(field)) {
                            if (field.equals("attributes")) {
                                pstmt.setBytes(i,getAttributesBytes());
                            }
                            pstmt.setString(i,properties.get(field));
                            i++;
                        }
                    }
                    Long l=ToolBox.now();
                    pstmt.setLong(i, l);
                    properties.put("last_modified", Long.toString(l));
                    pstmt.execute();
                }
                for (String field : dirtyFields) {
                    if (library.linkedField(field)) {
                        if (field.equals("keywords")) {
                            // remember currently linked keyword ids
                            ResultSet old=library.executeResEX("SELECT keyword_id FROM item_keyword_links WHERE item_id="+String.valueOf(id));
                            ArrayList<String> oldIDs=new ArrayList<>();
                            while (old.next()) oldIDs.add(old.getString(1));
                            String[] keywordList=getS("keywords").split("\\|");
                            // establish all keyword links
                            for (String keyword : keywordList) {
                                PreparedStatement statement=library.dbConnection.prepareStatement("SELECT id FROM keywords where label = ?;");
                                statement.setString(1,keyword);
                                ResultSet rs=statement.executeQuery();
                                if (rs.next()) {
                                    String keyword_id=rs.getString(1);
                                    oldIDs.remove(keyword_id);
                                    library.executeEX("INSERT OR IGNORE INTO item_keyword_links (item_id, keyword_id) VALUES ("+id+","+keyword_id+");");
                                } else {
                                    String sql="INSERT INTO keywords (label) VALUES (?);";
                                    PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                                    pstmt.setString(1, keyword);
                                    pstmt.execute();
                                    ResultSet generatedKeys = pstmt.getGeneratedKeys();
                                    String keyword_id=String.valueOf(generatedKeys.getLong(1));
                                    library.executeEX("INSERT INTO item_keyword_links (item_id,keyword_id) ("+id+","+keyword_id+");");
                                }
                            }
                            // clean up old keyword links
                            for (String kid : oldIDs) {
                                library.executeEX("DELETE FROM item_keyword_links WHERE item_id="+id+" AND keyword_id="+kid+";");
                            }
                            library.deleteUnusedLinks("keyword", oldIDs);
                        }
                        int linkType=0;
                        int order=0;
                        for (String person : library.peopleFields) {
                            if (field.equals(person)) {
                                ResultSet old=library.executeResEX("SELECT person_id FROM item_person_links WHERE item_id="+String.valueOf(id));
                                ArrayList<String> oldIDs=new ArrayList<>();
                                while (old.next()) oldIDs.add(old.getString(1));
                                String[] personList=getS(person).split("\\|");
                                // delete all person links
                                library.executeEX("DELETE FROM item_person_links WHERE item_id="+id+" AND link_type="+String.valueOf(linkType)+";");
                                for (String p : personList) {
                                    String firstName=Parser.cutFrom(p,",").trim();
                                    String lastName=Parser.cutUntil(p,",").trim();
                                    PreparedStatement statement=library.dbConnection.prepareStatement("SELECT id FROM persons where last_name = ? AND first_name = ?;");
                                    statement.setString(1,lastName);
                                    statement.setString(2,firstName);
                                    ResultSet rs=statement.executeQuery();
                                    if (rs.next()) {
                                        String person_id=rs.getString(1);
                                        oldIDs.remove(person_id);
                                        library.executeEX("INSERT INTO item_person_links (item_id, person_id, link_type, ord) VALUES ("+id+","+person_id+","+String.valueOf(linkType)+","+String.valueOf(order)+");");
                                        order++;
                                    } else {
                                        String sql="INSERT INTO persons (last_name,first_name,search,last_modified) VALUES (?,?,?,?);";
                                        PreparedStatement pstmt=library.dbConnection.prepareStatement(sql);
                                        pstmt.setString(1, lastName);
                                        pstmt.setString(2, firstName);
                                        pstmt.setString(3, Person.toSearch(firstName,lastName));
                                        pstmt.setLong(4, ToolBox.now());
                                        pstmt.execute();
                                        ResultSet generatedKeys = pstmt.getGeneratedKeys();
                                        String person_id=String.valueOf(generatedKeys.getLong(1));
                                        library.executeEX("INSERT INTO item_person_links (item_id, person_id, link_type, ord) VALUES ("+id+","+person_id+","+String.valueOf(linkType)+","+String.valueOf(order)+");");
                                        order++;
                                    }
                                    
                                }
                                // clean up old personlinks
                                library.deleteUnusedLinks("person", oldIDs);
                            }
                            linkType++;
                        }
                        if (field.equals("attachment-path")) {
                            String path=properties.get("attachment-path");
                            String fileType=properties.get("attachment-filetype");
                            ResultSet rs=library.executeResEX("SELECT ord FROM item_attachment_links WHERE item_id="+id+";");
                            int ord=0;
                            while (rs.next()) {
                                if (rs.getInt(1)>=ord) ord=rs.getInt(1)+1;
                            }
                            
                        }
                    }
                }
                dirtyFields.clear();
                this.needsSaving=false;
                library.itemChanged(table,id);
            } else {
                throw (new Exception("No db Connection to save item to!"));
            }
        } else {
            System.out.println("No saving required");
        }
    }
    
    private HashMap<String, String> setAttributes(byte[] attBytes) throws IOException, ClassNotFoundException {
        if ((attBytes!=null) && (attBytes.length>2)) {
            ByteArrayInputStream bais = new ByteArrayInputStream(attBytes);
            ObjectInputStream ois = new ObjectInputStream(bais);
            ois.close();
            bais.close();
            return((HashMap) ois.readObject());
        } else {
            return(new HashMap<>());
        }
    }

    public byte[] getAttributesBytes() throws IOException {
        HashMap<String,String> attributes=new HashMap<>();
        for (String key : properties.keySet()) {
            if (!tableHeaders.contains(key)) {
                attributes.put(key,properties.get(key));
            }
        }
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(attributes);
        return(baos.toByteArray());
    }
    
    public String getShortNames(String field) {
        String person=getS(field);
        if (person.indexOf(",")==-1) return(person.replaceAll("\\|",", "));
       return(Parser.cutUntilLast(person.replaceAll(", .*?\\|", ", "),",").trim());
    }
    
    /**
     * Turn an author string into a short string
     */
    public String getBibTeXNames(String field) {
        String person=getS(field);
        return (person.replaceAll("\\|", " and "));
    }

    /**
     * Create LaTex author from BibTeX one //NEXT
     */
    public String getNames3(String field) {
        String personList=getS(field);
        if (personList.indexOf(",")==-1) return(personList.replaceAll("\\|",", "));
        String[] people=personList.split("\\|");
        String person;
        String out="";
        try {
            for(int j=0;j<people.length;j++) {
                person=people[j];
                if (person.indexOf(",")>-1) {
                    person=Parser.cutFrom(person,",").trim()+" "+Parser.cutUntil(person,",").trim();
                    if (person.indexOf(".")==-1) {
                        String prenomes=Parser.cutUntilLast(person," ").trim();
                        person=Parser.cutFromLast(person," ").trim();
                        int i=prenomes.lastIndexOf(" ");
                        while (i>-1) {
                            person=prenomes.substring(i+1,i+2)+". "+person;
                            prenomes=prenomes.substring(0,i).trim();
                            i=prenomes.lastIndexOf(" ");
                        }
                        person=prenomes.substring(0,1)+". "+person;
                    }
                } else person=people[j];
                out+=person+", ";
            }
        } catch (Exception e) { out+="Error"; }
        out=Parser.cutUntilLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.cutUntilLast(out,", ")+" and "+Parser.cutFromLast(out,", ");
        return(out);
    }

    /**
     * Create LaTex author from BibTeX one //NEXT
     */
    public String getNames3WithLinks(String field) {
        String[] people=getS(field).split("\\|");
        String[] peopleIDs=getS(field+"_ids").split("\\|");
        String person;
        String out="";
        try {
            for(int j=0;j<people.length;j++) {
                person=people[j];
                if (person.indexOf(",")>-1) {
                    person=Parser.cutFrom(person,",").trim()+" "+Parser.cutUntil(person,",").trim();
                    if (person.indexOf(".")==-1) {
                        String prenomes=Parser.cutUntilLast(person," ").trim();
                        person=Parser.cutFromLast(person," ").trim();
                        int i=prenomes.lastIndexOf(" ");
                        while (i>-1) {
                            person=prenomes.substring(i+1,i+2)+". "+person;
                            prenomes=prenomes.substring(0,i).trim();
                            i=prenomes.lastIndexOf(" ");
                        }
                        person=prenomes.substring(0,1)+". "+person;
                    }
                } else person=people[j];
                out+="<a href='http://$$person."+peopleIDs[j]+"'>"+person+"</a>, ";
            }
        } catch (Exception e) { library.RSC.outEx(e);out+="Error"; }
        out=Parser.cutUntilLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.cutUntilLast(out,", ")+" and "+Parser.cutFromLast(out,", ");
        return(out);
    }
    
    /**
     * Create usual authors list
     */
    public String getNames4(String field) {
        String person=getS(field);
        String[] authors=person.split("\\|");
        String author;
        String out="";
        try {
            for(int j=0;j<authors.length;j++) {
                author=authors[j];
                if (author.indexOf(",")>-1)
                    author=Parser.cutFrom(author,",").trim()+" "+Parser.cutUntil(author,",").trim();
                out+=author+", ";
            }
        } catch (Exception e) { out+="Error"; }
        out=Parser.cutUntilLast(out,", ");
        if (out.indexOf(", ")>-1)
           out=Parser.cutUntilLast(out,", ")+" and "+Parser.cutFromLast(out,", ");
        return(out);
    }
    
    
}
