package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.util.logging.Level;

/**
 * Part of the download process.
 *
 * Requests a specific chunk, defined by a file and the index of a chunk, from a peer.
 * The peer will respond with the contents of the chunk.
 *
 * For example, if peer A requests chunk 1, file: tmp.txt from peer B, peer B will
 * respond with 500 kilobytes from tmp.txt, starting from the 501st byte.
 */
public class ChunkDownloadRequest extends JSONMessage {
    /**
     * File that chunk is requested from.
     */
    public FileInfo fileInfo;
    /**
     * Index of chunk.
     */
    public int chunk;

    /**
     * Instantiates a new Chunk download request.
     */
    public ChunkDownloadRequest() {}

    /**
     * Instantiates a new Chunk download request.
     *
     * @param fileInfo the file info
     * @param chunk    the chunk
     */
    public ChunkDownloadRequest(FileInfo fileInfo, int chunk){
        this.chunk = chunk;
        this.fileInfo = fileInfo;
    }

    /**
     * Default handler for this request.
     *
     * Gets the location of the file and sends back a chunk download response..
     */
    @DefaultHandler(type = ChunkDownloadRequest.class)
    public static class ChunkDownloadRequestHandler implements MessageHandler {
        /**
         * Called when response is received
         * @param m the response
         */
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadRequest chunkDownloadRequest =  m.getMessage();

            // Get filename and chunk index
            String requestedFileName = chunkDownloadRequest.fileInfo.getName();
            int chunk = chunkDownloadRequest.chunk;
            Environment env = m.getEnv();

            // Get actual location of file in filesystem
            String requestedFileLocation = env.getDirectory(requestedFileName);
            try {
                // Send back chunk download response
                m.respond(new ChunkDownloadResponse(requestedFileName, requestedFileLocation, chunk, env), null);
            }catch (Exception e) {
                env.log("Error serving download", e);
            }
        }
    }
}
