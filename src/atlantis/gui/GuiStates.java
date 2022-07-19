/*
 Atlantis Software GUI package
*/

package atlantis.gui;

import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JComponent;

/**
 *
 * @author cnsaeman
 */
public class GuiStates {
    
    public HashMap<String,HashMap<String,Boolean>> states;
    public HashMap<String,HashMap<String,ArrayList<JComponent>>> directlyEnabledComponents;
    public HashMap<String,ArrayList<HasManagedStates>> notificationList;
    
    public GuiStates() {
        super();
        states=new HashMap<>();
        notificationList=new HashMap<>();
        directlyEnabledComponents=new HashMap<>();
    }

    /**
     * Set a state in a particular group with a particular name. In most cases, use "adjustState" instead.
     * 
     * @param stateGroup
     * @param stateName
     * @param state 
     */
    public void setState(String stateGroup, String stateName, boolean state) {
        if (!states.containsKey(stateGroup)) {
            states.put(stateGroup, new HashMap<>());
        }
        states.get(stateGroup).put(stateName, state);
    }
    
    /**
     * Return current state
     * 
     * @param stateGroup
     * @param stateName
     * @return 
     */
    public boolean getState(String stateGroup, String stateName) {
        if (!states.containsKey(stateGroup)) return(false);
        return(states.get(stateGroup).get(stateName));
    }

    /**
     * Set a state in a particular group with a particular name and adjust guiComponents
     * 
     * @param stateGroup
     * @param stateName
     * @param state 
     */
    public void adjustState(String stateGroup, String stateName, boolean state) {
        if (!states.containsKey(stateGroup)) {
            states.put(stateGroup, new HashMap<>());
        }
        states.get(stateGroup).put(stateName, state);
        if (directlyEnabledComponents.get(stateGroup).get(stateName)!=null) {
            for (JComponent component : directlyEnabledComponents.get(stateGroup).get(stateName)) {
                component.setEnabled(state);
            }
        }
        for (HasManagedStates component : notificationList.get(stateGroup)) {
            component.adjustStates();
        }
    }
    
    public void adjustStates(String stateGroup) {
        for (String stateName : states.get(stateGroup).keySet()) {
            if (directlyEnabledComponents.get(stateGroup).containsKey(stateName)) {
                for (JComponent component : directlyEnabledComponents.get(stateGroup).get(stateName)) {
                    component.setEnabled(states.get(stateGroup).get(stateName));
                }
            }
            for (HasManagedStates component : notificationList.get(stateGroup)) {
                component.adjustStates();
            }
        }
    }

    public void registerDirectlyEnabledComponent(String stateGroup, String stateName, JComponent component) {
        if (!states.containsKey(stateGroup)) {
            states.put(stateGroup, new HashMap<>());
        }
        if (!directlyEnabledComponents.containsKey(stateGroup)) {
            directlyEnabledComponents.put(stateGroup, new HashMap<>());
        }
        if (!notificationList.containsKey(stateGroup)) {
            notificationList.put(stateGroup, new ArrayList<>());
        }
        HashMap<String,ArrayList<JComponent>> lst=directlyEnabledComponents.get(stateGroup);
        if (!lst.containsKey(stateName)) {
            lst.put(stateName, new ArrayList<>());
        }
        lst.get(stateName).add(component);
    }

    public void registerDirectlyEnabledComponent(String stateGroup, String stateName, JComponent[] components) {
        for (JComponent component : components) {
            GuiStates.this.registerDirectlyEnabledComponent(stateGroup,stateName,component);
        }
    }
    
    public void registerListener(String stateGroup, HasManagedStates listener) {
        if (!notificationList.containsKey(stateGroup)) {
            notificationList.put(stateGroup, new ArrayList<>());
        }
        notificationList.get(stateGroup).add(listener);
    }
    
    public void unregister(String stateGroup) {
        states.remove(stateGroup);
        directlyEnabledComponents.remove(stateGroup);
        notificationList.remove(stateGroup);
    }
        
}
