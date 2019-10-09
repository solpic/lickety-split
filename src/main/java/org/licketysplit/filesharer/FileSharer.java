package org.licketysplit.filesharer;

import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileSharer {
    private SecureSocket client;

    public FileSharer() {}

    public void upSync(SecureSocket socket, ShareableFile file) throws Exception {

    }

    public void downSync(SecureSocket socket) throws Exception {
//        FileOutputStream fos = new FileOutputStream("received2testfile.txt");
//        byte[] buffer = new byte[4096];
//
//        int fileSize = -1;
//        while(fileSize < 0){
//            fileSize = socket.readInt();
//        }
//
//        int read = 0;
//        int totalRead = 0;
//        int remaining = fileSize;
//        while((read = socket.receiveData(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
//            totalRead += read;
//            remaining -= read;
//            fos.write(buffer, 0, read);
//        }
//
//        fos.close();
    }

    public static class ChunkDownloadResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            ChunkDownloadResponse decodedMessage = (ChunkDownloadResponse) m.getMessage();
            try {
                FileOutputStream fos = new FileOutputStream("/Users/williamnewman/Testing/t2/tester.txt");
                fos.write(decodedMessage.data, 0, decodedMessage.data.length);
                fos.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void download(SecureSocket socket, String fileName) throws Exception {
        socket.sendFirstMessage(new ChunkDownloadRequest(fileName), new ChunkDownloadResponseHandler());
    }

}
