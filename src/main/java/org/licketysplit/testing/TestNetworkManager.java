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
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;

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
        File logDir = logPath.toFile();
        if(!logDir.exists()) {
            Files.createDirectory(logPath);
        }
        if(clearLogs) this.clearLogs();
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

        return env;
    }

    public void clearLogs() throws Exception{
        FileUtils.cleanDirectory(logPath.toFile());
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
        PeerInfoDirectory.PeerInfo rootInfo = new PeerInfoDirectory.PeerInfo();
        rootInfo.setUsername(rootUser);
        rootInfo.setServerIp("localhost");
        rootInfo.setServerPort(port.toString());
        rootInfo.setTimestamp(new Date());
        byte[][] keys = rootInfo.generateIdentityKey();
        rootInfo.setIdentityKey(keys[0]);

        File rootIdKeyFile = File.createTempFile("rootIdKey", null);
        rootIdKeyFile.deleteOnExit();
        KeyStore rootIdKeyStore = new KeyStore(rootIdKeyFile.getPath());
        rootIdKeyStore.setKey(keys[1]);
        rootInfo.setNewUserConfirmation(PeerInfoDirectory.generateNewUserConfirm(rootUser, keys[0], rootKeyStore.getKey()));

        info.newPeer(rootInfo);


        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, "localhost");
        UserInfo user = new UserInfo(rootUser, serverInfo);
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm, true);
        File log = Paths.get(logPath.toString(), rootUser+".log").toFile();
        env.getLogger().setLogFile(log);

        pm.setEnv(env);
        env.setInfo(info);
        env.setIdentityKey(rootIdKeyStore);

        info.save();
        return env;
    }

}
