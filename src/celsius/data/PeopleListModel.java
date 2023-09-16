package celsius.data;

import celsius.components.library.Library;
import celsius.tools.ToolBox;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import javax.swing.DefaultListModel;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author cnsaeman
 */
public class PeopleListModel implements ListModel {
    
    public ArrayList<String> ids; // comma searated list of ids with this last name
    public ArrayList<String> lastNames;
    public LinkedHashMap<String,LinkedHashMap<String,String>> lookUp;
    public boolean containsData;
    public Library library;
    public String query1;
    public String query2;

    public PeopleListModel() {
        containsData=false;
    }
    
    public PeopleListModel(Library library, String query1, String query2) throws SQLException {
        this.library=library;
        this.query1=query1;
        this.query2=query2;
        loadData();
    }
    
    public void loadData() {
        ids=new ArrayList<>();
        lastNames=new ArrayList<>();
        lookUp=new LinkedHashMap<>();
        containsData=true;
        try {
            ResultSet rs = library.executeResEX(query1, query2);

            while (rs.next()) {
                String id = rs.getString(1);
                String lastName = rs.getString(2);
                String firstNames = rs.getString(3);
                if (firstNames.startsWith("|")) {
                    firstNames = "<empty>" + firstNames;
                }
                ids.add(id);
                lastNames.add(lastName);
                if ((lastName != null) && (lastName.length() > 0)) {
                    if (!lookUp.containsKey(lastName)) {
                        lookUp.put(lastName, new LinkedHashMap<>());
                    }
                    if ((firstNames != null) && (firstNames.length() > 0)) {
                        String[] firstNameList = ToolBox.stringToArray(firstNames);
                        String[] idList = ToolBox.stringToArray(id);
                        for (int i = 0; i < firstNameList.length; i++) {
                            lookUp.get(lastName).put(firstNameList[i], idList[i]);
                        }
                    } else {
                        lookUp.get(lastName).put("", id);
                    }
                }
            }
        } catch (Exception ex) {
            library.RSC.outEx(ex);
        }
    }

    @Override
    public int getSize() {
        if (containsData) return(ids.size());
        return(0);
    }

    @Override
    public Object getElementAt(int i) {
        return(lastNames.get(i));
    }

    @Override
    public void addListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public DefaultListModel getFirstNameModel(int[] ids) {
        if (!containsData || ids.length<1) return(new DefaultListModel()); 
        ArrayList<String> firstNames=new ArrayList<>();
        for (int i : ids) {
            if (i<lastNames.size()) {
                String lastName = lastNames.get(i);
                for (String firstName : lookUp.get(lastName).keySet()) {
                    firstNames.add(firstName);
                }
            }
        }
        Collections.sort(firstNames);
        DefaultListModel DLM=new DefaultListModel();
        DLM.addAll(firstNames);
        return(DLM);
    }
        
}
