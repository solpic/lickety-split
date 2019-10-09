package org.licketysplit.env;

import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileManager;

public class Environment {
    public PeerManager getPm() {
        return pm;
    }

    public UserInfo getUserInfo() {
        return userInfo;
    }

    UserInfo userInfo;
    EnvLogger logger;
    FileSharer fS;
    FileManager fM;

    public Environment(UserInfo userInfo, PeerManager pm) {
        this.userInfo = userInfo;
        this.pm = pm;
        logger = new EnvLogger(userInfo.getUsername());
    }

    public void setFS(FileSharer fS){
        this.fS = fS;
    }

    public FileSharer getFS(){
        return this.fS;
    }

    public void setFM(FileManager fS){
        this.fM = fM;
    }

    public FileManager getFM(){
        return this.fM;
    }


    public EnvLogger getLogger() {
        return logger;
    }

    PeerManager pm;

}
