/*
package org.licketysplit.gui;

// SHOWS SPLASH SCREEN
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;

class Splash extends JFrame{

    // frame
    static JFrame f;
    //GIF icon added to Jlabel
    private ImageIcon splashGif = new ImageIcon("src/main/Resources/splash.gif");
    public static JLabel lsGif;

    // main class
    public static void main(String[] args) {
        // create a new frame

        Splash s = new Splash() {
            JFrame f = new JFrame("frame");
            // create a object
            ImageIcon splashGif = new ImageIcon("src/main/Resources/splash.gif");
            JLabel lsGif = new JLabel(splashGif);

            //ImageIcon splashGif = new ImageIcon("src/main/Resources/splash.gif");

        };
        // create a panel
        JPanel p = new JPanel();
        //JButton b = new JButton("click");
        //p.setBorder(BorderFactory.createLineBorder(Color.GREEN, 50));
        //p.setBackground((Color.GREEN));
        f.add(p);
        // set the size of frame
        f.setSize(550, 550);
        f.setBackground((Color.BLUE));
        f.setLocationRelativeTo(null);
        f.add(lsGif);
        f.setUndecorated(true);
        //f.pack();
        f.setVisible(true);
        f.setBackground(Color.blue);

        int i = 0;

for (i = 0; i < 1000; i++) {

            if (i % 2 == 0) {
                f.setBackground(Color.GRAY);
                //lsGif.setVisible(false);
                lsGif.setBackground(Color.DARK_GRAY);
            } else {
                p.setBorder(BorderFactory.createLineBorder(Color.BLACK, 10));
                //lsGif.setVisible(true);
            }
        }

        f.setOpacity(.73f);
        f.setVisible(true);
        try {
                Thread.sleep((long) (5000));
            } catch (InterruptedException e) {}

        f.setVisible(false);
        try {
            Thread.sleep((long) (5000));
        } catch (InterruptedException e) {}
        f.dispose();

    }
}

*/
