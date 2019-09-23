package org.licketysplit.gui;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class GUI extends JFrame {
    //private JButton downsyncbutton;
    //private JButton upsyncbutton;
    //private JList mylist;
    private JPanel rootPanel;
    private JButton downbutton;
    private JButton upbutton;
    private JComboBox mybox;

    public GUI()
    {
        add(rootPanel);
        setTitle("P2P file sharing");
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        upbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(rootPane, "File is upsyncing");
            }
        });
        downbutton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(rootPane, "File is Downsyncing");
            }
        });
    }
}
