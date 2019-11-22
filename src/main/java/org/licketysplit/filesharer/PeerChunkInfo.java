package org.licketysplit.filesharer;
import org.licketysplit.syncmanager.FileInfo;

import java.io.File;
import java.util.ArrayList;

public class PeerChunkInfo {
    private ArrayList<Integer> chunks;
    private FileInfo fileInfo;

    public PeerChunkInfo(){
        this.chunks = new ArrayList<Integer>();
    }

    public PeerChunkInfo(File file, FileInfo fileInfo){
         this.fileInfo = fileInfo;
         double chunkSize = 1024;
         double preciseChunks = file.length() / chunkSize;
         int chunks;
         if(fileInfo.getLength() == file.length()) chunks = (int) Math.ceil(preciseChunks);
         else chunks = (int) Math.floor(preciseChunks);
         this.chunks = new ArrayList<Integer>();
         for(int i = 0; i < chunks; i++){
             this.chunks.add(i);
         }
    }

    public boolean hasChunk(int chunk){
        for(int i = 0; i < this.chunks.size(); i++){
            if(this.chunks.get(i) == chunk){
                return true;
            }
        }
        return false;
    }

    public int chunksLength(){
        return this.chunks.size();
    }

    public ArrayList<Integer> getChunks() {
        return chunks;
    }
}
