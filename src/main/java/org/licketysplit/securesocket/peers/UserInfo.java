package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

/**
 * Stores metadata about a peer.
 */
public class UserInfo {
    /**
     * Message used to send our user info and peer info directory
     * to a peer.
     */
    public static class UserIDMessage extends JSONMessage {
        /**
         * Our user info.
         */
        private UserInfo userInfo;
        /**
         * Our peer info directory.
         */
        PeerInfoDirectory.PeerInfo peerInfo;

        /**
         * Gets user info.
         *
         * @return the user info
         */
        public UserInfo getUserInfo() {
            return userInfo;
        }

        /**
         * Sets user info.
         *
         * @param userInfo the user info
         */
        public void setUserInfo(UserInfo userInfo) {
            this.userInfo = userInfo;
        }

        /**
         * Instantiates a new User id message.
         */
        public UserIDMessage() {
        }

        /**
         * Gets peer info.
         *
         * @return the peer info
         */
        public PeerInfoDirectory.PeerInfo getPeerInfo() {
            return peerInfo;
        }

        /**
         * Sets peer info.
         *
         * @param peerInfo the peer info
         */
        public void setPeerInfo(PeerInfoDirectory.PeerInfo peerInfo) {
            this.peerInfo = peerInfo;
        }

        /**
         * Instantiates a new User id message.
         *
         * @param userInfo the user info
         * @param peerInfo the peer info
         */
        public UserIDMessage(UserInfo userInfo, PeerInfoDirectory.PeerInfo peerInfo) {
            this.userInfo = userInfo;
            this.peerInfo = peerInfo;
        }

    }

    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * Sets username.
     *
     * @param username the username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * Gets server.
     *
     * @return the server
     */
    public PeerManager.ServerInfo getServer() {
        return server;
    }

    /**
     * Sets server.
     *
     * @param server the server
     */
    public void setServer(PeerManager.ServerInfo server) {
        this.server = server;
    }

    /**
     * Instantiates a new User info.
     */
    public UserInfo() {
    }

    /**
     * Stores information on where the peer is listening for
     * incoming TCP connections (IP and port)
     */
    private PeerManager.ServerInfo server;
    /**
     * The peer's username.
     */
    private String username;

    /**
     * Instantiates a new User info.
     *
     * @param username the username
     * @param server   the server
     */
    public UserInfo(String username, PeerManager.ServerInfo server) {
        this.username = username;
        this.server = server;
    }

    /**
     * Overriding this allows us to use this as a key in a Map.
     * This does an equality check on two UserInfo objects,
     * but we are really just comparing the usernames.
     * @param o the object to compare with
     * @return true if objects are equal
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserInfo userObj = (UserInfo) o;
        return userObj.username.equals(username);
    }

    /**
     * We override this to use UserInfo objects as keys in a Map.
     * Generates a hashCode from the username.
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return username.hashCode();
    }
}
