package org.licketysplit.gui;
import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;

import static java.lang.Thread.sleep;


public class Splash1 extends JFrame{

    public static void main(String args[]) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                Splash1 sp1 = new Splash1();
            }
        });
    }


    static JFrame f = new JFrame("frame");

    // create a object
    static ImageIcon splashGif = new ImageIcon("src/main/Resources/splash.gif");
    static JLabel lsGif = new JLabel(splashGif);

            Splash1() {
                JPanel p = new JPanel();
                this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            p.setBorder(BorderFactory.createLineBorder(Color.GRAY,10,true));
            f.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            f.add(p);
            // set the size of frame
            f.setSize(550, 550);
            setLocationRelativeTo(null);
            f.add(lsGif);
            f.setBackground(Color.blue);
            f.setUndecorated(true);
            f.pack();
            f.setVisible(true);

            }
}


