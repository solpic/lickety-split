package org.licketysplit.testing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.testing.TestHarness.P2PTestInfo;

import java.io.File;
import java.util.Random;

public class EC2Test {
    @Test
    public void itWorks() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        String shouldRedeployVal = System.getProperty("shouldRedeploy");
        boolean shouldRedeploy = "yes".equals(shouldRedeployVal);
        String useLocalThreadedVal = System.getProperty("useLocalThreaded");
        boolean localThreaded = "yes".equals(useLocalThreadedVal);
        System.out.println(String.format(
                "Should redeploy -> '%s', so val is %b, local threaded -> '%s', so val is %b",
                shouldRedeployVal, shouldRedeploy,
                useLocalThreadedVal,
                localThreaded
        ));

        TestHarness.P2PTestInfo results = testHarness.generateNetwork(0, 5, shouldRedeploy, localThreaded);
        return;
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
