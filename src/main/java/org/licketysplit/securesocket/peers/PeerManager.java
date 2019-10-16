package org.licketysplit.securesocket.peers;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.onconnect.NewConnectionHandler;
import org.licketysplit.securesocket.onconnect.NotifyPeersOnConnect;
import org.licketysplit.securesocket.onconnect.SyncPeerListOnConnect;

import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Base64;
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
        onConnectHandlers.add(new NotifyPeersOnConnect());
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

    public void listenInNewThread() throws Exception {
        new Thread(() -> {
            try {
                listen();
            }catch(Exception e) {

            }
        }).start();
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
        fmt.append("Peer list ("+peers.size()+" peers): \n");
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

    public void messageAllPeers(Message m, MessageHandler handler) throws Exception {
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(m, handler);
        }
    }

    @DefaultHandler(type= SecurityHandshake.SendPublicKeyMessage.class)
    public static class ReceivePublicKeyMessage implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            m.getEnv().getLogger().log(Level.INFO, "Receiving public key, generating symmetric key and sending");
            SecurityHandshake.SendPublicKeyMessage msg = m.getMessage();
            AsymmetricCipher encryptor = new AsymmetricCipher();
            encryptor.setPublicKey(msg.getKey());

            SymmetricCipher symmetricCipher = new SymmetricCipher();
            SymmetricCipher.SymmetricKey symmetricKey = symmetricCipher.generateKey();


            byte[] encryptedKey = encryptor.encrypt(symmetricKey.getKey().getEncoded());
            byte[] encryptedIv = encryptor.encrypt(symmetricKey.getIv());
            m.getEnv().getLogger().log(
                    Level.INFO, String.format("KEY: %s, IV: %s\n",
                            Base64.getEncoder().encodeToString(symmetricKey.getKey().getEncoded()),
                            Base64.getEncoder().encodeToString(symmetricKey.getIv())));
            SecurityHandshake.SendSymmetricKeyMessage nMsg = new SecurityHandshake.SendSymmetricKeyMessage(encryptedKey, encryptedIv);
            nMsg.activateEncryption();
            m.getConn().setCipher(symmetricCipher);
            m.respond(nMsg, null);
        }
    }

    void newConnectionHandlerClient(SecureSocket sock) throws Exception {
        log.log(Level.INFO, "Generating keypair and sending public key");
        AsymmetricCipher decryptor = new AsymmetricCipher();
        KeyPair keyPair = decryptor.generateKeyPair();
        decryptor.setPrivateKey(keyPair.getPrivate());
        sock.sendFirstMessage(new SecurityHandshake.SendPublicKeyMessage(keyPair.getPublic().getEncoded()),
                (ReceivedMessage m) -> {
                    log.log(Level.INFO, "Received symmetric key, starting user handshake");
                    SecurityHandshake.SendSymmetricKeyMessage msg = m.getMessage();
                    byte[] key = decryptor.decrypt(msg.getEncryptedKey());
                    byte[] iv = decryptor.decrypt(msg.getEncryptedIv());

                    log.log(
                            Level.INFO, String.format("Received KEY: %s, IV: %s\n",
                                    Base64.getEncoder().encodeToString(key),
                                    Base64.getEncoder().encodeToString(iv)));

                    SymmetricCipher symmetricCipher = new SymmetricCipher();
                    symmetricCipher.setKey(key, iv);
                    m.getConn().setCipher(symmetricCipher);
                    m.getConn().activateEncryption();

                    sock.sendFirstMessage(new UserInfo.UserIDMessage(env.getUserInfo()), (ReceivedMessage m3) -> {
                        //Handler for response
                        UserInfo.UserIDMessage userInfoMsg = m3.getMessage();
                        //m.getEnv().getLogger().log(Level.INFO, "Receiving FINAL handshake ID from: "+msg.getUserInfo().getUsername());
                        try {
                            m3.getConn().setServerInfo(userInfoMsg.getUserInfo().getServer());
                            m3.getEnv().getPm().confirmPeer(userInfoMsg.getUserInfo(), m3.getConn());
                        } catch(Exception e) {
                            m3.getEnv().getLogger().log(Level.SEVERE, "Exception while handshaking");
                            e.printStackTrace();
                        }
                    });
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
