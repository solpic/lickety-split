package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.IOException;
import java.util.logging.Level;

public class AddFileNotification extends JSONMessage {
    private FileInfo fileInfo;

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public AddFileNotification() {}

    public AddFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    @DefaultHandler(type = AddFileNotification.class)
    public static class AddFileNotificationHandler implements MessageHandler {
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
                e.printStackTrace();
            }
        }
    }
}


