package org.licketysplit.securesocket;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.peers.*;

import java.io.File;
import java.security.KeyPair;
import java.util.*;
import java.util.logging.Level;

import org.junit.jupiter.api.Test;
import org.licketysplit.testing.TestNetworkManager;
import org.licketysplit.testing.WaitUntil;

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



    @ParameterizedTest
    @ValueSource(ints = {10})
    public void newConnectionsWork(int numPeers) throws Exception {
        TestNetworkManager mgr = new TestNetworkManager();
        mgr.clearLogs();
        Environment rootEnv = mgr.getRootEnv();
        Object lock = new Object();
        long extendTime = 4000;
        WaitUntil rootWaitUntil = new WaitUntil();

        List<String> users = new ArrayList<>();
        class ServerThread extends Thread {
            Environment env;
            public void run() {
                try {
                    env = mgr.addPeer(false);
                    synchronized (users) {
                        users.add(env.getUserInfo().getUsername());
                    }
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

            public void checkPeerList(int n) {
                assertEquals(n, env.getPm().getPeers().size(),
                        String.format("For user: %s, expected: %d, got: %d",
                                env.getUserInfo().getUsername(), numPeers, env.getPm().getPeers().size()));
            }
        }

        rootEnv.getDebug().setTrigger("handshaking",
                (Object ...args)-> {
                    rootWaitUntil.extend(extendTime);
                });
        rootEnv.getPm().start();
        users.add(rootEnv.getUserInfo().getUsername());


        List<ServerThread> peers = new ArrayList<>();
        for(int i = 0; i<numPeers; i++) {
            ServerThread serverThread = new ServerThread();
            serverThread.start();
            peers.add(serverThread);
            Thread.sleep(1000);
        }
        rootWaitUntil.waitUntil(extendTime);
        assertEquals(numPeers, rootEnv.getPm().getPeers().size(),
                String.format("For user: %s, expected: %d, got: %d",
                        rootEnv.getUserInfo().getUsername(), numPeers, rootEnv.getPm().getPeers().size()));
        for (ServerThread peer : peers) {
            peer.checkPeerList(numPeers);
        }

        String toBeBanned = users.get(users.size()-1);
        rootEnv.getLogger().log(Level.INFO, "BANNING "+toBeBanned);
        rootEnv.getInfo().banUser(toBeBanned, rootEnv.getRootKey().getKey());

        Thread.sleep(10000);
        assertEquals(numPeers-1, rootEnv.getPm().getPeers().size(),
                String.format("For user: %s, expected: %d, got: %d",
                        rootEnv.getUserInfo().getUsername(), numPeers, rootEnv.getPm().getPeers().size()));
        for (ServerThread peer : peers) {
            if(!peer.env.getUserInfo().getUsername().equals(toBeBanned))
            peer.checkPeerList(numPeers-1);
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
