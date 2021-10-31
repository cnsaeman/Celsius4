/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius.tools;

import celsius.Resources;
import celsius.gui.SafeMessage;
import celsius.gui.MainFrame;
import celsius.data.Library;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;

/**
 *
 * @author cnsaeman
 */
public class Plugins extends HashMap<String,Plugin> {
    
    public static final String[] types={"auto-items","manual-items","export","import","manual-people"};

    //Rewrite as arrays

    public final HashMap<String,String> parameters;
    private final Resources RSC;

    public Plugins(Resources rsc) {
        super();
        RSC=rsc;
        parameters=new HashMap<>();
    }

    public void readInAvailablePlugins() {
        this.clear();
        parameters.clear();
        String[] listOfFiles=new File("plugins").list();
        for (String fn : listOfFiles) {
            if ((fn.startsWith("Plugin")) && (fn.endsWith(".class")) && (!fn.contains("$"))) {
                try {
                    RSC.out("Plugins>Loading Plugin: "+fn);
                    Plugin pl=new Plugin(Parser.cutUntil(fn,".class"));
                    put(pl.metaData.get("title"),pl);
                    // TODO set configuration paramters??
                }
                catch (Exception e) { 
                    RSC.out("Error loading Plugin "+fn);
                    RSC.outEx(e);
                }
            }
        }
        RSC.out("Plugins>Done reading in plugins");        
    }

    public ArrayList<Plugin> listPlugins(String type, Library Lib) {
        ArrayList<Plugin> ret= new ArrayList<>();
        String pluginsString=Lib.config.get("plugins-"+type);
        if (pluginsString==null) return(ret);
        String[] plugins=ToolBox.stringToArray(pluginsString);
        for (String plugin : plugins)
            if (this.containsKey(plugin))
                ret.add(get(plugin));
        return(ret);
    }

    public DefaultListModel getPluginsDLM() {
        DefaultListModel DLM = new DefaultListModel();
        ArrayList<String> pls=new ArrayList<>();
        for (String title : keySet())
            pls.add(title);
        Collections.sort(pls);
        for (String title : pls)
            DLM.addElement(title);
        return(DLM);
    }

    /**
     *
     * @param type : which type of plugin
     * @param library : which Library
     * @return
     */
    public DefaultListModel getPluginsDLM(String type, Library library) {
        System.out.println("Produce type "+type);
        DefaultListModel DLM = new DefaultListModel();
        if (library==null) return(DLM);
        String pluginsString=library.config.get("plugins-"+type);
        if (pluginsString==null) return(DLM);
        String[] plugins=ToolBox.stringToArray(pluginsString);
        for (String plugin : plugins) {
            if (this.containsKey(plugin)) {
                DLM.addElement(plugin);
            }
        }
        return(DLM);
    }

    /**
     *
     * @param type : which type of plugin
     * @param Lib : which Library
     * @param sorted : whether the output should be sorted alphabetically
     * @return
     */
    public DefaultComboBoxModel getPluginsDCBM(String type, Library Lib) {
        DefaultComboBoxModel DCBM = new DefaultComboBoxModel();
        if (Lib==null) return(DCBM);
        String pluginsString=Lib.config.get("plugins-"+type);
        if (pluginsString==null) return(DCBM);
        String[] plugins=ToolBox.stringToArray(pluginsString);
        for (String plugin : plugins) {
            if (this.containsKey(plugin)) {
                DCBM.addElement(plugin);
            }
        }
        return(DCBM);
    }

    public String getInfo(String name) {
        Plugin current = get(name);
        String params = parameters.get(name);
        return("Plugin: " + current.metaData.get("title") + "\nAuthor: " + current.metaData.get("author") + "\nVersion: " + current.metaData.get("version") +"\nParameters: " + params +"\n" + ToolBox.wrap(current.metaData.get("help")));
    }

    public String getText(String name) {
        Plugin theplug = get(name);
        String tmp;
        if (theplug!=null) {
            tmp="<html><b>"+theplug.metaData.get("title")+"</b>, Version: "+theplug.metaData.get("version");
            tmp+="<br/>Author: "+theplug.metaData.get("author")+"\n";
            tmp+="<br/>Class name: "+theplug.className+"\n<br/>";
            tmp+=theplug.metaData.get("help")+"\n";
            if (theplug.metaData.containsKey("parameter-help") && (!theplug.metaData.get("parameter-help").startsWith("none"))) {
                tmp+="<br/>Parameters: "+theplug.metaData.get("parameter-help")+"\n";
                tmp+="<br/>Default parameters: "+theplug.metaData.get("defaultParameters")+"\n";
            } else {
                tmp+="<br/>This plugin does not require any parameters.\n";
            }
            tmp+="<br/>Types: "+theplug.metaData.get("type")+"\n";
            tmp+="<br/>Required fields: "+theplug.metaData.get("requiredFields")+"\n";
            tmp+="<br/>Needs plain text of first page: "+theplug.metaData.get("needsFirstPage")+"\n";
            tmp+="<br/>Would like plain text of first page: "+theplug.metaData.get("wouldLikeFirstPage")+"\n";
            if (theplug.metaData.containsKey("longRunTime"))
                tmp+="<br/>Longer runtime: "+theplug.metaData.get("longRunTime");
        } else {
            tmp="<br/>not found!";
        }
        return(tmp+"</html>");
    }

    public void setParams(String name, String p) {
        parameters.put(name,p);
    }

    public String getParams(String name) {
        return(parameters.get(name));
    }

    /**
     * This procedure tries to add the plugin with name s and type type and returns true if successful.
     */
    public boolean add(Plugin p) throws Exception {
        boolean added=false;
        boolean exists=false;
        String title=p.metaData.get("title");
        if (containsKey(title))
            exists=true;
        if (!exists) {
            put(title,p);
            parameters.put(title, "");
            added=true;
            save();
        }
        return(added);
    }

    public void save() throws IOException {
        XMLHandler out=new XMLHandler("plugins/configuration.plugins.xml");
        out.clear();
        for (String t : keySet()) {
            out.addEmptyElement();
            out.put("name", get(t).className);
            String p=parameters.get(t);
            if ((p!=null) && (p.trim().length()>0))
                out.put("parameters", p);
        }
        out.writeBack();
    }

    public void updatePlugins() {
        MainFrame MF=RSC.MF;
        MF.jCBExpFilter.setModel(getPluginsDCBM("export",RSC.getCurrentlySelectedLibrary()));
        MF.guiPluginPanel.adjustPluginList();
        MF.guiInfoPanel.jCBBibPlugins.setModel(getPluginsDCBM("export",RSC.getCurrentlySelectedLibrary()));
        if (MF.jCBExpFilter.getItemCount()>0)
            MF.jCBExpFilter.setSelectedIndex(0);
    }
    
}
