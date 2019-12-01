package org.licketysplit.filesharer.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.filesharer.ShareableFile;
import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ChunkDownloadResponse extends Message {
    public byte[] data;

    public ChunkDownloadResponse() {}

    private String filePath;
    int chunk;
    Environment env;
    public ChunkDownloadResponse(String filePath, int chunk, Environment env){
        this.filePath = filePath;
        this.chunk = chunk;
        this.env = env;
    }


    @Override
    public byte[] toBytes() throws Exception {

        ShareableFile file = new ShareableFile(filePath, DownloadManager.chunkLengthRaw);

        try {
            return file.getChunk(chunk, env);
        } catch (Exception e){
            env.log("Error getting chunk", e);
            return null;
        }
    }

    @Override
    public void fromBytes(byte[] data) throws Exception {
        this.data = data;
    }
}
