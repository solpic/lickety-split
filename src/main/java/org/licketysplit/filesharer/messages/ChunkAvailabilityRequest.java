package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.util.logging.Level;

public class ChunkAvailabilityRequest extends JSONMessage {
    public FileInfo fileInfo;

    public ChunkAvailabilityRequest() {}

    public ChunkAvailabilityRequest(FileInfo fileInfo){
        this.fileInfo= fileInfo;
    }

    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            m.log("RECEIVED");
            ChunkAvailabilityRequest tstMsg = m.getMessage();
            m.log("1");
            String requestedFileName = tstMsg.fileInfo.getName();
            m.log("2");
            Environment env = m.getEnv();
            FileManager fm = env.getFM();
            m.log("3");
            if( fm.hasFile(requestedFileName)) {
                try {
                    m.respond(new ChunkAvailabilityResponse(fm.getFile(requestedFileName), tstMsg.fileInfo), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    m.respond(new ChunkAvailabilityResponse(), null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
