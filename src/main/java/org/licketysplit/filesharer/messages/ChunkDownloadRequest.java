package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class ChunkDownloadRequest extends Message {
    public String fileName;

    @Override
    public byte[] toBytes() {
        return fileName.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.fileName = new String(data);
    }


    public ChunkDownloadRequest() {}

    public ChunkDownloadRequest(String fileName){
        this.fileName = fileName;
    }

    @DefaultHandler(type = ChunkDownloadRequest.class)
    public static class FileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadRequest tstMsg = (ChunkDownloadRequest) m.getMessage();
            String requestedFileName = tstMsg.fileName;
            try {
                m.respond(new ChunkDownloadResponse(requestedFileName), null);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
