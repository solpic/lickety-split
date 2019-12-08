package org.licketysplit.securesocket.peers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;

import java.awt.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Stores info about each peer and the network.
 * Will be exchanged between peers to make sure everyone
 * has up-to-date information.
 * Specifically, it keeps a copy of the root public key, and
 * metadata for each peer.
 * This class can also be easily serialized/deserialized to a file.
 */
public class PeerInfoDirectory {
    /**
     * The root public key.
     */
    byte[] rootKey;


    /**
     * Syncs two info directories, updating where necessary and
     * returns true if changes were made. It handles syncing
     * by checking the timestamps of other entries and
     * using the most recent timestamp, assuming the entry
     * is verified by the root key.
     *
     * @param theirs the other guys peer info directory
     * @return if changes were made
     * @throws Exception the exception
     */
    public boolean syncInfo(PeerInfoDirectory theirs) throws Exception {
        boolean changed = false;
        for (Map.Entry<String, PeerInfo> peerInfoEntry : theirs.getPeers().entrySet()) {
            PeerInfo theirPeerInfo = peerInfoEntry.getValue();
            String username = peerInfoEntry.getKey();

            boolean shouldUpdate = false;
            boolean shouldReplace = false;
            synchronized (peers) {
                if (!peers.containsKey(username)) {
                    shouldUpdate = true;
                }else{
                    shouldReplace = true;
                    PeerInfo myPeerInfo = peers.get(username);
                    if(theirPeerInfo.getTimestamp().compareTo(myPeerInfo.getTimestamp())>0) {
                        shouldUpdate = true;
                    }
                }
            }
            if(shouldUpdate) {
                updatePeer(theirPeerInfo, true);
                changed = true;
            }
        }
        return changed;
    }

    /**
     * Bans a user. To ban a user we simply generate a BanInfo
     * object (which is only possible to generate if you have the root
     * private key), set the value in our map of peers, and then sync
     * our info directory with everyone. Other peers will see the ban,
     * verify it came from the root private key, and will update their
     * own peer info directories accordingly.
     *
     * @param username the user to ban
     * @param rootKey  the root private key
     * @throws Exception the exception
     */
    public void banUser(String username, byte[] rootKey) throws Exception {
        PeerInfo nPeerInfo = null;
        synchronized (peers) {
            if(peers.containsKey(username)) {
                PeerInfo peerInfo = peers.get(username);
                if(peerInfo.getBan()==null) {
                    PeerInfo.BanInfo ban = new PeerInfo.BanInfo();
                    // Generate BanInfo object and update our peer info directory
                    ban.setBanConfirmation(generateBan(username, rootKey));
                    nPeerInfo = peerInfo.copy();
                    nPeerInfo.setBan(ban);
                    nPeerInfo.setTimestamp(new Date());
                }else{
                    throw new Exception("Peer '"+username+"' already banned");
                }
            }else{
                throw new Exception("Peer '"+username+"' doesn't exist");
            }
        }
        updatePeer(nPeerInfo, true);
    }

    /**
     * Generates a SignedPayload object for creating a new user. New users
     * can only be added by root, so the SignedPayload is signed by the
     * root private key, and this SignedPayload can be confirmed by everyone using
     * the root public key. This SignedPayload contains the new user's username and
     * their identity key.
     *
     * @param username    the username of the new user
     * @param identityKey the public identity key of the new user
     * @param rootKey     the root private key
     * @return the signed payload
     * @throws Exception the exception
     */
    public static SignedPayload generateNewUserConfirm(String username, byte[] identityKey, byte[] rootKey) throws Exception{
        SignedPayload.Signer signer = new SignedPayload.Signer(rootKey);
        byte[] payload = newUserConfirmPayload(username, identityKey);
        return signer.sign(payload);
    }

    /**
     * Verifies that a SignedPayload confirming a new user was
     * actually signed by someone with the root private key.
     *
     * @param username    the username of the new user
     * @param identityKey the identity key of the new user
     * @param signed      the SignedPayload
     * @param rootKey     the root public key
     * @throws Exception the exception
     */
    public static void verifyNewUserConfirm(String username, byte[] identityKey, SignedPayload signed, byte[] rootKey) throws Exception {
        SignedPayload.Verifier verifier = new SignedPayload.Verifier(rootKey);
        // This will throw an exception if the payload isn't signed correctly
        verifier.verify(signed);
        byte[] payload = newUserConfirmPayload(username, identityKey);
        byte[] theirPayload = signed.getPayload();

        if(payload.length!=theirPayload.length) throw new Exception();
        for (int i = 0; i < payload.length; i++) {
            if(payload[i]!=theirPayload[i]) throw new Exception();
        }
    }

