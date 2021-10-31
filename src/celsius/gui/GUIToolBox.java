/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.gui;

import celsius.Resources;
import celsius.tools.FFilter;
import celsius.tools.ToolBox;
import static celsius.tools.ToolBox.optionsOC;
import static celsius.tools.ToolBox.optionsYN;
import static celsius.tools.ToolBox.optionsYNC;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.File;
import java.util.Enumeration;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

/**
 *
 * @author cnsaeman
 */
public class GUIToolBox {
    
    // Final Strings
    public final static Object[] optionsYNC = { "Yes", "No", "Cancel" };
    public final static Object[] optionsOC = { "OK", "Cancel" };
    public final static Object[] optionsYN = { "Yes", "No" };
        
    public static void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        // Traverse tree from root
        expandAll(tree, new TreePath(root), expand);
    }

    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        // Traverse children
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements();) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }

        // Expansion or collapse must be done bottom-up
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }    
    
    /**
     * Center the current JDialog frame over main frame
     */
    public static void centerDialog(JDialog frame,MainFrame MF) {
        if (MF.isShowing()) {
            Point p = MF.getLocationOnScreen();
            Dimension d = MF.getSize();
            frame.setLocation(p.x + (d.width / 2) - (frame.getWidth() / 2), p.y + (d.height / 2) - (frame.getHeight() / 2));
        } else {
            Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
            DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
            frame.setLocation(bounds.x + dm.getWidth() / 2 - (frame.getWidth() / 2), bounds.y + dm.getHeight() / 2 - (frame.getHeight() / 2));
        }
    }

    /**
     * Center the current JWindow frame on screen
     */
    public static void centerFrame(JFrame frame) {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        frame.setLocation(bounds.x+dm.getWidth()/2 - (frame.getWidth()/2),bounds.y+dm.getHeight()/2 - (frame.getHeight()/2));
    }
    
    
}
