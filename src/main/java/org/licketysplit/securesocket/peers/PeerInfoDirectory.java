package org.licketysplit.securesocket.peers;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyPair;
import java.util.*;

/*
Stores info about each peer and the network.
Will be exchanged between peers to make sure everyone
has up-to-date information.
 */
public class PeerInfoDirectory {
    byte[] rootKey;


    public static SignedPayload generateNewUserConfirm(String username, byte[] identityKey, byte[] rootKey) throws Exception{
        SignedPayload.Signer signer = new SignedPayload.Signer(rootKey);
        byte[] payload = newUserConfirmPayload(username, identityKey);
        return signer.sign(payload);
    }

    public static void verifyNewUserConfirm(String username, byte[] identityKey, SignedPayload signed, byte[] rootKey) throws Exception {
        SignedPayload.Verifier verifier = new SignedPayload.Verifier(rootKey);
        verifier.verify(signed);
        byte[] payload = newUserConfirmPayload(username, identityKey);
        byte[] theirPayload = signed.getPayload();

        if(payload.length!=theirPayload.length) throw new Exception();
        for (int i = 0; i < payload.length; i++) {
            if(payload[i]!=theirPayload[i]) throw new Exception();
        }
    }

    static byte[] newUserConfirmPayload(String username, byte[] idKey) {
        byte[] usernameBytes = username.getBytes();
        byte[] payload = new byte[usernameBytes.length+idKey.length];
        for(int i = 0; i<usernameBytes.length; i++) payload[i] = usernameBytes[i];
        for (int i = 0; i < idKey.length; i++) {
            payload[i+usernameBytes.length] = idKey[i];
        }
        return payload;
    }

    public PeerInfo myInfo(Environment env) {
        String myUsername = env.getUserInfo().getUsername();
        return peers.get(myUsername);
    }
    public static class PeerInfo {
        // Message used to verify that this user was added by root
        SignedPayload newUserConfirmation;


        public PeerInfo() {
        }

        public SignedPayload getNewUserConfirmation() {
            return newUserConfirmation;
        }

        public void setNewUserConfirmation(SignedPayload newUserConfirmation) {
            this.newUserConfirmation = newUserConfirmation;
        }

        public PeerManager.PeerAddress convertToPeerAddress() {
            int port = Integer.parseInt(serverPort);
            String ip = serverIp;
            PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, ip);
            UserInfo userInfo = new UserInfo(username, serverInfo);
            return new PeerManager.PeerAddress(ip, port, userInfo, serverInfo);
        }

        public byte[] getIdentityKey() {
            return identityKey;
        }

        public void setIdentityKey(byte[] identityKey) {
            this.identityKey = identityKey;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getServerIp() {
            return serverIp;
        }

        public void setServerIp(String serverIp) {
            this.serverIp = serverIp;
        }

        public String getServerPort() {
            return serverPort;
        }

        public void setServerPort(String serverPort) {
            this.serverPort = serverPort;
        }

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

        // Public key used to confirm a peer's identity
        byte[] identityKey;
        // Display username, unique, and banned usernames can't be used again
        String username;

        // Connection info
        String serverIp;
        String serverPort;

        Date timestamp;

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public static class BanInfo {
            // Message used to verify the ban message came from root
            Object banConfirmation;
            // List of users who have confirmed the ban, for tracking purposes
            List<String> confirmedBans;

            public Object getBanConfirmation() {
                return banConfirmation;
            }

            public void setBanConfirmation(Object banConfirmation) {
                this.banConfirmation = banConfirmation;
            }

            public List<String> getConfirmedBans() {
                return confirmedBans;
            }

            public void setConfirmedBans(List<String> confirmedBans) {
                this.confirmedBans = confirmedBans;
            }

            public BanInfo() {
            }
        }
    }

    public byte[] getRootKey() {
        return rootKey;
    }

    public void setRootKey(byte[] rootKey) {
        this.rootKey = rootKey;
    }

    public Map<String, PeerInfo> getPeers() {
        return peers;
    }

    public void setPeers(Map<String, PeerInfo> peers) {
        this.peers = peers;
    }

    public void newPeer(PeerInfo peer) {
        synchronized (peers) {
            peers.put(peer.getUsername(), peer);
        }
    }

    public void newPeerAndConfirm(PeerInfo peer) throws Exception {
        synchronized (peers) {
            if(peers.containsKey(peer.getUsername())) {
                throw new Exception("Peer username collision");
            }

            verifyNewUserConfirm(peer.getUsername(), peer.getIdentityKey(), peer.getNewUserConfirmation(), getRootKey());
            peers.put(peer.getUsername(), peer);
        }
    }

    public PeerInfoDirectory() {
    }

    String saveLocation;
    public PeerInfoDirectory(String filename) throws Exception{
        saveLocation = filename;
    }

    public void load() throws Exception {
        loadFromFile(saveLocation);
    }

    public void loadFromFile(String filename) throws Exception {
        fromJSONString(new String ( Files.readAllBytes( Paths.get(filename) ) ));
    }

    public void save() throws Exception{
        BufferedWriter writer = new BufferedWriter(new FileWriter(saveLocation));
        writer.write(toJSONString());
        writer.close();
    }



    Map<String, PeerInfo> peers;

    public String toJSONString() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this);
    }

    public void fromJSONString(String data) throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this).readValue(data);
    }

    // Initializes new root key for network
    // Returns public key of that keypair for storage
    public byte[] initializeNetwork() throws Exception {
        peers = new HashMap<>();
        AsymmetricCipher cipher = new AsymmetricCipher();
        KeyPair keyPair = cipher.generateKeyPair();
        rootKey = keyPair.getPrivate().getEncoded();
        return keyPair.getPublic().getEncoded();
    }
}
