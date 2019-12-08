package org.licketysplit.gui;

import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.testing.TestNetworkManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * GUI screen to allow the creation of a new network.
 * In it you specify your username, IP address, and port,
 * and it generates a bootstrap file for you to load the new
 * network into the StartScreen.
 */
public class CreateNewNetworkView extends JDialog {

    /**
     * Creates the a new network and corresponding
     * bootstrap file given this root user (username, ip, port).
     * Loads the network into the StartScreen table.
     *
     * @param username the username of root
     * @param ip       the ip of root
     * @param port     the port of root
     * @throws Exception the exception
     */
    void createTheNetwork(String username, String ip, String port) throws Exception {
        File infodir = File.createTempFile("infodir", null);
        PeerInfoDirectory info = new PeerInfoDirectory(infodir.getPath());
        byte[] rootKey = info.initializeNetwork();
        File rootKeyFile = File.createTempFile("rootkey", null);
        KeyStore rootKeyStore = new KeyStore(rootKeyFile.getPath());
        rootKeyStore.setKey(rootKey);
        rootKeyStore.save();

        PeerInfoDirectory.PeerInfo rootUser = new PeerInfoDirectory.PeerInfo();
        rootUser.setUsername(username);
        rootUser.setServerPort(port);
        rootUser.setServerIp(ip);
        rootUser.setTimestamp(new Date());
        byte[][] keys = rootUser.generateIdentityKey();
        rootUser.setIdentityKey(keys[0]);

        File idKeyFile = File.createTempFile("idkey", null);
        KeyStore idKeyStore = new KeyStore(idKeyFile.getPath());
        idKeyStore.setKey(keys[1]);
        idKeyStore.save();

        rootUser.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(username, keys[0], rootKey));
        info.newPeer(rootUser);

        info.save();
        File bootstrap = info.generateBootstrapFile(username, ip, port, keys[1], true, rootKey);
        SwingUtilities.invokeLater(() -> {
            start.createdNetwork(bootstrap);
        });

    }

    /**
     * Parent StartScreen instance
     */
    StartScreen start;

    /**
     * Instantiates a new Create new network view.
     *
     * @param start StartScreen that created this
     * @param frame the parent JFrame
     */
    public CreateNewNetworkView(StartScreen start, JFrame frame) {
        super(frame, "Create New Network");
        this.start = start;
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel usernameLbl = new JLabel("Your username: ");
        usernameLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField usernameField = new JTextField();

        JLabel ipLbl = new JLabel("Your IP Address: ");
        ipLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField ipFld = new JTextField();

        JLabel portLbl = new JLabel("Your Port: ");
        portLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField portFld = new JTextField();

        panel.add(usernameLbl, BorderLayout.WEST);
        panel.add(usernameField, BorderLayout.CENTER);

        panel.add(ipLbl, BorderLayout.WEST);
        panel.add(ipFld, BorderLayout.CENTER);

        panel.add(portLbl, BorderLayout.WEST);
        panel.add(portFld, BorderLayout.CENTER);

        JButton addUserButton = new JButton("Create Network");
        addUserButton.addActionListener((evt) -> {
            SwingUtilities.invokeLater(() -> {
                String username = "unknown";
                try {
                    username = usernameField.getText();
                    String ip = ipFld.getText();
                    String port = portFld.getText();

                    createTheNetwork(username, ip, port);
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
