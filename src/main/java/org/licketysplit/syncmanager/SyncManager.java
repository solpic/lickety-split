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

public class SyncManager {
    private Environment env;

    public SyncManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }



    public String getMD5(File file) throws Exception {
        InputStream is = Files.newInputStream(file.toPath());
        String md5 = DigestUtils.md5Hex(is);
        return md5;
    }

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

    public String addFile(String filePath) throws Exception {
        File file = new File(filePath);
        String name = v1Name(file.getName());
        addFileVersioned(filePath, name);
        return name;
    }
    String v1Name(String name) {
        return "v1 "+name;
    }
    public void addFileVersioned(String filePath, String versionName) throws Exception {
        FileInfo info = this.env.getFM().addFile(filePath, versionName);

        this.env.getLogger().log(Level.INFO, "Adding File: " + info.getName());
        syncManifests();
//        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
//        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
//            peer.getValue().sendFirstMessage(new AddFileNotification(info), null);
//        }
    }

    public class FileVersion {
        String name;
        int version;
        public void increase() {
            version++;
        }
        public String get() {
            return String.format("v%d %s", version, name);
        }
    }
    // vN filename
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

    public String updateFile(String fileToUpdate, String fileToUpdateWith) throws Exception {
        FileVersion version = getVersionFromName(fileToUpdate);
        deleteFile(fileToUpdate);
        version.version++;
        String name = version.get();
        addFileVersioned(fileToUpdateWith, name);
        return name;
    }

    //When a user updates a file they own
//    public void updateFile(String fileToUpdate, String fileToUpdateWith) throws Exception {
//        String dstPath = env.getFM().getSharedDirectoryPath(fileToUpdate);
//        File dstF = new File(dstPath);
//        if(dstF.exists()) dstF.delete();
//        FileUtils.copyFile(new File(fileToUpdateWith), dstF);
//
//        FileInfo fileInfo = new FileInfo(new File(fileToUpdate), new Date().getTime());
//        fileInfo.md5 = env.getSyncManager().getMD5(dstF);
//        fileInfo.deleted = false;
//        fileInfo.length = dstF.length();
//        env.getFM().updateFileInManifest(fileInfo);
//
//        this.env.getLogger().log(Level.INFO, "SENDING UPDATE: " + fileInfo.getName());
//
//        syncManifests();
//    }
    public void deleteFile(String fileName) throws Exception {
        FileInfo deletedFileInfo = new FileInfo(fileName, true);

        RandomAccessFile raf = FileAssembler.FileAccessor.getFile(env.getFM().getSharedDirectoryPath(fileName));
        raf.close();
        this.env.getFM().deleteFile(deletedFileInfo);

        this.env.getLogger().log(Level.INFO, "Deleting File: " + deletedFileInfo.getName());
        syncManifests();
//        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
//        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
//            peer.getValue().sendFirstMessage(new DeleteFileNotification(deletedFileInfo), null);
//        }
    }

    public void startUp() {
        //0. Connect to network
        //1. Check for configs, manifest folder
        //2. Update manifest
        //3. return file infos

    }

    public void syncManifests() throws Exception {
        JSONObject manifest = this.env.getFM().getManifest();

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new UpdateManifestRequest(manifest), null);
        }

    }

    public void syncManifestWith(SecureSocket sock) throws Exception {
        //env.log("Syncing manifest with "+sock.getPeerAddress().getUser().getUsername());
        JSONObject manifest = this.env.getFM().getManifest();
        sock.sendFirstMessage(new UpdateManifestRequest(manifest), null);
    }

    public HashMap<String, DownloadManager> getDownloads(){
        return this.env.getFS().getDownloads();
    }
}
