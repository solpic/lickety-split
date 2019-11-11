package org.licketysplit.filesharer;

import org.licketysplit.securesocket.*;

import java.io.*;
import java.nio.ByteBuffer;

public class ShareableFile extends File {
    private int chunkSize;

    public ShareableFile(String pathname, int chunkSize){
        super(pathname);
        this.chunkSize = chunkSize;
    }

    public byte[] getChunk(int chunk) throws IOException {
        DataInputStream in = new DataInputStream(new FileInputStream(this));
        int offset = 0;
        if(chunk > 0){
            offset = this.getOffset(chunk);
        }
        int spaceNeeded = this.getSpaceNeeded(chunk, offset);
        byte[] bytes = new byte[spaceNeeded];
        for(int i = 0; i < chunk; i++){
            in.read(bytes);
        }
        in.read(bytes);

        in.close();

        return bytes;
    }

    private int getOffset(int chunk){
        long length = this.length();
        int offset = chunk * this.chunkSize;
        if(offset > length){
            return 0;
        }

        return offset;
    }

    private int getSpaceNeeded(int chunk, long offset){
        if(offset + this.chunkSize > this.length()){
            return (int)(this.length() - offset);
        }

        return (int)this.chunkSize;
    }

}
