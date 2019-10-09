package org.licketysplit.securesocket.peers;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.onconnect.NewConnectionHandler;
import org.licketysplit.securesocket.onconnect.SyncPeerListOnConnect;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PeerManager implements SecureSocket.NewConnectionCallback {
    Environment env;
    EnvLogger log;
    List<NewConnectionHandler> onConnectHandlers;
    public static class ServerInfo {
        int port;
        String ip;

        public String getIp() {
            return ip;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public ServerInfo() {}

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public ServerInfo(int port, String ip) {
            this.port = port;
            this.ip = ip;
        }
    }

    public ConcurrentHashMap<UserInfo, SecureSocket> getPeers() {
        return peers;
    }

    public PeerManager() {
        peers = new ConcurrentHashMap<>();
        initConnectHandlers();
    }
    void initConnectHandlers() {
        onConnectHandlers = new ArrayList<>();
        onConnectHandlers.add(new SyncPeerListOnConnect());
    }
    public void addConnectionHandler(NewConnectionHandler handler) {
        onConnectHandlers.add(handler);
    }
    public void setEnv(Environment env) {
        this.env = env;
        log = this.env.getLogger();
    }
    public void addPeer(PeerAddress peer) throws Exception {
        if(peer.getServerInfo()==null) return;
        // First check that we should connect
        if(peer.user.equals(env.getUserInfo())) return;

        if(peers.containsKey(peer)) return;
        log.log(Level.INFO,
                String.format("Connecting to user: %s, at IP: %s, port %d",
                        peer.getUser().getUsername(), peer.ip, peer.getServerInfo().port));
        newConnectionHandler(SecureSocket.connect(peer, env), false);
    }

    public void listen() throws Exception {
        SecureSocket.listen(this.env.getUserInfo().getServer().getPort(), this::onConnect, env);
    }

    public void initialize(PeerAddress initialPeer) throws Exception {
        addPeer(initialPeer);
    }

    public void confirmPeer(UserInfo user, SecureSocket sock) throws Exception {
        synchronized (peers) {
            SecureSocket oldVal = peers.putIfAbsent(user, sock);
            if(oldVal==null) {
                sock.setUserInfo(user);
                peerConfirmed(user, sock);
                logPeerList();
            }
        }
    }

    public void logPeerList() {
        StringBuilder fmt = new StringBuilder();
        fmt.append("Peer list: \n");
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            fmt.append(String.format("Username: %s\n", peer.getKey().getUsername()));
        }
        log.log(Level.INFO, fmt.toString());
    }

    public void peerConfirmed(UserInfo user, SecureSocket sock) throws Exception {
        log.log(Level.INFO, "New peer '"+user.getUsername()+"' has been confirmed");
        for (NewConnectionHandler handler : onConnectHandlers) {
            handler.connectionConfirmed(user, sock, env);
        }
    }

    ConcurrentHashMap<UserInfo, SecureSocket> peers;


    // Handler for receiving first message in chain
    @DefaultHandler(type = UserInfo.UserIDMessage.class)
    public static class HandshakeIDHandler implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) {
            UserInfo.UserIDMessage msg = m.getMessage();
            //m.getEnv().getLogger().log(Level.INFO, "Receiving INITIAL handshake ID from: "+msg.getUserInfo().getUsername());
            try {
                m.getConn().setServerInfo(msg.getUserInfo().getServer());
                m.respond(new UserInfo.UserIDMessage(m.getEnv().getUserInfo()), null);
                m.getEnv().getPm().confirmPeer(msg.getUserInfo(), m.getConn());
            } catch(Exception e) {
                m.getEnv().getLogger().log(Level.SEVERE,"Exception while handshaking");
                e.printStackTrace();
            }
        }
    }

    void newConnectionHandlerClient(SecureSocket sock) throws Exception {
        log.log(Level.INFO, "Sending handshake");
        sock.sendFirstMessage(new UserInfo.UserIDMessage(env.getUserInfo()), (ReceivedMessage m) -> {
            //Handler for response
            UserInfo.UserIDMessage msg = m.getMessage();
            //m.getEnv().getLogger().log(Level.INFO, "Receiving FINAL handshake ID from: "+msg.getUserInfo().getUsername());
            try {
                m.getConn().setServerInfo(msg.getUserInfo().getServer());
                m.getEnv().getPm().confirmPeer(msg.getUserInfo(), m.getConn());
            } catch(Exception e) {
                m.getEnv().getLogger().log(Level.SEVERE, "Exception while handshaking");
                e.printStackTrace();
            }
        });
    }

    public GetPeerListResponse getPeerListResponse() {
        List<PeerAddress> peerInfoList = new ArrayList<>();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peerInfoList.add(peer.getValue().getPeerAddress());
        }

        return new GetPeerListResponse(peerInfoList);
    }

    void newConnectionHandler(SecureSocket sock, boolean isServer) throws Exception{
        if(!isServer) {
            newConnectionHandlerClient(sock);
        }
    }

    @Override
    public void onConnect(SecureSocket sock) throws Exception {
        newConnectionHandler(sock, true);
    }

    public static class PeerAddress {
        private String ip;
        private int port;
        private UserInfo user;
        private ServerInfo serverInfo;

        public String id() {
            return String.format("%s:%d", ip, port);
        }

        public PeerAddress(String ip, int port, UserInfo user, ServerInfo serverInfo) {
            this.ip = ip;
            this.port = port;
            this.user = user;
            this.serverInfo = serverInfo;
        }

        public ServerInfo getServerInfo() {
            return serverInfo;
        }

        public void setServerInfo(ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
        }

        public UserInfo getUser() {
            return user;
        }

        public void setUser(UserInfo user) {
            this.user = user;
        }

        public PeerAddress(){}

        public void setIp(String ip) {
            this.ip = ip;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

    }
}
