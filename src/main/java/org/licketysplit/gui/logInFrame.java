package org.licketysplit.gui;


import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

public class logInFrame extends JFrame implements ActionListener {

    LSDemo ls;
    String value1;
    public Boolean letin = false;
    private static JProgressBar progbar1, progbar2;
    private static Border raisedbevel, loweredbevel, compound, compound2, compound3, compound4,
            bord1, bord2, bord3, bord4;
    private static JButton SUBMIT, QUIT, REGISTER;
    private static Font customFont, customFont46, customFont1, customFont22;
    private static JPanel emptyPanel, mainPanel, panel;
    private static JLabel userNameLabel, passwordLabel, randomLabel1,
            randomLabel2, BFolderLabel, appLabel;
    private static ImageIcon BlackFolder1, quitButtonImage;
    private static Image BlackFolder2;
    final JTextField text1, text2;
    final static boolean shouldFill = true;
    final static boolean shouldWeightX = true;
    final static boolean RIGHT_TO_LEFT = false;
    private static final Logger logger1 = LogManager.getLogger("logInFrame");
    Font myFont;

    public logInFrame() {

        try {
            //Props props = new Props();
            //System.out.println(props.
            logger1.isEnabled(Level.DEBUG);
            logger1.error("Application Startup");
            logger1.error("Checking Configuration");
            logger1.error("Turn debug off to improve performance");
            logger1.error("current logging level" + "  " + logger1.getLevel());
            logger1.info("STARTING UP");

            //create the font and size
            customFont = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/Resources/GatsbyFLF-Bold.ttf")).deriveFont(36f);
            customFont46 = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/Resources/GatsbyFLF-Bold.ttf")).deriveFont(78f);
            customFont22 = Font.createFont(Font.TRUETYPE_FONT, new File("src/main/Resources/GatsbyFLF-Bold.ttf")).deriveFont(16f);
            GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
            //register the font
            ge.registerFont(customFont);
        } catch (IOException e) {
            logger1.error(e);
            e.printStackTrace();
        } catch (FontFormatException e) {

            logger1.error(e);
            e.printStackTrace();
        }
        customFont22 = customFont.deriveFont(22);

        //Load images
        try {
            BlackFolder1 = new ImageIcon("src/main/Resources/BlackFolder1.png");
            quitButtonImage = new ImageIcon("src/main/Resources/closeButton.png");
            BFolderLabel = new JLabel(BlackFolder1);
            BlackFolder2 = new javax.swing.ImageIcon("src/main/Resources/BlackFolder2.png").getImage();
            this.setIconImage(BlackFolder2);

        } catch (Exception e) {
            logger1.error("error setting images, check Resources folder for png and path of Resources folder", e);
        }
        //2 progress bars
       /* progbar1 = new JProgressBar();
        progbar1.setValue(0);
        progbar1.setSize(1000, 50);
        progbar1.setBackground(Color.LIGHT_GRAY);
        progbar2 = new JProgressBar();
        progbar2.setValue(40);
        progbar2.setStringPainted(true);*/

        //custom borders
        raisedbevel = BorderFactory.createRaisedBevelBorder();
        loweredbevel = BorderFactory.createLoweredBevelBorder();
        Border bord1 = BorderFactory.createLineBorder(Color.DARK_GRAY, 2, true);
        Border bord2 = BorderFactory.createLineBorder(Color.BLACK, 2, true);
        Border bord3 = BorderFactory.createLineBorder(Color.WHITE, 2, true);
        Border bord4 = BorderFactory.createLineBorder(Color.DARK_GRAY, 2, true);
        loweredbevel = BorderFactory.createLoweredBevelBorder();
        compound = BorderFactory.createCompoundBorder(
                raisedbevel, loweredbevel);
        compound2 = BorderFactory.createCompoundBorder(
                bord1, bord2);
        compound3 = BorderFactory.createCompoundBorder(
                bord2, bord3);
        compound4 = BorderFactory.createCompoundBorder(
                bord3, bord4);

        //adding layout
        this.setLayout(new FlowLayout());

        //adding panel components//
        //username label and app label
        userNameLabel = new JLabel("user", SwingConstants.RIGHT);
        userNameLabel.setForeground(Color.WHITE);
        userNameLabel.setFont(customFont);
        appLabel = new JLabel("LiCkeTy SpLiT    ");
        appLabel.setForeground(Color.WHITE);
        appLabel.setFont(customFont46);
        //username text field
        text1 = new JTextField(10);
        text1.setSize(50, 50);
        text1.setForeground(Color.BLACK);
        text1.setBackground(Color.LIGHT_GRAY);

        //PASSWORD textfield
        passwordLabel = new JLabel("password", SwingConstants.RIGHT);
        passwordLabel.setFont(customFont);
        passwordLabel.setForeground(Color.WHITE);
        text2 = new JPasswordField(10);
        text2.setForeground(Color.BLACK);
        text2.addNotify();
        text2.setBackground(Color.LIGHT_GRAY);

        //REGISTER BUTTON
        REGISTER = new JButton("Register");
        REGISTER.setBorder(compound3);
        REGISTER.setFont(customFont22);
        REGISTER.setForeground(Color.LIGHT_GRAY);
        REGISTER.setBackground(Color.DARK_GRAY);
        REGISTER.setSize(20, 10);

        //QUIT BUTTON
        QUIT = new JButton(quitButtonImage);
        QUIT.setBorder(compound3);
        QUIT.setFont(customFont);
        QUIT.setForeground(Color.LIGHT_GRAY);
        QUIT.setBackground(Color.DARK_GRAY);

        //SUBMIT Button
        SUBMIT = new JButton("Let's Go");
        SUBMIT.setBorder(compound3);
        SUBMIT.setFont(customFont);
        SUBMIT.setForeground(Color.LIGHT_GRAY);
        SUBMIT.setBackground(Color.DARK_GRAY);


        //mainPanel to hold panel + BFolderLabel
        panel = new JPanel(new GridBagLayout());   //top half of login frame
        GridBagConstraints c = new GridBagConstraints();

        //c.fill = GridBagConstraints.HORIZONTAL;
        //add quit button to grid
        c.gridx = 9;
        c.gridy = 0;
        c.weightx = 10;
        c.weighty = 2;
        c.fill = GridBagConstraints.EAST;
        panel.add(QUIT, c);

        //add "lickety split title"
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 7;
        c.ipady = 10;
        c.fill = GridBagConstraints.CENTER;
        panel.add(appLabel, c);
        c.gridwidth = 1;
        c.insets = new Insets(2, 5, 0, 5);
        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 2;
        c.gridy = 3;
        panel.add(userNameLabel, c);
        c.gridx = 3;
        c.gridy = 3;
        panel.add(text1, c);
        c.gridx = 2;
        c.gridy = 4;
        panel.add(passwordLabel, c);
        c.gridx = 3;
        c.gridy = 4;
        panel.add(text2, c);
        c.gridx = 0;
        c.gridy = 5;
        panel.add(REGISTER, c);
        c.gridx = 3;
        c.gridy = 5;
        panel.add(SUBMIT, c);

        //add "lickety split title"
        c.gridx = 0;
        c.gridy = 1;
        c.gridwidth = 6;
        c.fill = GridBagConstraints.CENTER;
        panel.add(appLabel, c);
        panel.setBackground(Color.DARK_GRAY);
        panel.setBorder(compound4);
        //mainPanel
        mainPanel = new JPanel(new BorderLayout(10, 5));
        mainPanel.setBorder(compound3);
        //mainPanel.add(menuBar);
        mainPanel.add(panel, BorderLayout.PAGE_START);
        mainPanel.setBackground((Color.GRAY));
        mainPanel.add(BFolderLabel);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //remove title bar
        this.setUndecorated(true);
        this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        //translucent background
        this.setBackground(new Color(0, 0, 0, 128));
        this.add(mainPanel);
        //pack components into pane.
        pack();
        //set location of frame to the middle of screen
        this.setSize(520, 620);
        this.setLocationRelativeTo(null);
        text1.requestFocus();
        //Show Frame
        this.setVisible(true);

        QUIT.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                System.out.println("bah user quit button pressed");
                System.exit(0);
            }
        });
        text2.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER) {
                    SUBMIT.doClick();
                }
            }
        });
        SUBMIT.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent evt) {
                String value1 = text1.getText();
                String value2 = text2.getText();
                if (value1.equals("lick") && value2.equals("this")) {
                    remove(mainPanel);
                    remove(panel);
                    LoadingFrame testLoadBar = new LoadingFrame();
                    testLoadBar.go();
                    removeAll();
                    System.out.println("TEST LOADBAR FINISHED ?");
                    setVisible(false);
                    dispose();
                }
            }
        });
    }



        public static void main (String arg[])
        {
            try {
                System.out.println("hehehelaslldfa" + SwingUtilities.isEventDispatchThread());

                logInFrame lframe = new logInFrame();
                System.out.println("Login Form Launched");
                logger1.info("Login Form Launched");

            } catch (Exception e) {
                logger1.error("loginFrame cannot begin");
                JOptionPane.showMessageDialog(null, e.getMessage());
            }
        }

        @Override
        public void actionPerformed (ActionEvent e){
            System.out.println("actionPerformed");
            logger1.info("actionPerformed");

        }
    }


