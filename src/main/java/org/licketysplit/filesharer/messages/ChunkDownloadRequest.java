package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

public class ChunkDownloadRequest extends JSONMessage {
    public FileInfo fileInfo;
    public int chunk;

    public ChunkDownloadRequest() {}

    public ChunkDownloadRequest(FileInfo fileInfo, int chunk){
        this.chunk = chunk;
        this.fileInfo = fileInfo;
    }

    @DefaultHandler(type = ChunkDownloadRequest.class)
    public static class ChunkDownloadRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadRequest chunkDownloadRequest =  m.getMessage();
            String requestedFileName = chunkDownloadRequest.fileInfo.getName();
            int chunk = chunkDownloadRequest.chunk;
            // Break down file here with Shareable File
            //Iterate over chunks
            Environment env = m.getEnv();
            String requestedFileLocation = env.getDirectory(requestedFileName);
            try {
                m.respond(new ChunkDownloadResponse(requestedFileLocation, chunk), null);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
