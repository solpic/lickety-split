package org.licketysplit.securesocket.peers;

import com.sun.net.httpserver.Authenticator;
import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.env.Retrier;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.*;
import org.licketysplit.securesocket.onconnect.NewConnectionHandler;
import org.licketysplit.securesocket.onconnect.NotifyPeersOnConnect;
import org.licketysplit.securesocket.onconnect.SyncManifestOnConnect;
import org.licketysplit.securesocket.onconnect.SyncPeerListOnConnect;

import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PeerManager implements SecureSocket.NewConnectionCallback {
    Environment env;
    EnvLogger log;
    List<NewConnectionHandler> onConnectHandlers;

    public void confirmClosed(SecureSocket secureSocket) {
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            if(peer.getValue()==secureSocket) {
                peers.remove(peer.getKey());

                env.changes.runHandler("peerlist-changed", peers.size());
                return;
            }
        }
    }

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
        onConnectHandlers.add(new SyncManifestOnConnect());
    }
    public void addConnectionHandler(NewConnectionHandler handler) {
        onConnectHandlers.add(handler);
    }
    public void setEnv(Environment env) {
        this.env = env;
//        this.retry.env = env;
        log = this.env.getLogger();
    }


    void createSocketAndConnect(PeerAddress peer) throws Exception {
        if (peer.getServerInfo() == null) return;
        // First check that we should connect
        if (peer.getUser().getUsername().equals(env.getUserInfo().getUsername())) return;
//        env.log(String.format("Connecting to %s, I am %s %b",
//                peer.getUser().getUsername(),
//                env.getUserInfo().getUsername(),
//                peer.getUser().getUsername().equals(env.getUserInfo().getUsername())));

        if (peers.containsKey(peer)) return;
        for (Map.Entry<UserInfo, SecureSocket> peerInfo : peers.entrySet()) {
            if (peer.getUser().getUsername().equals(peerInfo.getKey().getUsername())) return;
        }
        //log.log(Level.INFO,
        //        String.format("Connecting to user: %s, at IP: %s, port %d",
        //                peer.getUser().getUsername(), peer.ip, peer.getServerInfo().port));
        newConnectionHandler(peer.getUser().getUsername(), SecureSocket.connect(peer, env), false);
    }

//    Retrier retry = new Retrier(new int[]{5000, 5000, 5000, 5000, 5000, 10000, 10000, 10000, 20000, 20000, 20000, 20000}, 60000);

    private void connectToPeer(PeerAddress peer) throws Exception {
        String username = peer.getUser().getUsername();
        if (peer.getServerInfo() == null) {
            return;
        }
        // First check that we should connect
        if (peer.getUser().getUsername().equals(env.getUserInfo().getUsername())) return;

        if (peers.containsKey(peer)) return;
        if(peers.keySet().stream().filter(e->e.getUsername().equals(username)).count()>0) {
            env.log(String.format("Peer already connected '%s', abort", username));
            try {
                throw new Exception("See stack");
            }catch(Exception e) {
                env.log("Aborting retry", e);
            }
            return;
        }else{
            env.log(String.format("Not connected to '%s', continuing", username));
        }
        new Thread(() -> {
            try {
                    createSocketAndConnect(peer);
            } catch(Exception e) {
                env.getLogger().log(Level.INFO,
                        "Error starting addPeer",
                        e);
            }
        }).start();
    }

//    public void retryAddPeer(PeerAddress peer) throws Exception {
//        addPeerWrapper(peer, false);
//    }
//
//    public void addPeer(PeerAddress peer) throws Exception {
//        addPeerWrapper(peer, true);
//    }

    public static class SyncInfoDir extends JSONMessage {
        PeerInfoDirectory info;

        public PeerInfoDirectory getInfo() {
            return info;
        }

        public void setInfo(PeerInfoDirectory info) {
            this.info = info;
        }

        public SyncInfoDir() {
        }

        public SyncInfoDir(PeerInfoDirectory info) {
            this.info = info;
        }
    }

    @DefaultHandler(type=SyncInfoDir.class)
    public static class SyncInfoDirHandler implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            SyncInfoDir msg = m.getMessage();
            m.getEnv().getLogger().log(Level.INFO, "Receiving sync info dir");
            m.getEnv().getInfo().syncInfo(msg.getInfo());
        }
    }

