package org.licketysplit.filesharer;

import org.licketysplit.securesocket.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

public class ShareableFile extends File {
    private long chunkSize;

    public ShareableFile(String pathname, long chunkSize){
        super(pathname);
        this.chunkSize = chunkSize;

    }

    public byte[] getChunk(int chunk) throws IOException {
        return new byte[1];
//        FileInputStream fis = new FileInputStream(this);
//        long offset = 0;
//        if(chunk > 0){
//            offset = this.getOffset(chunk);
//        }
//        int spaceNeeded = this.getSpaceNeeded(chunk, offset);
//            //A direct ByteBuffer should be slightly faster than a 'normal' one for IO-Operations
//        ByteBuffer bytes = ByteBuffer.allocateDirect(spaceNeeded);
//        fis.getChannel().read(bytes, offset);
//
//        byte[] readBytes = bytes.array();
//        fis.close();
//
//        return readBytes;
    }

    private long getOffset(int chunk){
        long length = this.length();
        long offset = chunk * this.chunkSize;
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
