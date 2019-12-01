package org.licketysplit.gui;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.UserInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

public class ListUsers extends JDialog {

    Object[][] getData(Environment env) {
        return env.getInfo()
                .getPeers()
                .values()
                .stream()
                .map(e -> new Object[]{
                                String.format("%s%s",
                                        e.getBan()!=null?"BANNED: ":"",
                                        e.getUsername()),
                                e.getServerIp(),
                                e.getServerPort(),
                                env.getPm().peerStatus(e.getUsername())
                        }
                )
                .toArray(Object[][]::new);
    }
    public ListUsers(Environment env, JFrame frame) {
        super(frame, "Listing Users");
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DefaultTableModel m = new DefaultTableModel();
        final JTable table = new JTable(m);
        m.setDataVector(getData(env),  new String[]{"Username", "IP Address", "Port", "Status"});

//        table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

        //Create the scroll pane and add the table to it.
        JScrollPane scrollPane = new JScrollPane(table);

        //Add the scroll pane to this panel.
        add(scrollPane);
        //panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        pack();
        setVisible(true);

        new Thread(() -> {
            while(isVisible()) {
                try {
                    m.setRowCount(0);
                    Object[][] data = getData(env);
                    for (Object[] datum : data) {
                        m.addRow(datum);
                    }
                    Thread.sleep(3000);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
