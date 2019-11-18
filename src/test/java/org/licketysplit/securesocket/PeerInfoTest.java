package org.licketysplit.securesocket;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
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

        public Environment addPeer() throws Exception {
            String username = String.format("%s-%d", rootUser, rand.nextInt(1000));
            PeerManager pm = new PeerManager();
            PeerManager.ServerInfo server;
            Integer port = nextPort();
            server = new PeerManager.ServerInfo(port, "localhost");
            Environment env = new Environment(new UserInfo(username, server), pm);
            pm.setEnv(env);
            env.getLogger().log(Level.INFO, "Server is: "+env.getUserInfo().getServer().getPort());

            File infoFile = new File("infofile-" + port);
            infoFile.deleteOnExit();
            FileUtils.copyFile(rootInfoFile, infoFile);

            PeerInfoDirectory info = new PeerInfoDirectory(infoFile.getPath());
            info.load();

            env.setInfo(info);

            return env;
        }

        public Environment getRootEnv() throws Exception {
            rootInfoFile = new File("testInfoDir");
            rootInfoFile.deleteOnExit();
            PeerInfoDirectory info = new PeerInfoDirectory(rootInfoFile.getPath());
            byte[] rootKey = info.initializeNetwork();

            File rootKeyFile = new File("rootKeyFile");
            rootKeyFile.deleteOnExit();
            KeyStore rootKeyStore = new KeyStore(rootKeyFile.getPath());
            rootKeyStore.setKey(rootKey);

            File idKeyFile = new File(String.format("idKey-%d", rand.nextInt(10000)));
            idKeyFile.deleteOnExit();
            KeyStore idKeyStore = new KeyStore(idKeyFile.getPath());

            Integer port = nextPort();
            PeerInfoDirectory.PeerInfo rootInfo = new PeerInfoDirectory.PeerInfo();
            rootInfo.setUsername(rootUser);
            rootInfo.setServerIp("localhost");
            rootInfo.setServerPort(port.toString());

            info.newPeer(rootInfo);


            PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, "localhost");
            UserInfo user = new UserInfo(rootUser, serverInfo);
            PeerManager pm = new PeerManager();
            Environment env = new Environment(user, pm);

            pm.setEnv(env);
            env.setInfo(info);

            info.save();
            return env;
        }

    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3, 5})
    public void newConnectionsWork(int numPeers) throws Exception {
        TestNetworkManager mgr = new TestNetworkManager("testuser");
        Environment rootEnv = mgr.getRootEnv();
        Object lock = new Object();
        class ServerThread extends Thread {
            public void run() {
                try {
                    Environment env = mgr.addPeer();
                    Thread.sleep(1000);
                    env.getPm().start();
                    /*
                    Map<String, PeerInfoDirectory.PeerInfo> peers = env.getInfo().getPeers();
                    Map.Entry<String, PeerInfoDirectory.PeerInfo> root = peers.entrySet().iterator().next();
                    env.getPm().initialize(root.getValue().convertToPeerAddress());
                    env.getPm().listenInNewThread();
                     */
                    Thread.sleep(2000);
                    assertEquals(env.getPm().getPeers().size(), 1);

                    synchronized (lock) {
                        lock.notify();
                    }
                } catch(Exception e) {
                    System.out.println("Exception!");
                    e.printStackTrace();
                }
            }
        }

        for(int i = 0; i<numPeers; i++) {
            ServerThread serverThread = new ServerThread();
            serverThread.start();
        }

        rootEnv.getPm().start();
        Thread.sleep(2000);
        assertEquals(rootEnv.getPm().getPeers().size(), numPeers);

        synchronized (lock) {
            lock.wait();
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
