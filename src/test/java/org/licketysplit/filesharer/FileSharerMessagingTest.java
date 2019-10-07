package org.licketysplit.filesharer;

import org.junit.Test;
import org.licketysplit.filesharer.messages.FileRequestMessage;
import org.licketysplit.filesharer.messages.FileRequestResponseMessage;
import org.licketysplit.securesocket.PeerInfo;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

import java.io.FileOutputStream;

public class FileSharerMessagingTest {

    public static class FileRequestResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            FileRequestResponseMessage decodedMessage = (FileRequestResponseMessage) m.getMessage();
            System.out.println(decodedMessage.data.length);
            try {
                 FileOutputStream fos = new FileOutputStream("/Users/williamnewman/Testing/t2/tester.txt");
                 fos.write(decodedMessage.data, 0, decodedMessage.data.length);
                 fos.close();
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void messagesWork() throws Exception {

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
                } catch (Exception e) {
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

        client.sendFirstMessage(new FileRequestMessage("/Users/williamnewman/Testing/t1/tester.txt"), new FileRequestResponseHandler());

        synchronized (lock) {
            lock.wait();
        }
    }
}

