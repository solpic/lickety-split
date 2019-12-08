package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.filesharer.ShareableFile;
import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

/**
 * Response to ChunkDownloadRequest message.
 *
 * Sends back the a part of a file as a byte array.
 * Files are broken up into 500 kbyte chunks (although this size could be changed,
 * 500 bytes was found to be a good size for download speed).
 * So chunk 0 is kbytes 0-499, chunk 1 is kbytes 500-999, chunk 2 is kbytes 1000-1499, etc..
 */
public class ChunkDownloadResponse extends Message {
    /**
     * Used to store the bytes of the file when this message is received.
     */
    public byte[] data;

    /**
     * Instantiates a new Chunk download response.
     */
    public ChunkDownloadResponse() {}

    /**
     * Path of file.
     */
    private String filePath;
    /**
     * Index of chunk.
     */
    int chunk;
    /**
     * This peer's environment.
     */
    Environment env;
    /**
     * Filename for looking up in manifest later on.
     */
    String filename;

    /**
     * Instantiates a new Chunk download response.
     *
     * @param filename the filename
     * @param filePath the file path
     * @param chunk    the chunk
     * @param env      the env
     */
    public ChunkDownloadResponse(String filename, String filePath, int chunk, Environment env){
        this.filename = filename;
        this.filePath = filePath;
        this.chunk = chunk;
        this.env = env;
    }


    /**
     * Serializer function. When the message is actually written to the TCP socket
     * this function is called to convert the message to a byte array.
     * In this case we are simply returning the byte array corresponding
     * to this particular chunk.
     *
     * It is notable that we only read from the file here, rather than in the constructor.
     * This way, we only store the chunk in RAM while we are sending it over the TCP socket,
     * rather than storing many chunks in RAM before they have actually been sent.
     * @return byte array representing chunk
     * @throws Exception
     */
    @Override
    public byte[] toBytes() throws Exception {
        // Create reference to file and read bytes corresponding to chunk
        ShareableFile file = new ShareableFile(filename, filePath, DownloadManager.chunkLengthRaw);

        try {
            return file.getChunk(chunk, env);
        } catch (Exception e){
            env.log("Error getting chunk", e);
            return null;
        }
    }

    /**
     * Deserializes this message from the raw bytes sent over the TCP socket
     * In this case, since we are simply sending the chunk raw, we simply store the chunk.
     * @param data the chunk byte array
     * @throws Exception
     */
    @Override
    public void fromBytes(byte[] data) throws Exception {
        this.data = data;
    }
}
