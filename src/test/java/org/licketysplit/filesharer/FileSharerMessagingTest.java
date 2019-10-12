//package org.licketysplit.filesharer;
//
//import org.junit.Test;
//import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
//import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
//import org.licketysplit.securesocket.PeerInfo;
//import org.licketysplit.securesocket.SecureSocket;
//import org.licketysplit.securesocket.messages.MessageHandler;
//import org.licketysplit.securesocket.messages.ReceivedMessage;
//
//import java.io.FileOutputStream;
//
//public class FileSharerMessagingTest {
//
//    public static class FileRequestResponseHandler implements MessageHandler {
//        @Override
//        public void handle(ReceivedMessage m) {
//            ChunkDownloadResponse decodedMessage = (ChunkDownloadResponse) m.getMessage();
//            try {
//                 FileOutputStream fos = new FileOutputStream("/Users/williamnewman/Testing/t2/tester.txt");
//                 fos.write(decodedMessage.data, 0, decodedMessage.data.length);
//                 fos.close();
//            }catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//    }
//
//    @Test
//    public void messagesWork() throws Exception {
//
//        int testPort = 10000;
//        Object lock = new Object();
//        class ServerThread extends Thread {
//            public void run() {
//                System.out.println("SERVER: Starting server thread");
//                try {
//                    System.out.println("SERVER: Listening");
//                    SecureSocket server = SecureSocket.listen(testPort);
//
//                    synchronized (lock) {
//                        lock.notify();
//                    }
//                } catch (Exception e) {
//                    System.out.println("Exception!");
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        ServerThread serverThread = new ServerThread();
//        serverThread.start();
//        Thread.sleep(1000);
//        System.out.println("CLIENT: Connecting to server");
//        SecureSocket client = SecureSocket.connect(new PeerInfo("localhost", testPort, true));
//
//        client.sendFirstMessage(new ChunkDownloadRequest("/Users/williamnewman/Testing/t1/tester.txt"), new FileRequestResponseHandler());
//
//        synchronized (lock) {
//            lock.wait();
//        }
//    }
//}
//
