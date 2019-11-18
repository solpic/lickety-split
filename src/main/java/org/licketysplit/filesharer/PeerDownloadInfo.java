package org.licketysplit.filesharer;

import com.google.common.collect.Sets;
import org.licketysplit.securesocket.SecureSocket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PeerDownloadInfo {
    private int openRequests;
    private PeerChunkInfo peerChunkInfo;
    private SecureSocket socket;
    private Random r;

    public PeerDownloadInfo(PeerChunkInfo peerChunkInfo, SecureSocket socket) {
        this.openRequests = 0;
        this.peerChunkInfo = peerChunkInfo;
        this.socket = socket;
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
    }

    public boolean hasChunk(int chunk) {
        return this.peerChunkInfo.hasChunk(chunk);
    }

    public int getRandomDesirableChunk(ArrayList<Integer> availableChunks){
        Set<Integer> availableChunkSet = new HashSet<Integer>(availableChunks);
        Set<Integer> peerChunkSet = new HashSet<Integer>(peerChunkInfo.getChunks());
        Object[] desirableChunkSet = Sets.intersection(availableChunkSet, peerChunkSet).toArray();
        if(desirableChunkSet.length > 0){
            int randomChunk = this.r.nextInt(desirableChunkSet.length);
            return (int) desirableChunkSet[randomChunk];
        }

        return -1;
    }

    public int getChunksLength(){
        return this.peerChunkInfo.chunksLength();
    }

    public void addOpenRequest(){
        this.openRequests++;
    }

    public void removeOpenRequest(){
        this.openRequests--;
    }

    public int getOpenRequests(){
        return this.openRequests;
    }

    public PeerChunkInfo getPeerChunkInfo() {
        return peerChunkInfo;
    }

    public SecureSocket getSocket() {
        return socket;
    }
}