    /**
     * Bans can only be issued by root users (users who have the
     * root private key). This generates a SignedPayload
     * object confirming a ban, and signs it using the root private key.
     *
     * @param username the user to ban
     * @param rootKey  the root private key
     * @return the ban confirmation
     * @throws Exception the exception
     */
    public static SignedPayload generateBan(String username, byte[] rootKey) throws Exception {
        SignedPayload.Signer signer = new SignedPayload.Signer(rootKey);
        return signer.sign(username.getBytes());
    }

    /**
     * Verifies a ban confirmation using the root public key. Peers
     * won't accept bans unless they confirm they came from root. An exception
     * is thrown if the ban can't be confirmed.
     *
     * @param username   the user to be banned
     * @param banConfirm the ban confirmation
     * @param rootKey    the root public key
     * @throws Exception the exception
     */
    public static void verifyBanConfirm(String username, SignedPayload banConfirm, byte[] rootKey) throws Exception {
        SignedPayload.Verifier verifier = new SignedPayload.Verifier(rootKey);
        verifier.verify(banConfirm);
        byte[] payload = username.getBytes();
        byte[] theirPayload = banConfirm.getPayload();

        if(payload.length!=theirPayload.length) throw new Exception();
        for (int i = 0; i < payload.length; i++) {
            if(payload[i]!=theirPayload[i]) throw new Exception();
        }
    }

    /**
     * Generates an unsigned byte array to be
     * used in a new user confirmation SignedPayload.
     * The byte array is simply a concatenation
     * of the user's username and their identity public key.
     *
     * @param username the new user
     * @param idKey    the new user's public identity key
     * @return the payload
     */
    static byte[] newUserConfirmPayload(String username, byte[] idKey) {
        byte[] usernameBytes = username.getBytes();
        byte[] payload = new byte[usernameBytes.length+idKey.length];
        for(int i = 0; i<usernameBytes.length; i++) payload[i] = usernameBytes[i];
        for (int i = 0; i < idKey.length; i++) {
            payload[i+usernameBytes.length] = idKey[i];
        }
        return payload;
    }

    /**
     * Gets reference to our own peer info object.
     *
     * @param env our Environment
     * @return the peer info
     */
    public PeerInfo myInfo(Environment env) {
        String myUsername = env.getUserInfo().getUsername();
        return peers.get(myUsername);
    }

    /**
     * This class is used to store metadata about each peer,
     * including their IP address and port, their username,
     * their public identity key, their new user confirmation SignedPayload,
     * and if they have been banned, their ban confirmation SignedPayload.
     */
    public static class PeerInfo {
        /**
         * Object used to verify that this user was added by root.
         */
        SignedPayload newUserConfirmation;


        /**
         * Instantiates a new Peer info.
         */
        public PeerInfo() {
        }

        /**
         * Creates a copy of this object.
         *
         * @return the copy
         */
        public PeerInfo copy() {
            PeerInfo cpy = new PeerInfo();
            cpy.setIdentityKey(identityKey);
            cpy.setNewUserConfirmation(newUserConfirmation);
            cpy.setServerIp(serverIp);
            cpy.setServerPort(serverPort);
            cpy.setTimestamp(timestamp);
            cpy.setUsername(username);
            cpy.setBan(ban);

            return cpy;
        }

        /**
         * Gets new user confirmation.
         *
         * @return the new user confirmation
         */
        public SignedPayload getNewUserConfirmation() {
            return newUserConfirmation;
        }

        /**
         * Sets new user confirmation.
         *
         * @param newUserConfirmation the new user confirmation
         */
        public void setNewUserConfirmation(SignedPayload newUserConfirmation) {
            this.newUserConfirmation = newUserConfirmation;
        }

        /**
         * Converts to a peer address for use by the PeerManager/SecureSocket.
         *
         * @return the PeerAddress
         */
        public PeerManager.PeerAddress convertToPeerAddress() {
            int port = Integer.parseInt(serverPort);
            String ip = serverIp;
            PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, ip);
            UserInfo userInfo = new UserInfo(username, serverInfo);
            return new PeerManager.PeerAddress(ip, port, userInfo, serverInfo);
        }

