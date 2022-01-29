/*
 Atlantis Software tools package
*/

package atlantis.tools;

/**
 *
 * @author cnsaeman
 */

import java.awt.*;
import javax.swing.*;

/**
 */
public class FillPainter implements Painter<JComponent> {

    private final Color color;

    public FillPainter(Color c) {
        color = c;
    }

    @Override
    public void paint(Graphics2D g, JComponent object, int width, int height) {
        g.setColor(color);
        g.fillRect(0, 0, width - 1, height - 1);
    }
}