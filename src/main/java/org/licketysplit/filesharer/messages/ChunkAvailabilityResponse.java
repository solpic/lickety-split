package org.licketysplit.filesharer.messages;
import org.licketysplit.filesharer.PeerChunkInfo;
import org.licketysplit.securesocket.messages.*;

import java.io.File;

public class ChunkAvailabilityResponse extends JSONMessage {
    public PeerChunkInfo peerChunkInfo;

    public ChunkAvailabilityResponse(File file) {
        this.peerChunkInfo = new PeerChunkInfo(file);
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