        /**
         * Get public identity key
         *
         * @return the key
         */
        public byte[] getIdentityKey() {
            return identityKey;
        }

        /**
         * Sets public identity key.
         *
         * @param identityKey the identity key
         */
        public void setIdentityKey(byte[] identityKey) {
            this.identityKey = identityKey;
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
         * Gets server ip.
         *
         * @return the server ip
         */
        public String getServerIp() {
            return serverIp;
        }

        /**
         * Sets server ip.
         *
         * @param serverIp the server ip
         */
        public void setServerIp(String serverIp) {
            this.serverIp = serverIp;
        }

        /**
         * Gets server port.
         *
         * @return the server port
         */
        public String getServerPort() {
            return serverPort;
        }

        /**
         * Sets server port.
         *
         * @param serverPort the server port
         */
        public void setServerPort(String serverPort) {
            this.serverPort = serverPort;
        }

        /**
         * Helper function used to generate an asymmetric identity keypair,
         * this is used when creating a new user. The public key is shared
         * with everyone, however only the new user gets a copy of the private key
         * (assuming root deletes their copy, which they should).
         *
         * @return the keys, keys[0] is the public key, keys[1] is the private key
         * @throws Exception the exception
         */
        public byte[][] generateIdentityKey() throws Exception {
            AsymmetricCipher cipher = new AsymmetricCipher();
            KeyPair keyPair = cipher.generateKeyPair();
            byte[] publicKey = keyPair.getPublic().getEncoded();
            byte[] privateKey = keyPair.getPrivate().getEncoded();

            byte[][] keys = new byte[2][];
            keys[0] = publicKey;
            keys[1] = privateKey;

            return keys;
        }

        /**
         * Public key used to confirm a peer's identity.
         */
        byte[] identityKey;
        /**
         * Display username, unique, and banned usernames can't be used again.
         */
        String username;

        /**
         * The Server ip to find this peer at.
         */
        String serverIp;
        /**
         * The Server port to find this peer at.
         */
        String serverPort;

        /**
         * The timestamp of when this was last edited.
         */
        Date timestamp;
        /**
         * If this user was banned, this confirms it.
         */
        BanInfo ban;

        /**
         * Gets ban.
         *
         * @return the ban
         */
        public BanInfo getBan() {
            return ban;
        }

        /**
         * Sets ban.
         *
         * @param ban the ban
         */
        public void setBan(BanInfo ban) {
            this.ban = ban;
        }

        /**
         * Gets timestamp.
         *
         * @return the timestamp
         */
        public Date getTimestamp() {
            return timestamp;
        }

        /**
         * Sets timestamp.
         *
         * @param timestamp the timestamp
         */
        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        /**
         * This class is used to confirm that a user was banned by root.
         */
        public static class BanInfo {
            /**
             * The Ban confirmation, used to verify the ban message came from root.
             */
            SignedPayload banConfirmation;
            /**
             * No longer used.
             */
            List<String> confirmedBans;

            /**
             * Gets ban confirmation.
             *
             * @return the ban confirmation
             */
            public SignedPayload getBanConfirmation() {
                return banConfirmation;
            }

            /**
             * Sets ban confirmation.
             *
             * @param banConfirmation the ban confirmation
             */
            public void setBanConfirmation(SignedPayload banConfirmation) {
                this.banConfirmation = banConfirmation;
            }

            /**
             * No longer used.
             *
             * @return the confirmed bans
             */
            public List<String> getConfirmedBans() {
                return confirmedBans;
            }

            /**
             * No longer used.
             *
             * @param confirmedBans the confirmed bans
             */
            public void setConfirmedBans(List<String> confirmedBans) {
                this.confirmedBans = confirmedBans;
            }

            /**
             * Instantiates a new Ban info.
             */
            public BanInfo() {
            }
        }
    }

    /**
     * Get root public key.
     *
     * @return the root key
     */
    public byte[] getRootKey() {
        return rootKey;
    }

    /**
     * Sets root key.
     *
     * @param rootKey the root key
     */
    public void setRootKey(byte[] rootKey) {
        this.rootKey = rootKey;
    }

    /**
     * Gets peers.
     *
     * @return the peers
     */
    public Map<String, PeerInfo> getPeers() {
        return peers;
    }

