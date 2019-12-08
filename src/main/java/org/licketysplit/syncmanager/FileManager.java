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

/**
 * The FileManager handles the manifest, handling updates, deletions, additions,
 * as well as syncing the manifest with other peers.
 */
public class FileManager {

    /**
     * Our environment.
     */
    private Environment env;

    /**
     * Instantiates a new File manager.
     */
    public FileManager(){}

    /**
     * Set env.
     *
     * @param env the env
     */
    public void setEnv(Environment env){
        this.env = env;
    }

    //INITIALIZATION

    /**
     * Initialize files.
     *
     * @param username the username
     */
    public void initializeFiles(String username){
        this.initializeConfigs(username);
    }

    /**
     * Callback function to update GUI.
     */
    DownloadChanged downloadChanged = null;
    /**
     * To prevent threading errors on the downloadChanged object.
     */
    Object downloadChangedLock = new Object();

    /**
     * Sets download changed callback.
     *
     * @param c the new callback
     */
    public void setDownloadChanged(DownloadChanged c) {
        synchronized (downloadChangedLock) {
            downloadChanged = c;
        }
    }

    /**
     * Trigger download changed callback.
     *
     * @param info the file for which the download has changed
     */
    public void triggerDownloadUpdate(FileInfo info) {
        synchronized (downloadChangedLock) {
            if(downloadChanged!=null) {
                downloadChanged.changed(info);
            }
        }
    }

    /**
     * The download changed callback interface.
     */
    public interface DownloadChanged {
        /**
         * Called when changed.
         *
         * @param info the info
         */
        void changed(FileInfo info);
    }

    /**
     * The manifest changed callback interface.
     */
    public interface ManifestChanged {
        /**
         * Called when changed.
         */
        void changed();
    }

    /**
     * Callback function to update GUI on manifest changes.
     */
    ManifestChanged guiChanges = null;
    /**
     * Lock to prevent thread errors on guiChanges object.
     */
    Object guiChangesLock = new Object();

    /**
     * Sets change handler.
     *
     * @param c the new handler
     */
    public void setChangeHandler(ManifestChanged c) {
        synchronized (guiChangesLock) {
            guiChanges = c;
        }
    }

    /**
     * Trigger gui changes and returns true if there is a handler set.
     *
     * @return true if there is a handler
     */
    public boolean triggerGUIChanges() {
        synchronized (guiChangesLock) {
            if(guiChanges!=null) {
                guiChanges.changed();
                return true;
            }
            return false;
        }
    }

