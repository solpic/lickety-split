package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.util.logging.Level;

/**
 * Sent when a file has been updated on the network.
 */
public class UpdateFileNotification extends JSONMessage {
    /**
     * Metadata about the updated file.
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
     * Instantiates a new Update file notification.
     */
    public UpdateFileNotification() {}

    /**
     * Instantiates a new Update file notification.
     *
     * @param fileInfo the file info
     */
    public UpdateFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    /**
     * Default handler for the UpdateFileNotification message.
     */
    @DefaultHandler(type = UpdateFileNotification.class)
    public static class UpdateFileRequestHandler implements MessageHandler {
        /**
         * Called when the message is received,
         * runs a callback on the FileManager.
         * @param m the received message
         */
        @Override
        public void handle(ReceivedMessage m) {
            UpdateFileNotification updateFileNotification = m.getMessage();
            FileInfo fileInfo = updateFileNotification.getFileInfo();
            Environment env = m.getEnv();
            FileSharer fS = env.getFS();
            SecureSocket conn = m.getConn();
            FileManager fm = env.getFM();
            env.getLogger().log(Level.INFO, "updating file: " + fileInfo.getName());
            try {
                fm.fileUpdatedNotification(fileInfo);
            } catch(Exception e){
                env.getLogger().log(Level.INFO, "Update file", e);
            }
        }
    }
}


