package org.licketysplit.syncmanager.messages;

import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;

import java.io.IOException;
import java.util.logging.Level;

public class DeleteFileNotification extends JSONMessage {
    private FileInfo fileInfo;

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public DeleteFileNotification() {}

    public DeleteFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    @DefaultHandler(type = DeleteFileNotification.class)
    public static class DeleteFileNotificationHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            DeleteFileNotification deleteFileNotification = m.getMessage();
            FileInfo fileInfo = deleteFileNotification.getFileInfo();
            m.getEnv().getFM().deleteFileNotification(fileInfo);
        }
    }
}


