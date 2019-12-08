package org.licketysplit.gui;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

/**
 * GUI screen to upload a new file to the network.
 * Opens up a file chooser and when the file is selected
 * add the file to the network with the SyncManager, trigger a GUI update,
 * and close this window.
 */
public class NewFileView  {
    /**
     * Instantiates a new New file view.
     *
     * @param env   this peers environment
     * @param frame the parent JFrame
     */
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
