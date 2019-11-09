package org.licketysplit.filesharer;

import org.licketysplit.syncmanager.FileInfo;

public class AssemblingFile{

    private FileInfo fileInfo;

    public AssemblingFile(FileInfo fileInfo){
        this.fileInfo = fileInfo;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void saveChunk(byte[] data, int chunk){
        System.out.println("Saved Chunk: " + chunk);
    }
}
