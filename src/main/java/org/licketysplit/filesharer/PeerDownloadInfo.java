package org.licketysplit.filesharer;

import com.google.common.collect.Sets;
import org.licketysplit.securesocket.SecureSocket;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

/**
 * Stores information about a peer's available chunks.
 */
public class PeerDownloadInfo {
    /**
     * Stores available chunks.
     */
    private PeerChunkInfo peerChunkInfo;
    /**
     * Socket for this peer.
     */
    private SecureSocket socket;
    /**
     * Random number generator.
     */
    private Random r;
    /**
     * Not used.
     */
    private boolean inUse;

    /**
     * Check if peer has chunk.
     *
     * @param c index of chunk
     * @return whether the peer has the chunk
     */
    public boolean hasChunk(int c) {
        return peerChunkInfo.hasChunk(c);
    }

    /**
     * Instantiates a new Peer download info.
     *
     * @param peerChunkInfo the peer chunk info
     * @param socket        the socket of this peer
     */
    public PeerDownloadInfo(PeerChunkInfo peerChunkInfo, SecureSocket socket) {
        this.peerChunkInfo = peerChunkInfo;
        this.socket = socket;
        this.inUse = false;
        this.r = new Random();
        this.r.setSeed(System.currentTimeMillis());
    }

    /**
     * Not used.
     *
     * @param inUse the in use
     */
    public void setInUse(boolean inUse){
        this.inUse = inUse;
    }

    /**
     * Not used.
     *
     * @return the boolean
     */
    public boolean getInUse(){
        return this.inUse;
    }


    /**
     * Not used.
     *
     * @param availableChunks the available chunks
     * @return the int
     */
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

    /**
     * Gets socket.
     *
     * @return the socket
     */
    public SecureSocket getSocket() {
        return socket;
    }
}
