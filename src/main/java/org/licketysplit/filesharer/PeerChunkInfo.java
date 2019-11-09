package org.licketysplit.filesharer;
import org.licketysplit.syncmanager.FileInfo;

import java.io.File;

public class PeerChunkInfo {
    private int[] chunks;

    public PeerChunkInfo(){
        this.chunks = new int[0];
    }

    public PeerChunkInfo(File file){
         long preciseChunks = file.length() / 1024;
         int chunks = (int) Math.ceil(preciseChunks);
         this.chunks = new int[chunks];
         for(int i = 0; i < chunks; i++){
             this.chunks[i] = i;
         }
    }

    public boolean hasChunk(int chunk){
        for(int i = 0; i < this.chunks.length; i++){
            if(this.chunks[i] == chunk){
                return true;
            }
        }
        return false;
    }

    public int chunksLength(){
        return this.chunks.length;
    }

    public int[] getChunks() {
        return chunks;
    }
}
