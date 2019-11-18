package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
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
    private ArrayList<Integer> availableChunks;
    private ArrayList<Integer> completedChunks;
    private Environment env;
    private ReentrantLock lock;
    private Random r;
    private Thread assemblingThread;

    public DownloadManager(FileInfo fileInfo, Environment env) throws IOException {
        this.assemblingFile = new AssemblingFile(fileInfo, env, this.getLengthInChunks(fileInfo));
        this.assemblingThread = new Thread(assemblingFile);
        this.assemblingThread.start();
        this.availableChunks = new ArrayList<Integer>();
        this.peers = new HashMap<UserInfo, PeerDownloadInfo>();
        this.completedChunks = new ArrayList<Integer>();
        this.env = env;
        this.lock = new ReentrantLock();
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
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
        int chunk = -1;
        PeerDownloadInfo peer = this.getPeers().get(userInfo);

        this.lock.lock(); // Lock so no concurrency issues
        try {
            chunk = peer.getRandomDesirableChunk(this.availableChunks);
            if(chunk < 0) return; // TODO(will) Maybe update available chunks at this point
            this.availableChunks.remove(Integer.valueOf(chunk));
        } finally {
            this.lock.unlock();
        }

        if(peer == null){
            System.out.println("No peer found");
        }
        this.sendDownloadRequest(chunk, userInfo, peer);
        // this.updatePeerList();
    }

    public void updatePeerList(){
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        //TODO(WILL)
        //Figure out if we need updatePeerList then implement it if necessary
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
        Set<Integer> chunks = new HashSet<Integer>(this.availableChunks);
        for(int i = 0; i < newChunks.size(); i++){
            chunks.add(newChunks.get(i));
        }
        this.availableChunks = new ArrayList<Integer>(chunks);
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
            ChunkDownloadResponse decodedMessage = m.getMessage();
            boolean isFinished = this.dManager.getAssemblingFile().saveChunk(decodedMessage.data, this.chunk);
            this.dManager.onChunkCompleted(this.chunk, this.userInfo, isFinished);
        }
    }
}

