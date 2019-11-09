package org.licketysplit.filesharer;

import org.licketysplit.securesocket.SecureSocket;

public class PeerDownloadInfo {
    private int openRequests;
    private PeerChunkInfo peerChunkInfo;
    private SecureSocket socket;

    public PeerDownloadInfo(PeerChunkInfo peerChunkInfo, SecureSocket socket) {
        this.openRequests = 0;
        this.peerChunkInfo = peerChunkInfo;
        this.socket = socket;
    }

    public boolean hasChunk(int chunk) {
        return this.peerChunkInfo.hasChunk(chunk);
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
