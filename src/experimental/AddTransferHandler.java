/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package experimental;

import celsius.gui.CelsiusTable;
import celsius.gui.MainFrame;
import celsius.data.Library;
import celsius.data.Item;
import celsius.gui.GUIToolBox;
import celsius.tools.ExecutionShell;
import celsius.tools.Parser;
import celsius.tools.TextFile;
import celsius.tools.ToolBox;
import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.ProgressMonitor;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.TransferHandler;

/**
 *
 * @author cnsaeman
 */
public class AddTransferHandler extends TransferHandler {

    public static final DataFlavor[] SUPPORTED_DATA_FLAVORS = new DataFlavor[]{
        DataFlavor.stringFlavor
    };

    MainFrame MF;
    Library lib;

    public AddTransferHandler(MainFrame mf) {
        super();
        MF = mf;
        lib=MF.RSC.getCurrentlySelectedLibrary();
    }

    @Override
    public boolean canImport(TransferHandler.TransferSupport support) {
        boolean canImport = false;
        for (DataFlavor flavor : SUPPORTED_DATA_FLAVORS) {
            if (support.isDataFlavorSupported(flavor)) {
                canImport = true;
                break;
            }
        }
        return canImport;
    }

    @Override
    public boolean importData(TransferHandler.TransferSupport support) {
        boolean accept = false;
        if (canImport(support)) {
            try {
                Transferable t = support.getTransferable();
                Component component = support.getComponent();
                if (component instanceof JButton) {
                    String out = (String) t.getTransferData(DataFlavor.stringFlavor);
                    if (out.startsWith("https://arxiv.org/abs/")) {
                        //MF.jPBSearch.setIndeterminate(true);
                        String url="https://arxiv.org/pdf/"+Parser.cutFrom(out, "https://arxiv.org/abs/");
                        ExecutionShell ES = new ExecutionShell("curl -L "+url+" --output out.pdf", 0, false);
                        ES.start();
                        ES.join();
                        if (ES.errorflag) {
//                            RSC.out("ADD>Error Message: " + ES.errorMsg);
                        }
                        if (new File("out.pdf").exists()) {
                            Item doc = createDoc();
                            doc.put("location", (new File("out.pdf")).getAbsolutePath());
                            doc.put("filetype", "pdf");
                            /*ThreadGetDetails TGD=new ThreadGetDetails(doc,MF.RSC,true);
                            TGD.start();
                            TGD.join();
                            addItem(doc);
                            ItemTable CDT=MF.RSC.makeNewTabAvailable(8, "Last added","search");
                            CDT.addItemFast(lib.lastAddedItem);
                            CDT.resizeTable(true);
                            for (ItemTable DT : MF.RSC.ItemTables) {
                                DT.refresh();
                            }*/
                            MF.guiInfoPanel.updateHTMLview();
                            MF.guiInfoPanel.updateGUI();
                            MF.updateStatusBar(true);
                        }
                        //MF.jPBSearch.setIndeterminate(false);
                    }
                    System.out.println(out);
                }
            } catch (Exception exp) {
                exp.printStackTrace();
            }
        }
        return accept;
    }
    
    private Item createDoc() {
        Item doc = new Item(lib);
        for (String tag : lib.itemPropertyKeys) {
            if (!tag.equals("addinfo") && !tag.equals("autoregistered") && !tag.equals("registered") && !tag.equals("id")) {
                doc.put(tag, null);
            }
        }
        if (lib.config.get("standard-item-fields")!=null) 
            for (String tag : lib.configToArray("standard-item-fields"))
                doc.put(tag, null);
        return (doc);
    }
    
    private void addItem(final Item item) {
        /* TODO try {
            if (!item.getS("$$beingadded").equals("")) return;
            item.put("$$beingadded","true");
            final Integer[] res=new Integer[1];
            /* TODO int dbl=lib.Doublette(item);
            if (dbl==10) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        res[0]=MF.RSC.MC.askQuestionABCD("An exact copy of the item\n"+item.toText(lib)+"\nis already existing in the library:\n"+lib.marker.toText()+"\nDelete the file "+item.get("location")+"?","Warning","Yes","No","Always","Abort");
                    }
                });
                if (res[0]==0) {
                    deleteItem(item,false);
                }
                if (res[0]==2) {
                    deleteItem(item,false);
                }
                if (res[0]==3) {
                    item.put("$$beingadded",null);
                }
                return;
            }
            if (dbl==5) {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        res[0]=MF.RSC.MC.askQuestionYN("A file with exactly the same length as the item\n"+item.toText(lib)+"\nis already existing in the library.\nProceed anyway?","Warning");
                    }
                });
                if (res[0]==JOptionPane.NO_OPTION) {
                    item.put("$$beingadded",null);
                    return;
                }
            }
            if (dbl==4) {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                        Object[] options=new Object[6];
                        options[0]="Delete"; options[1]="Replace"; options[2]="New Version"; options[3]="Replace All"; options[4]="Ignore"; options[5]="Cancel";
                        String msg="The item \n"+item.toText(lib)+
                                                   "\nis already existing in the library. You can\n"+
                                                   "- Delete the item in the inclusion folder\n"+
                                                   "- Replace the item in the library by this item\n"+
                                                   "- Replace the item but treat its current file as an additional version\n"+
                                                   "- Replace all items \n"+
                                                   "- Ignore\n"+
                                                   "- Cancel";
                        res[0]=JOptionPane.showOptionDialog(null, msg, "Warning",JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE,null, options, options[4]);
                        }
                    });
                if (res[0]==0) {
                    SwingUtilities.invokeAndWait(new Runnable() {
                        public void run() {
                            deleteItem(item,false);
                        }
                    });
                    return;
                }
                if ((res[0]==1) || (res[0]==3)) {
                    lib.replaceItem(item);
                    return;
                }
                if (res[0]==2) {
                    Item doc2=lib.marker;
                    doc2.shiftReplaceWithFile(MF.RSC,item.get("location"));
                    TextFile.Delete(item.get("plaintxt"));
                    return;
                }
                return;
            }
            int mode = 0;
            int i = lib.addItem(item, "", mode);
        } catch (Exception e) {
            MF.RSC.outEx(e);
        }*/
    }
    
    private void deleteItem(Item doc, boolean confirmed) {
        if (doc.getS("location").length()>0) {
            if (!confirmed) {
                int j = MF.RSC.askQuestionOC("Really delete the file " + doc.get("location") + "?", "Warning");
                if (j != JOptionPane.NO_OPTION) {
                    doc.deleteFilesOfAttachments();
                    MF.RSC.out("ADD>Deleting :: " + doc.get("filename"));
                    MF.RSC.out("ADD>Deleting :: " + doc.get("plaintxt"));
                } else {
                    return;
                }
            } else {
                doc.deleteFilesOfAttachments();
                MF.RSC.out("ADD>Deleting :: " + doc.get("filename"));
                MF.RSC.out("ADD>Deleting :: " + doc.get("plaintxt"));
            }
        }
    }
    
    
}
