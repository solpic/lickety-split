package org.licketysplit.gui;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.UserInfo;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * GUI view that lists all the peers in the network, as well as
 * other metadata such as whether you are connected to them,
 * whether they are banned,
 * their IP address and port..
 */
public class ListUsers extends JDialog {
    /**
     * Helper function to generate the array to put data into the table.
     *
     * @param env this peer's Environment
     * @return raw data for JTable
     */
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

    /**
     * Constructor/add components to frame.
     *
     * @param env   this peer's Environment
     * @param frame the parent JFrame
     */
    public ListUsers(Environment env, JFrame frame) {
        super(frame, "Listing Users");
        setLayout(new BorderLayout());
        JPanel panel = new JPanel();
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        DefaultTableModel m = new DefaultTableModel();
        final JTable table = new JTable(m);
        m.setDataVector(getData(env),  new String[]{"Username", "IP Address", "Port", "Status"});

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
