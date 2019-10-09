package org.licketysplit.env;

import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;

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

    public Environment(UserInfo userInfo, PeerManager pm) {
        this.userInfo = userInfo;
        this.pm = pm;
        logger = new EnvLogger(userInfo.getUsername());
    }

    public void setFS(FileSharer fS){
        this.fS = fS;
    }

    public FileSharer getFs(){
        return this.fS;
    }

    public EnvLogger getLogger() {
        return logger;
    }

    PeerManager pm;

}
