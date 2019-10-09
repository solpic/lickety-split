package org.licketysplit.securesocket.peers;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.UserInfo;
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
        newConnectionHandler(SecureSocket.connect(peer, env), false);
    }

    public void listen(int port) throws Exception {
        SecureSocket.listen(port, this::onConnect, env);
    }

    public void initialize(PeerAddress initialPeer) throws Exception {
        addPeer(initialPeer);
    }

    public void confirmPeer(UserInfo user, SecureSocket sock) throws Exception {
        synchronized (peers) {
            SecureSocket oldVal = peers.putIfAbsent(user, sock);
            if(oldVal!=null) {
                throw new Exception("Peer collision, someone is hacking?");
            }else{
                peerConfirmed(user, sock);
            }
        }
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
            m.getEnv().getLogger().log(Level.INFO, "Receiving handshake ID from: "+msg.getUsername());
            try {
                m.respond(m.getEnv().getUserInfo().getIDMessage(), null);
                m.getEnv().getPm().confirmPeer(msg.getUserInfo(), m.getConn());
            } catch(Exception e) {
                m.getEnv().getLogger().log(Level.SEVERE,"Exception while handshaking");
                e.printStackTrace();
            }
        }
    }

    void newConnectionHandlerClient(SecureSocket sock) throws Exception {
        sock.sendFirstMessage(env.getUserInfo().getIDMessage(), (ReceivedMessage m) -> {
            //Handler for response
            UserInfo.UserIDMessage msg = m.getMessage();
            m.getEnv().getLogger().log(Level.INFO, "Receiving handshake ID from: "+msg.getUsername());
            try {
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

    @DefaultHandler(type=GetPeerListRequest.class)
    public static class GetPeerListRequestHandler implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) {
            GetPeerListRequest r = m.getMessage();
            EnvLogger logger = m.getEnv().getLogger();
            logger.log(Level.INFO, "Sending peer list");
            try {
                m.respond(m.getEnv().getPm().getPeerListResponse(), null);
            }catch(Exception e) {
                logger.log(Level.SEVERE, "Error sending peer list");
            }
        }
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
        private boolean isServer;

        public String id() {
            return String.format("%s:%d", ip, port);
        }

        public PeerAddress(String ip, int port, boolean isServer) {
            this.ip = ip;
            this.port = port;
            this.isServer = isServer;
        }

        public void setIp(String ip) {
            this.ip = ip;
        }

        public void setPort(int port) {
            this.port = port;
        }

        public void setIsServer(boolean server) {
            isServer = server;
        }

        public String getIp() {
            return ip;
        }

        public int getPort() {
            return port;
        }

        public boolean getIsServer() { return isServer; }
    }
}
