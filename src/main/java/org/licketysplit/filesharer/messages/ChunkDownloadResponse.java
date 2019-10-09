package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ChunkDownloadResponse extends Message {
    public byte[] data;

    @Override
    public byte[] toBytes() {
        return data;
    }

    @Override
    public void fromBytes(byte[] data) {
        this.data = data;
    }

    public ChunkDownloadResponse() {}

    public ChunkDownloadResponse(String fileName){
        File file = new File(fileName);
        try {
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[(int)file.length()]; // quick workaround
            fis.read(buffer);
            this.data = buffer;
            fis.close();
        } catch (IOException e){
            e.printStackTrace();
        }
    }


}
