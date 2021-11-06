//
// Celsius Library System
// (w) by C. Saemann
//
// ThreadCreateBibTeX.java
//
// This class contains the thread for creating a bibtex file
//
// typesafe
//
// checked 11/2009
//

package celsius.SwingWorkers;

import celsius.tools.Plugin;
import celsius.data.Library;
import celsius.Resources;
import celsius.data.Item;
import celsius.data.TableRow;
import celsius.gui.MultiLineMessage;
import celsius.tools.ToolBox;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

public class SWApplyPlugin extends SwingWorker<Void,Void> {
    
    private final Library library;           // Library
    private final Resources RSC;
    private final ProgressMonitor progressMonitor;    // Progress label

    private final ArrayList<TableRow> tableRows;
    private final TableRow tableRow;
    public SWFinalizer swFinalizer;
    
    private final String TI;
    private final String parameters;

    private final Plugin plugin;
    private int loadLevel;
    
    private final HashMap<String, String> communication;

    private int done;
    
    
    // Constructor
    public SWApplyPlugin(Library lib, Resources rsc,ProgressMonitor p, Plugin pl, String para, ArrayList<TableRow> trs) {
        TI="ApplyPlugin>";
        library=lib; progressMonitor=p; RSC=rsc;
        parameters=para;
        tableRows=trs; plugin=pl;
        tableRow=null;
        determineLoadLevel();
        communication=new HashMap<>();
    }

    public SWApplyPlugin(Library lib, Resources rsc,ProgressMonitor p, Plugin pl, String para, TableRow tr) {
        TI="ApplyPlugin>";
        library=lib; progressMonitor=p; RSC=rsc;
        parameters=para;
        tableRows=null; plugin=pl;
        tableRow=tr;
        determineLoadLevel();
        communication=new HashMap<>();
    }
    