    /**
     * Sets peers.
     *
     * @param peers the peers
     */
    public void setPeers(Map<String, PeerInfo> peers) {
        this.peers = peers;
    }

    /**
     * Adds peer to map.
     *
     * @param peer the peer
     */
    public void newPeer(PeerInfo peer) {
        synchronized (peers) {
            peers.put(peer.getUsername(), peer);
        }
    }


    /**
     * Helper function to ZIP the files in the source array, into
     * the destination file zip, renaming the source files as
     * indicated by the array dst.
     *
     * @param src the files to zip
     * @param dst new names for the files
     * @param zip the target zip file
     * @throws Exception the exception
     */
    public static void zipFiles(String[] src, String[] dst, String zip) throws Exception {
        FileOutputStream fos = new FileOutputStream(zip);
        ZipOutputStream zipOut = new ZipOutputStream(fos);
        for (int i = 0; i < src.length; i++) {
            File fileToZip = new File(src[i]);
            FileInputStream fis = new FileInputStream(fileToZip);
            ZipEntry zipEntry = new ZipEntry(dst[i]);
            zipOut.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while((length = fis.read(bytes)) >= 0) {
                zipOut.write(bytes, 0, length);
            }
            fis.close();
        }
        zipOut.close();
        fos.close();
    }

    /**
     * Generates a bootstrap file. This file is used to load by the
     * application to load a new network. It is a zip file containing
     * a copy of the peer info directory, this new user's private identity key,
     * a userinfo file containing the new user's username, IP, and port, and finally,
     * if the new user is a root user, a copy of the root private key. This is all
     * the information that is needed for a new user to connect to a network for the
     * first time.
     *
     * @param username    the new user's username
     * @param ip          the new user's IP
     * @param port        the new user's port
     * @param identityKey the new user's private ID key
     * @param isRoot      if the new user is a root
     * @param rootkey     if root, the root private key
     * @return the bootstrap ZIP file
     * @throws Exception the exception
     */
    public File generateBootstrapFile(String username, String ip, String port, byte[] identityKey, boolean isRoot, byte[] rootkey) throws Exception {
        Path temp = Files.createTempDirectory("temp");
        File bootstrap = new File(Paths.get(temp.toString(),
                String.format("%s-p2p.zip", username)
        ).toString());

        // Create files
        File idkey = File.createTempFile("keyfile", null);
        File info = File.createTempFile("infodir", null);
        File userInfo = File.createTempFile("userinfo", null);
        File rootKeyFile = File.createTempFile("rootkey", null);
        if(isRoot) {
            KeyStore rootkeyStore = new KeyStore(rootKeyFile.getPath());
            rootkeyStore.setKey(rootkey);
            rootkeyStore.save();
        }


        save();
        FileUtils.writeStringToFile(info, toJSONString(), "UTF-8");
        KeyStore idkeystore = new KeyStore(idkey.getPath());
        idkeystore.setKey(identityKey);
        idkeystore.save();
        ObjectMapper objectMapper = new ObjectMapper();
        HashMap<String, String> userInfoMap = new HashMap<>();
        userInfoMap.put("username", username);
        userInfoMap.put("ip", ip);
        userInfoMap.put("port", port);
        FileUtils.writeStringToFile(userInfo, objectMapper.writeValueAsString(userInfoMap), "UTF-8");

        if(!isRoot) {
            zipFiles(
                    new String[]{info.getAbsolutePath(), idkey.getAbsolutePath(), userInfo.getAbsolutePath()},
                    new String[]{"peerinfodir", "idkey", "userinfo"},
                    bootstrap.getAbsolutePath()
            );
        }else{
            zipFiles(
                    new String[]{info.getAbsolutePath(), idkey.getAbsolutePath(), userInfo.getAbsolutePath(), rootKeyFile.getAbsolutePath()},
                    new String[]{"peerinfodir", "idkey", "userinfo", "rootkey"},
                    bootstrap.getAbsolutePath()
            );
        }

        idkey.delete();
        info.delete();
        userInfo.delete();
        if(isRoot) {
            rootKeyFile.delete();
        }

        return bootstrap;
    }

    /**
     * No longer used.
     *
     * @param peer the peer
     * @throws Exception the exception
     */
    public void newPeerCallback(PeerInfo peer) throws Exception {
//        env().getPm().newPeer(peer.getUsername());
    }

