package org.licketysplit.gui;

import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.File;
import java.util.Date;

import static java.lang.Thread.sleep;

public class MenuUtil {

    //this Menu called File has items: 1-? 2-goOnline/Offline 3-QUIT
    Image test;
    public static JMenu makeMenu1() {
        //initialize FileMenuButton
        ImageIcon userIcon = new javax.swing.ImageIcon("src/main/Resources/userProfile32.png");
        JMenu fileMenu = new JMenu();
        fileMenu.setIcon(userIcon);
        JPopupMenu settingsForm = new JPopupMenu();
        fileMenu.setMnemonic(KeyEvent.VK_W);
        JMenuItem item = null;
        //close menu item
        item = new JMenuItem("Settings");
        fileMenu.add(item);

        //go on or off line
        JMenuItem goOnlineOffline = new JMenuItem("Go" + (LSDemo.isOnline() ? " online" : " offline"));
        goOnlineOffline.setMnemonic(KeyEvent.VK_N);
        goOnlineOffline.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Connecting/Disconnecting");
                LSDemo.setOnline(!LSDemo.isOnline());
                goOnlineOffline.setText("Go" + (LSDemo.isOnline() ? " online" : " offline"));
            }
        });
        fileMenu.add(goOnlineOffline);
        //quit
        item = new JMenuItem("Quit");
        item.setMnemonic(KeyEvent.VK_Q);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Quit request");
                System.exit(0);
            }
        });
        fileMenu.add(item);
        return fileMenu;
    }

    //Menu 2 is Upload Button
    public static JMenu makeMenu2() {
        JMenu UploadButton = new JMenu();
        ImageIcon UploadIcon = new javax.swing.ImageIcon("src/main/Resources/uploadButton32.png");
        UploadButton.setIcon(UploadIcon);
        UploadButton.setToolTipText("Upload");
        UploadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Upload Pressed");
                //LSDemo.setActionHistory(" Uploaded" + " THIS FILE " + new Date().toString() );
                //String currentAction = LSDemo.actionHistory.getText();
                //goOnlineOffline.setText(LSDemo.isOnline() ? " online" : " offline");
            }
        });
        return UploadButton;
    }

    //Home Button
    public static JMenu makeMenu3() {
        JMenu HomeButton = new JMenu();
        ImageIcon HomeIcon = new javax.swing.ImageIcon("src/main/Resources/hutButton32.png");
        HomeButton.setIcon(HomeIcon);
        HomeButton.setToolTipText("Home");
        HomeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("HomeButton Pressed");
                //goOnlineOffline.setText(LSDemo.isOnline() ? " online" : " offline");
            }
        });
        return HomeButton;
    }
    // downloadButton on MENU
    public static JButton makeMenu4() {
        Border buttonPressedBorder = BorderFactory.createLineBorder(Color.RED,1);
        JButton downLoadButton = new JButton();
        ImageIcon downLoadIcon = new javax.swing.ImageIcon("src/main/Resources/downloadButton32.png");
        downLoadButton.setIcon(downLoadIcon);
        //downLoadButton.setDisabledIcon((downLoadIcon));
        //downLoadButton.setSelectedIcon(downLoadIcon);
        downLoadButton.setToolTipText("Download");
        downLoadButton.setFocusPainted(false);
        //downLoadButton.setFocusPainted(false);
        //downLoadButton.setFocusable(false);
        //downLoadButton.setDefaultCapable(false);
        /*downLoadButton.removeNotify();

        downLoadButton.setForeground(Color.white);*/
        downLoadButton.setContentAreaFilled(false);
        //System.out.println(downLoadButton.isEnabled());
        //System.out.println(downLoadButton.isRolloverEnabled());
        //System.out.println(downLoadButton.isBorderPainted());
        //System.out.println(downLoadButton.);
        //downLoadButton.setEnabled(false);
        downLoadButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("DownLoadButton Pressed");
                //downLoadButton.setBorder(buttonPressedBorder);
                downLoadButton.setContentAreaFilled(true);
                downLoadButton.setFocusPainted(true);
                downLoadButton.setContentAreaFilled(false);
                downLoadButton.setFocusPainted(true);


            }
                //goOnlineOffline.setText(LSDemo.isOnline() ? " online" : " offline");

        });
        return downLoadButton;
    }

    //sharebutton
    public static JMenu makeMenu5() {
        JMenu shareButton = new JMenu();
        ImageIcon shareIcon = new javax.swing.ImageIcon("src/main/Resources/ShareButton32.png");
        shareButton.setIcon(shareIcon);
        shareButton.setToolTipText("Shared Folder");
        shareButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("Shared Button Pressed");
                File newDirectory = new File((System.getProperty("user.home")) + File.separator + "applicationData");
                TableModel newTableModel = new TableModel(newDirectory);
                TableUtil.table.setModel(newTableModel);
                //goOnlineOffline.setText(LSDemo.isOnline() ? " online" : " offline");
            }
        });
        return shareButton;
    }
    public static String TitleBarDisplay() {
        int peersOnline = 0;
        //int peersOnline = getPeerCount();
        String lastLogon = "11-18-19 12:34:39";
        String appName = "    LicKet-Y-SpliT    ";
        String t = appName + "Peers Online:  " + peersOnline + "  Last Logon: " + lastLogon;
        return t;
    }


    public static JMenuBar makeMenuBar1() {
        //menuBar
        JMenuBar menuBar1 = new JMenuBar();
        menuBar1.setOpaque(true);
        //FileButton
        JMenu menu1 = MenuUtil.makeMenu1();
        //UploadButton
        JMenu uploadButton = MenuUtil.makeMenu2();
        //HomeButton
        JMenu homeButton = MenuUtil.makeMenu3();
        //DownloadButton
        JButton downloadButton = MenuUtil.makeMenu4();
        //ShareButton
        JMenu shareButton = MenuUtil.makeMenu5();
        shareButton.setEnabled(true);
        menuBar1.add(homeButton);
        menuBar1.add(shareButton);
        menuBar1.add(downloadButton);
        menuBar1.add(uploadButton);
        //Buttons on right side of menu bar
        menuBar1.add(Box.createHorizontalGlue());
        menuBar1.add(menu1);
        //menuBar1.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY,1, true));
        //menuBar1.setBackground(Color.DARK_GRAY);
        //menuBar1.setOpaque(true);
        return menuBar1;
    }
}


