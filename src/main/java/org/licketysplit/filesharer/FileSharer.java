package org.licketysplit.filesharer;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.syncmanager.FileManager;

import java.io.FileOutputStream;

public class FileSharer {
    private SecureSocket client;
    private Environment env;

    public FileSharer() {}

    public void setEnv(Environment env){
        this.env = env;
    }

    public void upSync(SecureSocket socket, ShareableFile file) throws Exception {

    }

    public void downSync(SecureSocket socket, JSONObject fileInfo) throws Exception {

    }

    public static class ChunkDownloadResponseHandler implements MessageHandler {
        public String fileName;

        public ChunkDownloadResponseHandler(String fileName){
            this.fileName = fileName;
        }

        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadResponse decodedMessage = (ChunkDownloadResponse) m.getMessage();
            try {
                Environment env = m.getEnv();
                FileManager fM = env.getFM();
                FileOutputStream fos = new FileOutputStream(env.getDirectory(this.fileName));
                fos.write(decodedMessage.data, 0, decodedMessage.data.length);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void download(SecureSocket socket, String fileName) throws Exception {
        socket.sendFirstMessage(new ChunkDownloadRequest(fileName), new ChunkDownloadResponseHandler(fileName));
    }


}
