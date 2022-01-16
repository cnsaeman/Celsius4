/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package celsius;

import celsius.data.Attachment;
import celsius.data.Item;
import celsius.gui.MainFrame;
import celsius.data.Library;
import celsius.data.TableRow;
import atlantis.tools.FileTools;
import atlantis.tools.JSONParser;
import celsius3.Library3;
import atlantis.tools.Parser;
import atlantis.tools.TextFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author cnsaeman
 */
public class CelsiusMain {

    public static MainFrame MF;
    public static Resources RSC;

    private static SplashScreen StartUp;
    
    public static void doWork() {
        /*String url="jdbc:sqlite:"+mainLibraryFile;
        try {
            Connection dbConnection = DriverManager.getConnection(url);
            boolean locked=false;
            try {
                dbConnection.prepareStatement("BEGIN EXCLUSIVE").execute();
                dbConnection.prepareStatement("COMMIT").execute();
            } catch (Exception e) {
                locked=true;
            }
            System.out.println("Locked status: "+locked);
        } catch (Exception e) {
            e.printStackTrace();
        }

        
        RSC.guiNotify=false;*/

        /*try {
            Library library = new Library("/home/cnsaeman/Celsius4/Libraries/MathsPhys", RSC);
            library.dbConnection.setAutoCommit(false);
            ResultSet rs = library.executeResEX("SELECT * FROM item_reference_links;");
            while (rs.next()) {
                library.executeEX("INSERT INTO item_item_links (item1_id,item2_id,link_type) VALUES ("+rs.getString(1)+","+rs.getString(2)+",0);");
            }
            rs = library.executeResEX("SELECT * FROM item_citation_links;");
            while (rs.next()) {
                library.executeEX("INSERT INTO item_item_links (item1_id,item2_id,link_type) VALUES ("+rs.getString(1)+","+rs.getString(2)+",1);");
            }
            library.dbConnection.commit();
            library.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }*/
        
        /*try {
            Library library=new Library("/home/cnsaeman/Celsius4/Libraries/Sheet Music",RSC);
            library.dbConnection.setAutoCommit(false);
            ResultSet rs=library.executeResEX("SELECT id,path FROM attachments;");
            while (rs.next()) {
                String fn=Parser.replace(rs.getString(2),"LD::","/home/cnsaeman/Celsius4/Libraries/Sheet Music/");
                try {
                    String md5=FileTools.md5checksum(fn);
                    String lastUpdated=String.valueOf((Files.getLastModifiedTime(Paths.get(fn))).toMillis()/1000);
                    System.out.println(md5);
                    System.out.println(lastUpdated);
                    if (md5!=null) {
                        library.executeEX("UPDATE attachments SET md5=?, createdTS=? WHERE id=?;",new String[]{md5,lastUpdated,rs.getString(1)});
                    }
                } catch (Exception ex) {
                    RSC.outEx(ex);
                }
            }
            library.dbConnection.commit();
            library.dbConnection.close();
        } catch (Exception ex) {
            RSC.outEx(ex);
        }*/
        System.exit(0);
    }


    public static void main(String args[]) throws Exception {
        RSC=new Resources();
        double gSF;
        if ((args.length>0) && (args[0].startsWith("scale="))) {
            gSF=Double.valueOf(Parser.cutFrom(args[0],"scale="));
        } else {
            gSF=1;
        }
        gSF=1.3;        
        System.out.println("Celsius "+RSC.VersionNumber);
        RSC.guiScaleFactor=gSF;
        RSC.initResources();
        RSC.logLevel=99;
        
        if (3>4) {
            doWork();
        } else {
            RSC.setLookAndFeel();
            java.awt.EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    StartUp = new SplashScreen(RSC.VersionNumber, true, RSC);
                    StartUp.setStatus("Initializing Resources...");

                    java.awt.EventQueue.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            StartUp.setStatus("Creating Main Frame...");
                        }
                    });
                    MF = new MainFrame();
                    MF.StartUp = StartUp;
                    MF.RSC = RSC;

                    RSC.setMainFrame(MF);
                    StartUp.setStatus("Loading Plugins...");
                    RSC.loadPlugins();
                    StartUp.setStatus("Laying out GUI...");
                    MF.gui1();
                    RSC.guiInformationPanel = MF.guiInfoPanel;
                    StartUp.setStatus("Setting Shortcuts...");
                    RSC.loadShortCuts();
                    MF.setShortCuts();
                    StartUp.setStatus("Loading Libraries...");
                    RSC.loadLibraries();
                    StartUp.setStatus("Final gui...");
                    MF.gui2();
                }
            });
        }
     
    }


}