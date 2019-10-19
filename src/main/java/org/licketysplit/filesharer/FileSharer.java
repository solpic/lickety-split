package org.licketysplit.filesharer;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.messages.AddFileNotification;

import java.io.FileOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

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
            ChunkDownloadResponse decodedMessage = m.getMessage();
            try {
                FileOutputStream fos = new FileOutputStream(m.getEnv().getDirectory(this.fileName));
                fos.write(decodedMessage.data, 0, decodedMessage.data.length);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void download(String fileName) throws Exception {
//        this.env.getLogger().log(Level.INFO, "Adding File: " + info.getName());

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new ChunkAvailabilityRequest(fileName), null);
        }
    }

    public void downloadFrom(SecureSocket socket, String fileName) throws Exception {
        socket.sendFirstMessage(new ChunkDownloadRequest(fileName), new ChunkDownloadResponseHandler(fileName));
    }


}
