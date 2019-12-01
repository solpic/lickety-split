package org.licketysplit.gui;


import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Date;

public class BanUserView extends JDialog {
    public BanUserView(Environment env, JFrame parent) {
        super(parent, "Add New User");

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(0, 2));
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        JLabel usernameLbl = new JLabel("Username: ");
        usernameLbl.setHorizontalAlignment(JLabel.RIGHT);
        JTextField usernameField = new JTextField();

        panel.add(usernameLbl, BorderLayout.WEST);
        panel.add(usernameField, BorderLayout.CENTER);

        JButton banUserButton = new JButton("Ban User");
        banUserButton.addActionListener((evt) -> {
            SwingUtilities.invokeLater(() -> {
                String username = "unknown";
                try {
                    username = usernameField.getText();
                    PeerInfoDirectory info = env.getInfo();
                    if(info.getPeers().containsKey(username)) {
                        info.banUser(username, env.getRootKey().getKey());
                        JOptionPane.showMessageDialog(
                                null,
                                String.format("User '%s' banned successfully", username)
                        );
                    }else{
                        throw new Exception(String.format("User '%s' does not exist", username));
                    }

                    setVisible(false);
                    dispose();
                } catch(Exception e) {
                    JOptionPane.showMessageDialog(
                            null,
                            String.format(
                                    "Error banning user '%s': %s", username, e.getMessage()
                            )
                    );
                    e.printStackTrace();
                }
            });
        });
        panel.add(banUserButton);

        panel.setBorder(new EmptyBorder(10, 10, 10, 10));
        add(panel);
        pack();
        setVisible(true);
    }
}
