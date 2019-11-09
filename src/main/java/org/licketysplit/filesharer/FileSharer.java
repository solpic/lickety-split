package org.licketysplit.filesharer;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkAvailabilityResponse;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class FileSharer {
    private SecureSocket client;
    private Environment env;

    public FileSharer() {}

    public void setEnv(Environment env){
        this.env = env;
    }

    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        public DownloadManager dManager;

        public ChunkAvailabilityRequestHandler(){}

        public ChunkAvailabilityRequestHandler(DownloadManager dManager){
            this.dManager = dManager;
        }

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkAvailabilityResponse decodedMessage = m.getMessage();
            PeerChunkInfo peerChunkInfo = decodedMessage.peerChunkInfo;
            dManager.addPeerAndRequestChunkIfPossible(peerChunkInfo, m.getConn());
        }
    }

    public void download(FileInfo fileInfo) throws Exception {
        DownloadManager dManager = new DownloadManager(fileInfo);

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            //log the chunks they have and send download request if applicable
            peer.getValue().sendFirstMessage(new ChunkAvailabilityRequest(fileInfo), new ChunkAvailabilityRequestHandler(dManager));
        }
    }

//    public static class ChunkDownloadResponseHandler implements MessageHandler {
//        public String fileName;
//
//        public ChunkDownloadResponseHandler(String fileName){
//            this.fileName = fileName;
//        }
//
//        @Override
//        public void handle(ReceivedMessage m) {
//            ChunkDownloadResponse decodedMessage = m.getMessage();
//            try {
//                FileOutputStream fos = new FileOutputStream(m.getEnv().getDirectory(this.fileName));
//                fos.write(decodedMessage.data, 0, decodedMessage.data.length);
//                fos.close();
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    public void downloadFrom(SecureSocket socket, String fileName) throws Exception {
//        socket.sendFirstMessage(new ChunkDownloadRequest(fileName), new ChunkDownloadResponseHandler(fileName));
//    }


}
