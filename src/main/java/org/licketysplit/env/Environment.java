package org.licketysplit.env;

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
