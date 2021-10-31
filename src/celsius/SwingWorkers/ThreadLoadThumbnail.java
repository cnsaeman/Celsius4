/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.SwingWorkers;

import celsius.Resources;
import celsius.gui.ThumbNail;
import celsius.tools.Parser;
import celsius.tools.ToolBox;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;

/**
 *
 * @author cnsaeman
 */
public class ThreadLoadThumbnail extends Thread {

    public int spx;
    public int spy;

    public ThumbNail TN;
    private Resources RSC;

    public ThreadLoadThumbnail(ThumbNail tn, Resources rsc) {
        TN = tn;
        RSC=rsc;
        spx=RSC.guiScale(140);
        spy=RSC.guiScale(160);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        String tn = TN.tableRow.get("thumbnail");
        final String es1 = TN.tableRow.toText(false);
        if (tn != null) {
            try {
                BufferedImage bf = ImageIO.read(new File(TN.tableRow.getCompletedDirKey("thumbnail")));
                int w = bf.getWidth();
                int h = bf.getHeight();
                double rx = (spx - 13.001) / w;
                double ry = (spy - 13.001) / h;
                double r = rx;
                if (ry < rx) {
                    r = ry;
                }
                //final ImageIcon scaled = new ImageIcon(bf.getScaledInstance((int) (r * w), (int) (r * h), Image.SCALE_FAST));
                BufferedImageOp op = new AffineTransformOp(
                  AffineTransform.getScaleInstance(r, r),
                  new RenderingHints(RenderingHints.KEY_INTERPOLATION,
                                     RenderingHints.VALUE_INTERPOLATION_BICUBIC));
                final ImageIcon scaled=new ImageIcon(op.filter(bf, null));
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        TN.jLIcon.setToolTipText(es1);
                        TN.jLIcon.setIcon(scaled);
                        TN.remove(TN.jTFDesc);
                        TN.repaint();
                        TN.revalidate();
                    }
                });
            } catch (Exception ex) {
                final Image scaled = TN.DT.MF.RSC.getImage("notavailable").getScaledInstance((spx - 3) / 2, (spx - 3) / 2, Image.SCALE_FAST);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        TN.jLIcon.setIcon(new ImageIcon(scaled));
                        TN.jTFDesc.setText("<html><center>" + Parser.replace(ToolBox.wrap(es1,25), "\n", "<br>") + "</center></html>"); // size is here still 0
                        TN.jLIcon.setToolTipText(es1);
                        TN.jTFDesc.setToolTipText(es1);
                    }
                });
            }
        } else {
            if (TN.tableRow.get("type") != null && !TN.tableRow.get("type").isBlank()) {
                final Image scaled = TN.DT.MF.RSC.icons.get(TN.tableRow.getIconField("type")).getImage().getScaledInstance((spx - 3) / 2, (spy - 3) / 2, Image.SCALE_FAST);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        TN.jLIcon.setIcon(new ImageIcon(scaled));
                    }
                });
            } else {
                final Image scaled = TN.DT.MF.RSC.getImage("default").getScaledInstance((spx - 3) / 2, (spy - 3) / 2, Image.SCALE_FAST);
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        TN.jLIcon.setIcon(new ImageIcon(scaled));
                    }
                });
            }

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    TN.jTFDesc.setText("<html><center>" + Parser.replace(ToolBox.wrap(es1,25), "\n", "<br>") + "</center></html>"); // size is here still 0
                    TN.jLIcon.setToolTipText(es1);
                    TN.jTFDesc.setToolTipText(es1);
                }
            });
        }
    }
}
