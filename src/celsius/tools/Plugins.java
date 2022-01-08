/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius.tools;

import atlantis.tools.Parser;
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

    public void updateExportPlugins() {
        MainFrame MF=RSC.MF;
        DefaultComboBoxModel DCBM = new DefaultComboBoxModel();
        DefaultListModel DLM=new DefaultListModel();
        Library library=RSC.getCurrentlySelectedLibrary();
        if (library!=null) {
            String pluginsString = library.config.get("plugins-export");
            if (pluginsString!=null) {
                String[] plugins = ToolBox.stringToArray(pluginsString);
                for (String plugin : plugins) {
                    if (containsKey(plugin)) {
                        DCBM.addElement(plugin);
                        DLM.addElement(plugin);
                    }
                }
            }
        }
        MF.guiInfoPanel.jCBBibPlugins.setModel(DCBM);
        MF.dialogExportBibliography.setListModel(DLM);
    }
    
}
