package org.licketysplit.gui;


import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Date;

public class NewUserView extends JDialog {
    public NewUserView(Environment env, JFrame frame) {

        super(frame, "Add New User");
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel usernameLbl = new JLabel("Username: ");
        usernameLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField usernameField = new JTextField();

        JLabel ipLbl = new JLabel("IP Address: ");
        ipLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField ipFld = new JTextField();

        JLabel portLbl = new JLabel("Port: ");
        portLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField portFld = new JTextField();

        panel.add(usernameLbl, BorderLayout.WEST);
        panel.add(usernameField, BorderLayout.CENTER);

        panel.add(ipLbl, BorderLayout.WEST);
        panel.add(ipFld, BorderLayout.CENTER);

        panel.add(portLbl, BorderLayout.WEST);
        panel.add(portFld, BorderLayout.CENTER);

        JButton addUserButton = new JButton("Add User");
        addUserButton.addActionListener((evt) -> {
            SwingUtilities.invokeLater(() -> {
                String username = "unknown";
                try {
                    username = usernameField.getText();
                    String ip = ipFld.getText();
                    String port = portFld.getText();
                    PeerInfoDirectory.PeerInfo peerInfo = new PeerInfoDirectory.PeerInfo();
                    peerInfo.setUsername(username);
                    peerInfo.setServerIp(ip);
                    peerInfo.setServerPort(port);
                    peerInfo.setTimestamp(new Date());
                    byte[][] keys = peerInfo.generateIdentityKey();
                    peerInfo.setIdentityKey(keys[0]);
                    peerInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(username, keys[0], env.getRootKey().getKey()));

                    env.getInfo().newPeerAndConfirm(peerInfo);
                    env.getInfo().save();


                    File bootstrap = env.getInfo().generateBootstrapFile(
                            username, ip, port,
                            keys[1],
                            false,
                            null
                    );
                    Desktop.getDesktop().open(bootstrap.getParentFile());

                    JOptionPane.showMessageDialog(
                            null,
                            String.format("User '%s' added successfully, showing bootstrap file", username)
                    );
                    setVisible(false);
                    dispose();
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(
                            null,
                            String.format("Error adding user '%s': %s", username, e.getMessage())
                    );
                    e.printStackTrace();
                }
            });
        });
        panel.add(addUserButton, BorderLayout.WEST);

        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel);
        pack();
        setVisible(true);
    }
}
