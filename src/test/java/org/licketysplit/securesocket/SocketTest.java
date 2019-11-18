package org.licketysplit.securesocket;


import java.nio.charset.Charset;
import java.util.Random;

/*
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

    @Test
    public void socketReadWriteOrder() throws Exception {
        int testPort = 10000;
        final Boolean done = false;

        class WriterThread implements Runnable {
            SecureSocket socket;
            String id;
            public WriterThread(SecureSocket socket, String id) {
                this.socket = socket;
                this.id = id;
            }

            public void run() {
                Random rand = new Random();

                int i = 0;
                while(!done) {
                    String msg = "Sending data from "+id+" # "+i;
                    System.out.println(msg);
                    byte[] data = msg.getBytes();
                    try {
                        this.socket.sendData(data);
                        Thread.sleep(rand.nextInt(1000));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    i++;
                }
            }
        }
        class ReaderThread implements Runnable {
            SecureSocket socket;
            String id;
            public ReaderThread(SecureSocket socket, String id) {
                this.socket = socket;
                this.id = id;
            }

            public void run() {
                while(!done) {
                    byte[] data = new byte[512];
                    try {
                        int len = this.socket.receiveData(data);
                        System.out.println(id+" received: "+new String(data));
                    }catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        Object lock = new Object();
        class ServerThread extends Thread {
            public void run() {
                System.out.println("SERVER: Starting server thread");
                try {
                    System.out.println("SERVER: Listening");
                    SecureSocket server = SecureSocket.listen(testPort);
                    new Thread(new ReaderThread(server, "SERVER_READER")).start();
                    new Thread(new WriterThread(server, "SERVER_WRITER")).start();
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


        new Thread(new ReaderThread(client, "CLIENT_READER")).start();
        new Thread(new WriterThread(client, "CLIENT_WRITER")).start();
        Thread.sleep(5000);

    }
}
*/