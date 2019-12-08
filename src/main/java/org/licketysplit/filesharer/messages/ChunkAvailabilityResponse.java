package org.licketysplit.filesharer.messages;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.filesharer.PeerChunkInfo;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;

import java.io.File;
import java.util.logging.Level;

/**
 * This message is a response to the ChunkAvailabilityRequest.
 *
 * If the file in question is already downloaded, it replies saying
 * the peer has all chunks. If the file download is in progress,
 * it checks with the DownloadManager instance and replies with
 * the chunks already downloaded and written by the download manager.
 */
public class ChunkAvailabilityResponse extends JSONMessage {
    /**
     * Payload containing information about which chunks are available.
     */
    public PeerChunkInfo peerChunkInfo;

    /**
     * Constructor that checks status of file and correctly assigns peerChunkInfo.
     * If download is in progress, then ask download manager which chunks we have
     * otherwise, say we have all chunks.
     *
     * @param env      the env
     * @param file     the file
     * @param fileInfo file metadata
     */
    public ChunkAvailabilityResponse(Environment env, File file, FileInfo fileInfo) {
        // If download is in progress, then ask download manager which chunks we have
        // otherwise, say we have all chunks
        this.peerChunkInfo = env.getFS().downloadInProgress(fileInfo) ? new PeerChunkInfo(env.getFS().getChunks(fileInfo), fileInfo) : new PeerChunkInfo(file, fileInfo);
    }

    /**
     * Default constructor.
     */
    public ChunkAvailabilityResponse() {
        this.peerChunkInfo = new PeerChunkInfo();
    }
}
