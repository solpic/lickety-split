package org.licketysplit.securesocket;

import java.util.List;
import java.net.*;
import java.io.*;

public class SecureSocket {
    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;

    public SecureSocket() {

    }

    public void close() throws Exception{
        in.close();
        out.close();
        socket.close();
    }

    public static SecureSocket listen(int port) throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket clientSocket = serverSocket.accept();
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());

        SecureSocket secureSocket = new SecureSocket();
        secureSocket.socket = clientSocket;
        secureSocket.out = output;
        secureSocket.in = input;

        return secureSocket;
    }

    public static SecureSocket connect(PeerInfo peer) throws Exception {
        Socket clientSocket = new Socket(peer.getIp(), peer.getPort());
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());

        SecureSocket secureSocket = new SecureSocket();
        secureSocket.socket = clientSocket;
        secureSocket.out = output;
        secureSocket.in = input;

        return secureSocket;
    }

    public void sendData(byte[] data) throws Exception {
        out.write(data);
    }

    public int receiveData(byte[] data, int offset, int len) throws Exception {
        return in.read(data, offset, len);
    }

    public int receiveData(byte[] data) throws Exception {
        return in.read(data);
    }

    public List<Peer> getPeerList() throws Exception {
        throw new Exception("Not implemented");
    }
}
