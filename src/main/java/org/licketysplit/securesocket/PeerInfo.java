package org.licketysplit.securesocket;

public class PeerInfo {
    private String ip;
    private int port;
    private boolean isServer;

    public PeerInfo(String ip, int port, boolean isServer) {
        this.ip = ip;
        this.port = port;
        this.isServer = isServer;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    public boolean getIsServer() { return isServer; }
}
