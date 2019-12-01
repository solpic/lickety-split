package org.licketysplit.syncmanager;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
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

    public void addFile(String filePath) throws Exception {
        FileInfo info = this.env.getFM().addFile(filePath);

        this.env.getLogger().log(Level.INFO, "Adding File: " + info.getName());
        syncManifests();
//        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
//        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
//            peer.getValue().sendFirstMessage(new AddFileNotification(info), null);
//        }
    }

    //When a user updates a file they own
    public void updateFile(String fileToUpdate, String fileToUpdateWith) throws Exception {
        File file = new File(fileToUpdate);

        String dstPath = env.getFM().getSharedDirectoryPath(fileToUpdate);
        File dstF = new File(dstPath);
        if(dstF.exists()) dstF.delete();
        FileUtils.copyFile(new File(fileToUpdateWith), dstF);

        FileInfo fileInfo = new FileInfo(new File(fileToUpdate), new Date().getTime());
        fileInfo.md5 = env.getSyncManager().getMD5(dstF);
        fileInfo.deleted = false;
        fileInfo.length = dstF.length();
        env.getFM().updateFileInManifest(fileInfo);

        this.env.getLogger().log(Level.INFO, "SENDING UPDATE: " + fileInfo.getName());

        syncManifests();
    }
    public void deleteFile(String fileName) throws Exception {
        FileInfo deletedFileInfo = new FileInfo(fileName, true);
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

    public void syncManifests(String exclude) throws Exception{
        JSONObject manifest = this.env.getFM().getManifest();

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            if(!peer.getKey().getUsername().equals(exclude))  peer.getValue().sendFirstMessage(new UpdateManifestRequest(manifest), null);
        }
    }
}
