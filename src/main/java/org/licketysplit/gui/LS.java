package org.licketysplit.gui;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

public class LS {

    public LS() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    setLookAndFeel(Props.NIMBUS_LF);
                    createAndShowGUI(null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void main(String[] args) throws InvocationTargetException, InterruptedException {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    setLookAndFeel(Props.NIMBUS_LF);
                    createAndShowGUI(args);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public static void createAndShowGUI(String[] args) throws Exception {

        File applicationData;
        File dir;
        if (args.length > 0)
            dir = new File(args[0]);
        else
            dir = new File((System.getProperty("user.home"))+ File.separator + "applicationData");
        //dir = new File(+ File.separator + "applicationData"));

        //dir = new File(System.getProperty("java"))
        applicationData = new File(dir + File.separator + "applicationData");

        //Model for directory contents
        TableModel tableModel = new TableModel(dir, applicationData);

        //Jtable
        JTable table = new JTable(tableModel);
        //TableModel newTableModel = new TableModel(newDir);
        //table.setModel(newTableModel);

        // Add Jtable to scrollPane
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(250, 200));
        JTextField dirPathTextField = new JTextField(26);

        // Create an action listener to display the given directory
        JButton displayDirButton = new JButton("VAULT");
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
        });


        ////PANEL
        JPanel ctrlPane = new JPanel();
        ctrlPane.add(dirPathTextField);
        ctrlPane.add(displayDirButton);
        ctrlPane.setBackground(Color.darkGray);
        //ctrlPane.setForeGround(Color.white);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                ctrlPane, tableScrollPane);
        splitPane.setDividerLocation(35);
        splitPane.setEnabled(false);

        JFrame frame = new JFrame("-|Lickety|Split|-");
        //JFrame frame = SwingUtilities.getRootPane();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        //JFrame frame = SwingUtilities.getRoot();
        frame.add(splitPane);
        frame.pack();
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
}
