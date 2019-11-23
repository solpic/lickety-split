package org.licketysplit.filesharer;

import com.google.common.collect.Sets;
import org.licketysplit.securesocket.SecureSocket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class PeerDownloadInfo {
    private PeerChunkInfo peerChunkInfo;
    private SecureSocket socket;
    private Random r;
    private boolean inUse;

    public PeerDownloadInfo(PeerChunkInfo peerChunkInfo, SecureSocket socket) {
        this.peerChunkInfo = peerChunkInfo;
        this.socket = socket;
        this.inUse = false;
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
    }

    public void setInUse(boolean inUse){
        this.inUse = inUse;
    }

    public boolean getInUse(){
        return this.inUse;
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

    public SecureSocket getSocket() {
        return socket;
    }
}
