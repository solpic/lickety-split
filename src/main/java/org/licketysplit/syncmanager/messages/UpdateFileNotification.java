package org.licketysplit.syncmanager.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class UpdateFileNotification extends Message {

    public String fileName;

    @Override
    public byte[] toBytes() {
        return fileName.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.fileName = new String(data);
    }


    public UpdateFileNotification() {}

    public UpdateFileNotification(String fileName){
        this.fileName = fileName;
    }

    @DefaultHandler(type = UpdateFileNotification.class)
    public static class UpdateFileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            UpdateFileNotification tstMsg = (UpdateFileNotification) m.getMessage();
            String updatedFileName = tstMsg.fileName;
            Environment env = m.getEnv();
            FileSharer fS = env.getFS();
            SecureSocket conn = m.getConn();
            try {
                fS.download(conn, updatedFileName);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}


