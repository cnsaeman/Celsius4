package celsius.data;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.ListModel;
import javax.swing.event.ListDataListener;

/**
 *
 * @author cnsaeman
 */
public class KeywordListModel implements ListModel {
    
    public ArrayList<String> ids; // comma searated list of ids with this last name
    public ArrayList<String> labels;
    public ArrayList<String> remarks;
    public boolean containsData;

    public KeywordListModel() {
        containsData=false;
    }
    
    public KeywordListModel(ResultSet rs) throws SQLException {
        ids=new ArrayList<>();
        labels=new ArrayList<>();
        remarks=new ArrayList<>();
        containsData=true;
        while (rs.next()) {
            ids.add(rs.getString(1));
            labels.add(rs.getString(2));
            remarks.add(rs.getString(3));
        }
    }

    @Override
    public int getSize() {
        if (containsData) return(ids.size());
        return(0);
    }

    @Override
    public Object getElementAt(int i) {
        return(labels.get(i));
    }

    @Override
    public void addListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public void removeListDataListener(ListDataListener ll) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    
}
