package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

public class UserInfo {
    public static class UserIDMessage extends JSONMessage {
        private UserInfo userInfo;
        PeerInfoDirectory.PeerInfo peerInfo;

        public UserInfo getUserInfo() {
            return userInfo;
        }

        public void setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }

        public UserIDMessage() {
        }

        public PeerInfoDirectory.PeerInfo getPeerInfo() {
            return peerInfo;
        }

        public void setPeerInfo(PeerInfoDirectory.PeerInfo peerInfo) {
            this.peerInfo = peerInfo;
        }

        public UserIDMessage(UserInfo userInfo, PeerInfoDirectory.PeerInfo peerInfo) {
            this.userInfo = userInfo;
            this.peerInfo = peerInfo;
        }

    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public PeerManager.ServerInfo getServer() {
        return server;
    }

    public void setServer(PeerManager.ServerInfo server) {
        this.server = server;
    }

    public UserInfo() {
    }

    private PeerManager.ServerInfo server;
    private String username;

    public UserInfo(String username, PeerManager.ServerInfo server) {
        this.username = username;
        this.server = server;
    }

    @Override

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userObj = (UserInfo) o;
        return userObj.username.equals(username);
    }

    @Override

    public int hashCode() {
        return username.hashCode();
    }
}
