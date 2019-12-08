package org.licketysplit.testing;

import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
/**
 * This class is only used for testing and doesn't affect normal usage of the application
 * whatsoever. It implements some helper functions to generate networks under various conditions.
 */
public class TestNetworkManager {
    String rootUser;
    Random rand;
    Integer testPortMin;
    File rootInfoFile;

    public TestNetworkManager(String username, boolean clearLogs) throws Exception {
        rootUser = username;
        rand = new Random(System.currentTimeMillis());
        testPortMin = 10000;
        usedPorts = new ArrayList<>();
//        File logDir = logPath.toFile();
//        if(!logDir.exists()) {
//            Files.createDirectory(logPath);
//        }
//        if(clearLogs) this.clearLogs();
    }

    public TestNetworkManager() throws Exception {
        this("testuser", true);
    }

    public static Path logPath = Paths.get("logs");

    List<Integer> usedPorts;

    public int nextPort() {
        synchronized (usedPorts) {
            int port;
            do {
                port = testPortMin + rand.nextInt(1000);
            } while (usedPorts.indexOf(port) != -1);
            usedPorts.add(port);
            return port;
        }
    }

    public Environment addPeer(boolean disableLogging) throws Exception {

        Integer port = nextPort();
        String username = String.format("%s-%d", rootUser, port);
        PeerManager pm = new PeerManager();
        PeerManager.ServerInfo server;
        server = new PeerManager.ServerInfo(port, "localhost");
        Environment env = new Environment(new UserInfo(username, server), pm, true);
        if(disableLogging) env.getLogger().disable();

        File log = Paths.get(logPath.toString(), username+".log").toFile();
        env.getLogger().setLogFile(log);
        pm.setEnv(env);
        //env.getLogger().log(Level.INFO, "Server is: "+env.getUserInfo().getServer().getPort());

        PeerInfoDirectory.PeerInfo peerInfo = new PeerInfoDirectory.PeerInfo();
        peerInfo.setUsername(username);
        peerInfo.setServerIp("localhost");
        peerInfo.setServerPort(port.toString());
        peerInfo.setTimestamp(new Date());
        byte[][] keys = peerInfo.generateIdentityKey();
        peerInfo.setIdentityKey(keys[0]);

        File idKeyFile = File.createTempFile("idkey-"+username, null);
        idKeyFile.deleteOnExit();
        KeyStore idKeyStore = new KeyStore(idKeyFile.getPath());
        idKeyStore.setKey(keys[1]);
        peerInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(username, keys[0], rootKeyStore.getKey()));

        File infoFile = File.createTempFile("infofile-" + port, null);
        infoFile.deleteOnExit();
        synchronized (rootInfoDir) {
            rootInfoDir.newPeer(peerInfo);
            rootInfoDir.save();
            FileUtils.copyFile(rootInfoFile, infoFile);
        }



        PeerInfoDirectory info = new PeerInfoDirectory(infoFile.getPath());
        info.load();

        env.setIdentityKey(idKeyStore);
        env.setInfo(info);
        info.env(env);

