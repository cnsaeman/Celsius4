/*
 * Class acting as an icon wallet.
 */

package celsius.images;

import celsius.CelsiusMain;
import celsius.gui.MainFrame;
import celsius.tools.Parser;
import celsius.tools.ToolBox;
import java.awt.Toolkit;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;

/**
 *
 * @author cnsaeman
 */
public class Icons extends HashMap<String,ImageIcon> {

    public final ArrayList<String> Types;
    public String basefolder;

    private void readIn(String folder,String pre) {
        String[] flist = (new File(folder)).list();
        String n;
        for (int i = 0; (i < flist.length); i++) {
            if (flist[i].endsWith(".png")) {
                try {
                    ImageIcon I = new ImageIcon(Toolkit.getDefaultToolkit().getImage(folder+ToolBox.filesep+flist[i]));
                    n=Parser.cutUntilLast(flist[i], ".png");
                    I.setDescription(n);
                    put(pre+n, I);
                    Types.add(pre+n);
                } catch (Exception e) {
                    e.printStackTrace();
                    System.out.println(flist[i]);
                }
            }
            if (new File(folder+ToolBox.filesep+flist[i]).isDirectory()) {
                readIn(folder+ToolBox.filesep+flist[i],flist[i]+ToolBox.filesep);
            }
        }        
    }
    
    private InputStream getResourceAsStream(String resource) {
        final InputStream in
                = getContextClassLoader().getResourceAsStream(resource);

        return in == null ? getClass().getResourceAsStream(resource) : in;
    }  
    
