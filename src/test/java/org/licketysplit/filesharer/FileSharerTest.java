package org.licketysplit.filesharer;

import org.junit.Test;
import org.licketysplit.securesocket.PeerInfo;
import org.licketysplit.securesocket.SecureSocket;

import java.net.URL;

public class FileSharerTest {
    @Test
    public void socketsShouldConnect() throws Exception {
        FileSharer sender = new FileSharer();

        int testPort = 10002;
        String testData = "Hello this is a test";
        Object lock = new Object();
        class ServerThread extends Thread {
            public void run() {
                FileSharer sender = new FileSharer();
                ShareableFile file = new ShareableFile("/Users/williamnewman/SeniorDesign/lickety-split/src/test.txt");
                System.out.println("SERVER: Starting server thread");
                try {
                    System.out.println("SERVER: Listening");
                    SecureSocket server = SecureSocket.listen(testPort);
                    byte[] data = new byte[256];
                    System.out.println("SERVER: Received connection");
                    sender.upSync(server, file);
                    System.out.println("SERVER: Received data: "+new String(data));
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
        SecureSocket client = SecureSocket.connect(new PeerInfo("localhost", testPort, true));
        sender.downSync(client);
        System.out.println("CLIENT: Connected, receiving data");
        client.sendData(testData.getBytes());
        synchronized (lock) {
            lock.wait();
        }
    }
}
