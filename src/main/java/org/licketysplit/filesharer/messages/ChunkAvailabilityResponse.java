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

    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class ChunkAvailabilityResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) throws Exception {
        }
    }
}
