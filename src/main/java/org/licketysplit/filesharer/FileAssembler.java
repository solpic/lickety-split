package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class FileAssembler implements Runnable{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private int numOfChunks;
    private Environment env;
    private RandomAccessFile file;
    private BlockingQueue<Chunk> chunks;
    private HashSet<Integer> completed;
    private IsFinished isFinished;

    public File downloadToPath;
    DownloadManager mgr;
    public FileAssembler(FileInfo fileInfo, Environment env, int lengthInChunks, IsFinished isFinished, DownloadManager mgr) throws IOException {
        this.mgr = mgr;
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.numOfChunks = 0;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        if(temp.exists())temp.delete();
        temp.createNewFile();
        downloadToPath = temp;
        this.file = new RandomAccessFile(temp, "rw");
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.chunks = new LinkedBlockingQueue<Chunk>();
        this.completed = new HashSet<Integer>();
        this.isFinished = isFinished;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public AtomicLong lastChunkWritten;
    long getLastChunkWrittenTime() {
        if(lastChunkWritten!=null) {
            return lastChunkWritten.get();
        }else{
            return -1;
        }
    }
    public AtomicLong totalChunks = new AtomicLong(0);
    public AtomicLong chunksDownloaded = new AtomicLong(0);
    public int chunkSize() {
        return DownloadManager.chunkLengthRaw;
    }
    @Override
    public void run() {
        try {
            lastChunkWritten = new AtomicLong(-1);
            totalChunks.set(lengthInChunks);
            chunksDownloaded.set(0);
            Chunk chunk;
            //consuming messages until exit message is received
            while (true) {
                chunk = this.chunks.take();
                if (chunk != null) {
                    if(chunk.chunk == -1) return; //Download canceled
                    if(!this.completed.contains(chunk.chunk)) {
                        if(chunk.bytes!=null) {
                            this.file.seek(chunk.chunk * DownloadManager.chunkLengthRaw);
                            this.file.write(chunk.bytes);
                            this.completed.add(chunk.chunk);
                            this.numOfChunks++;
                            chunksDownloaded.set(this.numOfChunks);
                            lastChunkWritten.set(System.currentTimeMillis());
                            this.mgr.changed();
                        }else{
                            env.log("Chunk has null bytes");
                        }
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
                    this.isFinished.setFinished(true);
                    this.file.close();
                    env.log("FINISHED AND PERFECT to "+downloadToPath);
                    return;
                }
            }
        } catch (InterruptedException | IOException e) {
            env.log("Error during assembler", e);
        }
    }

    public void saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
    }

    public void cancel(){
        this.chunks.add(new Chunk(new byte[0], -1)); //Add "poison pill" to blocking queue
    }
}
