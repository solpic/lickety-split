//package org.licketysplit.filesharer;
//
//import org.junit.Test;
//import org.licketysplit.env.Environment;
//import org.licketysplit.securesocket.messages.DefaultHandler;
//import org.licketysplit.securesocket.messages.Message;
//import org.licketysplit.securesocket.messages.MessageHandler;
//import org.licketysplit.securesocket.messages.ReceivedMessage;
//import org.licketysplit.securesocket.peers.PeerManager;
//import org.licketysplit.securesocket.peers.UserInfo;
//import org.licketysplit.syncmanager.FileManager;
//import org.licketysplit.syncmanager.SyncManager;
//
//import java.util.Random;
//import java.util.logging.Level;
//
//public class FileUpdateTest{
//
//    @Test
//    public void fileUpdatesWork() throws Exception {
//
//        Integer testPort = 10000;
//        final Integer[] nextPort = {testPort + 1};
//        Object lock = new Object();
//        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(testPort, "localhost");
//        UserInfo user = new UserInfo("merrill", serverInfo);
//        PeerManager.PeerAddress peer = new PeerManager.PeerAddress("localhost", testPort, user, serverInfo);
//        class ServerThread extends Thread {
//            public void run() {
//                Random rand = new Random();
//                String username = "merrill-";
//                username += Integer.toString(rand.nextInt(1000));
//                PeerManager pm = new PeerManager();
//                PeerManager.ServerInfo server;
//                synchronized (nextPort[0]) {
//                    server = new PeerManager.ServerInfo(nextPort[0], "localhost");
//                    nextPort[0]++;
//                }
//                Environment env = new Environment(new UserInfo(username, server), pm);
//                pm.setEnv(env);
//                env.getLogger().log(Level.INFO, "Server is: "+env.getUserInfo().getServer().getPort());
//                FileManager fm = new FileManager();
//                fm.initializeFiles(System.getProperty("user.home") + "/TestDestination/", "wnewman");
//                try {
//                    pm.initialize(peer);
//                    pm.listen();
//                    SyncManager sm = new SyncManager();
//                    sm.setEnv(env);
//                    System.out.println("updating-Merrill");
//                    sm.updateFile("tester.txt");
//                    synchronized (lock) {
//                        lock.notify();
//                    }
//                } catch(Exception e) {
//                    System.out.println("Exception!");
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        for(int i = 0; i<10; i++) {
//            ServerThread serverThread = new ServerThread();
//            serverThread.start();
//        }
//        Thread.sleep(1000);
//        FileManager fm = new FileManager();
//        fm.initializeFiles(System.getProperty("user.home") + "/TestFiles/", "wnewman");
//        PeerManager pm = new PeerManager();
//        Environment env = new Environment(user, pm);
//        pm.setEnv(env);
//        pm.listen();
//        System.out.println("listening - will");
//
//        synchronized (lock) {
//            lock.wait();
//        }
//    }
//}