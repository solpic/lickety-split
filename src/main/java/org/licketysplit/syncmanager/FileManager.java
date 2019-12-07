package org.licketysplit.syncmanager;

import java.io.File;
import java.io.*;

import org.apache.commons.io.*;
import org.json.JSONArray;
import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class FileManager {

    private Environment env;

    public FileManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }

    //INITIALIZATION

    public void initializeFiles(String username){
        this.initializeConfigs(username);
    }

    DownloadChanged downloadChanged = null;
    Object downloadChangedLock = new Object();

    public void setDownloadChanged(DownloadChanged c) {
        synchronized (downloadChangedLock) {
            downloadChanged = c;
        }
    }

    public void triggerDownloadUpdate(FileInfo info) {
        synchronized (downloadChangedLock) {
            if(downloadChanged!=null) {
                downloadChanged.changed(info);
            }
        }
    }

    public interface DownloadChanged {
        void changed(FileInfo info);
    }

    public interface ManifestChanged {
        void changed();
    }

    ManifestChanged guiChanges = null;
    Object guiChangesLock = new Object();
    public void setChangeHandler(ManifestChanged c) {
        synchronized (guiChangesLock) {
            guiChanges = c;
        }
    }

    public boolean triggerGUIChanges() {
        synchronized (guiChangesLock) {
            if(guiChanges!=null) {
                guiChanges.changed();
                return true;
            }
            return false;
        }
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
            env.log("Init error", e);
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
            env.log("Init manifest error", e);
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

    public static class FileStatus {
        public boolean hasFile = false;
        public boolean fileCorrupted = false;
        public boolean isDownloading = false;
        public float downloadProgress = 0.0f;
        public float downloadSpeed = 0.0f;
        public String name;
        public float size;
        public boolean deleted = false;
    }

    public FileStatus getFileStatus(FileInfo info) throws Exception {
        FileStatus fileStatus = new FileStatus();
        fileStatus.name = info.name;
        String name = info.name;
        DownloadManager downloadManager = env.getFS().currentProgress(name);
        if(downloadManager!=null) {
            fileStatus.isDownloading = true;
            fileStatus.downloadProgress = downloadManager.getProgress();
            fileStatus.downloadSpeed = downloadManager.getSpeed();
            fileStatus.size = downloadManager.getFileAssembler().totalChunks.get()*DownloadManager.chunkLengthRaw;
            return fileStatus;
        }
        JSONObject manifest = env.getFM().getManifest();
        Map entry = (Map)manifest.getJSONArray("files").toList()
                .stream()
                .filter(e -> ((Map)e).get("name").equals(info.name))
                .collect(Collectors.toList())
                .get(0);



        File file = new File(env.getFM().getSharedDirectoryPath(name));
        fileStatus.size = (Integer)entry.get("length");
        fileStatus.deleted = (Boolean)entry.get("deleted");
        if(file.exists()) {
            fileStatus.hasFile = true;

            try {
                String md5 = env.getSyncManager().getMD5(file);
                if (!md5.equals(entry.get("md5"))) {
                    fileStatus.fileCorrupted = true;
                    file.delete();
                    fileStatus.hasFile = false;
                    fileStatus.fileCorrupted = false;
                }
            } catch(Exception e) {
                env.getLogger().log(Level.INFO,
                        "Error checking md5",
                        e);
                fileStatus.fileCorrupted = true;
            }
        }
        return fileStatus;
    }

    //For constructing the file, the file name is the folder and the parts are the file
    public String getTempDirectoryPath(String directoryName, String fileName) {
        return this.env.getTempDirectory(directoryName, fileName);
    }

    public JSONObject getManifest() throws IOException {
        synchronized(manifestLock) {
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            try {
                if (!manifest.exists() || FileUtils.readFileToString(manifest, "UTF-8").equals("")) {
                    env.log("WRITING NEW MANIFEST");
                    FileUtils.writeStringToFile(manifest, "{\"files\":[]}", "UTF-8");
                }
                JsonToFile writer = new JsonToFile(manifest);
                return writer.getJSONObject();
            } catch (Exception e) {
                String manifestStr = FileUtils.readFileToString(manifest, "UTF-8");
                env.getLogger().log(Level.INFO,
                        String.format("Exception getting manifest, contents '%s'", manifestStr), e);
                System.exit(1);
                //FileUtils.writeStringToFile(manifest, "{\"files\":[]}", "UTF-8");
                try {
                    JsonToFile writer = new JsonToFile(manifest);
                    return writer.getJSONObject();
                } catch (Exception e2) {
                    throw e2;
                }
            }
        }
    }

    Object manifestLock = new Object();
    public boolean autoDownloadNewFiles = false;
    public boolean autoDownloadUpdates = false;
    public boolean syncManifests(JSONObject theirManifest, String theirUsername) throws IOException {
        synchronized (manifestLock) {
            env.log("Syncing manifest");
            JSONArray theirFiles = theirManifest.getJSONArray("files");
            JSONArray yourFiles = this.getManifest().getJSONArray("files");
            String theirManifestOriginal = theirManifest.toString(4);
            String yourManifestOriginal = this.getManifest().toString(4);
            int theirLen = theirFiles.length();
            int yourLen = yourFiles.length();
            boolean changed = false;
            if (theirFiles != null && yourFiles != null) {
                for (int i = 0; i < theirLen; i++) {
                    boolean alreadyExists = false;
                    JSONObject theirs = new JSONObject(theirFiles.get(i).toString());
                    for (int j = 0; j < yourLen; j++) {
                        JSONObject yours = new JSONObject(yourFiles.get(j).toString());
                        if (yours.getString("name").equals(theirs.getString("name"))) {
                            FileInfo laterStampFile = new FileInfo(this.compareTimestamp(yours, theirs));
                            if (laterStampFile.isDeleted()) {
                                this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(laterStampFile.getName()));
                            }else{
                                if(this.compareTimestamp(yours, theirs)!=yours) {
                                    if(!laterStampFile.isDeleted()) {
                                        changed = true;
                                        updateFileHandler(laterStampFile);
                                    }
                                }
                            }
                            yourFiles = this.replace(laterStampFile, yourFiles);
                            alreadyExists = true;
                            break;
                        }
                    }
                    if (!alreadyExists) {
                        yourFiles.put(theirs);
                        changed = true;
                        addFileHandler(new FileInfo(theirs));
                    }
                }
            }

            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            JsonToFile writer = new JsonToFile(manifest);
            theirManifest.put("files", yourFiles);
            writer.writeJSONObject(theirManifest);
                env.log(String.format("Synced manifest with %s, changed? %b\n\tOriginal: %s\n\tTheirs: %s\n\tFinal:%s",
                        changed,
                        theirUsername,
                        yourManifestOriginal,
                        theirManifestOriginal, theirManifest.toString(4)));
            return changed;
        }
    }

    private JSONObject compareTimestamp(JSONObject fileA, JSONObject fileB){
        return fileA.getLong("timeStamp") >= fileB.getLong("timeStamp") ? fileA : fileB;
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
    public FileInfo addFile(String fileLocation, String name) throws Exception{
        File source = new File(fileLocation);
        File dest = new File(this.getSharedDirectoryPath(name));
        FileInfo info = new FileInfo(source, new Date().getTime());
        info.name = name;
        info.md5 = env.getSyncManager().getMD5(source);
        try {
            this.addFileToManifest(info);
            this.addFileToFolder(source, dest);

            return info;
        } catch (IOException e) {
            env.log("Add file error", e);
        }

        return info;

    }

    public void addFileToManifest(FileInfo fileInfo) throws IOException {
        synchronized (manifestLock) {
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            JsonToFile writer = new JsonToFile(manifest);
            JSONObject files = writer.getJSONObject();

            JSONArray filesArray = files.getJSONArray("files");
            filesArray = this.replace(fileInfo, filesArray);
            files.put("files", filesArray);
            writer.writeJSONObject(files);
        }
    }

    private void addFileToFolder(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }


    public void deleteFile(FileInfo info) throws IOException {
        this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(info.getName()));
        this.updateFileInManifest(info);
    }

    public void updateFileInManifest(FileInfo info) throws IOException {
        synchronized (manifestLock) {
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            JsonToFile writer = new JsonToFile(manifest);
            JSONObject files = writer.getJSONObject();
            JSONArray filesArray = files.getJSONArray("files");
            filesArray = this.replace(info, filesArray);
            files.put("files", filesArray);
            writer.writeJSONObject(files);
        }
        triggerGUIChanges();
    }

    public void deleteFileFromFolderIfExists(String fileNameWithPath) throws IOException {
        if(new File(fileNameWithPath).exists()) {
            FileUtils.forceDelete(new File(fileNameWithPath));
        }
        triggerGUIChanges();
    }

    //replace old fileInfo with new fileInfo (info)
    private JSONArray replace(FileInfo info, JSONArray arr){
        JSONArray newArr = new JSONArray();
        int len = arr.length();
        boolean hasPut = false;
        if (arr != null) {
            for (int i=0;i<len;i++)
            {
                //If names are equal then replace with new file info
                if (new JSONObject(arr.get(i).toString()).getString("name").equals(info.getName()))
                {
                    hasPut = true;
                    newArr.put(new JSONObject(info));
                } else {
                    newArr.put(new JSONObject(arr.get(i).toString()));
                }
            }
        }
        if(!hasPut) {
            newArr.put(new JSONObject(info));
        }
        return newArr;
    }

    public void updateFileHandler(FileInfo fileInfo) {
        try {
            env.log("Update file handler for " + fileInfo.name);
            if (autoDownloadUpdates) {
                env.getFS().download(fileInfo);
            }
        } catch(Exception e) {
            env.getLogger().log(Level.INFO, "Error updating file", e);
        }
        triggerGUIChanges();
    }
    public void addFileHandler(FileInfo fileInfo) {
        try {
            env.log("Add file handler for " + fileInfo.name);
            if (autoDownloadNewFiles) {
                env.getFS().download(fileInfo);
            }
        } catch(Exception e) {
            env.getLogger().log(Level.INFO, "Error adding file", e);
        }
        triggerGUIChanges();
    }

    public void fileUpdatedNotification(FileInfo fileInfo) throws Exception{
        updateFileInManifest(fileInfo);
        try {
//                    fS.downloadFrom(conn, fileInfo.getName());
        } catch (Exception e) {
            env.log("Update error", e);
        }
        updateFileHandler(fileInfo);
    }

    public void addFileNotification(FileInfo fileInfo) throws Exception {
        addFileToManifest(fileInfo);
        try {
          //  fS.download(fileInfo);
        } catch (Exception e) {
            env.log("Add file error", e);
        }
        addFileHandler(fileInfo);
    }

    public void deleteFileNotification(FileInfo fileInfo) throws Exception {
        deleteFile(fileInfo);
    }
}
