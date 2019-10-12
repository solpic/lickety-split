package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.json.JSONObject;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.IOException;

public class UpdateFileNotification extends Message {

    public JSONObject fileInfo;

    @Override
    public byte[] toBytes() {
        return fileInfo.toString().getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.fileInfo = new JSONObject(data.toString());
    }


    public UpdateFileNotification() {}

    public UpdateFileNotification(JSONObject fileInfo){
        this.fileInfo = fileInfo;
    }

    @DefaultHandler(type = UpdateFileNotification.class)
    public static class UpdateFileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            UpdateFileNotification updateFileNotification = (UpdateFileNotification) m.getMessage();
            JSONObject fileInfo = updateFileNotification.fileInfo;
            Environment env = m.getEnv();
            FileSharer fS = env.getFS();
            SecureSocket conn = m.getConn();
            FileManager fm = env.getFM();
            try {
                fm.up(new FileInfo(fileInfo)); //
                try {
                    fS.download(conn, fileInfo.getString("fileName"));
                }catch (Exception e) {
                    e.printStackTrace();
                }
            } catch(IOException e){
                e.printStackTrace();
            }
        }
    }
}


