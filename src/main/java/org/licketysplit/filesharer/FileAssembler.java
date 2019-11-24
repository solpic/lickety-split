package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.*;
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
    private IsFinished isFinished;


    public FileAssembler(FileInfo fileInfo, Environment env, int lengthInChunks, IsFinished isFinished) throws IOException {
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
        this.isFinished = isFinished;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public static boolean equals(byte[] a, byte[] a2) {
        if (a == a2)
            return true;
        if (a == null || a2 == null)
            return false;

        int length = a.length;
        if (a2.length != length)
            return false;

        for (int i = 0; i < length; i++)
            if (a[i] != a2[i])
                return false;

        return true;
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
                        System.out.println("CHECKING");
                        ShareableFile sf = new ShareableFile(System.getProperty("user.home") + "/2.png",1024);
                        byte[] check = sf.getChunk(chunk.chunk);
                        String checkStr = Base64.getEncoder().encodeToString(check);
                        String chunkStr = Base64.getEncoder().encodeToString(chunk.bytes);
                        ReceivedMessage m = chunk.m;
                        if(!chunkStr.equals(checkStr)){
                            System.out.println("CANCELING");
                            this.cancel();
                        }
                        this.file.seek(chunk.chunk * 1024);
                        this.file.write(chunk.bytes);
                        this.completed.add(chunk.chunk);
                        this.numOfChunks++;
                        System.out.println("CURRENT " + this.numOfChunks + " GOAL: " + this.lengthInChunks);
                    } else{
                        System.out.println("DUPLICATE");
                    }
                }
                if(this.chunks.isEmpty() && this.numOfChunks == this.lengthInChunks){
                    ArrayList<Integer> completedList = new ArrayList<Integer>(this.completed);
                    Collections.sort(completedList);
                    for(int i = 0; i < completedList.size(); i++){
                        if(completedList.get(i) != i){
                            System.out.println("wrong " + i);
                            return;
                        }
                    }
                    this.isFinished.setFinished(true);
                    this.file.close();
                    System.out.println("FINISHED AND PERFECT");
                    return;
                }
            }
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void saveChunk(ReceivedMessage m, byte[] data, int chunk){
        this.chunks.add(new Chunk(m, data, chunk));
    }

    public void cancel(){
        this.chunks.add(new Chunk(null, new byte[0], -1)); //Add "poison pill" to blocking queue
    }

}
