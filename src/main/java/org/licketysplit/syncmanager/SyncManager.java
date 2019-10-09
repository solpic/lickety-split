package org.licketysplit.syncmanager;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;

import java.io.File;
import java.util.concurrent.ConcurrentHashMap;

public class SyncManager {
    private Environment env;

    public SyncManager(){}

    public void setEnv(Environment env){
        this.env = env;
    }

    public void updateFile(String fileName){
        File file = new File(fileName);
        PeerManager pm = env.getPm();
        ConcurrentHashMap<UserInfo, SecureSocket> peers = pm.getPeers();
        peers.forEach((key, value) -> {
            value.sendMessage(new UpdateFileNotification(file), null);
        });
    }


}
