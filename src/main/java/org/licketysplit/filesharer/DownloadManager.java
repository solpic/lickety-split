package org.licketysplit.filesharer;
import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.SecureSocket;

import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.syncmanager.FileInfo;

import java.util.*;

public class DownloadManager {
    private AssemblingFile assemblingFile;
    private ArrayList<PeerDownloadInfo> peers;
    private ArrayList<Integer> availableChunks; //Simplify
    private ArrayList<Integer> completedChunks;

    public DownloadManager(FileInfo fileInfo){
        this.assemblingFile = new AssemblingFile(fileInfo);
        this.peers = new ArrayList<PeerDownloadInfo>();
        this.availableChunks = new ArrayList<Integer>();
        this.completedChunks = new ArrayList<Integer>();
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
        int rand = new Random().nextInt(availableChunks.size());
        int chunk = this.availableChunks.get(rand);

        PeerDownloadInfo peer = this.findOptimalPeer(chunk);
        if(peer == null){
            System.out.println("No peer found");
        }
        this.availableChunks.remove(rand);
        peer.addOpenRequest();
        this.sendDownloadRequest(chunk, peer);
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
        for (PeerDownloadInfo peer: this.peers) {
            if(peer.hasChunk(chunk)){
                currOptimalPeer = this.comparePeers(currOptimalPeer, peer);
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
        public void handle(ReceivedMessage m) {
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

