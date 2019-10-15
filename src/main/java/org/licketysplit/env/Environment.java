package org.licketysplit.env;

import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileManager;

import java.nio.file.Path;
import java.nio.file.Paths;

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
    String directory;
    String configs;

    public String getDirectory(String fileName) {
        Path path = Paths.get(System.getProperty("user.home"), this.directory, fileName);
        return path.toString();
    }

    public void setDirectory(String directory) {
        this.directory = directory;
    }

    public void setConfigs(String configs){
        this.configs = configs;
    }

    public String getConfigs(String fileName){
        Path path = Paths.get(System.getProperty("user.home"), this.configs, fileName);
        return path.toString();
    }

    public String getConfigs(){
        return Paths.get(System.getProperty("user.home"), this.configs).toString();
    }

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

    public void setFM(FileManager fM){
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
