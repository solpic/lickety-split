package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class ChunkAvailabilityRequest extends Message {
    public String fileName;

    @Override
    public byte[] toBytes() {
        return fileName.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.fileName = new String(data);
    }


    public ChunkAvailabilityRequest() {}

    public ChunkAvailabilityRequest(String fileName){
        this.fileName = fileName;
    }

    public static class FileRequestResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadResponse decodedMessage = (ChunkDownloadResponse) m.getMessage();
            try {
                System.out.println(decodedMessage.data);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class FileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkAvailabilityRequest tstMsg = (ChunkAvailabilityRequest) m.getMessage();
            String requestedFileName = tstMsg.fileName;
            try {
                m.respond(new ChunkDownloadResponse(requestedFileName), new FileRequestResponseHandler());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
