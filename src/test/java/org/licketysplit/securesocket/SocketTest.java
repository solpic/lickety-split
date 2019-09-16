package org.licketysplit.securesocket;

import org.junit.Test;

import java.nio.charset.Charset;

public class SocketTest {
    @Test
    public void socketsShouldConnect() throws Exception {
        int testPort = 10000;
        String testData = "Hello this is a test";
        Object lock = new Object();
        class ServerThread extends Thread {
            public void run() {
                System.out.println("SERVER: Starting server thread");
                try {
                    System.out.println("SERVER: Listening");
                    SecureSocket server = SecureSocket.listen(testPort);
                    byte[] data = new byte[256];
                    System.out.println("SERVER: Received connection");
                    System.out.println("SERVER: Received "+server.receiveData(data)+" bytes");
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
        System.out.println("CLIENT: Connected, sending data");
        client.sendData(testData.getBytes());
        synchronized (lock) {
            lock.wait();
        }
    }
}
