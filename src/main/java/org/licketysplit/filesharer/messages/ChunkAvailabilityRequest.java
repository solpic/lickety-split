package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.util.logging.Level;

/**
 * This message is sent out during downloads. At the beginning of downloads
 * and at 15 second intervals, this message is sent out to all peers.
 *
 * It is asking a peer what chunks they have for a given file. If a peer
 * has downloaded the file fully, that will be all chunks.
 *
 * If the peer is currently downloading a file, that will only be the chunks
 * that have been downloaded so far.
 */
public class ChunkAvailabilityRequest extends JSONMessage {
    /**
     * The file that information is being requested for.
     */
    public FileInfo fileInfo;

    /**
     * Instantiates a new Chunk availability request.
     */
    public ChunkAvailabilityRequest() {}

    /**
     * Instantiates a new Chunk availability request.
     *
     * @param fileInfo the file metadata
     */
    public ChunkAvailabilityRequest(FileInfo fileInfo){
        this.fileInfo= fileInfo;
    }

    /**
     * Default handler for this message.
     *
     * The default handler will check if the file exists in the filesystem,
     * which means it has either been downloaded or is currently downloading.
     * hen it responds with a list of all the chunks this peer currently has.
     */
    @DefaultHandler(type = ChunkAvailabilityRequest.class)
    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        /**
         * Called when response is received
         * @param m the response
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkAvailabilityRequest tstMsg = m.getMessage();

            // The file being requested
            String requestedFileName = tstMsg.fileInfo.getName();
            Environment env = m.getEnv();
            FileManager fm = env.getFM();

            // Check if the file is in the filesystem
            if( fm.hasFile(requestedFileName)) {
                env.log("Has chunks for "+requestedFileName);
                try {
                    // If so, respond with available chunks
                    m.respond(new ChunkAvailabilityResponse(env, fm.getFile(requestedFileName), tstMsg.fileInfo), null);
                } catch (Exception e) {
                    env.log("Error handling chunk availability", e);
                }
            } else {
                env.log("Doesn't have chunks");
                try {
                    m.respond(new ChunkAvailabilityResponse(), null);
                } catch (Exception e) {
                    env.log("Error handling non-chunk availability", e);
                }
            }
        }
    }
}
