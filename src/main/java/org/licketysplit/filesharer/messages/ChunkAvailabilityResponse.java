package org.licketysplit.filesharer.messages;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.filesharer.PeerChunkInfo;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.File;
import java.util.logging.Level;

public class ChunkAvailabilityResponse extends JSONMessage {
    public PeerChunkInfo peerChunkInfo;

    public ChunkAvailabilityResponse(Environment env, File file, FileInfo fileInfo) {
        env.getLogger().log(Level.INFO, "HAS CHUNKS OF LENGTH: " + env.getFS().getChunksLength(fileInfo));
        this.peerChunkInfo = env.getFS().downloadInProgress(fileInfo) ? new PeerChunkInfo(env.getFS().getChunks(fileInfo), fileInfo) : new PeerChunkInfo(file, fileInfo);
    }

    public ChunkAvailabilityResponse() {
        this.peerChunkInfo = new PeerChunkInfo();
    }
}
