package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.syncmanager.FileManager;

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
    public static class ChunkDownloadRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadRequest tstMsg = (ChunkDownloadRequest) m.getMessage();
            String requestedFileName = tstMsg.fileName;
            Environment env = m.getEnv();
            FileManager fM = env.getFM();
            String requestedFileLocation = fM.getSharedDirectoryPath();
            try {
                m.respond(new ChunkDownloadResponse(requestedFileLocation, requestedFileName), null);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
