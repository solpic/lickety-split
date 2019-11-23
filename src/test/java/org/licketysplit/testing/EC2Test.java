package org.licketysplit.testing;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.licketysplit.testing.TestHarness.P2PTestInfo;
public class EC2Test {
    @Test
    public void itWorks() throws Exception {
        TestHarness testHarness = new TestHarness();
        //testHarness.uploadToBucket(testHarness.jarPath, "jar", "licketysplit-jar");
        P2PTestInfo hackMaster = new P2PTestInfo();
        boolean shouldRedeploy = "yes".equals(System.getProperty("shouldRedeploy"));
//        System.out.println("Should redeploy: "+System.getProperty("shouldRedeploy"));
        //testHarness.installJava8();
        //testHarness.stopInstances();
        //testHarness.runLocal("C:\\Users\\meps5\\IdeaProjects\\licketysplit\\test-data\\testuser-3\\run.sh.bat");
        TestHarness.P2PTestInfo results = testHarness.generateNetwork(10, 2, shouldRedeploy);
        return;
    }

    @Test
    public void stopAll() throws Exception {
        TestHarness testHarness = new TestHarness();
        testHarness.stopInstances();
        return;
    }
}