//    public void newPeer(String username) throws Exception {
//        addPeer(peerFromUsername(username));
//        messageAllPeers(new SyncInfoDir(env.getInfo()), null);
//    }
    public void userBanned(String username) throws Exception {
        synchronized (peers) {
            for (Map.Entry<UserInfo, SecureSocket> peerEntry : peers.entrySet()) {
                if(peerEntry.getKey().getUsername().equals(username)) {
                    peerEntry.getValue().close(false);
                    peers.remove(peerEntry.getKey());
                    break;
                }
            }
        }
        env.getLogger().log(Level.INFO, "Banned user and syncing info dir");
        messageAllPeers(new SyncInfoDir(env.getInfo()), null);
    }

    public void listen() throws Exception {
        SecureSocket.listen(this.env.getUserInfo().getServer().getPort(), this::onConnect, env);
    }

    public void listenInNewThread() throws Exception {
        new Thread(() -> {
            try{
                listen();
            } catch(Exception e){
                env.getLogger().log(Level.INFO, "Exception while listening", e);
            }
        }).start();
    }

    void connectToPeersInThread() {
        new Thread(() -> {
            do {
                try {
                    List<String> connectedPeers = peers.keySet().stream().map(e -> e.getUsername()).collect(Collectors.toList());
                    List<PeerAddress> peerAddresses = env.getInfo().getPeers().entrySet().stream().map(e -> e.getValue().convertToPeerAddress()).collect(Collectors.toList());

                    for (PeerAddress peerAddress : peerAddresses) {
                        if (!connectedPeers.contains(peerAddress.getUser().getUsername())) {
                            connectToPeer(peerAddress);
                        }
                    }
                    Thread.sleep(10000);
                }catch(Exception e) {
                    env.getLogger().log("Error during peer connector thread", e);
                }
            } while(true);
        }).start();
    }

    public void start() throws Exception {
        new Thread(() -> {
            try {
                env.getLogger().log("Starting peer manager");
                listenInNewThread();
                connectToPeersInThread();
//
//                Map<String, PeerInfoDirectory.PeerInfo> peers = env.getInfo().getPeers();
//                for (Map.Entry<String, PeerInfoDirectory.PeerInfo> peer : peers.entrySet()) {
//                    try {
//                        PeerAddress address = peer.getValue().convertToPeerAddress();
//                        addPeer(address);
//                    } catch(Exception e) {
//                        env.getLogger().log(Level.SEVERE,
//                                String.format("Couldn't connect to peer %s at ip %s, port %s",
//                                        peer.getValue().getUsername(),
//                                        peer.getValue().getServerIp(),
//                                        peer.getValue().getServerPort()), e);
//                    }
//                }
            }catch(Exception e) {
                env.log("Error during start", e);
                e.printStackTrace();
            }
        }).start();
    }

    public void confirmPeer(String username, UserInfo user, SecureSocket sock, boolean isServer) throws Exception {
        sock.peerUsername = username;
        synchronized (peers) {
            if(username.equals(env.getUserInfo().getUsername())) return;
            SecureSocket oldVal = peers.putIfAbsent(user, sock);
            if(oldVal==null) {
                sock.setUserInfo(user);
                peerConfirmed(user, sock, isServer);
                logPeerList();
            }
        }
    }

    public void logPeerList() {
        StringBuilder fmt = new StringBuilder();
        fmt.append("Peer list ("+peers.size()+" peers): \n");
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            fmt.append(String.format("\tUsername: %s\n", peer.getKey().getUsername()));
        }
        log.log(Level.INFO, fmt.toString());
    }

    public void peerConfirmed(UserInfo user, SecureSocket sock, boolean isServer) throws Exception {
        log.log(Level.INFO, "New peer '"+user.getUsername()+"' has been confirmed");
        log.log(Level.INFO, "Total peer count: "+peers.size());
        env.getDebug().trigger("peerConfirmed", user, sock);
        env.changes.runHandler("peerlist-changed", peers.size());
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
                    m.getEnv().log("Error during public", e);
                    e.printStackTrace();
                }
            }).start();
        }
    }

    ConcurrentHashMap<String, Object> handshakeLocks = new ConcurrentHashMap<String, Object>();
    Object getHandshakeLock(String username) {
        synchronized (handshakeLocks) {
            if(handshakeLocks.containsKey(username)) {
                return handshakeLocks.get(username);
            }else{
                handshakeLocks.put(username, new Object());
                return handshakeLocks.get(username);
            }
        }
    }

    int handshakeTimeout = 10000;
    void newConnectionHandlerServer(ReceivedMessage m) throws Exception {
        // NEXT MESSAGE
        // Receive public key
        String toUser = "unknown";
        boolean hasUsername = false;
        SecureSocket sock = m.getConn();
        SecureSocket.TimeoutException exceptor = () -> {
            throw new Exception("Message timed out");
        };
        try {
            env.getDebug().trigger("handshaking");
            SecurityHandshake.SendPublicKeyMessage msg = m.getMessage();
            toUser = msg.getUsername();
            hasUsername = true;

            m.getEnv().getLogger().log(Level.INFO, "Server beginning handshake with " + toUser);
            AsymmetricCipher encryptor = new AsymmetricCipher();
            encryptor.setPublicKey(msg.getKey());

            // Generate symmetric key
            SymmetricCipher symmetricCipher = new SymmetricCipher();
            SymmetricCipher.SymmetricKey symmetricKey = symmetricCipher.generateKey();


            byte[] encryptedKey = encryptor.encrypt(symmetricKey.getKey().getEncoded());
            byte[] encryptedIv = encryptor.encrypt(symmetricKey.getIv());
            env.log(String.format("Sending symmetric key '%s'", toUser));
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
             log.log(Level.INFO, String.format("Sending symmetric key WITH BAD BYTES, Waiting for user id message '%s'", toUser));
            ReceivedMessage userIdMessage = m.respondAndWait(nMsg, exceptor, handshakeTimeout);

            UserInfo.UserIDMessage userId = userIdMessage.getMessage();
            String username = userId.getUserInfo().getUsername();
            if (env.getInfo().getPeers().get(username) == null) {
                //log.log(Level.INFO, "Unknown username '"+username+"', attempting to add and verify");
                env.getInfo().newPeerAndConfirm(userId.getPeerInfo());
                env.getInfo().save();
            }
            userIdMessage.getEnv().getDebug().trigger("handshaking");
            m.getEnv().getLogger().log(Level.INFO, String.format("Receiving INITIAL handshake ID '%s'", toUser));
            m.getConn().setServerInfo(userId.getUserInfo().getServer());

            m = userIdMessage.respondAndWait(new UserInfo.UserIDMessage(userIdMessage.getEnv().getUserInfo(), env.getInfo().myInfo(env)), exceptor, handshakeTimeout);


            byte[] theirIdKey = env.getInfo().getPeers().get(userId.getUserInfo().getUsername()).getIdentityKey();
            byte[] plaintext = new byte[AsymmetricCipher.idBlockSize()];
            AsymmetricCipher cipher = new AsymmetricCipher();
            cipher.setPublicKey(theirIdKey);
            Random rnd = new Random(System.currentTimeMillis());
            rnd.nextBytes(plaintext);
            AsymmetricCipher myCipher = new AsymmetricCipher();
            myCipher.setPrivateKey(env.getIdentityKey().getKey());

            MapMessage theirIdConfirm = m.getMessage();
            byte[] ciphertext = Base64.getDecoder().decode((String) theirIdConfirm.val().get("ciphertext"));
            MapMessage idConfirmResponse = new MapMessage();
            idConfirmResponse.val().put("plaintext", myCipher.decrypt(ciphertext));
            idConfirmResponse.val().put("ciphertext", cipher.encrypt(plaintext));

            log.log(Level.INFO, "Sending ID confirm response and checking their ID");
            m = m.respondAndWait(idConfirmResponse, exceptor, handshakeTimeout);
            MapMessage idConfirm = m.getMessage();
            if (!keysEqual(plaintext, Base64.getDecoder().decode((String) idConfirm.val().get("plaintext")))) {
                throw new Exception();
            }


            m.getEnv().getLogger().log(Level.INFO, "Server ending handshake with " + toUser);
            m.getEnv().getPm().confirmPeer(toUser, userId.getUserInfo(), m.getConn(), true);

        }catch (Exception e) {
            env.getLogger().log(
                    Level.INFO,
                    String.format("Handshaking error"),
                    e
            );
            sock.close(false);
//            if(hasUsername) {
//                retryAddPeer(peerFromUsername(toUser));
//            }
            return;
//            e.printStackTrace();
//            throw new Exception("Error handshaking with "+toUser);
        }
    }

    public PeerAddress peerFromUsername(String username) {
        try {
            return env.getInfo().getPeers().get(username).convertToPeerAddress();
        } catch(Exception e) {
            env.getLogger().log(Level.INFO,
                    "Error getting peer info for "+username, e);
            return null;
        }
    }

    public String peerStatus(String username) {
        if(peers.keySet().stream().filter(e->e.getUsername().equals(username)).count()>0) {
            return "Connected";
        }else if (env.getUserInfo().getUsername().equals(username)){
            return "This is you";
        }else if(env.getInfo().peers.get(username).getBan()!=null) {
            return "Banned";
        }else {
            return "N/A";
        }
    }

    void newConnectionHandlerClient(String lockUsername, SecureSocket sock) throws Exception {
        Object handshakeLock = getHandshakeLock(lockUsername);
        synchronized (handshakeLock) {
            String toUser = "unknown";
            boolean hasUsername = false;
            SecureSocket.TimeoutException exceptor = () -> {
                throw new Exception("Message timed out");
            };
            try {
                // NEXT MESSAGE
                // SEND PUBLIC KEY
                log.log(Level.INFO, "Client starting handshake");
                env.getDebug().trigger("handshaking");
                AsymmetricCipher decryptor = new AsymmetricCipher();
                KeyPair keyPair = decryptor.generateKeyPair();
                decryptor.setPrivateKey(keyPair.getPrivate());
                SecurityHandshake.SendPublicKeyMessage publicKeyMsg =
                        new SecurityHandshake.SendPublicKeyMessage(keyPair.getPublic().getEncoded(), env.getUserInfo().getUsername());

                ReceivedMessage m = sock.sendMessageAndWait(
                        publicKeyMsg,
                        SecureSocket.timeoutFactory(String.format("Message timed out")),
                        handshakeTimeout
                );


                // NEXT MESSAGE
                // RECEIVE SYMMETRIC KEY
                env.getDebug().trigger("handshaking");
                SecurityHandshake.SendSymmetricKeyMessage msg = m.getMessage();

                toUser = msg.getUsername();
                hasUsername = true;
                log.log(Level.INFO, "Received symmetric key WITH BAD BYTES, starting user handshake with " + toUser);

                byte[] key = decryptor.decrypt(msg.getEncryptedKey());
                byte[] iv = decryptor.decrypt(msg.getEncryptedIv());

                log.log(String.format("Received key from '%s'", toUser));
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
                ReceivedMessage userIdResponse = m.respondAndWait(new UserInfo.UserIDMessage(env.getUserInfo(), env.getInfo().myInfo(env)), exceptor, handshakeTimeout);
                log.log(Level.INFO, "Received user ID from " + toUser);
                env.getDebug().trigger("handshaking");
                UserInfo.UserIDMessage userInfoMsg = userIdResponse.getMessage();

                String username = userInfoMsg.getUserInfo().getUsername();

                if (env.getInfo().getPeers().get(username) == null) {
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

                log.log(Level.INFO, "Checking their ID");
                ReceivedMessage theirIdConfirm = userIdResponse.respondAndWait(idConfirm, exceptor, handshakeTimeout);
                MapMessage idConfirmResponse = theirIdConfirm.getMessage();

                if (!keysEqual(plaintext, Base64.getDecoder().decode((String) idConfirmResponse.val().get("plaintext")))) {
                    throw new Exception("ID verification failed");
                }
                byte[] ciphertext = Base64.getDecoder().decode((String) idConfirmResponse.val().get("ciphertext"));
                AsymmetricCipher myCipher = new AsymmetricCipher();
                myCipher.setPrivateKey(env.getIdentityKey().getKey());

                MapMessage confirmMyId = new MapMessage();
                confirmMyId.val().put("plaintext", myCipher.decrypt(ciphertext));

                //log.log(Level.INFO, "Verifying own ID");
                theirIdConfirm.respond(confirmMyId, null);

                log.log(Level.INFO, "Client ending handshake with " + username);
                m.getEnv().getPm().confirmPeer(username, userInfoMsg.getUserInfo(), userIdResponse.getConn(), false);
            } catch (Exception e) {
                env.getLogger().log(Level.INFO, "Handshake error", e);
                e.printStackTrace();
                sock.close(false);
//                if (hasUsername) {
//                    retryAddPeer(peerFromUsername(toUser));
//                }
                throw new Exception("Error handshaking with " + toUser);
            }
        }
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

        return new GetPeerListResponse(peerInfoList, env.getInfo());
    }

    void newConnectionHandler(String username, SecureSocket sock, boolean isServer) throws Exception{
        if(sock==null) return;
        if(!isServer) {
            new Thread(() -> {
                try {
                    newConnectionHandlerClient(username, sock);
                }catch(Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    @Override
    public void onConnect(SecureSocket sock) throws Exception {
        newConnectionHandler(null, sock, true);
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
