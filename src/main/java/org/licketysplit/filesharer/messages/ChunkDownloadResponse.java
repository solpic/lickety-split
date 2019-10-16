package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


public class ChunkDownloadResponse extends JSONMessage {
    public byte[] data;

    public ChunkDownloadResponse() {}

    public ChunkDownloadResponse(String filePath){
        File file = new File(filePath);
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
