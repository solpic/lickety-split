package org.licketysplit.syncmanager;

import org.junit.Test;

public class FileManagerTest {
    @Test
    public void filesShouldCreate() throws Exception {
        FileManager fileManager = new FileManager();
        fileManager.initializeFiles(System.getProperty("user.home") + "/TestDestination/", "wnewman");
        fileManager.addFile(System.getProperty("user.home") + "/TestFiles/tester2.txt");
    }
}
