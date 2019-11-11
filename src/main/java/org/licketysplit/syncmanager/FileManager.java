package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.licketysplit.env.Environment;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;

public class FileManager {

    private Environment env;

    public FileManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }

    //INITIALIZATION

    public void initializeFiles(String username){
        this.initializeConfigs(username);
        this.initializeManifest();
    }

    private void initializeConfigs(String username) {
        //Create's configs file and fills it with configs JSON object {sharedDirectory, username}
        new File(this.getConfigsPath()).mkdir();
        File configs = new File(this.getConfigsPath( ".configs.txt"));
        try {
            configs.createNewFile();
            JsonToFile writer = new JsonToFile(configs);
            ConfigsInfo info = new ConfigsInfo(username);
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
            JSONObject manifestObj = new JSONObject("{files: []}" );
            writer.writeJSONObject(manifestObj);

        } catch(IOException e){
            e.printStackTrace();
        }
    }

    // MISC

    public boolean hasFile(String fileName){
        return new File(this.getSharedDirectoryPath(fileName)).exists();
    }

    public File getFile(String fileName) {return new File(this.getSharedDirectoryPath(fileName));}

    //GETTERS

    private String getConfigsPath(){
        return this.env.getConfigs();
    }

    private String getConfigsPath(String fileName){
        return this.env.getConfigs(fileName);
    }

    public String getSharedDirectoryPath(String fileName){
        return this.env.getDirectory(fileName);
//        try{
//            JSONObject obj = new JsonToFile(new File(this.getConfigsPath( ".configs.txt"))).getJSONObject();
//            return obj.getString("sharedDirectory");
//        } catch(IOException e){
//            e.printStackTrace();
//        }
//        return "";
    }

    //For constructing the file, the file name is the folder and the parts are the file
    public String getTempDirectoryPath(String directoryName, String fileName) {
        return this.env.getTempDirectory(directoryName, fileName);
    }

    public JSONObject getManifest() throws IOException {
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        return writer.getJSONObject();
    }

    public void syncManifests(JSONObject theirManifest) throws IOException {
        JSONArray theirFiles = theirManifest.getJSONArray("files");
        JSONArray yourFiles = this.getManifest().getJSONArray("files");
        int theirLen = theirFiles.length();
        int yourLen = yourFiles.length();
        if(theirFiles != null && yourFiles != null){
            for(int i = 0; i < theirLen; i++){
                boolean alreadyExists = false;
                JSONObject theirs = new JSONObject(theirFiles.get(i).toString());
                for(int j = 0; j < yourLen; j++) {
                    JSONObject yours = new JSONObject(yourFiles.get(j).toString());
                    if (yours.getString("name").equals(theirs.getString("name"))) {
                        FileInfo laterStampFile = new FileInfo(this.compareTimestamp(yours, theirs));
                        if(laterStampFile.isDeleted()){
                            this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(laterStampFile.getName()));
                        }
                        yourFiles = this.replace(laterStampFile, yourFiles);
                        alreadyExists = true;
                        break;
                    }
                }
                if(!alreadyExists) {
                    yourFiles.put(theirs);
                }
            }
        }

        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        theirManifest.put("files", yourFiles);
        writer.writeJSONObject(theirManifest);
    }

    private JSONObject compareTimestamp(JSONObject fileA, JSONObject fileB){
        return fileA.getLong("timeStamp") > fileB.getLong("timeStamp") ? fileA : fileB;
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
    public FileInfo addFile(String fileLocation){
        File source = new File(fileLocation);
        File dest = new File(this.getSharedDirectoryPath(source.getName()));
        FileInfo info = new FileInfo(source, new Date().getTime());
        try {
            this.addFileToManifest(info);
            this.addFileToFolder(source, dest);

            return info;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return info;

    }

    public void addFileToManifest(FileInfo fileInfo) throws IOException {
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        JSONObject files = writer.getJSONObject();
        JSONArray filesArray = files.getJSONArray("files");
        filesArray.put(new JSONObject(fileInfo));
        writer.writeJSONObject(files);
    }

    private void addFileToFolder(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }

    //Updates file in manifest and in folder
    public void updateFile(String fileNameWithPath){
        Path path = Paths.get(fileNameWithPath);
        File newFile = new File(path.toString());
        File oldFile = new File(this.env.getDirectory(path.getFileName().toString()));
        FileInfo info = new FileInfo(newFile, new Date().getTime());
        try {
            this.updateFileInManifest(info);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            this.addFileToFolder(newFile, oldFile); // Hopefully this works...
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void deleteFile(FileInfo info) throws IOException {
        this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(info.getName()));
        this.updateFileInManifest(info);
    }

    public void updateFileInManifest(FileInfo info) throws IOException {
        File manifest = new File(this.getConfigsPath(".manifest.txt"));
        JsonToFile writer = new JsonToFile(manifest);
        JSONObject files = writer.getJSONObject();
        JSONArray filesArray = files.getJSONArray("files");
        filesArray = this.replace(info, filesArray);
        files.put("files", filesArray);
        writer.writeJSONObject(files);
    }

    public void deleteFileFromFolderIfExists(String fileNameWithPath) throws IOException {
        if(new File(fileNameWithPath).exists()) FileUtils.forceDelete(new File(fileNameWithPath));
    }

    //replace old fileInfo with new fileInfo (info)
    private JSONArray replace(FileInfo info, JSONArray arr){
        JSONArray newArr = new JSONArray();
        int len = arr.length();
        if (arr != null) {
            for (int i=0;i<len;i++)
            {
                //If names are equal then replace with new file info
                if (new JSONObject(arr.get(i).toString()).getString("name").equals(info.getName()))
                {
                    newArr.put(new JSONObject(info));
                } else {
                    newArr.put(new JSONObject(arr.get(i).toString()));
                }
            }
        }
        return newArr;
    }
}
