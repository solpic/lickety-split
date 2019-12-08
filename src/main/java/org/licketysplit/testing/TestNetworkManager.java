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
    /**
     * The Root user.
     */
    String rootUser;
    /**
     * The Rand.
     */
    Random rand;
    /**
     * The Test port min.
     */
    Integer testPortMin;
    /**
     * The Root info file.
     */
    File rootInfoFile;

    /**
     * Instantiates a new Test network manager.
     *
     * @param username  the username
     * @param clearLogs the clear logs
     * @throws Exception the exception
     */
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

    /**
     * Instantiates a new Test network manager.
     *
     * @throws Exception the exception
     */
    public TestNetworkManager() throws Exception {
        this("testuser", true);
    }

    /**
     * The constant logPath.
     */
    public static Path logPath = Paths.get("logs");

    /**
     * The Used ports.
     */
    List<Integer> usedPorts;

    /**
     * Next port int.
     *
     * @return the int
     */
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

    /**
     * Add peer environment.
     *
     * @param disableLogging the disable logging
     * @return the environment
     * @throws Exception the exception
     */
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

    /**
     * Clear logs.
     *
     * @throws Exception the exception
     */
    public void clearLogs() throws Exception{
        FileUtils.cleanDirectory(logPath.toFile());
    }

    /**
     * The type Peer gen info.
     */
    public static class PeerGenInfo {
        /**
         * The Username.
         */
        public String username;
        /**
         * The Ip.
         */
        public String ip;
        /**
         * The Port.
         */
        public Integer port;
        /**
         * The Instance id.
         */
        public String instanceId;
        /**
         * The Is root.
         */
        public boolean isRoot;
        /**
         * The Is local.
         */
        public boolean isLocal;
        /**
         * The Local threaded.
         */
        public boolean localThreaded;

        /**
         * The Working dir.
         */
        public String workingDir;
        /**
         * The Cmd.
         */
        public String cmd;
        /**
         * The Cmd path.
         */
        public String cmdPath;
        /**
         * The Args.
         */
        public String[] args;

        /**
         * Instantiates a new Peer gen info.
         *
         * @param username      the username
         * @param ip            the ip
         * @param port          the port
         * @param instanceId    the instance id
         * @param isRoot        the is root
         * @param isLocal       the is local
         * @param localThreaded the local threaded
         */
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

    /**
     * The type Test network data info.
     */
    public static class TestNetworkDataInfo {
        /**
         * The Root key file.
         */
        public String rootKeyFile;
        /**
         * The Info file.
         */
        public String infoFile;
        /**
         * The Id keys.
         */
        public Map<String, String> idKeys;
        /**
         * The Peers.
         */
        public List<PeerGenInfo> peers;
    }

    /**
     * Generate network with peers test network data info.
     *
     * @param dst   the dst
     * @param peers the peers
     * @return the test network data info
     * @throws Exception the exception
     */
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

    /**
     * The Root info dir.
     */
    PeerInfoDirectory rootInfoDir;
    /**
     * The Root key store.
     */
    KeyStore rootKeyStore;

    /**
     * Gets root env.
     *
     * @return the root env
     * @throws Exception the exception
     */
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
