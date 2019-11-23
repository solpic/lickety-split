package org.licketysplit.filesharer;
import java.io.*;

public class ShareableFile extends File{
    private int chunkSize;

    public ShareableFile(String pathname, int chunkSize) {
        super(pathname);
        this.chunkSize = chunkSize;
    }

    public synchronized byte[] getChunk(int chunk) throws IOException {
        System.out.println("OPEN");
        int offset = 0;
        if(chunk > 0) offset = this.getOffset(chunk); //RENAME, offset is misnomer
        int spaceNeeded = this.getSpaceNeeded(chunk, offset);
        byte[] bytes = new byte[spaceNeeded];
        RandomAccessFile raf = new RandomAccessFile(this, "r");
        raf.seek(chunk*1024);
        raf.read(bytes);
        raf.close();
        System.out.println("CLOSING");
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