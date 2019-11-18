package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AssemblingFile implements Runnable{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private ArrayList<Integer> completedChunks = new ArrayList<Integer>();
    private Environment env;
    private RandomAccessFile file;
    private boolean isFinished;
    private BlockingQueue<Chunk> chunks;


    public AssemblingFile(FileInfo fileInfo, Environment env, int lengthInChunks) throws IOException {
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        temp.createNewFile();
        this.file = new RandomAccessFile(temp, "rw");
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.isFinished = false;
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
                    System.out.println("WRITING CHUNK: " + chunk.chunk);
                    this.file.seek(chunk.chunk * 1024);
                    this.file.write(chunk.bytes);
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
        if (this.isFinished) return true;
        return false;

    }

    private void writeChunkToFile(byte[] data, int chunk) throws IOException{

    }
}
