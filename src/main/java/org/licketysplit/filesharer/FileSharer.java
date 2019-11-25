package org.licketysplit.filesharer;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkAvailabilityResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

interface UpdateDownloads{
    void update();
}

public class FileSharer {
    private Environment env;
    private HashMap<String, DownloadManager> downloads;

    public FileSharer() {
        this.downloads = new HashMap<String, DownloadManager>();
    }

    public void setEnv(Environment env) {
        this.env = env;
    }

    public HashMap<String, DownloadManager> getDownloads(){
        return this.downloads;
    }

    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        public DownloadManager dManager;
        public UserInfo userInfo;

        public ChunkAvailabilityRequestHandler(DownloadManager dManager, UserInfo userInfo) {
            this.dManager = dManager;
            this.userInfo = userInfo;
        }

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkAvailabilityResponse decodedMessage = m.getMessage();
            m.log(String.format("Chunk availability response "));
            PeerChunkInfo peerChunkInfo = decodedMessage.peerChunkInfo;
            dManager.addPeerAndRequestChunkIfPossible(peerChunkInfo, m.getConn(), this.userInfo);
        }
    }

    private Map<String, DownloadManager> currentDownloads = new HashMap<>();

    public void cancelOrFinish(FileInfo file) {
        synchronized (currentDownloads) {
            if(currentDownloads.containsKey(file.name)) {
                env.log(String.format("Removing download reservation for '%s'", file.name));
                currentDownloads.remove(file.name);
            }else{
                env.log(String.format("Tried to remove reservation for nonexistent '%s'", file.name));
            }
        }
    }


    public DownloadManager download(FileInfo fileInfo) throws Exception {
        DownloadManager dManager;
        synchronized (currentDownloads) {
            if(currentDownloads.containsKey(fileInfo.name)) {
                return null;
            }else{
                env.log(String.format("Reserving download for '%s'", fileInfo.name));
                UpdateDownloads updateDownloads = () -> this.downloads.remove(fileInfo);
                dManager = new DownloadManager(fileInfo, this.env, updateDownloads);
                currentDownloads.put(fileInfo.name, dManager);
            }
        }
        this.downloads.put(fileInfo.getName(), dManager);
        this.env.getLogger().log(Level.INFO, "DOWNLOADS SIZE: " + this.downloads.size());
        Thread dThread = new Thread(dManager);
        dThread.start();
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            //log the chunks they have and send download request if applicable
            peer.getValue().sendFirstMessage(new ChunkAvailabilityRequest(fileInfo), new ChunkAvailabilityRequestHandler(dManager, peer.getKey()));
        }

        return dManager; // So UI has access to download manager (primarily for cancellation)
    }

    public boolean downloadInProgress(FileInfo fileInfo){
        synchronized(currentDownloads) {
            if(currentDownloads.containsKey(fileInfo.name) ) return true;
        }
        return false;
    }

    public ArrayList<Integer> getChunks(FileInfo fileInfo){
        return this.downloads.get(fileInfo.getName()).getCompletedChunks();
    }

    public int getChunksLength(FileInfo fileInfo){
        return this.downloads.containsKey(fileInfo.getName()) ? this.downloads.get(fileInfo.getName()).getCompletedChunks().size() : -1;
    }
}
