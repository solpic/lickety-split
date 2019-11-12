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
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

public class DownloadManager{
    private AssemblingFile assemblingFile;
    private ArrayList<PeerDownloadInfo> peers;
    private ArrayList<Integer> availableChunks; //Simplify
    private ArrayList<Integer> completedChunks;
    private Environment env;
    private ReentrantLock lock;
    private Random r;


    public DownloadManager(FileInfo fileInfo, Environment env) throws IOException {
        this.assemblingFile = new AssemblingFile(fileInfo, env, this.getLengthInChunks(fileInfo));
        this.peers = new ArrayList<PeerDownloadInfo>();
        this.availableChunks = new ArrayList<Integer>();
        this.completedChunks = new ArrayList<Integer>();
        this.env = env;
        this.lock = new ReentrantLock();
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
    }

    private int getLengthInChunks(FileInfo fileInfo){
        long preciseChunks = fileInfo.getLength() / 1024;
        return (int) Math.ceil(preciseChunks);
    }

    public void addPeerAndRequestChunkIfPossible(PeerChunkInfo peerInfo, SecureSocket socket) throws Exception {
        this.peers.add(new PeerDownloadInfo(peerInfo, socket));
        this.updateAvailableChunks(peerInfo);
        this.requestRandomChunk();
    }

    public ArrayList<PeerDownloadInfo> getPeers(){
        return this.peers;
    }

    public void requestRandomChunk() throws Exception {
        if(availableChunks.size() == 0){
            return;
        }
        int chunk;
        this.lock.lock();
        try {
            int rand = this.r.nextInt(availableChunks.size());
            chunk = this.availableChunks.get(rand);
            this.availableChunks.remove(rand);
        } finally {
            this.lock.unlock();
        }

        PeerDownloadInfo peer = this.findOptimalPeer(chunk);
        if(peer == null){
            System.out.println("No peer found");
        }
        peer.addOpenRequest();
        this.sendDownloadRequest(chunk, peer);
        this.updatePeerList();
    }

    public void updatePeerList(){
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();


    }

    public void sendDownloadRequest(int chunk, PeerDownloadInfo peer) throws Exception {
        peer.getSocket().sendFirstMessage(new ChunkDownloadRequest(this.assemblingFile.getFileInfo(), chunk), new ChunkDownloadRequestHandler(peer, this.assemblingFile, chunk, this)); //need to close request and remove chunk
    }

    public void onChunkCompleted(int chunk, PeerDownloadInfo peer) throws Exception {
        peer.removeOpenRequest();
        this.completedChunks.add(chunk);
        this.requestRandomChunk();
    }

    public void updateAvailableChunks(PeerChunkInfo peerChunkInfo){
        int[] newChunks = peerChunkInfo.getChunks();
        Set<Integer> chunks = new HashSet<Integer>(this.availableChunks);
        for(int i = 0; i < newChunks.length; i++){
            chunks.add(newChunks[i]);
        }
        this.availableChunks = new ArrayList<Integer>(chunks);
    }

    private PeerDownloadInfo findOptimalPeer(int chunk){
        PeerDownloadInfo currOptimalPeer = null;
        while(currOptimalPeer == null){
            int rand = r.nextInt(this.peers.size());
            System.out.println("Curr peer: " + rand + " Curr size: " + this.peers.size());
            if(this.peers.get(rand).hasChunk(chunk)){
                currOptimalPeer = this.peers.get(rand);
            }
        }

        return currOptimalPeer;
    }

    private PeerDownloadInfo comparePeers(PeerDownloadInfo peerOne, PeerDownloadInfo peerTwo){
        if(peerOne == null){
            return peerTwo;
        }

        if(peerTwo.getOpenRequests() < peerOne.getOpenRequests()){
            //If peerTwo has less openRequests then  opt for them
            return peerTwo;
        }

        if(peerTwo.getOpenRequests() == peerOne.getOpenRequests() && peerOne.getChunksLength() > peerTwo.getChunksLength()){
            //If same amt of openRequests then opt for one with less available chunks
            return peerTwo;
        }

        return peerOne;
    }


    public static class ChunkDownloadRequestHandler implements MessageHandler {
        public PeerDownloadInfo peer;
        public AssemblingFile assemblingFile;
        public int chunk;
        public DownloadManager dManager;

        public ChunkDownloadRequestHandler(PeerDownloadInfo peer, AssemblingFile assemblingFile, int chunk, DownloadManager dManager){
            this.peer = peer;
            this.assemblingFile = assemblingFile;
            this.chunk = chunk;
            this.dManager = dManager;
        }

        @Override
        public void handle(ReceivedMessage m) throws IOException {
            ChunkDownloadResponse decodedMessage = m.getMessage();
            this.assemblingFile.saveChunk(decodedMessage.data, this.chunk);
            try {
                this.dManager.onChunkCompleted(this.chunk, this.peer);
            } catch(Exception e){
                e.printStackTrace();
            }
        }
    }
}

