package org.licketysplit.securesocket;

import org.junit.Test;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.PeerManager;

import java.util.Random;

public class MessagingTest {
    @DefaultHandler(type = TestMessage.class)
    public static class TestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            TestMessage tstMsg = (TestMessage)m.getMessage();
            System.out.println("Received: "+tstMsg.data);
            if(tstMsg.data>1000) System.exit(0);
            try {
                m.respond(new TestMessage(tstMsg.data*2), new TestHandler());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static class TestMessage extends Message {
        @Override
        public byte[] toBytes() {
            return data.toString().getBytes();
        }

        @Override
        public void fromBytes(byte[] data) {
            this.data = Integer.parseInt(new String(data));
        }
        public TestMessage() {}

        public Integer data;
        public TestMessage(int data) {
            this.data = data;
        }
    }

    @Test
    public void messagesWork() throws Exception {
/*
        int testPort = 10000;
        Object lock = new Object();
        class ServerThread extends Thread {
            public void run() {
                System.out.println("SERVER: Starting server thread");
                try {
                    System.out.println("SERVER: Listening");
                    SecureSocket server = SecureSocket.listen(testPort);

                    synchronized (lock) {
                        lock.notify();
                    }
                } catch(Exception e) {
                    System.out.println("Exception!");
                    e.printStackTrace();
                }
            }
        }

        ServerThread serverThread = new ServerThread();
        serverThread.start();
        Thread.sleep(1000);
        System.out.println("CLIENT: Connecting to server");
        SecureSocket client = SecureSocket.connect(new PeerManager.PeerInfo("localhost", testPort, true));

        client.sendFirstMessage(new TestMessage(2), new TestHandler());
        //client.sendMessage(new TestMessage("hello"), null);

        synchronized (lock) {
            lock.wait();
        }*/
    }

    @Test
    public void newConnectionsWork() throws Exception {

        int testPort = 10000;
        Object lock = new Object();
        PeerManager.PeerAddress peer = new PeerManager.PeerAddress("localhost", testPort, true);
        class ServerThread extends Thread {
            public void run() {
                Random rand = new Random();
                String username = "merrill-";
                username += Integer.toString(rand.nextInt(1000));
                System.out.println("SERVER: Starting server thread");
                PeerManager pm = new PeerManager();
                Environment env = new Environment(new UserInfo(username), pm);
                pm.setEnv(env);
                try {
                    System.out.println("SERVER: Listening");
                    pm.initialize(peer);
                    synchronized (lock) {
                        lock.notify();
                    }
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
        Thread.sleep(1000);

        PeerManager pm = new PeerManager();
        Environment env = new Environment(new UserInfo("merrill"), pm);
        pm.setEnv(env);
        pm.listen(testPort);

        synchronized (lock) {
            lock.wait();
        }
    }
}
