/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package celsius.components.tableTabs;

import celsius.Resources;
import celsius.components.tableTabs.Thumbnail;
import atlantis.tools.Parser;
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

    public final Thumbnail thumbnail;
    private final Resources RSC;

    public ThreadLoadThumbnail(Thumbnail thumbnail, Resources RSC) {
        this.thumbnail = thumbnail;
        this.RSC=RSC;
        spx=RSC.guiScale(140);
        spy=RSC.guiScale(160);
        setPriority(Thread.MIN_PRIORITY);
    }

    @Override
    public void run() {
        final String es1 = thumbnail.tableRow.toText(false);
        if (thumbnail.tableRow.hasThumbnail()) {
            try {
                BufferedImage bf = ImageIO.read(new File(thumbnail.tableRow.getThumbnailPath()));
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
                SwingUtilities.invokeLater(() -> {
                    thumbnail.jLIcon.setToolTipText(es1);
                    thumbnail.jLIcon.setIcon(scaled);
                    thumbnail.remove(thumbnail.jTFDesc);
                    thumbnail.repaint();
                    thumbnail.revalidate();
                });
            } catch (Exception ex) {
                final Image scaledImage=RSC.getImage("notavailable").getScaledInstance((spx - 3) / 2, (spx - 3) / 2, Image.SCALE_FAST);
                SwingUtilities.invokeLater(() -> {
                    thumbnail.jLIcon.setIcon(new ImageIcon(scaledImage));
                    thumbnail.jTFDesc.setText("<html><center>" + Parser.replace(ToolBox.wrap(es1,25), "\n", "<br>") + "</center></html>"); // size is here still 0
                    thumbnail.jLIcon.setToolTipText(es1);
                    thumbnail.jTFDesc.setToolTipText(es1);
                });
            }
        } else {
            Image image;
            if (thumbnail.tableRow.get("type") != null && !thumbnail.tableRow.get("type").isBlank()) {
                image=RSC.icons.get(thumbnail.tableRow.getIconField("type")).getImage();
            } else {
                image=RSC.getImage("default");
            }
            final Image scaledImage=image.getScaledInstance((spx - 3) / 2, (spy - 3) / 2, Image.SCALE_FAST);
                SwingUtilities.invokeLater(() -> {
                    thumbnail.jLIcon.setIcon(new ImageIcon(scaledImage));
            });

            SwingUtilities.invokeLater(() -> {
                thumbnail.jTFDesc.setText("<html><center>" + Parser.replace(ToolBox.wrap(es1,25), "\n", "<br>") + "</center></html>"); // size is here still 0
                thumbnail.jLIcon.setToolTipText(es1);
                thumbnail.jTFDesc.setToolTipText(es1);
            });
        }
    }
}
