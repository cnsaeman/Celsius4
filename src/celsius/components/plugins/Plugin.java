//
// Celsius Library System v2
// (w) by C. Saemann
//
// Plugin.java
//
// This class contains the interface for plugins
//
// not typesafe
// 
// checked 16.09.2007
//

package celsius.components.plugins;

import celsius.data.Item;
import celsius.data.TableRow;
import celsius.tools.ToolBox;
import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author cnsaeman
 * 
 * This is a container class for loading actual plugins and applying these
 */
public class Plugin {
    
    public final String className;
    private final Class c;
    
    public final HashMap<String,String> metaData;
    public final String[] requiredFields;
    
    private final Method Initialize;
    
    // String firstpage = 
    
    /** Creates a new instance of Plugin */
    public Plugin(String f) throws Exception {
        className=f;
        URLClassLoader ucl = new URLClassLoader(new URL[] { (new File("plugins")).toURI().toURL() });
        try {
            c = Class.forName(className,true,ucl);
        } catch (Exception ex) {
            throw(ex);
        }

        metaData=(HashMap<String,String>)c.getDeclaredField("metaData").get(c);
        
        requiredFields=ToolBox.stringToArray(metaData.get("requiredFields"));
        
        System.out.println(f);
        Initialize=(Method)c.getDeclaredMethod("Initialize",TableRow.class,HashMap.class,ArrayList.class);
    }
    
    /**
     * initialize: Information i, Messages m
     */
    public Thread Initialize(TableRow i, HashMap<String,String> communication, ArrayList<String> m) throws Exception {
        Thread thread=(Thread)c.getDeclaredConstructor().newInstance();
        Object[] args=new Object[] { i, communication, m };
        Initialize.invoke(thread,args);
        return(thread);
    }
    
    public boolean needsFirstPage() {
        if (!metaData.containsKey("needsFirstPage")) return (false);
        return(metaData.get("needsFirstPage").equals("yes"));
    }

    public boolean wouldLikeFirstPage() {
        if (!metaData.containsKey("wouldLikeFirstPage")) return (false);
        return(metaData.get("wouldLikeFirstPage").equals("yes"));
    }
    
}
 