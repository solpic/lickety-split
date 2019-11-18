package org.licketysplit.filesharer;

import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class AssemblingFile{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private ArrayList<Integer> chunks = new ArrayList<Integer>();
    private Environment env;
    private RandomAccessFile file;
    private boolean isFinished;

    public AssemblingFile(FileInfo fileInfo, Environment env, int lengthInChunks) throws IOException {
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        temp.createNewFile();
        this.file = new RandomAccessFile(temp, "rw");
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.isFinished = false;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public boolean saveChunk(byte[] data, int chunk) throws IOException {
        if(this.isFinished) return true;
        this.chunks.add(chunk);
        this.writeChunkToFile(data, chunk);
        if(this.chunks.size() == lengthInChunks){
            this.isFinished = true;
        }

        return this.isFinished;
    }

    private void writeChunkToFile(byte[] data, int chunk) throws IOException{
        this.file.seek(chunk*1024);
        this.file.write(data);
    }
}
