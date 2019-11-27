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


    // Syncs two info directories, updating where necessary
    // Returns true if changes were made
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

    public void banUser(String username, byte[] rootKey) throws Exception {
        PeerInfo nPeerInfo = null;
        synchronized (peers) {
            if(peers.containsKey(username)) {
                PeerInfo peerInfo = peers.get(username);
                if(peerInfo.getBan()==null) {
                    PeerInfo.BanInfo ban = new PeerInfo.BanInfo();
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

    public static SignedPayload generateBan(String username, byte[] rootKey) throws Exception {
        SignedPayload.Signer signer = new SignedPayload.Signer(rootKey);
        return signer.sign(username.getBytes());
    }

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
        BanInfo ban;

        public BanInfo getBan() {
            return ban;
        }

        public void setBan(BanInfo ban) {
            this.ban = ban;
        }

        public Date getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Date timestamp) {
            this.timestamp = timestamp;
        }

        public static class BanInfo {
            // Message used to verify the ban message came from root
            SignedPayload banConfirmation;
            // List of users who have confirmed the ban, for tracking purposes
            List<String> confirmedBans;

            public SignedPayload getBanConfirmation() {
                return banConfirmation;
            }

            public void setBanConfirmation(SignedPayload banConfirmation) {
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

    public void newPeerCallback(PeerInfo peer) throws Exception {

    }

    Environment e;

    public void env(Environment e) {
        this.e = e;
    }

    public Environment env() {
        return e;
    }

    public void banPeerCallback(PeerInfo peer) throws Exception {
        env().getPm().userBanned(peer.getUsername());
    }

    public void newPeerAndBanCallback(PeerInfo peer) throws Exception {
        env().getPm().userBanned(peer.getUsername());
    }

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

    public void newPeerAndConfirm(PeerInfo peer) throws Exception {
        updatePeer(peer, false);
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
        synchronized(peers) {
            return mapper.writeValueAsString(this);
        }
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
        rootKey = keyPair.getPublic().getEncoded();
        return keyPair.getPrivate().getEncoded();
    }
}
