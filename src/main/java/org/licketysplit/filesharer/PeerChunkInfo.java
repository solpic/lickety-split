package org.licketysplit.filesharer;
import org.licketysplit.syncmanager.FileInfo;

import java.io.File;
import java.util.ArrayList;

/**
 * Stores information about what chunks a peer has.
 */
public class PeerChunkInfo {
    /**
     * List of what chunks a peer has.
     */
    private ArrayList<Integer> chunks;
    /**
     * File that this is in reference to.
     */
    private FileInfo fileInfo;

    /**
     * Instantiates a new Peer chunk info.
     */
    public PeerChunkInfo(){
        this.chunks = new ArrayList<Integer>();
    }

    /**
     * Initialize class with all chunks.
     *
     * @param file     the file
     * @param fileInfo the file metadata
     */
    public PeerChunkInfo(File file, FileInfo fileInfo){
         this.fileInfo = fileInfo;
         double chunkSize = DownloadManager.chunkLengthRaw;
         double preciseChunks = file.length() / chunkSize;
         int chunks;
         if(fileInfo.getLength() == file.length()) chunks = (int) Math.ceil(preciseChunks);
         else chunks = (int) Math.floor(preciseChunks);
         this.chunks = new ArrayList<Integer>();
         for(int i = 0; i < chunks; i++){
             this.chunks.add(i);
         }
    }

    /**
     * Instantiates a new Peer chunk info with specified chunks.
     *
     * @param chunks   the chunks
     * @param fileInfo the file metadata
     */
    public PeerChunkInfo(ArrayList<Integer> chunks, FileInfo fileInfo){
        this.chunks = chunks;
        this.fileInfo = fileInfo;
    }

    /**
     * Check if peer has chunk.
     *
     * @param chunk the chunk
     * @return whether peer has the chunk
     */
    public boolean hasChunk(int chunk){
        return this.chunks.contains(chunk);
    }

    /**
     * Not used.
     *
     * @return the int
     */
    public int chunksLength(){
        return this.chunks.size();
    }

    /**
     * Gets chunks.
     *
     * @return the chunks
     */
    public ArrayList<Integer> getChunks() {
        return chunks;
    }
}
