package org.licketysplit.gui;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class NewFileView  {
    public NewFileView(Environment env, JFrame frame) {
        File selectedFile = null;
        try {
            JFileChooser fc = new JFileChooser();
            int returnVal = fc.showOpenDialog(frame);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                selectedFile = fc.getSelectedFile();
                env.getSyncManager().addFile(selectedFile.getAbsolutePath());
                JOptionPane.showMessageDialog(
                        frame,
                        String.format("Successfully added file '%s'", selectedFile.getName())
                );
                env.getFM().triggerGUIChanges();
            }
        } catch(Exception e) {
            String s = selectedFile!=null?selectedFile.getName():"none";
            JOptionPane.showMessageDialog(
                    frame,
                    String.format(
                            "Error adding file '%s': %s", s, e.getMessage()
                    )
            );
            e.printStackTrace();
        }
    }
}
