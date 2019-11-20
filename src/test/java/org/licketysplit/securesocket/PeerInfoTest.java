package org.licketysplit.securesocket;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.licketysplit.env.Debugger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.*;

import java.io.File;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.*;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PeerInfoTest {
    @Test
    public void shouldExportToJSON() throws Exception{
        String testFileName = "testfile.json";
        PeerInfoDirectory info = new PeerInfoDirectory(testFileName);
        info.initializeNetwork();
        info.save();

        PeerInfoDirectory info2 = new PeerInfoDirectory(testFileName);
        info2.load();

        assertArrayEquals(info.getRootKey(), info2.getRootKey());

        new File(testFileName).delete();
    }

    @Test
    public void keystoreShouldStoreAndLoad() throws Exception {
        Random random = new Random();
        byte[] key = new byte[random.nextInt(100)+100];
        random.nextBytes(key);

        File file = new File("testkey");
        KeyStore storer = new KeyStore(file.getPath());
        storer.setKey(key);
        storer.save();

        KeyStore loader = new KeyStore(file.getPath());
        loader.load();

        assertArrayEquals(storer.getKey(), loader.getKey());

        file.delete();
    }

    void initTestNetwork() throws Exception {

    }

    public static String logPath = "logs/";
    public static class TestNetworkManager {
        String rootUser;
        Random rand;
        Integer testPortMin;
        File rootInfoFile;
        public TestNetworkManager(String username) {
            rootUser = username;
            rand = new Random(System.currentTimeMillis());
            testPortMin = 10000;
            usedPorts = new ArrayList<>();

        }

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

            File log = new File(logPath+username+".log");
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
            File log = new File(logPath+rootUser+".log");
            env.getLogger().setLogFile(log);

            pm.setEnv(env);
            env.setInfo(info);
            env.setIdentityKey(rootIdKeyStore);

            info.save();
            return env;
        }

    }

    public static class WaitUntil {
        public WaitUntil() {
            stopTimeLock = new Object();
            stopTime = -1;
        }

        long stopTime;
        Object stopTimeLock;
        public void waitUntil(long initial) throws Exception{
            synchronized (stopTimeLock) {
                stopTime = System.currentTimeMillis() + initial;
            }

            boolean done = false;
            do{
                long delta = stopTime-System.currentTimeMillis();
                if(delta>0) {
                    Thread.sleep(delta);
                    if(System.currentTimeMillis()>stopTime) done = true;
                }else{
                    done = true;
                }
            } while(!done);
        }

        public void extend(long time) {
            synchronized (stopTimeLock) {
                if(stopTime>=0) {
                    stopTime = System.currentTimeMillis()+time;
                }
            }
        }
    }

    void clearLogs() throws Exception{
        FileUtils.cleanDirectory(new File(logPath));
    }

    @ParameterizedTest
    @ValueSource(ints = {10})
    public void newConnectionsWork(int numPeers) throws Exception {
        clearLogs();
        TestNetworkManager mgr = new TestNetworkManager("testuser");
        Environment rootEnv = mgr.getRootEnv();
        Object lock = new Object();
        long extendTime = 2000;
        WaitUntil rootWaitUntil = new WaitUntil();
        class ServerThread extends Thread {
            Environment env;
            public void run() {
                try {
                    env = mgr.addPeer(false);

                    env.getDebug().setTrigger("handshaking",
                            (Object ...args)-> {
                                rootWaitUntil.extend(extendTime);
                            });
                    env.getPm().start();
                    synchronized (lock) {
                        lock.wait();
                    }
                } catch(Exception e) {
                    System.out.println("Exception!");
                    e.printStackTrace();
                }
            }

            public void checkPeerList() {
                assertEquals(numPeers, env.getPm().getPeers().size(),
                        String.format("For user: %s, expected: %d, got: %d",
                                env.getUserInfo().getUsername(), numPeers, env.getPm().getPeers().size()));
            }
        }

        rootEnv.getDebug().setTrigger("handshaking",
                (Object ...args)-> {
                    rootWaitUntil.extend(extendTime);
                });
        rootEnv.getPm().start();


        List<ServerThread> peers = new ArrayList<>();
        for(int i = 0; i<numPeers; i++) {
            ServerThread serverThread = new ServerThread();
            serverThread.start();
            peers.add(serverThread);
        }
        rootWaitUntil.waitUntil(extendTime);
        assertEquals(numPeers, rootEnv.getPm().getPeers().size(),
                String.format("For user: %s, expected: %d, got: %d",
                        rootEnv.getUserInfo().getUsername(), numPeers, rootEnv.getPm().getPeers().size()));
        for (ServerThread peer : peers) {
            peer.checkPeerList();
        }
        synchronized (lock) {
            lock.notifyAll();
        }
    }

    @Test
    public void signedPayloadsWork() throws Exception {
        long start = System.currentTimeMillis();
        int count = 1000;
        AsymmetricCipher cipher = new AsymmetricCipher();
        KeyPair keyPair = cipher.generateKeyPair();
        for(int i = 0; i<count; i++) {


            SignedPayload.Signer signer = new SignedPayload.Signer(keyPair.getPublic().getEncoded());
            SignedPayload.Verifier verifier = new SignedPayload.Verifier(keyPair.getPrivate().getEncoded());

            byte[] payload = new byte[1000];
            Random random = new Random();
            random.nextBytes(payload);

            SignedPayload signedPayload = signer.sign(payload);
            verifier.verify(signedPayload);
        }
        long end = System.currentTimeMillis();
        System.out.println((double)(end-start)/(double)count);
    }
}
