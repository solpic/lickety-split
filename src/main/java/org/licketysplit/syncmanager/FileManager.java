package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class FileManager {

    public FileManager(){}

    //INITIALIZATION

    public void initializeFiles(String directoryLocation, String username){
        Path path = Paths.get(System.getProperty("user.home"), "LicketySplit");
        boolean directoryCreated = new File(path.toString()).mkdirs();
        if(!directoryCreated) { // If directory created then don't initialize, mostly for testing right now
            return;
        }
        this.initializeConfigs(directoryLocation, username);
        this.initializeManifest();
    }

    private void initializeConfigs(String directoryLocation, String username) {
        //Create's configs file and fills it with configs JSON object {sharedDirectory, username}
        File configs = new File(this.getConfigsPath( ".configs.txt"));
        try {
            configs.createNewFile();
            JsonToFile writer = new JsonToFile(configs);
            ConfigsInfo info = new ConfigsInfo(username, directoryLocation);
            writer.writeJSONObject(new JSONObject(info.toString()));
        } catch(IOException e){
            e.printStackTrace();
        }
    }

    private void initializeManifest() {
        //Create manifest and initialize with empty array
        try {
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            manifest.createNewFile();
            JsonToFile writer = new JsonToFile(manifest);
            JSONArray arr = new JSONArray("[]" );
            writer.writeJSONArray(arr);

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    //GETTERS

    private String getConfigsPath(String fileName){
        Path path = Paths.get(System.getProperty("user.home"), "LicketySplit", fileName);
        return path.toString();
    }

    public String getSharedDirectoryPath(){
        try{
            JSONObject obj = new JsonToFile(new File(this.getConfigsPath( ".configs.txt"))).getJSONObject();
            return obj.getString("sharedDirectory");
        } catch(IOException e){
            e.printStackTrace();
        }
        return "";
    }

    public String getUsername(){
        try{
            JSONObject obj = new JsonToFile(new File(this.getConfigsPath( ".manifest.txt"))).getJSONObject();
            return obj.getString("username");
        } catch(IOException e){
            e.printStackTrace();
        }

        return "";
    }

    //Adds file to manifest and folder
    public void addFile(String fileLocation){
        File source = new File(fileLocation);
        File dest = new File(this.getSharedDirectoryPath() + source.getName());
        try {
            FileInfo info = new FileInfo(source, new Date().getTime());
            this.addFileToManifest(info);
            this.addFileToFolder(source, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public void addFileToManifest(FileInfo fileInfo) throws IOException {
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        JSONArray arr = writer.getJSONArray();
        arr.put(new JSONObject(fileInfo.toString()));
        writer.writeJSONArray(arr);
    }

    private void addFileToFolder(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }

    //Updates file in manifest and in folder
    public void updateFile(String fileNameWithPath){
        Path path = Paths.get(fileNameWithPath);
        File newFile = new File(path.toString());
        File oldFile = new File(this.getSharedDirectoryPath() + path.getFileName());
        FileInfo info = new FileInfo(newFile, new Date().getTime());
        try {

            this.addFileToFolder(newFile, oldFile); // Hopefully this works...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void updateFileInManifest(FileInfo info){
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        JSONArray arr = writer.getJSONArray();
        arr.
        arr.put(new JSONObject(info.toString()));
        writer.writeJSONArray(arr);

    }
}
