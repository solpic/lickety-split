package org.licketysplit.filesharer;

import org.licketysplit.securesocket.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;

public class FileSharer {
    private SecureSocket client;

    public FileSharer() {}

    public void upSync(SecureSocket socket, ShareableFile file) throws Exception {
//        FileInputStream fis = new FileInputStream(file);
//        byte[] buffer = new byte[4096];
//        socket.writeInt((int) file.length());
//
//        while (fis.read(buffer) > 0) {
//            socket.sendData(buffer);
//        }
//
//        fis.close();
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


}
