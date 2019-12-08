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

/**
 * This class fulfills a few major functions. One, it stores all SecureSocket
 * instances that have been fully handshaked. This means that if another
 * part of an application wants to send a message to a handshaked peer,
 * they need to go through this class first. Secondly, this class implements
 * the handshaking code, which is quite complex.
 */
public class PeerManager implements SecureSocket.NewConnectionCallback {
    /**
     * This peer's environment.
     */
    Environment env;
    /**
     * The logger.
     */
    EnvLogger log;
    /**
     * Callback functions to be called after handshaking
     */
    List<NewConnectionHandler> onConnectHandlers;

    /**
     * Called when a socket has been closed for some reason. Removes it
     * from the map.
     *
     * @param secureSocket the socket being closed
     */
    public void confirmClosed(SecureSocket secureSocket) {
        // Find socket in map and remove
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            if(peer.getValue()==secureSocket) {
                peers.remove(peer.getKey());

                env.changes.runHandler("peerlist-changed", peers.size());
                return;
            }
        }
    }

    /**
     * Class containing information about how to connect to a peer,
     * their IP address and port.
     */
    public static class ServerInfo {
        /**
         * The Port.
         */
        int port;
        /**
         * The IP address.
         */
        String ip;

        /**
         * Gets ip.
         *
         * @return the ip
         */
        public String getIp() {
            return ip;
        }

        /**
         * Sets ip.
         *
         * @param ip the ip
         */
        public void setIp(String ip) {
            this.ip = ip;
        }

        /**
         * Instantiates a new Server info.
         */
        public ServerInfo() {}

        /**
         * Gets port.
         *
         * @return the port
         */
        public int getPort() {
            return port;
        }

        /**
         * Sets port.
         *
         * @param port the port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Instantiates a new Server info.
         *
         * @param port the port
         * @param ip   the ip
         */
        public ServerInfo(int port, String ip) {
            this.port = port;
            this.ip = ip;
        }
    }

    /**
     * Gets map of handshaked and connected peers.
     *
     * @return the map of peers
     */
    public ConcurrentHashMap<UserInfo, SecureSocket> getPeers() {
        return peers;
    }

    /**
     * Instantiates a new Peer manager.
     */
    public PeerManager() {
        peers = new ConcurrentHashMap<>();
        initConnectHandlers();
    }

    /**
     * Registers some handlers to be called after successful handshake.
     * These handlers will be called immediately after handshaking.
     */
    void initConnectHandlers() {
        onConnectHandlers = new ArrayList<>();
        onConnectHandlers.add(new SyncPeerListOnConnect());
        onConnectHandlers.add(new NotifyPeersOnConnect());
        onConnectHandlers.add(new SyncManifestOnConnect());
    }

    /**
     * No longer used.
     *
     * @param handler the handler
     */
    public void addConnectionHandler(NewConnectionHandler handler) {
        onConnectHandlers.add(handler);
    }

    /**
     * Sets Environment.
     *
     * @param env the env
     */
    public void setEnv(Environment env) {
        this.env = env;
//        this.retry.env = env;
        log = this.env.getLogger();
    }


    /**
     * Attempts to initiate a TCP connection and then handshake with a
     * peer indicated by this PeerAddress. First checks to make sure that
     * we aren't already connected/handshaked with this peer before continuing.
     *
     * @param peer the peer to connect to
     * @throws Exception the exception
     */
    void createSocketAndConnect(PeerAddress peer) throws Exception {
        if (peer.getServerInfo() == null) return;
        // Check this this isn't us
        if (peer.getUser().getUsername().equals(env.getUserInfo().getUsername())) return;

        // Check we aren't connected by looking for the PeerAddress
        if (peers.containsKey(peer)) return;
        // Check that we aren't connected by looking for the username
        for (Map.Entry<UserInfo, SecureSocket> peerInfo : peers.entrySet()) {
            if (peer.getUser().getUsername().equals(peerInfo.getKey().getUsername())) return;
        }

        // Otherwise, connect and handshake
        newConnectionHandler(peer.getUser().getUsername(), SecureSocket.connect(peer, env), false);
    }

    /**
     * Checks that we aren't connected to a peer, then calls
     * createSocketAndConnect in a new thread.
     * @param peer to connect to
     * @throws Exception
     */
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

    /**
     * Message used to sync two peer's peer info directories.
     */
    public static class SyncInfoDir extends JSONMessage {
        /**
         * The peer info directory that is being sent.
         */
        PeerInfoDirectory info;

        /**
         * Gets info directory.
         *
         * @return the info
         */
        public PeerInfoDirectory getInfo() {
            return info;
        }

        /**
         * Sets info directory.
         *
         * @param info the info
         */
        public void setInfo(PeerInfoDirectory info) {
            this.info = info;
        }

        /**
         * Instantiates a new Sync info dir.
         */
        public SyncInfoDir() {
        }

        /**
         * Instantiates a new Sync info dir.
         *
         * @param info the info
         */
        public SyncInfoDir(PeerInfoDirectory info) {
            this.info = info;
        }
    }

    /**
     * Default handler for a SyncInfoDir message.
     * The handler will take their PeerInfoDirectory and sync it with ours.
     * If there are changes, we will then sync our info directory with all
     * connected to peers to propagate those changes. This sounds performance
     * heavy, but this only happens if there are new users or bans which is quite rare.
     */
    @DefaultHandler(type=SyncInfoDir.class)
    public static class SyncInfoDirHandler implements MessageHandler {

        /**
         * Called when the message is received, calls the syncInfo function
         * on their PeerInfoDirectory.
         * @param m the received message
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            SyncInfoDir msg = m.getMessage();
            m.getEnv().getLogger().log(Level.INFO, "Receiving sync info dir");
            m.getEnv().getInfo().syncInfo(msg.getInfo());
        }
    }

    /**
     * Callback for when the PeerInfoDirectory discovers a user has been
     * banned. This finds the corresponding socket for this user and closes it
     * and then syncs our peer info directory with all our peers to propagate
     * the ban.
     *
     * @param username the user being banned
     * @throws Exception the exception
     */
    public void userBanned(String username) throws Exception {
        // Close socket if we are connected
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
        // Propagate ban to all peers
        messageAllPeers(new SyncInfoDir(env.getInfo()), null);
    }

    /**
     * Runs a TCP socket listen at the specified port, this method blocks.
     *
     * @throws Exception the exception
     */
    public void listen() throws Exception {
        SecureSocket.listen(this.env.getUserInfo().getServer().getPort(), this::onConnect, env);
    }

    /**
     * Calls the listen function in a new thread, since it blocks.
     *
     * @throws Exception the exception
     */
    public void listenInNewThread() throws Exception {
        new Thread(() -> {
            try{
                listen();
            } catch(Exception e){
                env.getLogger().log(Level.INFO, "Exception while listening", e);
            }
        }).start();
    }

    /**
     * This method handles initiating new connections to peers. It is started
     * when the PeerManager is started, and every 10 seconds it does the following:
     * gets a list of peers we are connected to, gets a list of all peers from the
     * peer info directory, for each peer we aren't connected to it attempts to connect
     * to said peer.
     */
    void connectToPeersInThread() {
        new Thread(() -> {
            do {
                try {
                    // Peers we are connected to
                    List<String> connectedPeers = peers.keySet().stream().map(e -> e.getUsername()).collect(Collectors.toList());
                    // Peers that are in the info directory
                    List<PeerAddress> peerAddresses = env.getInfo().getPeers().entrySet().stream().map(e -> e.getValue().convertToPeerAddress()).collect(Collectors.toList());

                    for (PeerAddress peerAddress : peerAddresses) {
                        if (!connectedPeers.contains(peerAddress.getUser().getUsername())) {
                            // If we aren't already connected to a peer attempt connection
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

    /**
     * This is the main entry point for the backend portion of the application.
     * This function will start two new threads, one that listens for
     * incoming TCP connections, and another thread that attempts connections to
     * all peers (and retries every 10 seconds).
     *
     * @throws Exception the exception
     */
    public void start() throws Exception {
        new Thread(() -> {
            try {
                env.getLogger().log("Starting peer manager");
                listenInNewThread();
                connectToPeersInThread();
            }catch(Exception e) {
                env.log("Error during start", e);
                e.printStackTrace();
            }
        }).start();
    }

    /**
     * Callback function when a handshake has completed successfully.
     * This inserts the new socket into the peers map, logs the peer list,
     * and calls another callback function to call the onConnect handlers.
     *
     * @param username the confirmed peer
     * @param user     metadata about the confirmed peer
     * @param sock     the socket that this was confirmed on
     * @param isServer true if we accepted an incoming TCP connection on the socket
     * @throws Exception the exception
     */
    public void confirmPeer(String username, UserInfo user, SecureSocket sock, boolean isServer) throws Exception {
        sock.peerUsername = username;
        synchronized (peers) {
            // This shouldn't happen, but we just want to make sure
            // we didn't just handshake with ourself
            if(username.equals(env.getUserInfo().getUsername())) return;
            SecureSocket oldVal = peers.putIfAbsent(user, sock);
            if(oldVal==null) {
                sock.setUserInfo(user);
                peerConfirmed(user, sock, isServer);
                logPeerList();
            }
        }
    }

    /**
     * Logs the list of peers in a human readable format,
     * for debugging purposes.
     */
    public void logPeerList() {
        StringBuilder fmt = new StringBuilder();
        fmt.append("Peer list ("+peers.size()+" peers): \n");
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            fmt.append(String.format("\tUsername: %s\n", peer.getKey().getUsername()));
        }
        log.log(Level.INFO, fmt.toString());
    }

    /**
     * Called when a peer has been confirmed. This function
     * does a bit of logging then runs all the NewConnectionHandlers
     * stored in the onConnectHandlers list.
     *
     * @param user     metadata about the confirmed peer
     * @param sock     the socket we confirmed on
     * @param isServer true if on this socket we are the server
     * @throws Exception the exception
     */
    public void peerConfirmed(UserInfo user, SecureSocket sock, boolean isServer) throws Exception {
        log.log(Level.INFO, "New peer '"+user.getUsername()+"' has been confirmed");
        log.log(Level.INFO, "Total peer count: "+peers.size());
        env.getDebug().trigger("peerConfirmed", user, sock);
        env.changes.runHandler("peerlist-changed", peers.size());
        for (NewConnectionHandler handler : onConnectHandlers) {
            handler.connectionConfirmed(user, sock, env);
        }
    }

    /**
     * Map of all connected peers.
     */
    ConcurrentHashMap<UserInfo, SecureSocket> peers;


    /**
     * Sends a message out to all peers.
     *
     * @param m       the message to send
     * @param handler the respond handler
     * @throws Exception the exception
     */
    public void messageAllPeers(Message m, MessageHandler handler) throws Exception {
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peer.getValue().sendFirstMessage(m, handler);
        }
    }


    /**
     * DefaultHandler for the SendPublicKeyMessage message. This
     * message is the first message that is sent during the handshake process,
     * and it is sent by the client. So upon receiving this message, we initiate
     * the server handshake procedure which is in newConnectionHandlerServer.
     */
    @DefaultHandler(type= SecurityHandshake.SendPublicKeyMessage.class)
    public static class ReceivePublicKeyMessage implements MessageHandler {

        /**
         * Called when this message is received, initiates server handshake
         * process.
         * @param m the received message
         * @throws Exception
         */
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

    /**
     * This is a map of username keys, to lock values. This prevents us from
     * accidentally handshaking with the same user more than once concurrently.
     * To handshake with a user you have to first acquire the lock.
     */
    ConcurrentHashMap<String, Object> handshakeLocks = new ConcurrentHashMap<String, Object>();

    /**
     * Gets the handshake lock for a username. If there is no lock object
     * we create one.
     *
     * @param username the user we want to handshake with
     * @return the handshake lock
     */
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

    /**
     * The timeout time in milliseconds for when messages should be considered
     * failed during handshaking. This prevents us from deadlocking on a failed handshake.
     */
    int handshakeTimeout = 10000;

    /**
     * This method runs the entire handshaking procedure on the server side.
     * Handshaking is a rather complex process, which has two goals. First,
     * to enable encryption, and second, to confirm the other peer's identity. <br><br>
     * Handshaking begins when the client generates an RSA keypair and sends
     * the server the public key. The server then generates an AES key, encrypts
     * it using the given public key, and sends the encrypted key to the client.
     * After sending that message the server then decrypts and encrypts all messages
     * using said AES key.<br><br>
     * The client decrypts AES key, and enables encryption so that all future messages
     * sent over the socket are encrypted and decrypted using the AES key. <br>
     * The client then responds with a message containing its username and other metadata,
     * and the server responds back with its own username and metadata. <br><br>
     * Next comes the identity confirmation process. The client then generates 128 bytes
     * of random data, which we will refer to as plaintext1.
     * Using the server's public identity key, the client encrypts the plaintext1
     * into ciphertext1. The client sends ciphertext1 to the server. <br><br>
     * The server then uses its own private identity key to decrypt ciphertext1
     * and stores it as plaintext2. Next, it generates 128 bytes of random data
     * which it stores as plaintext3. It then encrypts plaintext3 into ciphertext3
     * using the clients public identity key. Finally, it sends plaintext2 and ciphertext3
     * to the client. <br><br>
     *
     * The client can now compare plaintext1 to plaintext2. If they match, it means that
     * the server has the correct private identity key, and the client has now confirmed the
     * server's identity. The client then also decrypts ciphertext3 into plaintext4 using
     * its own private identity key. The client then sends plaintext4 to the server.
     * <br><br>
     * Now the server can compare plaintext4 to plaintext3, and if they match, this means
     * that the client has the correct private identity key, and the server has now confirmed
     * the clients identity. Handshaking is now fully complete on both sides.
     *
     *
     * @param m the received SendPublicKeyMessage
     * @throws Exception the exception
     */
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


            // Instantiate symmetric key message object
            SecurityHandshake.SendSymmetricKeyMessage nMsg = new SecurityHandshake.SendSymmetricKeyMessage(encryptedKey, encryptedIv, m.getEnv().getUserInfo().getUsername());
            env.getDebug().trigger("handshaking");
            nMsg.activateEncryption();
            m.getConn().setCipher(symmetricCipher);


            log.log(Level.INFO, String.format("Sending symmetric key WITH BAD BYTES, Waiting for user id message '%s'", toUser));

            // Send symmetric key to client and wait for response
            ReceivedMessage userIdMessage = m.respondAndWait(nMsg, exceptor, handshakeTimeout);

            // Receive user info as a response
            UserInfo.UserIDMessage userId = userIdMessage.getMessage();
            String username = userId.getUserInfo().getUsername();

            // If we don't know about this user, add them to the peer info directory
            // assuming we can verify their NewUserConfirmation
            if (env.getInfo().getPeers().get(username) == null) {
                //log.log(Level.INFO, "Unknown username '"+username+"', attempting to add and verify");
                env.getInfo().newPeerAndConfirm(userId.getPeerInfo());
                env.getInfo().save();
            }
            userIdMessage.getEnv().getDebug().trigger("handshaking");
            m.getEnv().getLogger().log(Level.INFO, String.format("Receiving INITIAL handshake ID '%s'", toUser));
            m.getConn().setServerInfo(userId.getUserInfo().getServer());

            // Send our user info to the client and wait for response
            m = userIdMessage.respondAndWait(new UserInfo.UserIDMessage(userIdMessage.getEnv().getUserInfo(), env.getInfo().myInfo(env)), exceptor, handshakeTimeout);

            // They sent us encrypted data which we will now decrypt to verify our identity
            // Their public ID key
            byte[] theirIdKey = env.getInfo().getPeers().get(userId.getUserInfo().getUsername()).getIdentityKey();
            // Array to hold random data
            byte[] plaintext = new byte[AsymmetricCipher.idBlockSize()];

            // Create cipher using their public key
            AsymmetricCipher cipher = new AsymmetricCipher();
            cipher.setPublicKey(theirIdKey);
            Random rnd = new Random(System.currentTimeMillis());
            // Generate our own random data to verify their identity
            rnd.nextBytes(plaintext);

            // Create decryption cipher using our own private key
            AsymmetricCipher myCipher = new AsymmetricCipher();
            myCipher.setPrivateKey(env.getIdentityKey().getKey());

            MapMessage theirIdConfirm = m.getMessage();

            // Get the encrypted data they sent us
            byte[] ciphertext = Base64.getDecoder().decode((String) theirIdConfirm.val().get("ciphertext"));
            MapMessage idConfirmResponse = new MapMessage();

            // Decrypt the data they sent us and add to response message
            // Encrypt the data we generated and add to response message
            idConfirmResponse.val().put("plaintext", myCipher.decrypt(ciphertext));
            idConfirmResponse.val().put("ciphertext", cipher.encrypt(plaintext));

            log.log(Level.INFO, "Sending ID confirm response and checking their ID");

            // Send message and wait for response
            m = m.respondAndWait(idConfirmResponse, exceptor, handshakeTimeout);
            MapMessage idConfirm = m.getMessage();

            // They decrypted the encrypted data we sent them, now we check
            // that it is identical to the original data, if so,
            // we have verified their identity
            if (!keysEqual(plaintext, Base64.getDecoder().decode((String) idConfirm.val().get("plaintext")))) {
                throw new Exception();
            }


            m.getEnv().getLogger().log(Level.INFO, "Server ending handshake with " + toUser);

            // Call the peer confirm callback
            m.getEnv().getPm().confirmPeer(toUser, userId.getUserInfo(), m.getConn(), true);

        }catch (Exception e) {
            // Exception handler
            env.getLogger().log(
                    Level.INFO,
                    String.format("Handshaking error"),
                    e
            );
            // Close the socket
            sock.close(false);
            return;
        }
    }

    /**
     * No longer used.
     *
     * @param username the username
     * @return the peer address
     */
    public PeerAddress peerFromUsername(String username) {
        try {
            return env.getInfo().getPeers().get(username).convertToPeerAddress();
        } catch(Exception e) {
            env.getLogger().log(Level.INFO,
                    "Error getting peer info for "+username, e);
            return null;
        }
    }

    /**
     * Get the status of a connection with a peer, for use by the GUI
     *
     * @param username the peer
     * @return the string
     */
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
    /**
     * This method runs the entire handshaking procedure on the client side.
     * Handshaking is a rather complex process, which has two goals. First,
     * to enable encryption, and second, to confirm the other peer's identity. <br><br>
     * Handshaking begins when the client generates an RSA keypair and sends
     * the server the public key. The server then generates an AES key, encrypts
     * it using the given public key, and sends the encrypted key to the client.
     * After sending that message the server then decrypts and encrypts all messages
     * using said AES key.<br><br>
     * The client decrypts AES key, and enables encryption so that all future messages
     * sent over the socket are encrypted and decrypted using the AES key. <br><br>
     * The client then responds with a message containing its username and other metadata,
     * and the server responds back with its own username and metadata. <br><br>
     * Next comes the identity confirmation process. The client then generates 128 bytes
     * of random data, which we will refer to as plaintext1.
     * Using the server's public identity key, the client encrypts the plaintext1
     * into ciphertext1. The client sends ciphertext1 to the server. <br><br>
     * The server then uses its own private identity key to decrypt ciphertext1
     * and stores it as plaintext2. Next, it generates 128 bytes of random data
     * which it stores as plaintext3. It then encrypts plaintext3 into ciphertext3
     * using the clients public identity key. Finally, it sends plaintext2 and ciphertext3
     * to the client. <br><br>
     *
     * The client can now compare plaintext1 to plaintext2. If they match, it means that
     * the server has the correct private identity key, and the client has now confirmed the
     * server's identity. The client then also decrypts ciphertext3 into plaintext4 using
     * its own private identity key. The client then sends plaintext4 to the server.
     * <br><br>
     * Now the server can compare plaintext4 to plaintext3, and if they match, this means
     * that the client has the correct private identity key, and the server has now confirmed
     * the clients identity. Handshaking is now fully complete on both sides.

     *
     * @param lockUsername the username we are connecting to
     * @param sock         the socket we are connecting on
     * @throws Exception the exception
     */
    void newConnectionHandlerClient(String lockUsername, SecureSocket sock) throws Exception {
        // First we lock the handshakeLock object, which ensures that
        // we aren't handshaking to the same user multiple times
        Object handshakeLock = getHandshakeLock(lockUsername);
        synchronized (handshakeLock) {
            String toUser = "unknown";
            boolean hasUsername = false;
            SecureSocket.TimeoutException exceptor = () -> {
                throw new Exception("Message timed out");
            };
            try {
                log.log(Level.INFO, "Client starting handshake");
                env.getDebug().trigger("handshaking");

                // Generate RSA keypair
                AsymmetricCipher decryptor = new AsymmetricCipher();
                KeyPair keyPair = decryptor.generateKeyPair();
                decryptor.setPrivateKey(keyPair.getPrivate());

                SecurityHandshake.SendPublicKeyMessage publicKeyMsg =
                        new SecurityHandshake.SendPublicKeyMessage(keyPair.getPublic().getEncoded(), env.getUserInfo().getUsername());

                // Send RSA keypair and wait for SendSymmetricKeyMessage response
                ReceivedMessage m = sock.sendMessageAndWait(
                        publicKeyMsg,
                        SecureSocket.timeoutFactory(String.format("Message timed out")),
                        handshakeTimeout
                );


                env.getDebug().trigger("handshaking");
                SecurityHandshake.SendSymmetricKeyMessage msg = m.getMessage();

                toUser = msg.getUsername();
                hasUsername = true;
                log.log(Level.INFO, "Received symmetric key WITH BAD BYTES, starting user handshake with " + toUser);

                // Get the key and IV spec from the message
                byte[] key = decryptor.decrypt(msg.getEncryptedKey());
                byte[] iv = decryptor.decrypt(msg.getEncryptedIv());

                log.log(String.format("Received key from '%s'", toUser));

                // Create a new cipher using the given key and IV spec
                SymmetricCipher symmetricCipher = new SymmetricCipher();
                symmetricCipher.setKey(key, iv);
                m.getConn().setCipher(symmetricCipher);
                m.getConn().activateEncryption();

                // Send our user info and wait for response
                ReceivedMessage userIdResponse = m.respondAndWait(new UserInfo.UserIDMessage(env.getUserInfo(), env.getInfo().myInfo(env)), exceptor, handshakeTimeout);
                log.log(Level.INFO, "Received user ID from " + toUser);
                env.getDebug().trigger("handshaking");

                // Get UserIDMessage as response
                UserInfo.UserIDMessage userInfoMsg = userIdResponse.getMessage();

                String username = userInfoMsg.getUserInfo().getUsername();

                // If user is not already in our peer info directory
                // add them, assuming we can verify their signed
                // user confirmation
                if (env.getInfo().getPeers().get(username) == null) {
                    //     log.log(Level.INFO, "Unknown username '"+username+"', attempting to add and verify");
                    env.getInfo().newPeerAndConfirm(userInfoMsg.getPeerInfo());
                    env.getInfo().save();
                }
                m.getConn().setServerInfo(userInfoMsg.getUserInfo().getServer());

                // Now we confirm identity using the identity keys
                MapMessage idConfirm = new MapMessage();
                byte[] plaintext = new byte[AsymmetricCipher.idBlockSize()];

                // Create cipher using their public identity key
                byte[] theirIdKey = env.getInfo().getPeers().get(username).getIdentityKey();
                AsymmetricCipher cipher = new AsymmetricCipher();
                cipher.setPublicKey(theirIdKey);

                // Generate random byte array to encrypt
                Random rnd = new Random(System.currentTimeMillis());
                rnd.nextBytes(plaintext);

                // Encrypt the random byte array and add to message
                idConfirm.val().put("ciphertext", cipher.encrypt(plaintext));

                log.log(Level.INFO, "Checking their ID");

                // Send message and wait for response
                ReceivedMessage theirIdConfirm = userIdResponse.respondAndWait(idConfirm, exceptor, handshakeTimeout);
                MapMessage idConfirmResponse = theirIdConfirm.getMessage();

                // They decrypted our random plaintext using their private ID key
                // If the plaintext they sent us matches the original plaintext,
                // this confirms they have the private ID key, which confirms their identity
                if (!keysEqual(plaintext, Base64.getDecoder().decode((String) idConfirmResponse.val().get("plaintext")))) {
                    throw new Exception("ID verification failed");
                }

                // Get the ciphertext they sent us
                byte[] ciphertext = Base64.getDecoder().decode((String) idConfirmResponse.val().get("ciphertext"));

                // Create a new cipher using our own private ID key
                AsymmetricCipher myCipher = new AsymmetricCipher();
                myCipher.setPrivateKey(env.getIdentityKey().getKey());

                // Decrypt the ciphertext they sent us, using our own private ID key
                // and add the plaintext to the message
                MapMessage confirmMyId = new MapMessage();
                confirmMyId.val().put("plaintext", myCipher.decrypt(ciphertext));

                // Send the plaintext back to them
                theirIdConfirm.respond(confirmMyId, null);

                log.log(Level.INFO, "Client ending handshake with " + username);

                // Call the confirmPeer callback
                m.getEnv().getPm().confirmPeer(username, userInfoMsg.getUserInfo(), userIdResponse.getConn(), false);
            } catch (Exception e) {
                // Exception handler
                env.getLogger().log(Level.INFO, "Handshake error", e);
                e.printStackTrace();

                // Close the socket
                sock.close(false);
                throw new Exception("Error handshaking with " + toUser);
            }
        }
    }

    /**
     * Compares two byte arrays and returns true if they are equal.
     * This is used during identity verification to compare
     * the original plaintext to the decrypted plaintext to confirm
     * they match.
     *
     * @param a one byte array
     * @param b another byte array
     * @return true if the arrays are identical
     */
    public boolean keysEqual(byte[] a, byte[] b) {
        if(a.length!=b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if(a[i]!=b[i]) return false;
        }
        return true;
    }

    /**
     * Gets the current list of peers.
     *
     * @return the peer list
     */
    public GetPeerListResponse getPeerListResponse() {
        List<PeerAddress> peerInfoList = new ArrayList<>();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            peerInfoList.add(peer.getValue().getPeerAddress());
        }

        return new GetPeerListResponse(peerInfoList, env.getInfo());
    }

    /**
     * Called by the client or server when there is a new connection,
     * although if it is the server this does nothing. If it is the client
     * this starts the client handshake procedure in a new thread,
     * which will automatically
     * trigger the server handshake procedure from a DefaultHandler.
     *
     * @param username the user we connected to
     * @param sock     the socket
     * @param isServer true if we are the server on this socket
     * @throws Exception the exception
     */
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

    /**
     * Callback function for new TCP connections on the server.
     * @param sock the socket we accepted
     * @throws Exception
     */
    @Override
    public void onConnect(SecureSocket sock) throws Exception {
        newConnectionHandler(null, sock, true);
    }

    /**
     * Stores how to connect to a peer, mainly their username,
     * IP address and port.
     */
    public static class PeerAddress {
        /**
         * Their IP
         */
        private String ip;
        /**
         * Their port
         */
        private int port;
        /**
         * User metadata including username
         */
        private UserInfo user;
        /**
         * IP address and port of where they are listening.
         */
        private ServerInfo serverInfo;

        /**
         * For logging.
         *
         * @return the string
         */
        public String id() {
            return String.format("%s:%d", ip, port);
        }

        /**
         * Instantiates a new Peer address.
         *
         * @param ip         the ip
         * @param port       the port
         * @param user       the user
         * @param serverInfo the server info
         */
        public PeerAddress(String ip, int port, UserInfo user, ServerInfo serverInfo) {
            this.ip = ip;
            this.port = port;
            this.user = user;
            this.serverInfo = serverInfo;
        }

        /**
         * Gets server info.
         *
         * @return the server info
         */
        public ServerInfo getServerInfo() {
            return serverInfo;
        }

        /**
         * Sets server info.
         *
         * @param serverInfo the server info
         */
        public void setServerInfo(ServerInfo serverInfo) {
            this.serverInfo = serverInfo;
        }

        /**
         * Gets user info.
         *
         * @return the user
         */
        public UserInfo getUser() {
            return user;
        }

        /**
         * Sets user info.
         *
         * @param user the user
         */
        public void setUser(UserInfo user) {
            this.user = user;
        }

        /**
         * Instantiates a new Peer address.
         */
        public PeerAddress(){}

        /**
         * Sets ip.
         *
         * @param ip the ip
         */
        public void setIp(String ip) {
            this.ip = ip;
        }

        /**
         * Sets port.
         *
         * @param port the port
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Gets ip.
         *
         * @return the ip
         */
        public String getIp() {
            return ip;
        }

        /**
         * Gets port.
         *
         * @return the port
         */
        public int getPort() {
            return port;
        }

    }
}
