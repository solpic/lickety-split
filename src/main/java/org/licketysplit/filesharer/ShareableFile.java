package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;

import java.io.*;
import java.util.logging.Level;

public class ShareableFile extends File{
    private int chunkSize;

    public ShareableFile(String pathname, int chunkSize) {
        super(pathname);
        this.chunkSize = chunkSize;
    }

    public synchronized byte[] getChunk(int chunk, Environment env) throws Exception {
        int delay = 5000;
        while(true) {
            try {
                int offset = 0;
                if (chunk > 0) offset = this.getOffset(chunk); //RENAME, offset is misnomer
                int spaceNeeded = this.getSpaceNeeded(chunk, offset);
                byte[] bytes = new byte[spaceNeeded];
                RandomAccessFile raf = new RandomAccessFile(this, "r");
                raf.seek(chunk * DownloadManager.chunkLengthRaw);
                raf.readFully(bytes);
                raf.close();
                return bytes;
            } catch (OutOfMemoryError e) {
                env.log(
                        String.format("Out of memory while downloading chunk, waiting %d seconds", delay/1000)
                );
                Thread.sleep(delay);
                delay *= 2;
            }
        }
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
