package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.SecureSocket;

import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class DownloadManager {

    private AssemblingFile assemblingFile;
    private HashMap<UserInfo, PeerDownloadInfo> peers;
    private ArrayList<Integer> necessaryAndAvailableChunks;
    private ArrayList<Integer> completedChunks;
    private Environment env;
    private ReentrantLock lock;
    private Random r;
    private Thread assemblingThread;
    private int updateAfterTwenty;
    private boolean isCanceled;

    public DownloadManager(FileInfo fileInfo, Environment env) throws IOException {
        this.assemblingFile = new AssemblingFile(fileInfo, env, this.getLengthInChunks(fileInfo));
        this.assemblingThread = new Thread(assemblingFile);
        this.assemblingThread.start();
        this.necessaryAndAvailableChunks = new ArrayList<Integer>();
        this.peers = new HashMap<UserInfo, PeerDownloadInfo>();
        this.completedChunks = new ArrayList<Integer>();
        this.env = env;
        this.lock = new ReentrantLock();
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
        this.updateAfterTwenty = 0;
        this.isCanceled = false;
    }

    public AssemblingFile getAssemblingFile() {
        return assemblingFile;
    }

    private int getLengthInChunks(FileInfo fileInfo){
        double chunkLength = 1024.0;
        double preciseChunks = fileInfo.getLength() / chunkLength;
        return (int) Math.ceil(preciseChunks);
    }

    public void addPeerAndRequestChunkIfPossible(PeerChunkInfo peerInfo, SecureSocket socket, UserInfo userInfo) throws Exception {
        this.peers.put(userInfo, new PeerDownloadInfo(peerInfo, socket));
        this.updateAvailableChunks(peerInfo);
        this.requestRandomChunk(userInfo);
    }

    public HashMap<UserInfo, PeerDownloadInfo> getPeers(){
        return this.peers;
    }

    public void requestRandomChunk(UserInfo userInfo) throws Exception {
        if(isCanceled) return;

        int chunk = -1;
        PeerDownloadInfo peer = this.getPeers().get(userInfo);

        if(peer == null) return;

        this.lock.lock(); // Lock so no concurrency issues
        try {
            chunk = peer.getRandomDesirableChunk(this.necessaryAndAvailableChunks);
            if(chunk < 0) return; // TODO(will) Maybe update available chunks at this point
            this.necessaryAndAvailableChunks.remove(Integer.valueOf(chunk));
        } finally {
            this.lock.unlock();
        }

        this.sendDownloadRequest(chunk, userInfo, peer);
        if(this.updateAfterTwenty == 20) {
            this.updateAfterTwenty = 0;
            this.updatePeerList();
        } else {
            this.updateAfterTwenty++;
        }
    }

    public void updatePeerList() throws Exception {
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        Set<UserInfo> newPeers = new HashSet<UserInfo>(peers.keySet());
        Set<UserInfo> currPeers = new HashSet(this.getPeers().keySet());
        newPeers.removeAll(currPeers);
        ArrayList<UserInfo> newPeersLs = new ArrayList<UserInfo>(newPeers);
        for (UserInfo peer: newPeersLs) {
            peers.get(peer).sendFirstMessage(new ChunkAvailabilityRequest(this.assemblingFile.getFileInfo()), new FileSharer.ChunkAvailabilityRequestHandler(this, peer));
        }
    }

    public void sendDownloadRequest(int chunk, UserInfo userInfo, PeerDownloadInfo peer) throws Exception {
        peer.getSocket().sendFirstMessage(new ChunkDownloadRequest(this.assemblingFile.getFileInfo(), chunk), new ChunkDownloadRequestHandler(chunk,  this, userInfo)); //need to close request and remove chunk
    }

    public void onChunkCompleted(int chunk, UserInfo userInfo, boolean isFinished) throws Exception {
        if(isFinished){
            return;
        }
        this.completedChunks.add(chunk);
        this.requestRandomChunk(userInfo);
    }

    public void updateAvailableChunks(PeerChunkInfo peerChunkInfo){
        ArrayList<Integer> newChunks = peerChunkInfo.getChunks();
        Set<Integer> chunks = new HashSet<Integer>(this.necessaryAndAvailableChunks);
        for(int i = 0; i < newChunks.size(); i++){
            chunks.add(newChunks.get(i));
        }
        this.necessaryAndAvailableChunks = new ArrayList<Integer>(chunks);
    }

    public void cancelDownload(){
        this.assemblingFile.cancel();
        this.isCanceled = true;
    }

    public static class ChunkDownloadRequestHandler implements MessageHandler {
        public PeerDownloadInfo peer;
        public int chunk;
        public DownloadManager dManager;
        public UserInfo userInfo;

        public ChunkDownloadRequestHandler(int chunk, DownloadManager dManager, UserInfo userInfo){
            this.chunk = chunk;
            this.dManager = dManager;
            this.userInfo = userInfo;
        }

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            //If error add chunk back to availableChunks.
            ChunkDownloadResponse decodedMessage = m.getMessage();
            boolean isFinished = this.dManager.getAssemblingFile().saveChunk(decodedMessage.data, this.chunk);
            this.dManager.onChunkCompleted(this.chunk, this.userInfo, isFinished);
        }
    }
}

