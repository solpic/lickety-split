package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileManager;

public class ChunkAvailabilityRequest extends JSONMessage {
    public String fileName;

    public ChunkAvailabilityRequest() {}

    public ChunkAvailabilityRequest(String fileName){
        this.fileName = fileName;
    }

    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkAvailabilityRequest tstMsg = m.getMessage();
            String requestedFileName = tstMsg.fileName;
            Environment env = m.getEnv();
            FileManager fm = env.getFM();
            if( fm.hasFile(requestedFileName)) {
                try {
                    m.respond(new ChunkAvailabilityResponse(true, requestedFileName), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    m.respond(new ChunkAvailabilityResponse(false, ""), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
