package org.licketysplit.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import static java.lang.Thread.sleep;

public class LSDemo {
    public static String getUserProfile() {
        return userProfile;
    }
    public static void setUserProfile(String user) {
        userProfile = user;
    }

    static String userProfile;
    static JFrame frame = new JFrame("    LicKet-Y-SpliT    ");
    static ImageIcon splashGif = new ImageIcon("src/main/Resources/splash.gif");
    static JLabel lsGif = new JLabel(splashGif);
    private static boolean Lidone;
    private static boolean ONLINE;
    public static Splash1 sp1;
    final static String[] actionHistory = {"HISTORY"};

    public static String[] getActionHistory() {
        return actionHistory;
    }
    public static void setActionHistory(String action) {

        String temp = action + "/n" + (getActionHistory());
        actionHistory[0] = temp;

    }

    public static Boolean isOnline() {
        return ONLINE;
    }
    public static void setOnline(boolean online) {
        ONLINE = online;
    }
    public void setLidone(boolean lidone) {
        Lidone = lidone;
    }

    public static void createAndShowGUI(String[] args) throws Exception {


        //TODO need to create folder for appdata if applicationData Folder does not exist
        File applicationData;
        File dir;
        if (args.length > 0)
            dir = new File(args[0]);
        else
            dir = new File((System.getProperty("user.home")) + File.separator + "applicationData");

        applicationData = dir;
        System.out.println("DIR= " + dir);
        //System.out.println("ApplicationData: " + applicationData);
        //JMenuBar creation
        JMenuBar menuBar = MenuUtil.makeMenuBar1();
        frame.setJMenuBar(menuBar);

        //Table Model 1 ._> show directory contents
        TableModel tableModel = new TableModel(dir, applicationData);
        JScrollPane tableScrollPane = TableUtil.makeTable(dir, applicationData);
        tableScrollPane.setPreferredSize(new Dimension(250, 200));

        /*//  display the given directory
        JButton displayDirButton = new JButton("Load Directory");
        displayDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                String dirPath = dirPathTextField.getText();
                if (dirPath != null && !"".equals(dirPath)) {
                    File newDir = new File(dirPath);
                    TableModel newTableModel = new TableModel(newDir);
                    table.setModel(newTableModel);
                } else {
                    JOptionPane.showMessageDialog(null,
                            "PATH EMPTY", "Error",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        });*/

        ////ctrlPANEL
        /*JPanel ctrlPane = new JPanel();
        ctrlPane.add(dirPathTextField);
        ctrlPane.add(displayDirButton);
        ctrlPane.setBackground(Color.darkGray);
        ctrlPane.setBackground(Color.LIGHT_GRAY);
        ctrlPane.setBorder(BorderFactory.createLineBorder(Color.white,2,true);


        *///infoPANEL
        JPanel ctrlPane = TableUtil.makeCPanel();
        JPanel infoPane = new JPanel();
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                ctrlPane, tableScrollPane);
        splitPane.setDividerLocation(35);
        splitPane.setEnabled(false);
        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                splitPane, infoPane);
        splitPane.setDividerLocation(35);
        splitPane.setEnabled(false);

        Image BlackFolder2 = new ImageIcon("src/main/Resources/BlackFolder2@2x.png").getImage();
        frame.setIconImage(BlackFolder2);
        frame.setTitle(MenuUtil.TitleBarDisplay());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(splitPane2);
        frame.pack();
        frame.setSize(1200,800);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public static void setLookAndFeel(String lf) throws Exception {
        try {
            for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if (lf.equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
    }
    public static void setPhase(boolean bool) throws Exception {
        try {
            Lidone = bool;
        } catch (Exception e) {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        }
    }





    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        ///LOG IN / REGISTER FRAME
        try {
            //logInFrame lframe = new logInFrame();
        } catch (Exception e) { }
        Lidone = false; //login not complete
        System.out.println(("LogIn or Register"));
        while (Lidone) { //while not logged in wait for logged in status
            //System.out.println("waiting for Lidone");
        }
        if (false) { //show GIF for a quick second
            System.out.println("Logged in");
            //frame.setUndecorated(true);
            frame.setSize(500,500);
            frame.add(lsGif);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            //wait 1 secs to show GIF
            sleep(5000);
            //wait(1000);
            System.out.println("GIF DONE");
            frame.remove(lsGif);
            frame.setVisible(false);
            

        }
        Boolean go = frame.isVisible();
        while(go){
            System.out.println("yesboy");
        }
        try {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    try {
                        setLookAndFeel(Props.LF);
                        createAndShowGUI(args);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }catch(Exception e){}
    }//end main

}

