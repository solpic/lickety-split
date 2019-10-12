//package org.licketysplit.syncmanager;
//
//import org.json.JSONObject;
//import org.junit.Rule;
//import org.junit.Test;
//import org.junit.contrib.java.lang.system.SystemOutRule;
//
//
//import java.io.File;
//import java.io.FileWriter;
//import java.io.IOException;
//
//public class FileManagerTest {
//    @Rule
//    public final SystemOutRule systemOutRule = new SystemOutRule().enableLog();
//
//    @Test
//    public void filesShouldCreate() throws Exception {
//         FileManager fileManager = new FileManager();
//         fileManager.initializeFiles(System.getProperty("user.home") + "/TestDestination/", "wnewman");
//         fileManager.addFile(System.getProperty("user.home") + "/TestFiles/tester2.txt");
//    }
//}
