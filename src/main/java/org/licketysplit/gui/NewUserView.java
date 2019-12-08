package org.licketysplit.gui;


import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.Date;

/**
 * A GUI screen to add a new user.
 * Allows you to input a username, IP address, and Port number.
 */
public class NewUserView extends JDialog {
    /**
     * Instantiates a new New user view, with peer's env and the parent JFrame
     *
     * @param env   this peers Environment
     * @param frame parent JFrame
     */
    public NewUserView(Environment env, JFrame frame) {
        super(frame, "Add New User");
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        // Add username label and input field
        JLabel usernameLbl = new JLabel("Username: ");
        usernameLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField usernameField = new JTextField();

        // Add IP address label and input field
        JLabel ipLbl = new JLabel("IP Address: ");
        ipLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField ipFld = new JTextField();

        // Add port label and input field
        JLabel portLbl = new JLabel("Port: ");
        portLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField portFld = new JTextField();

        panel.add(usernameLbl, BorderLayout.WEST);
        panel.add(usernameField, BorderLayout.CENTER);

        panel.add(ipLbl, BorderLayout.WEST);
        panel.add(ipFld, BorderLayout.CENTER);

        panel.add(portLbl, BorderLayout.WEST);
        panel.add(portFld, BorderLayout.CENTER);

        // Add user button
        JButton addUserButton = new JButton("Add User");
        addUserButton.addActionListener((evt) -> {
            SwingUtilities.invokeLater(() -> {
                String username = "unknown";
                try {
                    username = usernameField.getText();
                    String ip = ipFld.getText();
                    String port = portFld.getText();

                    // Creates a new peer info object and load with provided arguments
                    PeerInfoDirectory.PeerInfo peerInfo = new PeerInfoDirectory.PeerInfo();
                    peerInfo.setUsername(username);
                    peerInfo.setServerIp(ip);
                    peerInfo.setServerPort(port);
                    peerInfo.setTimestamp(new Date());

                    // Generate a new identity key for this user and assign in peer info
                    byte[][] keys = peerInfo.generateIdentityKey();
                    peerInfo.setIdentityKey(keys[0]);

                    // Generate a new user confirmation payload and assign in peer info
                    peerInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(username, keys[0], env.getRootKey().getKey()));

                    // Add peer in PeerInfoDirectory, this will also automatically sync our peerInfoDirectory
                    // with all of the currently connected peers
                    env.getInfo().newPeerAndConfirm(peerInfo);
                    // Write peerInfoDirectory to file
                    env.getInfo().save();

                    // Generate a bootstrap ZIP file and show in the file browser
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