    /**
     * Create's configs file and fills it with configs JSON object {sharedDirectory, username}
     * @param username our username
     */
    private void initializeConfigs(String username) {
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

    /**
     * No longer used.
     */
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


    /**
     * Returns true if a file exists.
     *
     * @param fileName the file name
     * @return true if exists
     */
    public boolean hasFile(String fileName){
        return new File(this.getSharedDirectoryPath(fileName)).exists();
    }

    /**
     * Gets a File object for a file in the shared folder.
     *
     * @param fileName the file name
     * @return the file
     */
    public File getFile(String fileName) {return new File(this.getSharedDirectoryPath(fileName));}

    /**
     * Returns path to configs folder.
     * @return the path
     */
    private String getConfigsPath(){
        return this.env.getConfigs();
    }

    /**
     * Returns path to a file in the configs folder.
     * @param fileName the file
     * @return the full path
     */
    private String getConfigsPath(String fileName){
        return this.env.getConfigs(fileName);
    }

    /**
     * Gets path to a file in the shared directory.
     *
     * @param fileName the file name
     * @return the path
     */
    public String getSharedDirectoryPath(String fileName){
        return this.env.getDirectory(fileName);
    }

    /**
     * Helper class used to represent the status of a file for the GUI.
     */
    public static class FileStatus {
        /**
         * True if we have the file.
         */
        public boolean hasFile = false;
        /**
         * True if the file is corrupted.
         */
        public boolean fileCorrupted = false;
        /**
         * True if we are downloading.
         */
        public boolean isDownloading = false;
        /**
         * The download progress.
         */
        public float downloadProgress = 0.0f;
        /**
         * The Download speed in bytes/second.
         */
        public float downloadSpeed = 0.0f;
        /**
         * The name of the file.
         */
        public String name;
        /**
         * The size of the file in bytes.
         */
        public float size;
        /**
         * True if the file has been deleted.
         */
        public boolean deleted = false;
    }

    /**
     * Gets a FileStatus object for a given file.
     *
     * @param info the file
     * @return status of the file
     * @throws Exception the exception
     */
    public FileStatus getFileStatus(FileInfo info) throws Exception {
        FileStatus fileStatus = new FileStatus();
        fileStatus.name = info.name;
        String name = info.name;

        // Checks if a download is in progress and if so fill relevant fields
        DownloadManager downloadManager = env.getFS().currentProgress(name);
        if(downloadManager!=null) {
            fileStatus.isDownloading = true;
            fileStatus.downloadProgress = downloadManager.getProgress();
            fileStatus.downloadSpeed = downloadManager.getSpeed();
            fileStatus.size = downloadManager.getFileAssembler().totalChunks.get()*DownloadManager.chunkLengthRaw;
            return fileStatus;
        }

        // Otherwise find the manifest info
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

    /**
     * No longer used.
     *
     * @param directoryName the directory name
     * @param fileName      the file name
     * @return the temp directory path
     */
    public String getTempDirectoryPath(String directoryName, String fileName) {
        return this.env.getTempDirectory(directoryName, fileName);
    }

    /**
     * Loads the manifest from file into a JSONObject. If the
     * manifest doesn't exist or is corrupted, write an empty manifest.
     *
     * @return the manifest
     * @throws IOException the io exception
     */
    public JSONObject getManifest() throws IOException {
        synchronized(manifestLock) {
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            try {
                // Write an empty manifest
                if (!manifest.exists() || FileUtils.readFileToString(manifest, "UTF-8").equals("")) {
                    env.log("WRITING NEW MANIFEST");
                    FileUtils.writeStringToFile(manifest, "{\"files\":[]}", "UTF-8");
                }
                // Read manifest to JSONObject and return
                JsonToFile writer = new JsonToFile(manifest);
                return writer.getJSONObject();
            } catch (Exception e) {
                // Error parsing manifest, die
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

    /**
     * Lock to prevent threading errors when editing/accessing the manifest.
     */
    Object manifestLock = new Object();
    /**
     * True if downloads should start automatically.
     */
    public boolean autoDownloadNewFiles = false;
    /**
     * True if updates should start automatically.
     */
    public boolean autoDownloadUpdates = false;

    /**
     * Given another peer's manifests, updates our manifest based
     * on the differences between the manifests.
     *
     * @param theirManifest their manifest
     * @param theirUsername their username
     * @return true if changes were made
     * @throws IOException the io exception
     */
    public boolean syncManifests(JSONObject theirManifest, String theirUsername) throws IOException {
        synchronized (manifestLock) {
            env.log("Syncing manifest");

            // Convert our manifest and their manifest to arrays
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
                            // Get the more recent entry from both manifests
                            FileInfo laterStampFile = new FileInfo(this.compareTimestamp(yours, theirs));
                            // If the file has been deleted, delete our local copy
                            if (laterStampFile.isDeleted()) {
                                this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(laterStampFile.getName()));
                            }else{
                                // Call update file callback if necessary
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
                    // Call addFileHandler callback if necessary
                    if (!alreadyExists) {
                        yourFiles.put(theirs);
                        changed = true;
                        addFileHandler(new FileInfo(theirs));
                    }
                }
            }

            // Write updated manifest to file and log
            File manifest = new File(this.getConfigsPath(".manifest.txt"));
            JsonToFile writer = new JsonToFile(manifest);
            theirManifest.put("files", yourFiles);
            writer.writeJSONObject(theirManifest);
                env.log(String.format("Synced manifest with %s, changed? %b\n\tOriginal: %s\n\tTheirs: %s\n\tFinal:%s",
                        changed,
                        theirUsername,
                        yourManifestOriginal,
                        theirManifestOriginal, theirManifest.toString(4)));

                // If changes were made, show in the GUI
            if(changed)
                triggerGUIChanges();
            return changed;
        }
    }


    /**
     * Given two manifest entries, returns the more recent.
     * @param fileA one manifest entry
     * @param fileB another manifest entry
     * @return the most recent of the two
     */
    private JSONObject compareTimestamp(JSONObject fileA, JSONObject fileB){
        return fileA.getLong("timeStamp") >= fileB.getLong("timeStamp") ? fileA : fileB;
    }

    /**
     * Gets username from the config folder.
     *
     * @return our username
     */
    public String getUsername(){
        try{
            JSONObject obj = new JsonToFile(new File(this.getConfigsPath( ".manifest.txt"))).getJSONObject();
            return obj.getString("username");
        } catch(IOException e){
            e.printStackTrace();
        }

        return "";
    }

    /**
     * This is called to add/upload a file to the network. It copies
     * the file to the shared directory folder, and then adds the file to the manifest.
     *
     *
     * @param fileLocation the location of the file
     * @param name         the name of the file
     * @return the metadata of the file
     * @throws Exception the exception
     */
    public FileInfo addFile(String fileLocation, String name) throws Exception{
        File source = new File(fileLocation);
        File dest = new File(this.getSharedDirectoryPath(name));
        FileInfo info = new FileInfo(source, new Date().getTime());
        info.name = name;

        // Calculate MD5 of file and save
        info.md5 = env.getSyncManager().getMD5(source);
        try {
            // Adds file to manifest which also notifies all peers
            this.addFileToManifest(info);
            // Copies the file to the shared files directory
            this.addFileToFolder(source, dest);

            return info;
        } catch (IOException e) {
            env.log("Add file error", e);
        }

        return info;

    }

    /**
     * Adds a FileInfo entry to the manifest.
     *
     * @param fileInfo the new file metadata
     * @throws IOException the io exception
     */
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

    /**
     * Copies a file into the shared files folder.
     * @param source the file to be copied
     * @param dest the destination to copy to
     * @throws IOException error
     */
    private void addFileToFolder(File source, File dest) throws IOException {
        FileUtils.copyFile(source, dest);
    }


    /**
     * Delete files a file from the shared files folder and
     * updates the corresponding manifest entry.
     *
     * @param info the info
     * @throws IOException the io exception
     */
    public void deleteFile(FileInfo info) throws IOException {
        this.deleteFileFromFolderIfExists(this.getSharedDirectoryPath(info.getName()));
        this.updateFileInManifest(info);
    }

    /**
     * Updates the entry of a file in the manifest
     * and triggers GUI changes.
     *
     * @param info the updated file metadata
     * @throws IOException the io exception
     */
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

    /**
     * Delete file from folder if exists then trigger GUI changes.
     *
     * @param fileNameWithPath the file name with path
     * @throws IOException the io exception
     */
    public void deleteFileFromFolderIfExists(String fileNameWithPath) throws IOException {
        if(new File(fileNameWithPath).exists()) {
            FileUtils.forceDelete(new File(fileNameWithPath));
        }
        triggerGUIChanges();
    }

    /**
     * Replaces an entry in the manifest with a new one.
     * @param info the new entry
     * @param arr the manifest
     * @return the new manifest
     */
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

    /**
     * Called when a file has been updated, by the current user
     * or a remote peer.
     *
     * @param fileInfo the updated file
     */
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

    /**
     * Called when a file has been added by the current user
     * or a remote peer.
     *
     * @param fileInfo the file info
     */
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

    /**
     * Called by UpdateFileNotification, updates manifest.
     *
     * @param fileInfo the file info
     * @throws Exception the exception
     */
    public void fileUpdatedNotification(FileInfo fileInfo) throws Exception{
        updateFileInManifest(fileInfo);
        try {
//                    fS.downloadFrom(conn, fileInfo.getName());
        } catch (Exception e) {
            env.log("Update error", e);
        }
        updateFileHandler(fileInfo);
    }

    /**
     * Called by AddFileNotification, updates manifest.
     *
     * @param fileInfo the file info
     * @throws Exception the exception
     */
    public void addFileNotification(FileInfo fileInfo) throws Exception {
        addFileToManifest(fileInfo);
        try {
          //  fS.download(fileInfo);
        } catch (Exception e) {
            env.log("Add file error", e);
        }
        addFileHandler(fileInfo);
    }

    /**
     * Called by delete file notification, updates manifest
     * and deletes the file.
     *
     * @param fileInfo the file info
     * @throws Exception the exception
     */
    public void deleteFileNotification(FileInfo fileInfo) throws Exception {
        deleteFile(fileInfo);
    }
}
