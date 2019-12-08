package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Sent when a file is added to the network.
 */
public class AddFileNotification extends JSONMessage {
    /**
     * Metadata about the file.
     */
    private FileInfo fileInfo;

    /**
     * Gets file info.
     *
     * @return the file info
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * Sets file info.
     *
     * @param fileInfo the file info
     */
    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    /**
     * Instantiates a new Add file notification.
     */
    public AddFileNotification() {}

    /**
     * Instantiates a new Add file notification.
     *
     * @param fileInfo the file info
     */
    public AddFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    /**
     * Default handler for AddFileNotifications.
     */
    @DefaultHandler(type = AddFileNotification.class)
    public static class AddFileNotificationHandler implements MessageHandler {
        /**
         * Runs callback on the file manager.
         * @param m the received message
         */
        @Override
        public void handle(ReceivedMessage m) {
            AddFileNotification updateFileNotification = m.getMessage();
            FileInfo fileInfo = updateFileNotification.getFileInfo();
            Environment env = m.getEnv();
            FileSharer fS = env.getFS();
            SecureSocket conn = m.getConn();
            FileManager fm = env.getFM();
            env.getLogger().log(Level.INFO, "adding file: " + fileInfo.getName());
            try {
                fm.addFileNotification(fileInfo);
            } catch(Exception e){
                env.getLogger().log(Level.INFO, "Add file", e);
            }
        }
    }
}


