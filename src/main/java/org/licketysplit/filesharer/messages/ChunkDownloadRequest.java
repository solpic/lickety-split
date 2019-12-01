package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.util.logging.Level;

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
            Environment env = m.getEnv();
            String requestedFileLocation = env.getDirectory(requestedFileName);
            try {
                m.respond(new ChunkDownloadResponse(requestedFileLocation, chunk, env), null);
            }catch (Exception e) {
                env.log("Error serving download", e);
            }
        }
    }
}
