package org.licketysplit.filesharer;

import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class AssemblingFile{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private ArrayList<Integer> chunks = new ArrayList<Integer>();
    private Environment env;
    private File intermediateFolder;

    public AssemblingFile(FileInfo fileInfo, Environment env, int lengthInChunks){
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.intermediateFolder = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name) + "temp");
        this.intermediateFolder.mkdir();



    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void saveChunk(byte[] data, int chunk) throws IOException {
        this.chunks.add(chunk);
        this.writeChunkToFile(data, chunk);
        System.out.println(this.chunks.size() + " --- " + this.lengthInChunks);
        if(this.chunks.size() == lengthInChunks){
            System.out.println("Compiling File");
            this.compileFile();
        }
    }

    private void writeChunkToFile(byte[] data, int chunk){
        File file = new File(this.env.getFM().getTempDirectoryPath(fileInfo.name + "temp", fileInfo.name + chunk));
        try {
            file.createNewFile();
            OutputStream os = new FileOutputStream(file);
            os.write(data);
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void compileFile() throws IOException {
        File [] chunks = this.intermediateFolder.listFiles();
        File completeFile = new File(this.env.getDirectory(this.fileInfo.name));
        OutputStream os = new FileOutputStream(completeFile);
        byte [] chunk = new byte[1024];
        for (int i = 0; i < chunks.length; i++) {
            DataInputStream in = new DataInputStream(new FileInputStream(chunks[i]));
            in.read(chunk);
            os.write(chunk);
            in.close();
        }
        os.close();
        FileUtils.deleteDirectory(this.intermediateFolder);
    }
}
