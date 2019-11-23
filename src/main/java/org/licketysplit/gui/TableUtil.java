package org.licketysplit.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;

public class TableUtil {

    public static JTable table;
    public TableUtil() {


    }

    public static JScrollPane makeTable(File dir, File applicationData) {
        TableModel tableModel = new TableModel(dir, applicationData);
        //Jtable
        //JTable table = new JTable(tableModel);
        table = new JTable(tableModel);
        // Add Jtable to scrollPane
        JScrollPane tableScrollPane = new JScrollPane(table);
        tableScrollPane.setPreferredSize(new Dimension(250, 200));
        JTextField dirPathTextField = new JTextField(26);

        //  display the given directory

        return tableScrollPane;
    }
    public static JPanel makeCPanel() {
        JTextField dirPathTextField = new JTextField(26);
        JButton displayDirButton = new JButton("Load Directory");
        displayDirButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                System.out.println("loading directory");
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
        dirPathTextField.addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode()==KeyEvent.VK_ENTER) {
                    displayDirButton.doClick();
                }
            }
        });
        JPanel ctrlPane = new JPanel();
        ctrlPane.add(dirPathTextField);
        ctrlPane.add(displayDirButton);
        ctrlPane.setBackground(Color.darkGray);
        ctrlPane.setBackground(Color.LIGHT_GRAY);
        ctrlPane.setBorder(BorderFactory.createLineBorder(Color.white,2,true));
        return ctrlPane;

    }


}
