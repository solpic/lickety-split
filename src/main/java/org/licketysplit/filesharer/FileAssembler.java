package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FileAssembler implements Runnable{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private int numOfChunks;
    private Environment env;
    private RandomAccessFile file;
    private BlockingQueue<Chunk> chunks;
    private HashSet<Integer> completed;


    public FileAssembler(FileInfo fileInfo, Environment env, int lengthInChunks) throws IOException {
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.numOfChunks = 0;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        if(temp.exists())temp.delete();
        temp.createNewFile();
        this.file = new RandomAccessFile(temp, "rw");
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.chunks = new LinkedBlockingQueue<Chunk>();
        this.completed = new HashSet<Integer>();
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
                    if(chunk.chunk == -1) return; //Download canceled
                    if(!this.completed.contains(chunk.chunk)) {
                        this.file.seek(chunk.chunk * 1024);
                        this.file.write(chunk.bytes);
                        this.completed.add(chunk.chunk);
                        this.numOfChunks++;
                        env.log("CURRENT " + this.numOfChunks + " GOAL: " + this.lengthInChunks);
                    } else{
                        env.log("DUPLICATE");
                    }
                }
                if(this.chunks.isEmpty() && this.numOfChunks == this.lengthInChunks){
                    ArrayList<Integer> completedList = new ArrayList<Integer>(this.completed);
                    Collections.sort(completedList);
                    for(int i = 0; i < completedList.size(); i++){
                        if(completedList.get(i) != i){
                            env.log("wrong " + i);
                            return;
                        }
                    }
                    env.log("FINISHED AND PERFECT");
                    return;
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public boolean saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
        if (this.numOfChunks == this.lengthInChunks){
            return true;
        }

        return false;

    }

    public void cancel(){
        this.chunks.add(new Chunk(new byte[0], -1)); //Add "poison pill" to blocking queue
    }
}
