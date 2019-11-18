package org.licketysplit.env;

import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
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

    KeyStore rootKey;
    KeyStore identityKey;

    PeerInfoDirectory info;

    public KeyStore getRootKey() {
        return rootKey;
    }

    public void setRootKey(KeyStore rootKey) {
        this.rootKey = rootKey;
    }

    public KeyStore getIdentityKey() {
        return identityKey;
    }

    public void setIdentityKey(KeyStore identityKey) {
        this.identityKey = identityKey;
    }

    public PeerInfoDirectory getInfo() {
        return info;
    }

    public void setInfo(PeerInfoDirectory info) {
        this.info = info;
    }

    public Environment(UserInfo userInfo, PeerManager pm) {
        this.userInfo = userInfo;
        this.pm = pm;
        logger = new EnvLogger(userInfo.getUsername());
    }

    public EnvLogger getLogger() {
        return logger;
    }

    PeerManager pm;

}
