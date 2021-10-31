//
// Celsius Library System v2
// (w) by C. Saemann
//
// SpalshScreen.java
//
// This class displays the splash screen of Celsius v2
//
// typesafe
// 
// checked 16.09.2007
//

package celsius;

import celsius.gui.MainFrame;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;

public class SplashScreen extends JFrame implements FocusListener{
   
    private JLabel Status;
    
    private JLabel Title1, Title2;
    private JLabel Title1S, Title2S;

   public SplashScreen(String version, boolean label, Resources RSC) {
        setTitle("Starting Celsius");
        setIconImage(Toolkit.getDefaultToolkit().getImage(CelsiusMain.class.getResource("images/main_icon.png")));
        ImageIcon imageIcon = new ImageIcon(getClass().getResource("images/splash.jpg"));
        Image preimage = imageIcon.getImage(); // transform it 
        Image newimg = preimage.getScaledInstance(RSC.guiScale(preimage.getWidth(null)),RSC.guiScale(preimage.getHeight(null)), java.awt.Image.SCALE_SMOOTH); // scale it the smooth way  
        imageIcon = new ImageIcon(newimg);  // transform it back
        JLabel image = new JLabel(imageIcon);
        Status= new JLabel("Starting Celsius");
        Status.setOpaque(true);
        Status.setForeground(Color.white);
        Status.setBackground(new Color(0x1B468B));
        Status.setFont(new java.awt.Font("SansSerif",1,RSC.guiScale(16)));
        Title1= new JLabel("Celsius Library System");
        Title1.setOpaque(false);
        Title1.setForeground(new Color(10,10,30));
        Title1.setFont(new java.awt.Font("Serif",1,RSC.guiScale(28)));
        Title2= new JLabel(version+" (w) by Christian Saemann");
        Title2.setOpaque(false);
        Title2.setForeground(new Color(10,10,30));
        Title2.setFont(new java.awt.Font("Serif",1,RSC.guiScale(18)));
        Title1S= new JLabel("Celsius Library System");
        Title1S.setOpaque(false);
        Title1S.setForeground(new Color(210,210,230));
        Title1S.setFont(new java.awt.Font("Serif",1,RSC.guiScale(28)));
        Title2S= new JLabel(version+" (w) by Christian Saemann");
        Title2S.setOpaque(false);
        Title2S.setForeground(new Color(210,210,230));
        Title2S.setFont(new java.awt.Font("Serif",1,RSC.guiScale(18)));
        FontMetrics f = Status.getFontMetrics(Status.getFont());
        
        // Add widgets to content pane
        Container content_pane = getContentPane();
        content_pane.setLayout(null);
        if (label) content_pane.add(Status);
        content_pane.add(Title1);
        content_pane.add(Title2);
        content_pane.add(Title1S);
        content_pane.add(Title2S);
        content_pane.add(image);
        
        Dimension ImageSize = image.getPreferredSize();
        setSize(ImageSize);
        image.setBounds( 0, 0, ImageSize.width, ImageSize.height );
        
        Status.setLocation( 0 , RSC.guiScale(5));
        Dimension statusSize = new Dimension( getSize().width, f.getHeight());
        Status.setSize( statusSize );
        Status.setHorizontalAlignment(SwingConstants.CENTER);
        
        Title1.setLocation(0,RSC.guiScale(360));
        Title2.setLocation(0,RSC.guiScale(390));
        Title1.setSize(new Dimension(getSize().width,RSC.guiScale(35)));
        Title1.setHorizontalAlignment(SwingConstants.CENTER);
        Title2.setSize(new Dimension(getSize().width,RSC.guiScale(30)));
        Title2.setHorizontalAlignment(SwingConstants.CENTER);
        Title1S.setLocation(0,RSC.guiScale(360));
        Title2S.setLocation(0,RSC.guiScale(390));
        Title1S.setSize(new Dimension(getSize().width+4,RSC.guiScale(35+4)));
        Title1S.setHorizontalAlignment(SwingConstants.CENTER);
        Title2S.setSize(new Dimension(getSize().width+4,RSC.guiScale(30+4)));
        Title2S.setHorizontalAlignment(SwingConstants.CENTER);
        addFocusListener(this);

        // close splash upon mouseclick
        if (!label) addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                setVisible(false);
                dispose();
            }
        });

        // Center frame
        centerFrame(this);
        this.setUndecorated(true);
        setVisible(true);
        update(getGraphics());
    }    
    
    public void setTimeTrigger() {
        final Runnable closerRunner = new Runnable() {
            public void run() {
                setVisible(false);
                dispose();
            }
        };
        Runnable waitRunner = new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1500); SwingUtilities.invokeAndWait(closerRunner);
                } catch(Exception e) {  e.printStackTrace(); }
            }
        };
        
        Thread splashThread = new Thread(waitRunner, "SplashThread");
        splashThread.setPriority(Thread.MIN_PRIORITY);
        splashThread.start();
    }
    
    public void setStatus(final String s) {
        Status.setText(s);
        Status.update(Status.getGraphics());
    }

    public void focusGained(FocusEvent e) {
        update(getGraphics());
    }
    
    public static void centerFrame(JFrame frame) {
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        DisplayMode dm = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDisplayMode();
        frame.setLocation(bounds.x+dm.getWidth()/2 - (frame.getWidth()/2),bounds.y+dm.getHeight()/2 - (frame.getHeight()/2));
    }    

    public void focusLost(FocusEvent e) {

    }
    
}