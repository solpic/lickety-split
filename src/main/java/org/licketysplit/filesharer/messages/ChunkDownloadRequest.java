package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileManager;

public class ChunkDownloadRequest extends JSONMessage {
    public String fileName;

    public ChunkDownloadRequest() {}

    public ChunkDownloadRequest(String fileName){
        this.fileName = fileName;
    }

    @DefaultHandler(type = ChunkDownloadRequest.class)
    public static class ChunkDownloadRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadRequest chunkDownloadRequest =  m.getMessage();
            String requestedFileName = chunkDownloadRequest.fileName;
            // Break down file here with Shareable File
            //Iterate over chunks
            Environment env = m.getEnv();
            FileManager fM = env.getFM();
            String requestedFileLocation = env.getDirectory(requestedFileName);
            try {
                m.respond(new ChunkDownloadResponse(requestedFileLocation), null);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
