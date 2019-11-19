package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AssemblingFile implements Runnable{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private int numOfChunks;
    private Environment env;
    private RandomAccessFile file;
    private BlockingQueue<Chunk> chunks;


    public AssemblingFile(FileInfo fileInfo, Environment env, int lengthInChunks) throws IOException {
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.numOfChunks = 0;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        temp.createNewFile();
        this.file = new RandomAccessFile(temp, "rw");
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.chunks = new LinkedBlockingQueue<Chunk>();
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    @Override
    public void run() {
        try {
            Chunk chunk;
            //consuming messages until exit message is received
            while (true) {
                chunk = this.chunks.take();
                if (chunk != null) {
                    if(chunk.chunk == -1) break; //Download canceled
                    this.file.seek(chunk.chunk * 1024);
                    this.file.write(chunk.bytes);
                }
                if(this.chunks.isEmpty() && this.numOfChunks == this.lengthInChunks){
                    System.out.println("FINISHED AND BREAK");
                    break;
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
        this.numOfChunks++;
        if (this.numOfChunks == this.lengthInChunks){
            System.out.println("FINISHED");
            return true;
        }

        return false;

    }

    public void cancel(){
        this.chunks.add(new Chunk(new byte[0], -1)); //Add "poison pill" to blocking queue
    }
}
