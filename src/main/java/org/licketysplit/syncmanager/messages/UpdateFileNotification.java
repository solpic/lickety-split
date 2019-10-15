package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.*;
import org.json.JSONObject;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.IOException;
import java.util.logging.Level;

public class UpdateFileNotification extends JSONMessage {
    private FileInfo fileInfo;

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public UpdateFileNotification() {}

    public UpdateFileNotification(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    @DefaultHandler(type = UpdateFileNotification.class)
    public static class UpdateFileRequestHandler implements MessageHandler {
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
                 fm.updateFileInManifest(fileInfo);
                try {
                    fS.download(conn, fileInfo.getName());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}


