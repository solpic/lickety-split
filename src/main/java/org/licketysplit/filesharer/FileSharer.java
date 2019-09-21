package org.licketysplit.filesharer;

import org.licketysplit.securesocket.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileSharer {
    private SecureSocket client;
    private ShareableFile file;

    public FileSharer() {

    }

    public void loadFile(String pathname) {
        this.file = new ShareableFile(pathname);
    }

    public void upSync(SecureSocket socket, ShareableFile file) throws Exception {
        FileInputStream fis = new FileInputStream(file);
        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            socket.sendData(buffer);
        }

        fis.close();
    }

    public void downSync(SecureSocket socket) throws Exception {
        FileOutputStream fos = new FileOutputStream("received2testfile.txt");
        byte[] buffer = new byte[4096];

        int fileSize = 9; // Send file size in separate msg
        int read = 0;
        int totalRead = 0;
        int remaining = fileSize;

        while((read = socket.receiveData(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
            totalRead += read;
            remaining -= read;
            System.out.println("read " + totalRead + " bytes.");
            fos.write(buffer, 0, read);
        }

        fos.close();
    }


}
