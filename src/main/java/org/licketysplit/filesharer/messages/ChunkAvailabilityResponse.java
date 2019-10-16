package org.licketysplit.filesharer.messages;
import org.licketysplit.securesocket.messages.*;

public class ChunkAvailabilityResponse extends JSONMessage {
    public boolean hasFile;
    public String fileName;

    public ChunkAvailabilityResponse() {
    }

    public ChunkAvailabilityResponse(boolean hasFile, String fileName) {
        this.hasFile = hasFile;
        this.fileName = fileName;
    }

    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class ChunkAvailabilityResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkAvailabilityResponse req = m.getMessage();
            boolean hasFile = req.hasFile;
            String fileName = req.fileName;
            if( hasFile ){
                m.respond( new ChunkDownloadRequest(fileName), null);
                // Send Chunk Download Request
            }
        }
    }
}
