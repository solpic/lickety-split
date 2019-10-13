package org.licketysplit.syncmanager;

import org.json.JSONObject;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.messages.UpdateFileNotification;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class SyncManager {
    private Environment env;

    public SyncManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }

    //When a user updates a file they own
    public void updateFile(String fileNameWithPath){
        File file = new File(fileNameWithPath);
        FileInfo fileInfo = new FileInfo(file, new Date().getTime());

        FileManager fm = env.getFM();
        fm.updateFile(fileNameWithPath);
        PeerManager pm = env.getPm();
        ConcurrentHashMap<UserInfo, SecureSocket> peers = pm.getPeers();

        peers.forEach((key, value) -> {
            try {
                value.sendFirstMessage(new UpdateFileNotification(new JSONObject(fileInfo.toString())), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
