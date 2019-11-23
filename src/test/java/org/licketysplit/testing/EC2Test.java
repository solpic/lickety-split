package org.licketysplit.testing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.licketysplit.testing.TestHarness.P2PTestInfo;

import java.io.File;

public class EC2Test {
    @Test
    public void itWorks() throws Exception {
        TestHarness testHarness = new TestHarness();
        P2PTestInfo hackMaster = new P2PTestInfo();
        boolean shouldRedeploy = "yes".equals(System.getProperty("shouldRedeploy"));

        TestHarness.P2PTestInfo results = testHarness.generateNetwork(20, 0, shouldRedeploy);
        return;
    }

    @Test
    public void stopAll() throws Exception {
        TestHarness testHarness = new TestHarness();
        testHarness.stopInstances();
        return;
    }
}
