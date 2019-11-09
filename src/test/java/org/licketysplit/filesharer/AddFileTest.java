package org.licketysplit.filesharer;

import org.junit.Test;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.SyncManager;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Random;
import java.util.logging.Level;

public class AddFileTest{

    @Test
    public void addFileWorks() throws Exception {

        Integer testPort = 10000;
        final Integer[] nextPort = {testPort + 1};
        Object lock = new Object();
        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(testPort, "localhost");
        UserInfo user = new UserInfo("merrill", serverInfo);
        PeerManager.PeerAddress peer = new PeerManager.PeerAddress("localhost", testPort, user, serverInfo);
        class ServerThread extends Thread {
            public void run() {
                Random rand = new Random();
                String username = "merrill-";
                username += Integer.toString(rand.nextInt(1000));
                PeerManager pm = new PeerManager();
                FileManager fm = new FileManager();
                SyncManager sm = new SyncManager();
                FileSharer fs = new FileSharer();
                PeerManager.ServerInfo server;
                synchronized (nextPort[0]) {
                    server = new PeerManager.ServerInfo(nextPort[0], "localhost");
                    nextPort[0]++;
                }
                Environment env = new Environment(new UserInfo(username, server), pm);
                env.getLogger().log(Level.INFO, "Server is: "+env.getUserInfo().getServer().getPort());
                String directory =  "Test";
                String configs = "configs";
                initialize(env, fs, fm, pm, sm, directory, configs);
                try {
                    pm.initialize(peer);
                    pm.listenInNewThread();
                    env.getLogger().log(Level.INFO, "Sending update");
                    System.out.println("added 1");
                    sm.addFile(System.getProperty("user.home") + "/1");
                    // sm.addFile(System.getProperty("user.home") + "/tester.txt");
                    while(true){}

                } catch(Exception e) {
                    System.out.println("Exception!");
                    e.printStackTrace();
                }
            }
        }

        for(int i = 0; i<1; i++) {
            ServerThread serverThread = new ServerThread();
            serverThread.start();
        }
        FileManager fm = new FileManager();
        FileSharer fs = new FileSharer();
        //fm.initializeFiles(System.getProperty("user.home") + "/Test1/", "wnewman");
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm);
        String directory = "Test1";
        String configs = "configs1";
        SyncManager sm = new SyncManager();
        initialize(env, fs, fm, pm, sm, directory, configs);
        pm.listenInNewThread();
        Thread.sleep(5000);
        System.out.println("downloading 1");
        fs.download(new FileInfo("1", false));
        while(true){}

    }

    public void initialize(Environment env, FileSharer fs, FileManager fm, PeerManager pm, SyncManager sm, String shared, String configs){
        deleteFilesInFolder(shared);
        deleteFilesInFolder(configs);
        initializeFilesInFolder(shared);
        env.setFM(fm);
        env.setFS(fs);
        env.setDirectory(shared);
        env.setConfigs(configs);
        fs.setEnv(env);
        fm.setEnv(env);
        pm.setEnv(env);
        sm.setEnv(env);
        fm.initializeFiles("default");
    }

    public void deleteFilesInFolder(String folder){
        File index = new File(Paths.get(System.getProperty("user.home"), folder).toString());
        if(!index.exists()){
            return;
        }
        String[]entries = index.list();
        if(entries.length == 0){
            return;
        }
        for(String s: entries){
            File currentFile = new File(index.getPath(),s);
            currentFile.delete();
        }
    }

    public void initializeFilesInFolder(String folder){
        new File(Paths.get(System.getProperty("user.home"), folder).toString()).mkdir();
        File tester = new File(Paths.get(System.getProperty("user.home"), folder, "tester.txt").toString());
        try{
            tester.createNewFile();
        } catch(IOException e){
            e.printStackTrace();
        }

    }
}