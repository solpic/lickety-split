package org.licketysplit.syncmanager;

import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.messages.AddFileNotification;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.messages.DeleteFileNotification;
import org.licketysplit.syncmanager.messages.UpdateFileNotification;

import java.io.File;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class SyncManager {
    private Environment env;

    public SyncManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }

    //When a user updates a file they own
    public void updateFile(String fileNameWithPath) throws Exception {
        File file = new File(fileNameWithPath);
        FileInfo fileInfo = new FileInfo(file, new Date().getTime()); //Create updated file info obj

        FileManager fm = env.getFM();
        PeerManager pm = env.getPm();

        fm.updateFile(fileNameWithPath);

        this.env.getLogger().log(Level.INFO, "SENDING UPDATE: " + fileInfo.getName());

        ConcurrentHashMap<UserInfo, SecureSocket> peers = pm.getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new UpdateFileNotification(fileInfo), null);
        }
    }

    public void addFile(String filePath) throws Exception {
        FileInfo info = this.env.getFM().addFile(filePath);

        this.env.getLogger().log(Level.INFO, "Adding File: " + info.getName());

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new AddFileNotification(info), null);
        }
    }

    public void deleteFile(String fileName) throws Exception {
        FileInfo deletedFileInfo = new FileInfo(fileName, false);
        this.env.getFM().deleteFile(deletedFileInfo);

        this.env.getLogger().log(Level.INFO, "Deleting File: " + deletedFileInfo.getName());

        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(new DeleteFileNotification(deletedFileInfo), null);
        }
    }
}
