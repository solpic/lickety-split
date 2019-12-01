package org.licketysplit.testing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.licketysplit.env.Debugger;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.testing.TestHarness.P2PTestInfo;

import java.io.File;
import java.nio.file.Paths;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class EC2Test {
    public static boolean shouldRedeploy() {
        return "yes".equals( System.getProperty("shouldRedeploy"));
    }
    public static boolean localThreaded() {
        return "yes".equals(System.getProperty("useLocalThreaded"));
    }

    @Test
    public void itWorks() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());
        testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        ConcurrentHashMap<String, String> hasFiles = new ConcurrentHashMap<>();
        int remoteCount = 20;
        int localCount = 0;
        AtomicInteger running = new AtomicInteger(remoteCount + localCount);
        Debugger.global().setTrigger("count", (Object ...args) -> {
            running.decrementAndGet();
            System.out.println("RUNNING: "+running.get());
            if(running.get()==0) {
                try {
                    testHarness.finish();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        testHarness.cleanAllRunning();
        TestHarness.P2PTestInfo results = testHarness.generateNetwork(remoteCount, localCount, "peerstart", shouldRedeploy(), localThreaded(), false);
        assertEquals(running.get(), 0);
    }

    @Test
    public void deployTestNetwork() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        Logger allLogs = TestHarness.fileOnlyLogger("allLogs", Paths.get("everything.log").toString());
        Logger testStatus = TestHarness.fileAndConsoleLogger("testStatus", Paths.get("test-results.log").toString());
        testHarness.allLogs = allLogs;
        testHarness.testStatusLogger = testStatus;
        ConcurrentHashMap<String, String> hasFiles = new ConcurrentHashMap<>();
        int remoteCount = 5;
        testHarness.cleanAllRunning();
        TestHarness.P2PTestInfo results = testHarness.generateNetwork(remoteCount, 1, "headless", shouldRedeploy(), localThreaded(), true);
    }

    @Test
    public void logDownloader() throws Exception {
        TestHarness testHarness = new TestHarness();
        testHarness.logDownloader();
    }

    @Test
    public void cleanAllRunning() throws Exception {
        TestHarness testHarness = new TestHarness();

    }

    @Test
    public void encryptionTest() throws Exception {
        SymmetricCipher cipher = new SymmetricCipher();
        SymmetricCipher.SymmetricKey symmetricKey = cipher.generateKey();
        cipher.setKey(symmetricKey.getKey().getEncoded(), symmetricKey.getIv());

        Random random = new Random();
        for(int i = 0 ; i<1000; i++) {
            int size = random.nextInt(20000)+100;
            byte[] plaintext = new byte[size];
            random.nextBytes(plaintext);

            byte[] ciphertext = cipher.encrypt(plaintext);
            System.out.println(plaintext.length+" -> "+ciphertext.length);
            byte[] decrypted = cipher.decrypt(ciphertext);

            assertArrayEquals(plaintext, decrypted);
        }
    }

    @Test
    public void stopAll() throws Exception {
        TestHarness testHarness = new TestHarness();
        testHarness.stopInstances();
        return;
    }
}
