package org.licketysplit.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import javax.swing.border.Border;


import static java.util.concurrent.TimeUnit.*;
public class Splash2tmp extends JFrame  {

    public Splash2tmp() throws InterruptedException, IOException, FontFormatException {

        Font customFont1 = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/Resources/GatsbyFLF-Bold.ttf")).deriveFont(36f);
        JProgressBar pb = new JProgressBar();
        //pb.setSize(1200, 1200);
        Border bord1 = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 5, true);
        Border bord2 = BorderFactory.createLineBorder(Color.GRAY, 30);
        Border bord3 = BorderFactory.createLineBorder(Color.DARK_GRAY, 15, true) ;
        final int MAX = 1000;
        setFocusableWindowState(true);
        setState(NORMAL);
        JPanel panel22 = new JPanel();
        JLabel whereAreWe = new JLabel(" GIVE IT A SEC  ");
        whereAreWe.setFont(customFont1); whereAreWe.setForeground(Color.WHITE);
        whereAreWe.setBorder(bord1);
        panel22.setBackground(Color.DARK_GRAY);
        Border compoundBorder1 = BorderFactory.createCompoundBorder(
                bord1, bord2);
        Border compoundBorder2 = BorderFactory.createCompoundBorder(
                bord3, compoundBorder1);
        //panel22.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 15));
        panel22.setBorder(compoundBorder1);
        panel22.setOpaque(true);
        //pb.setBorder(BorderFactory.createLineBorder(Color.GRAY, 40));
        pb.setBorder(compoundBorder2);
        pb.setBackground(Color.DARK_GRAY);
        pb.setFont(customFont1);
        pb.setStringPainted(true);


        panel22.add(pb);
        panel22.add(whereAreWe);
        panel22.setSize(800, 800);
        //setAutoRequestFocus(true);
        add(panel22);
        this.setUndecorated(true);
        //this.setAlwaysOnTop(true);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

        //MICROSECONDS.sleep(80000);
        for (int i = 0; i <= MAX; i++) {
            requestFocus();
            MICROSECONDS.sleep(40000);
            pb.setValue(i);
            if (i % 2 == 0) {
                pb.setBackground(Color.WHITE);
                pb.setBorder(bord3);
            } else {
                pb.setBackground(Color.BLACK);
            }

            // update progressbar
        /*for (int i = 0; i <= MAX; i++) {
            requestFocus();
            final int currentValue = i;
            final int tmp = currentValue;
            MICROSECONDS.sleep(40000);
            pb.setValue(tmp);
            if (tmp % 2 == 0) {
                pb.setBackground(Color.GRAY);
            }else {
                pb.setBackground(Color.DARK_GRAY);
            }
            if (tmp > 30 && tmp < 70) {
                pb.setString("Wrapping up");
            }
            else if (tmp >= 70) {
                pb.setForeground(Color.ORANGE);
                pb.setString("Almost there..");
            }else if (tmp % 5 == 0)
                pb.setBackground(Color.WHITE);
            else
                pb.setString("Loading..");

        }
*/
        }
    }
    //remove progress bar
    public static void main(String arg[])
    {
        try
        {
            Splash2tmp s = new Splash2tmp();

        }
        catch(Exception e)
        {
            //logger1.error("loadbar issue");
            JOptionPane.showMessageDialog(null, e.getMessage());}
    }
}

