package org.licketysplit.filesharer.messages;
import org.licketysplit.filesharer.PeerChunkInfo;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;

import java.io.File;

public class ChunkAvailabilityResponse extends JSONMessage {
    public PeerChunkInfo peerChunkInfo;

    public ChunkAvailabilityResponse(File file, FileInfo fileInfo) {
        this.peerChunkInfo = new PeerChunkInfo(file, fileInfo);
    }

    public ChunkAvailabilityResponse() {
        this.peerChunkInfo = new PeerChunkInfo();
    }
}
