package org.licketysplit.gui;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.SocketException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.swing.*;
import javax.swing.border.Border;


public class LoadingFrame {

    public static void main(String args[]) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                go();
            }
        });
    }
    public static void quit() {
        //LSDemo lsdemo = new LSDemo();
        //System.exit(0);

    }
    public static void go() {
        Font customFont1 = null;
        try {
            customFont1 = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/Resources/GatsbyFLF-Bold.ttf")).deriveFont(36f);
        } catch (FontFormatException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        //ImageIcon blackF = new ImageIcon("src/main/Resources/BlackFolder48.png");
        ImageIcon blackF = new ImageIcon("src/main/Resources/folder64.png");
        Border bord1 = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 5, true);
        Border bord2 = BorderFactory.createLineBorder(Color.GRAY, 30);
        Border bord3 = BorderFactory.createLineBorder(Color.DARK_GRAY, 15, true) ;
        JFrame frame = new JFrame();
        JPanel panel22 = new JPanel();
        JProgressBar pb = new JProgressBar();
        JLabel whereAreWe = new JLabel("Launch",blackF, SwingConstants.CENTER);
        whereAreWe.setFont(customFont1); whereAreWe.setForeground(Color.WHITE);
        panel22.setBackground(Color.DARK_GRAY);
        Border compoundBorder1 = BorderFactory.createCompoundBorder(
                bord1, bord2);
        Border compoundBorder2 = BorderFactory.createCompoundBorder(
                bord3, compoundBorder1);
        panel22.setBorder(compoundBorder1);
        pb.setIndeterminate(false);
        int max = 100;
        pb.setMaximum(max);
        pb.setBorder(compoundBorder2);
        pb.setBackground(Color.DARK_GRAY);
        pb.setFont(customFont1);
        panel22.setLayout(new BorderLayout());
        panel22.add(whereAreWe, BorderLayout.EAST);
        panel22.add(pb, BorderLayout.PAGE_END);
        frame.add(panel22);
        frame.setUndecorated(true);
        frame.pack();
        frame.setSize(600,250);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        new Task_IntegerUpdate(pb, max, whereAreWe).execute();

        //frame.dispose();

    }
    public static class myPair {
        private final int perc;
        private final String text;
        myPair(int perc, String text) {
            this.perc = perc;
            this.text = text;
        }
    }

    static class Task_IntegerUpdate extends SwingWorker<Void, myPair> {

        JProgressBar pb;
        int max;
        JLabel label;
        public Task_IntegerUpdate(JProgressBar pb, int max, JLabel label) {
            this.pb = pb;
            this.max = max;
            this.label = label;
        }

        @Override
        protected void process(List<myPair> chunks) {

            myPair pair = chunks.get(chunks.size()-1); //grab last value in array
            pb.setValue(pair.perc);
            if(pair.text == "stop");{
            //System.out.println("stop recieved");
            }
            if (pair.perc % 2 == 0) {
                pb.setBackground(Color.WHITE);
            } else {
                pb.setBackground(Color.BLACK);
            }
            System.out.println(pair.text);
            label.setIconTextGap((int)((pair.perc)*(2.4)));
            pb.setString(pair.text);
            label.setText("   Loading " + pair.perc + " of " + max + "  ");
        }

        @Override
        protected Void doInBackground() throws Exception {
            for(int i = 0; i < max; i++) {
                Thread.sleep(75);// change to increase or decrease
                publish(new myPair(i,"ok"));
                //if(i>30){ publish(new myPair(i,"looking for peers"));}
            }
            return null;
        }

        @Override
        protected void done() {
            try {
                String ip = lsNetwork.getIP4(0);
                label.setText("<html><font color=black size=12><b>DONE    </b></html>");
                get();
                ImageIcon blackFoldernow = new ImageIcon("src/main/Resources/BlackFolder2@2x.png");
                pb.getTopLevelAncestor().setVisible(false);
                //JOptionPane.showMessageDialog(pb.getParent(), "Success", "Success", JOptionPane.INFORMATION_MESSAGE);
                //JOptionPane.showMessageDialog(pb.getParent(),"","",JOptionPane.PLAIN_MESSAGE,blackFoldernow);
                Object[] possibleValues = { "IP1: "+ lsNetwork.getIP4(0), "IP2 " +lsNetwork.getIP4(0),
                         "IP3  " + lsNetwork.getIP4(0) };
                Object selectedValue = JOptionPane.showInputDialog(null,
                        "Choose IP Address to Bind", "Input",
                        JOptionPane.INFORMATION_MESSAGE, blackFoldernow,
                        possibleValues, possibleValues[0]);

                ///set ip to BIND based on user selectedValue Object
                //TODO set ip to BIND
                LSDemo.setPhase(true);
                //stop signal
                pb.getTopLevelAncestor().removeAll();


                quit();
                publish(new myPair(101, "stop"));
            } catch (ExecutionException | InterruptedException | SocketException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