    /**
     * The Environment.
     */
    Environment e;

    /**
     * Sets the Environment.
     *
     * @param e the e
     */
    public void env(Environment e) {
        this.e = e;
    }

    /**
     * Gets the Environment.
     *
     * @return the environment
     */
    public Environment env() {
        return e;
    }

    /**
     * Called when the application has been alerted of a new ban.
     *
     * @param peer the peer
     * @throws Exception the exception
     */
    public void banPeerCallback(PeerInfo peer) throws Exception {
        env().getPm().userBanned(peer.getUsername());
    }

    /**
     * Called when the application is alerted of a user for the first time,
     * but that user is banned.
     *
     * @param peer the peer
     * @throws Exception the exception
     */
    public void newPeerAndBanCallback(PeerInfo peer) throws Exception {
        env().getPm().userBanned(peer.getUsername());
    }

    /**
     * Updates a PeerInfo object with new information.
     *
     * @param peer          the peer
     * @param shouldReplace if true, replace the old object
     * @throws Exception the exception
     */
    public void updatePeer(PeerInfo peer, boolean shouldReplace) throws Exception {
        synchronized (peers) {
            boolean willReplace = false;
            if(peers.containsKey(peer.getUsername())) {
                if(shouldReplace) {
                    willReplace = true;
                }else {
                    throw new Exception("Peer username collision");
                }
            }

            verifyNewUserConfirm(peer.getUsername(), peer.getIdentityKey(), peer.getNewUserConfirmation(), getRootKey());
            boolean banned = false;
            if(peer.getBan()!=null) {
                verifyBanConfirm(peer.getUsername(), peer.getBan().getBanConfirmation(), getRootKey());
                banned = true;
            }
            peers.put(peer.getUsername(), peer);
            if(!banned&&!willReplace) {
                newPeerCallback(peer);
            }else if(willReplace&&banned) {
                banPeerCallback(peer);
            }else if(!willReplace&&banned) {
                newPeerAndBanCallback(peer);
            }
        }
    }

    /**
     * Add a new peer and confirm their SignedPayload new user confirmation.
     *
     * @param peer the peer
     * @throws Exception the exception
     */
    public void newPeerAndConfirm(PeerInfo peer) throws Exception {
        updatePeer(peer, false);
    }

    /**
     * Instantiates a new Peer info directory.
     */
    public PeerInfoDirectory() {
    }

    /**
     * Where this class is serialized to.
     */
    String saveLocation;

    /**
     * Instantiates a new Peer info directory.
     *
     * @param filename file to save to
     * @throws Exception the exception
     */
    public PeerInfoDirectory(String filename) throws Exception{
        saveLocation = filename;
    }

    /**
     * Loads this class instance from the save location.
     *
     * @throws Exception the exception
     */
    public void load() throws Exception {
        loadFromFile(saveLocation);
    }

    /**
     * Deserializes from file.
     *
     * @param filename the filename
     * @throws Exception the exception
     */
    public void loadFromFile(String filename) throws Exception {
        fromJSONString(new String ( Files.readAllBytes( Paths.get(filename) ) ));
    }

    /**
     * Writes to file.
     *
     * @throws Exception the exception
     */
    public void save() throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(saveLocation));
        writer.write(toJSONString());
        writer.close();
    }


    /**
     * Metadata about each peer.
     */
    Map<String, PeerInfo> peers;

    /**
     * Serializes this instance to a JSON string.
     *
     * @return the serialized string
     * @throws Exception the exception
     */
    public String toJSONString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        synchronized(peers) {
            return mapper.writeValueAsString(this);
        }
    }

    /**
     * Deserialize this instance from a JSON string.
     *
     * @param data the JSON string
     * @throws Exception the exception
     */
    public void fromJSONString(String data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this).readValue(data);
    }

    /**
     * Called when creating a new network.
     * Creates a new root keypair and returns the
     * private key for the root user to store.
     * Initializes new root key for network
     *
     * @return the root private key
     * @throws Exception the exception
     */
    public byte[] initializeNetwork() throws Exception {
        peers = new HashMap<>();
        AsymmetricCipher cipher = new AsymmetricCipher();
        KeyPair keyPair = cipher.generateKeyPair();
        rootKey = keyPair.getPublic().getEncoded();
        return keyPair.getPrivate().getEncoded();
    }
}
