package org.licketysplit.testing;

import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.SyncManager;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TestRunner {
    public TestRunner() {

    }
    public static String rootKeyFilename = "rootkey";
    public static String idKeyFilename = "idkey";
    public static String infoFilename = "infodir";
    public static String cmdFilename = "run.sh";
    public Environment getEnv(String ip, int port, String username, boolean isRoot, String localPath) throws Exception {
        PeerInfoDirectory info = new PeerInfoDirectory(Paths.get(localPath, infoFilename).toString());
        info.load();

        KeyStore rootKeyStore = null;
        if(isRoot) {
            rootKeyStore = new KeyStore(Paths.get(localPath, rootKeyFilename).toString());
            rootKeyStore.load();
        }

        KeyStore idKeyStore = new KeyStore(Paths.get(localPath, idKeyFilename).toString());
        idKeyStore.load();

        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, ip);
        org.licketysplit.securesocket.peers.UserInfo user = new UserInfo(username, serverInfo);
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm, true);
        File log = Paths.get(localPath, "log").toFile();
        env.getLogger().setLogFile(log);

        pm.setEnv(env);
        env.setInfo(info);
        env.setIdentityKey(idKeyStore);
        if(isRoot) env.setRootKey(rootKeyStore);
        info.env(env);

        FileManager fm = new FileManager();
        FileSharer fs = new FileSharer();

        String directory = "sharedFilesffff";
        File sharedDir = new File(Paths.get(localPath, directory).toString());
        if(sharedDir.exists()) {
            FileUtils.cleanDirectory(sharedDir);
        }else {
            sharedDir.mkdir();
        }
        String configs = Paths.get(localPath, "configs1").toString();
        SyncManager sm = new SyncManager();

        env.setFM(fm);
        env.setFS(fs);
        env.setDirectory(sharedDir.getAbsolutePath());
        env.setConfigs(configs);
        fs.setEnv(env);
        fm.setEnv(env);
        pm.setEnv(env);
        sm.setEnv(env);
        env.setSyncManager(sm);

        fm.initializeFiles("default");

        return env;
    }

    int userNumber(String username) throws Exception{
        int i = username.indexOf('-');
        int i2 = username.indexOf('-', i + 1);

        return Integer.parseInt(username.substring(i+1, i2));
    }

    void runMain(Environment env, int usernumber, String localPath) throws Exception{
        env.getLogger().log(Level.INFO, String.format("I am user number %d", usernumber));

        String contents = "this is a test file";
        if(usernumber==0) {
            env.log("Running user 0 handler");
            Thread.sleep(5000);
            SyncManager sm = env.getSyncManager();
            File hello = Paths.get(localPath, "hello.txt").toFile();
            hello.createNewFile();
            hello.deleteOnExit();
            FileUtils.writeStringToFile(hello, contents, (String)null);
            sm.addFile(hello.getPath());
            Thread.sleep(20000);
        }else{
            Thread.sleep(15000);
//            env.getFS().download(new FileInfo("hello.txt", false, contents.length()));
//            Thread.sleep(5000);
            File f = new File(env.getFM().getSharedDirectoryPath("hello.txt"));
            if(f.exists()) {
                String downloadedContents = FileUtils.readFileToString(f, "UTF-8");


                boolean equals = contents.equals(downloadedContents);
                String message = equals ? "Files match" : String.format("Files don't match, contents: %s, downloaded: %s", contents, downloadedContents);
                env.log(String.format("Assert: %b, Message: %s", equals, message));
            }else{
                env.log("Assert: false, Message: no file");
            }
        }
//        if(usernumber==1) {
//            env.log("Running user 1 handler");
//            Thread.sleep(10000);
//            FileSharer fs = env.getFS();
//            fs.download(new FileInfo("hello.txt", false, (int) contents.length()));
//            Thread.sleep(10000);
//
//            String downloadedContents = FileUtils.readFileToString(
//                    new File(env.getFM().getSharedDirectoryPath("hello.txt")), "UTF-8");
//
//
//            boolean equals = contents.equals(downloadedContents);
//            env.log(String.format(
//                    "FINISHED ->\nContents: %s\nDownloaded contents: %s\nEquals?: %b",
//                    contents, downloadedContents, equals));
//        }

    }

    public void startPeer(String[] args) throws Exception {
        String username = args[1];
        String ip = args[2];
        int port = Integer.parseInt(args[3]);

        boolean isRoot = false;
        if(args.length>4)isRoot = "isroot=yes".equals(args[4]);
        String localPath = ".";
        boolean isLocalThread = false;
        if(args.length>5) {
            localPath = args[5];
            isLocalThread = true;
        }

        Environment env = getEnv(ip, port, username, isRoot, localPath);
        env.getLogger().log(Level.INFO, String.format("Starting peer at IP: %s, ARGS: %s", ip, Arrays.stream(args).collect(Collectors.joining(", "))));
        env.getPm().start();

        try {
            runMain(env, userNumber(username), localPath);
        } catch (Exception e) {
            env.log("Exception in runner!");
            env.getLogger().log(Level.SEVERE, "RunMain exception", e);
        }

        env.getLogger().log(Level.INFO, "Waiting 30 seconds");
        //Thread.sleep(50000);
        env.getLogger().log(Level.INFO, "Exiting");
        if(!isLocalThread)  System.exit(0);
    }

    public void run(String[] args) throws Exception {
        if(args==null||args.length==0) {
            throw new Exception("Expected arguments");
        }
        startPeer(args);
    }
}
