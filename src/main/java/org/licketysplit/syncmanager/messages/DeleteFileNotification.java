package org.licketysplit.syncmanager.messages;

import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;

import java.io.IOException;
import java.util.logging.Level;

/**
 * Sent when a file is deleted from the network.
 */
public class DeleteFileNotification extends JSONMessage {
    /**
     * Metadata about the deleted file.
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
     * Instantiates a new Delete file notification.
     */
    public DeleteFileNotification() {}

    /**
     * Instantiates a new Delete file notification.
     *
     * @param fileInfo the file info
     */
    public DeleteFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    /**
     * Default handler for the DeleteFileNotification.
     */
    @DefaultHandler(type = DeleteFileNotification.class)
    public static class DeleteFileNotificationHandler implements MessageHandler {
        /**
         * Called when the message is received, runs a callback on the FileManager.
         * @param m the received message
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            DeleteFileNotification deleteFileNotification = m.getMessage();
            FileInfo fileInfo = deleteFileNotification.getFileInfo();
            m.getEnv().getFM().deleteFileNotification(fileInfo);
        }
    }
}


