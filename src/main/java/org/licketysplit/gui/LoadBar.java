    package org.licketysplit.gui;

import javax.swing.*;
import java.awt.*;

public class LoadBar extends JFrame {

    final int MAX = 100;
    JProgressBar pb = new JProgressBar();
    //JFrame frame22 = new JFrame();
    JPanel panel22 = new JPanel();
    public LoadBar() {
        //frame22.setAlwaysOnTop(true);
        JFrame frame22 = this;
        //frame22.getContentPane();
        panel22.setBackground(Color.DARK_GRAY);
        pb.setBorder(BorderFactory.createLineBorder(Color.lightGray, 10));
        pb.setBackground(Color.DARK_GRAY);
        pb.setStringPainted(true);
        panel22.add(pb);
        panel22.setSize(600,300);
        //frame22.setContentPane(panel22);
        frame22.setLocationRelativeTo(null);
        frame22.setUndecorated(true);
        frame22.setBackground(Color.RED);
        frame22.add(panel22);
        frame22.pack();
        frame22.setVisible(true);

//        setUndecorated(true);
//        add(panel22);
//        setContentPane(panel22);
//        pack();
//        setLocationRelativeTo(null);
//        setVisible(true);
//        toFront();

        // update progressbar
        for (int i = 0; i <= MAX; i++) {
            final int currentValue = i;
            try {
                SwingUtilities.invokeLater(new Runnable() {

                    public void run() {
                        final int tmp = currentValue;
                        pb.setValue(tmp);
                        if (tmp % 2 == 0) {
                            pb.setBackground(Color.GRAY);
                        }else
                            pb.setBackground(Color.DARK_GRAY);
                        if (tmp > 30 && tmp < 70)
                            pb.setString("Wrapping up");
                        else if (tmp >= 70) {
                            pb.setForeground(Color.ORANGE);
                            pb.setString("Almost there..");
                        }else if (tmp % 5 == 0)
                            pb.setBackground(Color.WHITE);
                        else
                            pb.setString("Loading..");


                    }

                });
                java.lang.Thread.sleep(100);
            } catch (InterruptedException e) {
                JOptionPane.showMessageDialog(this, e.getMessage());
            }
            this.getContentPane().remove(pb);

        }
        //frame22.dispose();
    }
        //remove progress bar
        public static void main(String arg[])
        {
            try
            {

                LoadBar lb = new LoadBar();


            }
            catch(Exception e)
            {
                //logger1.error("loadbar issue");
                JOptionPane.showMessageDialog(null, e.getMessage());}
        }
    }

