package org.licketysplit.filesharer.messages;

import org.licketysplit.filesharer.ShareableFile;
import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ChunkDownloadResponse extends JSONMessage {
    public byte[] data;

    public ChunkDownloadResponse() {}

    public ChunkDownloadResponse(String filePath, int chunk){
        ShareableFile file = new ShareableFile(filePath, 1024);
        try {
            this.data = file.getChunk(chunk); // quick workaround
        } catch (IOException e){
            e.printStackTrace();
        }
    }


}
