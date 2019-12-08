package org.licketysplit.syncmanager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.filesharer.FileAssembler;
import org.licketysplit.syncmanager.messages.AddFileNotification;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.messages.DeleteFileNotification;
import org.licketysplit.syncmanager.messages.UpdateFileNotification;
import org.licketysplit.syncmanager.messages.UpdateManifestRequest;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * The sync manager wraps interaction with the FileManager to make
 * some jobs a little easier.
 */
public class SyncManager {
    /**
     * Our environment.
     */
    private Environment env;

    /**
     * Instantiates a new Sync manager.
     */
    public SyncManager(){}

    /**
     * Set env.
     *
     * @param env the env
     */
    public void setEnv(Environment env){
        this.env = env;
    }


    /**
     * Calculates the MD5 digest for a file.
     *
     * @param file the file
     * @return the MD5 digest
     * @throws Exception the exception
     */
    public String getMD5(File file) throws Exception {
        InputStream is = Files.newInputStream(file.toPath());
        String md5 = DigestUtils.md5Hex(is);
        return md5;
    }

    /**
     * For a given file, calculates the md5 digest
     * and compares it to what it should be based on the manifest.
     *
     * @param name the name of the file to check
     * @return true if the calculated MD5 matches what's in the manifest
     * @throws Exception the exception
     */
    public boolean checkMD5AgainstManifest(String name) throws Exception {
        String ourMD5 = getMD5(new File(env.getFM().getSharedDirectoryPath(name)));

        JSONObject manifest = env.getFM().getManifest();
        JSONArray files = manifest.getJSONArray("files");
        for (Object o : files) {
            JSONObject f = (JSONObject)o;
            FileInfo fileInfo = new FileInfo(f);
            if(fileInfo.name.equals(name)) {
                return fileInfo.md5.equals(ourMD5);
            }
        }
        throw new Exception(String.format("File '%s' not found in manifest", name));
    }

    /**
     * Adds/uploads a file to the network. This is what the GUI calls when
     * it wants to upload a file. Added files are automatically versioned
     * as v1.
     *
     * @param filePath the file to upload
     * @return the versioned name of the file
     * @throws Exception the exception
     */
    public String addFile(String filePath) throws Exception {
        File file = new File(filePath);
        String name = v1Name(file.getName());
        addFileVersioned(filePath, name);
        return name;
    }

    /**
     * Generates the V1 name of a file.
     *
     * @param name the filename
     * @return the V1 versioned name
     */
    String v1Name(String name) {
        return "v1 "+name;
    }

    /**
     * Adds a file to the network from the given path, and using
     * the given name. Then, syncs the manifest with all connected
     * peers to propagate the change.
     *
     * @param filePath    the file path
     * @param versionName the name of the file
     * @throws Exception the exception
     */
    public void addFileVersioned(String filePath, String versionName) throws Exception {
        FileInfo info = this.env.getFM().addFile(filePath, versionName);

        this.env.getLogger().log(Level.INFO, "Adding File: " + info.getName());
        syncManifests();
    }

    /**
     * Used to keep track of the version of a file, using the filename.
     */
    public class FileVersion {
        /**
         * The base name of the file.
         */
        String name;
        /**
         * The version number of the file
         */
        int version;

        /**
         * Increases the version number.
         */
        public void increase() {
            version++;
        }

        /**
         * Generates a versioned name for the file.
         *
         * @return the versioned name
         */
        public String get() {
            return String.format("v%d %s", version, name);
        }
    }

    /**
     * Calculates the FileVersion object from a given filename.
     *
     * @param name the filename
     * @return the version from the filename
     * @throws Exception the exception
     */
    public FileVersion getVersionFromName(String name) throws Exception {
        try {
            if(name.charAt(0)!='v') {
                throw new Exception("No v found at beginning of version: "+name);
            }
            int i = name.indexOf(' ');
            if(i==-1) throw new Exception("No space found in version name: "+name);
            String versionStr = name.substring(1, i);
            int version = Integer.parseInt(versionStr);
            String rawName = name.substring(i+1, name.length());
            FileVersion f = new FileVersion();
            f.name = rawName;
            f.version = version;
            return f;
        }catch (Exception e) {
            env.log("Error versioning file: ", e);
            throw e;
        }
    }

    /**
     * Updates a file by deleting the old file, and adding a new file
     * with an increased version name.
     *
     * @param fileToUpdate     the name of the old file
     * @param fileToUpdateWith the file to replace it with
     * @return the new name
     * @throws Exception the exception
     */
    public String updateFile(String fileToUpdate, String fileToUpdateWith) throws Exception {
        FileVersion version = getVersionFromName(fileToUpdate);
        deleteFile(fileToUpdate);
        version.version++;
        String name = version.get();
        addFileVersioned(fileToUpdateWith, name);
        return name;
    }

    /**
     * Deletes a file from the network by closing the file handle,
     * deleting the local copy and syncing manifests to propagate
     * the change.
     *
     * @param fileName the file to delete
     * @throws Exception the exception
     */
    public void deleteFile(String fileName) throws Exception {
        FileInfo deletedFileInfo = new FileInfo(fileName, true);

        try {
            RandomAccessFile raf = FileAssembler.FileAccessor.getFile(env.getFM().getSharedDirectoryPath(fileName));
            raf.close();
        }catch(Exception e) {
            env.log("Error closing file", e);
        }
        this.env.getFM().deleteFile(deletedFileInfo);

        this.env.getLogger().log(Level.INFO, "Deleting File: " + deletedFileInfo.getName());
        syncManifests();
    }

    /**
     * No longer used.
     */
    public void startUp() {
        //0. Connect to network
        //1. Check for configs, manifest folder
        //2. Update manifest
        //3. return file infos

    }

    /**
     * Sync our manifest with all connected peers.
     *
     * @throws Exception the exception
     */
    public void syncManifests() throws Exception {
        JSONObject manifest = this.env.getFM().getManifest();

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new UpdateManifestRequest(manifest), null);
        }

    }

    /**
     * Sync our manifest with one particular peer.
     *
     * @param sock the socket of the peer
     * @throws Exception the exception
     */
    public void syncManifestWith(SecureSocket sock) throws Exception {
        //env.log("Syncing manifest with "+sock.getPeerAddress().getUser().getUsername());
        JSONObject manifest = this.env.getFM().getManifest();
        sock.sendFirstMessage(new UpdateManifestRequest(manifest), null);
    }

    /**
     * No longer used.
     *
     * @return the hash map
     */
    public HashMap<String, DownloadManager> getDownloads(){
        return this.env.getFS().getDownloads();
    }
}
