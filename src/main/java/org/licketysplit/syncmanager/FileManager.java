package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.nio.file.Path;
import java.nio.file.Paths;

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
        try {
            new File(this.getConfigsPath(".manifest.txt")).createNewFile();
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
        File manifest = new File(this.getConfigsPath(".manifest.txt"));

        try {
            JsonToFile writer = new JsonToFile(manifest);
            FileInfo info = new FileInfo(source);
            if(manifest.length() == 0) { //If manifest not yet created
                JSONArray arr = new JSONArray("[" + info.toString() + "]" );
                writer.writeJSONArray(arr);
                this.addFileHelper(source, dest);
            } else {
                JSONArray arr = writer.getJSONArray();
                arr.put(new JSONObject(info.toString()));
                writer.writeJSONArray(arr);
                this.addFileHelper(source, dest);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void addFileHelper(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }

    public void UpdateFileInManifest(String fileNameWithPath){
        Path path = Paths.get(fileNameWithPath);
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        File newFile = new File(path.toString());
        File oldFile = new File(this.getSharedDirectoryPath() + path.getFileName());
        try {
            JsonToFile writer = new JsonToFile(manifest);
            FileInfo info = new FileInfo(newFile);
            JSONArray arr = writer.getJSONArray();
            arr.put(new JSONObject(info.toString()));
            writer.writeJSONArray(arr);
            this.addFileHelper(newFile, oldFile); // Hopefully this works...
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
