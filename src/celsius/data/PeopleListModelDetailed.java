package celsius.data;

import java.util.ArrayList;
import javax.swing.ListModel;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

/**
 *
 * @author cnsaeman
 */
public class PeopleListModelDetailed implements ListModel {
    
    public ArrayList<Person> people;
    public final ArrayList<ListDataListener> listeners;
    public final ArrayList<String> ids;
    
    public PeopleListModelDetailed(ArrayList<Person> p) {
        people=p;
        listeners=new ArrayList<>();
        ids=new ArrayList<>();
    }

    @Override
    public int getSize() {
        if (people==null) return(0);
        return(people.size());
    }

    @Override
    public Object getElementAt(int i) {
        return(people.get(i).getName(0));
    }
    
    public String getIdentifier(Person p) {
        if (p.id!=null) return(p.id);
        return(p.get("last_name")+", "+p.get("first_name"));
    }
    
    public void add(Person p) {
        if (!ids.contains(getIdentifier(p))) {
            people.add(p);
            ids.add(getIdentifier(p));
            for (ListDataListener ll : listeners) {
                ll.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, people.size() - 1, people.size() - 1));
            }
        }
    }
    
    public void add(ArrayList<Person> persons) {
        for (Person p : persons) {
            if (!ids.contains(getIdentifier(p))) {
                people.add(p);
                ids.add(getIdentifier(p));
            }
        }
        if (!persons.isEmpty()) {
            for (ListDataListener ll : listeners) {
                ll.intervalAdded(new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, people.size() - persons.size(), people.size() - 1));
            }
        }
    }
    
    public void remove(int p) {
        ids.remove(getIdentifier(people.get(p)));
        people.remove(p);
        for (ListDataListener ll : listeners) {
            ll.intervalRemoved(new ListDataEvent(this,ListDataEvent.INTERVAL_REMOVED,p,p));
        }
    }
    
    /**
     * Swap two elements, first one has to be the smaller one!
     * @param p1
     * @param p2 
     */
    public void swap(int p1,int p2) {
        Person p=people.get(p2);
        people.remove(p2);
        people.add(p1, p);
        for (ListDataListener ll : listeners) {
            ll.contentsChanged(new ListDataEvent(this,ListDataEvent.CONTENTS_CHANGED,p1,p2));
        }
    }

    @Override
    public void addListDataListener(ListDataListener ll) {
        listeners.add(ll);
    }

    @Override
    public void removeListDataListener(ListDataListener ll) {
        listeners.remove(ll);
    }
    
}