    private ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }    

    public Icons(String bf) {
        super();
        basefolder=bf;
        if (basefolder.endsWith(ToolBox.filesep)) basefolder.substring(0,basefolder.length()-1);
        try {
            loadIcon("iconmonstr-plus-6.svg.16");
            loadIcon("iconmonstr-plus-6.svg.16.2x");
            loadIcon("add.2x");
            loadIcon("Add Icon.2x");
            loadIcon("Add Icon");
            loadIcon("add");
            loadIcon("application_view_tile.2x");
            loadIcon("application_view_tile");
            loadIcon("arrow_out.2x");
            loadIcon("arrow_out");
            loadIcon("arrow_right.2x");
            loadIcon("arrow_right");
            loadIcon("bullet_cross.2x");
            loadIcon("bullet_cross");
            loadIcon("closebtn2.2x");
            loadIcon("closebtn2");
            loadIcon("closebtn.2x");
            loadIcon("closebtn");
            loadIcon("closebtn_sm.2x");
            loadIcon("closebtn_sm");
            loadIcon("cross.2x");
            loadIcon("cross");
            loadIcon("default.2x");
            loadIcon("default");
            loadIcon("edit_add.2x");
            loadIcon("edit_add");
            loadIcon("find.2x");
            loadIcon("find");
            loadIcon("folder.2x");
            loadIcon("folder_explore.2x");
            loadIcon("folder_explore");
            loadIcon("folder_link.2x");
            loadIcon("folder_link");
            loadIcon("folder");
            loadIcon("folder_star.2x");
            loadIcon("folder_star");
            loadIcon("folder_table.2x");
            loadIcon("folder_table");
            loadIcon("forward_gray.2x");
            loadIcon("forward_gray");
            loadIcon("forward_green.2x");
            loadIcon("forward_green");
            loadIcon("iconmonstr-book-4.svg.24.2x");
            loadIcon("iconmonstr-book-4.svg.24");
            loadIcon("iconmonstr-calendar-5.svg.24.2x");
            loadIcon("iconmonstr-calendar-5.svg.24");
            loadIcon("iconmonstr-crop-12.svg.24.2x");
            loadIcon("iconmonstr-crop-12.svg.24");
            loadIcon("iconmonstr-edit-9.svg.24.2x");
            loadIcon("iconmonstr-edit-9.svg.24");
            loadIcon("iconmonstr-file-38.svg.24.2x");
            loadIcon("iconmonstr-file-38.svg.24");
            loadIcon("iconmonstr-folder-30.svg.24.2x");
            loadIcon("iconmonstr-folder-30.svg.24");
            loadIcon("iconmonstr-info-6.svg.24.2x");
            loadIcon("iconmonstr-info-6.svg.24");
            loadIcon("iconmonstr-key-2.svg.24.2x");
            loadIcon("iconmonstr-key-2.svg.24");
            loadIcon("iconmonstr-link-1.svg.24.2x");
            loadIcon("iconmonstr-link-1.svg.24");
            loadIcon("iconmonstr-pen-7.svg.24.2x");
            loadIcon("iconmonstr-pen-7.svg.24");
            loadIcon("iconmonstr-picture-1.svg.24.2x");
            loadIcon("iconmonstr-picture-1.svg.24");
            loadIcon("iconmonstr-save-14.svg.24.2x");
            loadIcon("iconmonstr-save-14.svg.24");
            loadIcon("iconmonstr-user-22-24");
            loadIcon("iconmonstr-user-22.svg.24.2x");
            loadIcon("iconmonstr-user-22.svg.24");
            loadIcon("iconmonstr-wrench-6.svg.24.2x");
            loadIcon("iconmonstr-wrench-6.svg.24");
            loadIcon("iconmonstr-wrench-10.svg.16");
            loadIcon("iconmonstr-wrench-10.svg.16.2x");
            loadIcon("main_icon.2x");
            loadIcon("main_icon");
            loadIcon("notavailable.2x");
            loadIcon("notavailable");
            loadIcon("play_gray.2x");
            loadIcon("play_gray");
            loadIcon("play_green.2x");
            loadIcon("play_green");
            loadIcon("plugin.2x");
            loadIcon("plugin");
            loadIcon("rewind_gray.2x");
            loadIcon("rewind_gray");
            loadIcon("rewind_green.2x");
            loadIcon("rewind_green");
            loadIcon("search2.2x");
            loadIcon("search2");
            loadIcon("search.2x");
            loadIcon("search");
            loadIcon("Settings_32.2x");
            loadIcon("Settings_32");
            loadIcon("star.2x");
            loadIcon("star");
            loadIcon("stop_gray.2x");
            loadIcon("stop_gray");
            loadIcon("stop_red.2x");
            loadIcon("stop_red");
            loadIcon("iconmonstr-file-14.svg.16");
            loadIcon("iconmonstr-file-14.svg.16.2x");
            loadIcon("iconmonstr-note-29.svg.16");
            loadIcon("iconmonstr-note-29.svg.16.2x");
            loadIcon("iconmonstr-download-20.svg.16");
            loadIcon("iconmonstr-download-20.svg.16.2x");
            loadIcon("iconmonstr-square-4.svg.16");
            loadIcon("iconmonstr-square-4.svg.16.2x");
            loadIcon("iconmonstr-checkbox-4.svg.16");
            loadIcon("iconmonstr-checkbox-4.svg.16.2x");
            loadIcon("iconmonstr-checkbox-28.svg.16");
            loadIcon("iconmonstr-checkbox-28.svg.16.2x");
            loadIcon("iconmonstr-shape-20.svg.16");
            loadIcon("iconmonstr-shape-20.svg.16.2x");
            loadIcon("iconmonstr-star-7.svg.16");
            loadIcon("iconmonstr-star-7.svg.16.2x");
            
        } catch(Exception e) {
            e.printStackTrace();
        }
        Types=new ArrayList<String>();
        readIn(basefolder,"");
    }

    private void loadIcon(String s) {
        try {
            ImageIcon I = new ImageIcon(Toolkit.getDefaultToolkit().getImage(CelsiusMain.class.getResource("images/" + s+".png")));
            I.setDescription(s);
            put(s, I);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ImageIcon getIcon(String s) {
        //if (s==null) return(get("default"));
        if (s==null) return(null);
        if (s.equals("")) return(null);
        if (s.length()==0) return(get("default"));
        if (!containsKey(s)) return(get("notavailable"));
        return(get(s));
    }

    public DefaultComboBoxModel getDCBM() {
        DefaultComboBoxModel DCBM = new DefaultComboBoxModel();
        for (String ft : Types) {
            DCBM.addElement(ft);
        }
        return(DCBM);
    }
   
}
