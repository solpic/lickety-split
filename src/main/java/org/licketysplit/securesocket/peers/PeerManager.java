package org.licketysplit.securesocket.peers;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.securesocket.onconnect.NewConnectionHandler;
import org.licketysplit.securesocket.onconnect.NotifyPeersOnConnect;
import org.licketysplit.securesocket.onconnect.SyncPeerListOnConnect;

import java.security.KeyPair;
import java.util.*;
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
        //log.log(Level.INFO,
        //        String.format("Connecting to user: %s, at IP: %s, port %d",
        //                peer.getUser().getUsername(), peer.ip, peer.getServerInfo().port));
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

    public void start() throws Exception {
        listenInNewThread();

        Map<String, PeerInfoDirectory.PeerInfo> peers = env.getInfo().getPeers();
        for (Map.Entry<String, PeerInfoDirectory.PeerInfo> peer : peers.entrySet()) {
            PeerAddress address = peer.getValue().convertToPeerAddress();
            addPeer(address);
        }
    }

    public void confirmPeer(UserInfo user, SecureSocket sock) throws Exception {
        synchronized (peers) {
            SecureSocket oldVal = peers.putIfAbsent(user, sock);
            if(oldVal==null) {
                sock.setUserInfo(user);
                peerConfirmed(user, sock);
                //logPeerList();
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
        env.getDebug().trigger("peerConfirmed", user, sock);
        for (NewConnectionHandler handler : onConnectHandlers) {
            handler.connectionConfirmed(user, sock, env);
        }
    }

    ConcurrentHashMap<UserInfo, SecureSocket> peers;



    public void messageAllPeers(Message m, MessageHandler handler) throws Exception {
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(m, handler);
        }
    }



    @DefaultHandler(type= SecurityHandshake.SendPublicKeyMessage.class)
    public static class ReceivePublicKeyMessage implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            new Thread(() -> {
                try {
                    m.getEnv().getPm().newConnectionHandlerServer(m);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }


    void newConnectionHandlerServer(ReceivedMessage m) throws Exception {
        // NEXT MESSAGE
        // Receive public key
        env.getDebug().trigger("handshaking");
        SecurityHandshake.SendPublicKeyMessage msg = m.getMessage();
        String toUser = msg.getUsername();
        m.getEnv().getLogger().log(Level.INFO, "Server beginning handshake with "+toUser);
        AsymmetricCipher encryptor = new AsymmetricCipher();
        encryptor.setPublicKey(msg.getKey());

        // Generate symmetric key
        SymmetricCipher symmetricCipher = new SymmetricCipher();
        SymmetricCipher.SymmetricKey symmetricKey = symmetricCipher.generateKey();


        byte[] encryptedKey = encryptor.encrypt(symmetricKey.getKey().getEncoded());
        byte[] encryptedIv = encryptor.encrypt(symmetricKey.getIv());
//        m.getEnv().getLogger().log(
//                Level.INFO, String.format("Sending KEY to %s: %s, IV: %s",
//                        toUser,
//                        Base64.getEncoder().encodeToString(symmetricKey.getKey().getEncoded()),
//                        Base64.getEncoder().encodeToString(symmetricKey.getIv())));
//        env.getDebug().trigger("handshaking");



        // NEXT MESSAGE
        // Send symmetric key
        SecurityHandshake.SendSymmetricKeyMessage nMsg = new SecurityHandshake.SendSymmetricKeyMessage(encryptedKey, encryptedIv, m.getEnv().getUserInfo().getUsername());
        env.getDebug().trigger("handshaking");
        nMsg.activateEncryption();
        m.getConn().setCipher(symmetricCipher);



        // NEXT MESSAGE
        // Exchange user ids
       // log.log(Level.INFO, "Waiting for user id message");
        ReceivedMessage userIdMessage = m.respondAndWait(nMsg);

        UserInfo.UserIDMessage userId = userIdMessage.getMessage();
        String username = userId.getUserInfo().getUsername();
        if(env.getInfo().getPeers().get(username)==null) {
            //log.log(Level.INFO, "Unknown username '"+username+"', attempting to add and verify");
            env.getInfo().newPeerAndConfirm(userId.getPeerInfo());
            env.getInfo().save();
        }
        userIdMessage.getEnv().getDebug().trigger("handshaking");
        //m.getEnv().getLogger().log(Level.INFO, "Receiving INITIAL handshake ID");
        m.getConn().setServerInfo(userId.getUserInfo().getServer());

        m = userIdMessage.respondAndWait(new UserInfo.UserIDMessage(userIdMessage.getEnv().getUserInfo(), env.getInfo().myInfo(env)));


        byte[] theirIdKey = env.getInfo().getPeers().get(userId.getUserInfo().getUsername()).getIdentityKey();
        byte[] plaintext = new byte[AsymmetricCipher.idBlockSize()];
        AsymmetricCipher cipher = new AsymmetricCipher();
        cipher.setPublicKey(theirIdKey);
        Random rnd = new Random(System.currentTimeMillis());
        rnd.nextBytes(plaintext);
        AsymmetricCipher myCipher = new AsymmetricCipher();
        myCipher.setPrivateKey(env.getIdentityKey().getKey());

        MapMessage theirIdConfirm = m.getMessage();
        byte[] ciphertext = Base64.getDecoder().decode((String)theirIdConfirm.val().get("ciphertext"));
        MapMessage idConfirmResponse = new MapMessage();
        idConfirmResponse.val().put("plaintext", myCipher.decrypt(ciphertext));
        idConfirmResponse.val().put("ciphertext", cipher.encrypt(plaintext));

        //log.log(Level.INFO, "Sending ID confirm response and checking their ID");
        m = m.respondAndWait(idConfirmResponse);
        MapMessage idConfirm = m.getMessage();
        if(!keysEqual(plaintext, Base64.getDecoder().decode((String)idConfirm.val().get("plaintext")))) {
            throw new Exception();
        }


        m.getEnv().getLogger().log(Level.INFO, "Server ending handshake with "+toUser);
        m.getEnv().getPm().confirmPeer(userId.getUserInfo(), m.getConn());
    }

    void newConnectionHandlerClient(SecureSocket sock) throws Exception {
        // NEXT MESSAGE
        // SEND PUBLIC KEY
        log.log(Level.INFO, "Client starting handshake");
        env.getDebug().trigger("handshaking");
        AsymmetricCipher decryptor = new AsymmetricCipher();
        KeyPair keyPair = decryptor.generateKeyPair();
        decryptor.setPrivateKey(keyPair.getPrivate());
        SecurityHandshake.SendPublicKeyMessage publicKeyMsg =
                new SecurityHandshake.SendPublicKeyMessage(keyPair.getPublic().getEncoded(), env.getUserInfo().getUsername());

        ReceivedMessage m = sock.sendMessageAndWait(publicKeyMsg);



        // NEXT MESSAGE
        // RECEIVE SYMMETRIC KEY
        env.getDebug().trigger("handshaking");
       // log.log(Level.INFO, "Received symmetric key, starting user handshake");
        SecurityHandshake.SendSymmetricKeyMessage msg = m.getMessage();

        String toUser = msg.getUsername();
        byte[] key = decryptor.decrypt(msg.getEncryptedKey());
        byte[] iv = decryptor.decrypt(msg.getEncryptedIv());

//        log.log(
//                Level.INFO, String.format("Received KEY from: %s, Key: %s, IV: %s",
//                        toUser,
//                        Base64.getEncoder().encodeToString(key),
//                        Base64.getEncoder().encodeToString(iv)));

        SymmetricCipher symmetricCipher = new SymmetricCipher();
        symmetricCipher.setKey(key, iv);
        m.getConn().setCipher(symmetricCipher);
        m.getConn().activateEncryption();

        // NEXT MESSAGE
        // SEND USER INFO
        ReceivedMessage userIdResponse = m.respondAndWait(new UserInfo.UserIDMessage(env.getUserInfo(), env.getInfo().myInfo(env)));
        //log.log(Level.INFO, "Received user ID");
        env.getDebug().trigger("handshaking");
        UserInfo.UserIDMessage userInfoMsg = userIdResponse.getMessage();

        String username = userInfoMsg.getUserInfo().getUsername();

        if(env.getInfo().getPeers().get(username)==null) {
       //     log.log(Level.INFO, "Unknown username '"+username+"', attempting to add and verify");
            env.getInfo().newPeerAndConfirm(userInfoMsg.getPeerInfo());
            env.getInfo().save();
        }
        m.getConn().setServerInfo(userInfoMsg.getUserInfo().getServer());

        // Now we confirm identity using the identity keys
        MapMessage idConfirm = new MapMessage();
        byte[] plaintext = new byte[AsymmetricCipher.idBlockSize()];
        byte[] theirIdKey = env.getInfo().getPeers().get(username).getIdentityKey();
        AsymmetricCipher cipher = new AsymmetricCipher();
        cipher.setPublicKey(theirIdKey);
        Random rnd = new Random(System.currentTimeMillis());
        rnd.nextBytes(plaintext);

        idConfirm.val().put("ciphertext", cipher.encrypt(plaintext));

        //log.log(Level.INFO, "Checking their ID");
        ReceivedMessage theirIdConfirm = userIdResponse.respondAndWait(idConfirm);
        MapMessage idConfirmResponse = theirIdConfirm.getMessage();

        if(!keysEqual(plaintext, Base64.getDecoder().decode((String)idConfirmResponse.val().get("plaintext")))) {
            throw new Exception();
        }
        byte[] ciphertext = Base64.getDecoder().decode((String)idConfirmResponse.val().get("ciphertext"));
        AsymmetricCipher myCipher = new AsymmetricCipher();
        myCipher.setPrivateKey(env.getIdentityKey().getKey());

        MapMessage confirmMyId = new MapMessage();
        confirmMyId.val().put("plaintext", myCipher.decrypt(ciphertext));

        //log.log(Level.INFO, "Verifying own ID");
        theirIdConfirm.respond(confirmMyId, null);

        log.log(Level.INFO, "Client ending handshake with "+username);
        m.getEnv().getPm().confirmPeer(userInfoMsg.getUserInfo(), userIdResponse.getConn());
    }

    public boolean keysEqual(byte[] a, byte[] b) {
        if(a.length!=b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if(a[i]!=b[i]) return false;
        }
        return true;
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
            new Thread(() -> {
                try {
                    newConnectionHandlerClient(sock);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
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