    public void determineLoadLevel() {
        ArrayList<String> alwaysLoadedKeys = library.configToArrayList("item-table-column-fields");
        // no additional fields required
        loadLevel=1;
        for (String key : plugin.requiredFields) {
            if (!alwaysLoadedKeys.contains(key)) {
                // all fields from item table required
                loadLevel=2;
                if (!library.itemPropertyKeys.contains(key)) {
                    loadLevel=3;
                }
            }
        }
    }

    
    @Override
    protected Void doInBackground() {
        StringBuffer out = new StringBuffer(10000);
        try {
            if (tableRow!=null) {
                RSC.out(TI + "Applying Plugin to single item :: " + ToolBox.getCurrentDate()+"::"+tableRow.toText(true));
                applyToTableRow(tableRow, out);
            } else {
                RSC.out(TI + "Applying Plugins started :: " + ToolBox.getCurrentDate());

                done = 0;
                if (tableRows == null) {
                    String tags="*";
                    if (loadLevel==1) tags=library.itemTableSQLTags;
                    ResultSet rs=library.executeResEX("SELECT "+tags+" FROM items;");
                    while (rs.next()) {
                        Item item=new Item(library,rs);
                        item.currentLoadLevel=2;
                        done++;
                        applyToTableRow(item, out);
                        if (progressMonitor != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressMonitor.setProgress(done);
                                }
                            });
                        }
                        if ((progressMonitor != null) && (progressMonitor.isCanceled())) {
                            break;
                        }
                    }
                } else {
                    for (TableRow tableRow : tableRows) {
                        done++;
                        applyToTableRow(tableRow, out);
                        // TODO: move to setProgress an PM.
                        if (progressMonitor != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                public void run() {
                                    progressMonitor.setProgress(done);
                                }
                            });
                        }
                    }
                }
                String mD = plugin.metaData.get("finalize");
                if ((mD != null) && mD.equals("yes")) {
                    try {
                        finalizePlugin(out);
                    } catch (Exception ex) {
                        RSC.out("Error while finalizing plugin: " + ex.toString());
                        RSC.outEx(ex);
                        RSC.showWarning("Error while finalizing plugin:\n" + ex.toString(), "Exception:");
                    }
                }
                if (swFinalizer != null) {
                    swFinalizer.finalize(communication, out);
                }
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        RSC.MF.setThreadMsg("Ready.");
                    }
                });
                RSC.out(TI + "Plugin done.");
                if (progressMonitor != null) {
                    progressMonitor.close();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
       }
        RSC.out(TI + "finished.");
        return null;
    }

    private void applyToTableRow(TableRow tableRow, StringBuffer out) {
        try 
        {
            /*if (plugin.metaData.containsKey("questions")) {
                String[] questions=plugin.metaData.get("questions").split("\\|");
                for (int i=0;i<questions.length;i++) {
                    String q=questions[i];
                    String type=Parser.cutUntil(q, ":");
                    String target=Parser.cutFromLast(q,":");
                    String question=Parser.cutUntil(Parser.cutFrom(q,":"),":");
                    if (!Information.containsKey(target)) {
                        if (type.equals("line")) {
                            SingleLineEditor SLE=new SingleLineEditor(RSC, question, "", true);
                            SLE.setVisible(true);
                            if (!SLE.cancel) {
                                Information.put(target, SLE.text);
                            } else {
                                Information.put(target,null);
                            }
                        }
                        if (type.equals("multiline")) {
                            MultiLineEditor MLE=new MultiLineEditor(RSC, question,"");
                            MLE.setVisible(true);
                            if (!MLE.cancel) {
                                Information.put(target, MLE.text);
                            } else {
                                Information.put(target,null);
                            }
                        }
                        if (type.equals("file")) {


                        }
                    }
                }
            }*/
            ArrayList<String> msg = new ArrayList<String>();
            try {
                RSC.out("Actually running plugin: "+plugin.className);
                
                // load all required data
                if (loadLevel>1) {
                    if (loadLevel>2) {
                        tableRow.loadLevel(3);
                    } else {
                        tableRow.loadLevel(2);
                    }
                }
                
                // start plugin, if all ok
                Thread tst = plugin.Initialize(tableRow,communication, msg);
                tst.start();
                tst.join();
                String s1=communication.get("output");
                if ((s1!=null) && (s1.length()>0)) {
                    out.append(s1);
                }
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            for(String m : msg)
                RSC.out(m);
        } catch (Exception ex) { 
            RSC.out(TI+"Error writing file:"); 
            RSC.outEx(ex);
        }
        if ((tableRow.id!=null) && tableRow.needsSaving()) {
            try {
                tableRow.save();
            } catch (Exception ex) {
                RSC.outEx(ex);
            }
            tableRow.notifyChanged();
        }
    }

    private void finalizePlugin(StringBuffer out) {
        try // Reading addinfo of all entries
        {
            communication.put("$$finalize","yes");
            communication.put("$$saveInformation","yes");
            /*if (plugin.metaData.containsKey("questions")) {
                String[] questions=plugin.metaData.get("questions").split("\\|");
                for (int i=0;i<questions.length;i++) {
                    String q=questions[i];
                    String type=Parser.cutUntil(q, ":");
                    String ltarget=Parser.cutFromLast(q,":");
                    String question=Parser.cutUntil(Parser.cutFrom(q,":"),":");
                    if (!Information.containsKey(ltarget)) {
                        if (type.equals("line")) {
                            SingleLineEditor SLE=new SingleLineEditor(RSC, question, "", true);
                            SLE.setVisible(true);
                            if (!SLE.cancel) {
                                Information.put(ltarget, SLE.text);
                            } else {
                                Information.put(ltarget,null);
                            }
                        }
                        if (type.equals("multiline")) {
                            MultiLineEditor MLE=new MultiLineEditor(RSC, question,"");
                            MLE.setVisible(true);
                            if (!MLE.cancel) {
                                Information.put(ltarget, MLE.text);
                            } else {
                                Information.put(ltarget,null);
                            }
                        }
                    }
                }
            }*/
            ArrayList<String> msg = new ArrayList<String>();
            try {
                Thread tst = plugin.Initialize(null, communication, msg);
                tst.start();
                tst.join();
                String s1=communication.get("output");
                if ((s1!=null) && (s1.length()>0)) {
                    out.append(s1);
                }
                if (communication.containsKey("showOutput")) {
                    SwingUtilities.invokeLater(new Runnable() {
                        public void run() {
                            MultiLineMessage MLM=new MultiLineMessage(RSC, "Plugin reports",communication.get("showOutput"));
                            MLM.setVisible(true);
                        }
                    });
                }
            } catch (Exception ex) {
                RSC.out("jIP>Error while finalizing BibPlugin: " + plugin.metaData.get("title") + ". Exception: " + ex.toString());
                RSC.showWarning("Error while finalizing Bibplugins\nMessage:\n" + ex.toString(), "Exception:");
                RSC.outEx(ex);
            }
        } catch (Exception ecx) { RSC.out(TI+"Error writing file: "+ecx.toString()); }
    }

    
}