        return env;
    }

    public void clearLogs() throws Exception{
        FileUtils.cleanDirectory(logPath.toFile());
    }

    public static class PeerGenInfo {
        public String username;
        public String ip;
        public Integer port;
        public String instanceId;
        public boolean isRoot;
        public boolean isLocal;
        public boolean localThreaded;

        public String workingDir;
        public String cmd;
        public String cmdPath;
        public String[] args;

        public PeerGenInfo(String username, String ip, Integer port, String instanceId,
                           boolean isRoot, boolean isLocal, boolean localThreaded) {
            this.username = username;
            this.ip = ip;
            this.port = port;
            this.instanceId = instanceId;
            this.isRoot  = isRoot;
            this.isLocal = isLocal;
            this.localThreaded = localThreaded;
        }
    }

    public static class TestNetworkDataInfo {
        public String rootKeyFile;
        public String infoFile;
        public Map<String, String> idKeys;
        public List<PeerGenInfo> peers;
    }

    public TestNetworkDataInfo generateNetworkWithPeers(String dst, List<PeerGenInfo> peers) throws Exception {
        FileUtils.cleanDirectory(new File(dst));
        File infodir = new File(Paths.get(dst, "infodir").toString());
        PeerInfoDirectory info = new PeerInfoDirectory(infodir.getPath());
        byte[] rootKey = info.initializeNetwork();
        File rootKeyFile = new File(Paths.get(dst, "rootkey").toString());
        KeyStore rootKeyStore = new KeyStore(rootKeyFile.getPath());
        rootKeyStore.setKey(rootKey);
        rootKeyStore.save();

        Map<String, String> idKeyFiles = new HashMap<>();
        for (PeerGenInfo peer : peers) {
            PeerInfoDirectory.PeerInfo peerInfo = new PeerInfoDirectory.PeerInfo();
            peerInfo.setUsername(peer.username);
            peerInfo.setServerIp(peer.ip);
            peerInfo.setServerPort(Integer.toString(peer.port));
            peerInfo.setTimestamp(new Date());
            byte[][] keys = peerInfo.generateIdentityKey();
            peerInfo.setIdentityKey(keys[0]);

            File idKeyFile = new File(Paths.get(dst, peer.username+".id.key").toString());
            idKeyFiles.put(peer.username, idKeyFile.getPath());
            KeyStore idKeyStore = new KeyStore(idKeyFile.getPath());
            idKeyStore.setKey(keys[1]);
            idKeyStore.save();
            peerInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(peer.username, keys[0], rootKey));

            info.newPeer(peerInfo);
        }

        info.save();
        TestNetworkDataInfo data = new TestNetworkDataInfo();
        data.idKeys = idKeyFiles;
        data.infoFile = infodir.getPath();
        data.rootKeyFile = rootKeyFile.getPath();
        data.peers = peers;
        return data;
    }

    PeerInfoDirectory rootInfoDir;
    KeyStore rootKeyStore;

    public Environment getRootEnv() throws Exception {
        rootInfoFile = File.createTempFile("testInfoDir", null);
        rootInfoFile.deleteOnExit();
        PeerInfoDirectory info = new PeerInfoDirectory(rootInfoFile.getPath());
        rootInfoDir = info;
        byte[] rootKey = info.initializeNetwork();

        File rootKeyFile = File.createTempFile("rootKeyFile", null);
        rootKeyFile.deleteOnExit();
        rootKeyStore = new KeyStore(rootKeyFile.getPath());
        rootKeyStore.setKey(rootKey);

        Integer port = nextPort();
        String username = String.format("%s-%d", rootUser, port);
        PeerInfoDirectory.PeerInfo rootInfo = new PeerInfoDirectory.PeerInfo();
        rootInfo.setUsername(username);
        rootInfo.setServerIp("localhost");
        rootInfo.setServerPort(port.toString());
        rootInfo.setTimestamp(new Date());
        byte[][] keys = rootInfo.generateIdentityKey();
        rootInfo.setIdentityKey(keys[0]);

        File rootIdKeyFile = File.createTempFile("rootIdKey", null);
        rootIdKeyFile.deleteOnExit();
        KeyStore rootIdKeyStore = new KeyStore(rootIdKeyFile.getPath());
        rootIdKeyStore.setKey(keys[1]);
        rootInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(username, keys[0], rootKeyStore.getKey()));

        info.newPeer(rootInfo);


        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, "localhost");
        UserInfo user = new UserInfo(username, serverInfo);
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm, true);
        File log = Paths.get(logPath.toString(), "ROOT"+".log").toFile();
        env.getLogger().setLogFile(log);

        pm.setEnv(env);
        env.setInfo(info);
        env.setIdentityKey(rootIdKeyStore);
        env.setRootKey(rootKeyStore);
        info.env(env);

        info.save();
        return env;
    }

}
