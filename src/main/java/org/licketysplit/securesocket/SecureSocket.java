package org.licketysplit.securesocket;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.net.*;
import java.io.*;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;

public class SecureSocket {
    private Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;
    private Environment env;
    private EnvLogger log;
    private UserInfo userInfo;
    private PeerManager.ServerInfo serverInfo;

    private SymmetricCipher cipher;
    private boolean useEncryption = false;

    public SymmetricCipher getCipher() {
        return cipher;
    }

    public void activateEncryption() {
        useEncryption = true;
    }

    public void setCipher(SymmetricCipher cipher) {
        this.cipher = cipher;
    }

    //<editor-fold desc="Constructor/deconstructor">
    public SecureSocket(Socket socket, DataOutputStream out, DataInputStream in, Environment env, UserInfo userInfo, PeerManager.ServerInfo server) throws Exception {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.env = env;
        this.log = env.getLogger();
        this.userInfo = userInfo;
        this.serverInfo = server;

        initMessagingService();
    }

    public PeerManager.ServerInfo getServerInfo() {
        return serverInfo;
    }

    public void setServerInfo(PeerManager.ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    boolean shouldClose = false;
    public void close() throws Exception{
        in.close();
        out.close();
        socket.close();
        shouldClose = true;
    }
    public static interface NewConnectionCallback {
        void onConnect(SecureSocket sock) throws Exception;
    }

    public PeerManager.PeerAddress getPeerAddress() {
        return new PeerManager.PeerAddress(socket.getInetAddress().toString(), socket.getPort(), userInfo, getServerInfo());
    }

    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    public static void listen(int port, NewConnectionCallback fnc, Environment env) throws Exception {
        if(port<=0) {
            throw new Exception("Can't listen on negative port");
        }
        ServerSocket serverSocket = new ServerSocket(port);
        while(true) {
            //env.getLogger().log(Level.INFO,
            //        "Listening for new connection on port: "+port);
            Socket clientSocket = serverSocket.accept();
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            //env.getLogger().log(Level.INFO,"Accepting new connection");
            fnc.onConnect(new SecureSocket(clientSocket, output, input, env, null, null));
        }
    }



    public static class ConnectionError extends Exception {
        public ConnectionError(String e) {
            super(e);
        }
    }

    public static SecureSocket connect(PeerManager.PeerAddress peer, Environment env) throws Exception {
        try {
            String ip = peer.getServerInfo().getIp();
            int port = peer.getServerInfo().getPort();
            if(port<0) {
                throw new Exception("Can't connect to negative port");
            }
            env.getLogger().log(Level.INFO, "Attempting to connect to IP: " + ip + ", port: " + port);
            Socket clientSocket = new Socket(ip, port);
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            env.getLogger().log(Level.INFO, "Connected to IP: " + ip + ", port: " + port);
            return new SecureSocket(clientSocket, output, input, env, peer.getUser(), null);
        } catch(Exception e) {
            e.printStackTrace();
            throw new ConnectionError(String.format("%s: Error connecting to IP: %s, port: %d",
                    env.getUserInfo().getUsername(), peer.getServerInfo().getIp(), peer.getServerInfo().getPort()));
        }
    }
    class MessagePair {
        Message msg;
        MessageHandler handler;
        Integer respondingToMessage;

        public MessagePair(Message msg, MessageHandler handler, Integer respondingToMessage) {
            this.msg = msg;
            this.handler = handler;
            this.respondingToMessage = respondingToMessage;
        }
    }
    public static class MessagingError extends Exception {
        public MessagingError(String error) {
            super(error);
        }
    }
    public Environment getEnv() {
        return env;
    }
    private BlockingQueue<MessagePair> messages;
    private ConcurrentHashMap<Integer, MessageHandler> messageHandlers;
    private static ConcurrentHashMap<Integer, MessageHandler> defaultHandlers;
    private static Class[] messageCodes;
    public static Object defaultHandlersLock = new Object();

    final int headersSize = 4*4;

    Integer socketErrorCount = 0;
    protected void socketError(){
        synchronized (socketErrorCount) {
            socketErrorCount++;
            if(socketErrorCount>4) {
                try {
                    close();
                }
                catch(Exception e) {
                    env.getLogger().log(Level.SEVERE, "Couldn't close socket");
                }
            }
        }
    }
    class SocketReader implements Runnable {

        ConcurrentLinkedQueue<Message> oldMessages;
        @Override
        public void run() {
            oldMessages = new ConcurrentLinkedQueue<>();
            while(true) {
                try {
//                    log.log(Level.OFF, "Awaiting next message");
                    // Read message


                    //ID to map to handler on this end
                    Integer responseId = null;
                    Integer myId = null;
                    Integer classCode = null;
                    Integer size = null;
                    byte[] payload = null;

                    String username = env.getUserInfo().getUsername();
                    Integer headersSize = in.readInt();
//                    log.log(Level.OFF, String.format("Read headers size: %d", headersSize));

                    byte[] headers = new byte[headersSize];
                    in.read(headers, 0, headersSize);
                    byte[] oldHeaders = new byte[headersSize];
                    for (int i = 0; i < headers.length; i++) {
                        oldHeaders[i] = headers[i];
                    }

                    byte[] headersFinal = null;
                    if(useEncryption) {
//                        log.log(Level.OFF,
//                                String.format("Ciphertext: %s",
//                                Base64.getEncoder().encodeToString(headers)));
                        headersFinal = cipher.decrypt(headers);
                    }else{
                        headersFinal = headers;
                    }
                    ByteBuffer headersBuffer = ByteBuffer.wrap(headersFinal);
                    responseId = headersBuffer.getInt();
                    myId = headersBuffer.getInt();
                    classCode = headersBuffer.getInt();
                    size = headersBuffer.getInt();

                    if(responseId<0||myId<-1||classCode<0||size<0) {
                        env.getLogger().log(Level.SEVERE, "Negative value");
                        throw new MessagingError(String.format("For '%s', negative value from '%s'", env.getUserInfo().getUsername(),
                                userInfo!=null?userInfo.getUsername():"unknown"));
                    }
                    payload = new byte[size];
//                    log.log(Level.OFF, String.format("Reading payload of size: %d", size));
                    in.read(payload, 0, size);

                    byte[] payloadFinal = null;
                    if(useEncryption) {
                        payloadFinal = cipher.decrypt(payload);
                    }else{
                        payloadFinal = payload;
                    }
//                    log.log(Level.OFF,
//                            String.format(
//                                    "Received message of \n\tsize: %d, \n\tMyID: %d, \n\tResponseID: %d, \n\tclasscode: %d, \n\tname: %s",
//                            size, myId, responseId, classCode, messageCodes[classCode].getName()));

                    //log.log(Level.INFO, new String(payloadFinal));
                    Message msg;// = Message.factory(classCode, payload);
                    msg = (Message)messageCodes[classCode].getConstructor().newInstance();
                    msg.fromBytes(payloadFinal);
                    MessageHandler messageHandler;
                    if(messageHandlers.containsKey(myId)) {
                         messageHandler = messageHandlers.get(myId);
                    }else{
                        messageHandler = defaultHandlers.get(classCode);
                    }
                    oldMessages.add(msg);
//                    log.log(Level.OFF, "Calling response handler");
                    messageHandler.handle(new ReceivedMessage(msg, SecureSocket.this, responseId, env));
//                    log.log(Level.OFF, "Done calling response handler");
                } catch (Exception e) {
                    socketError();
                    if(shouldClose) return;
                    else {
                        log.log(Level.SEVERE, "Error in reader thread");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // Sits/blocks on output stream
    // Gets next message from queue, generates unused ID, sends message and puts handler in map
    class SocketWriter implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    // Get next message
                    MessagePair nextMessage = messages.take();

                    // Generate ID and save handler
                    Integer id = assignHandler(nextMessage.handler);
                    Integer classCode = getOpCode(nextMessage.msg);
                    byte[] payload = nextMessage.msg.toBytes();

                    if(id<0||nextMessage.respondingToMessage<-1||classCode<0) {
                        env.getLogger().log(Level.SEVERE, "Negative value");
                        throw new MessagingError("Negative value");
                    }
                    ByteBuffer headers = ByteBuffer.allocate(headersSize);
                    headers.putInt(id);
                    headers.putInt(nextMessage.respondingToMessage);
                    headers.putInt(classCode);
                    byte[] payloadBytes = null;

                    if (useEncryption) {
                        payloadBytes = cipher.encrypt(payload);
                    }else{
                        payloadBytes = payload;
                    }
                    headers.putInt(payloadBytes.length);
                    byte[] headersBytes = null;
                    if(useEncryption) {
                        headersBytes = cipher.encrypt(headers.array());
                    }else{
                        headersBytes = headers.array();
                    }

//                    log.log(Level.OFF, String.format(
//                            "Sending message with \n\tID: %d, \n\tResponseID: %d, \n\tCode: %d, \n\tClass: %s, \n\tSize: %d, \n\tUsing encryption: %b",
//                            id, nextMessage.respondingToMessage, classCode, messageCodes[classCode].getName(), payload.length, useEncryption));

                    out.writeInt(headersBytes.length);
                    out.write(headersBytes);
                    out.write(payloadBytes);
                    out.flush();

                    if(nextMessage.msg.doesActivateEncryption()) {
                        activateEncryption();
                    }
                } catch (Exception e) {
                    socketError();
                    if(shouldClose) return;
                    else {
                        log.log(Level.SEVERE, "Exception during socket writer");
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static int getOpCode(Message m) throws Exception{
        Class<? extends Message> clazz = m.getClass();
        for (int i = 0; i < messageCodes.length; i++) {
            if(messageCodes[i].equals(clazz)) return i;
        }

        throw new Exception("Unregistered message class");
    }

    public static void initDefaultHandlers() throws Exception {
        synchronized (defaultHandlersLock) {
            if (defaultHandlers == null && messageCodes == null) {
                defaultHandlers = new ConcurrentHashMap<>();
                Reflections reflections = new Reflections("org.licketysplit");
                Set<Class<? extends Message>> messageTypes = reflections.getSubTypesOf(Message.class);
                messageCodes =  messageTypes.toArray(new Class[messageTypes.size()]);

                Arrays.sort(messageCodes, (Class a, Class b) -> a.getName().compareTo(b.getName()));
                //for (int i = 0; i < messageCodes.length; i++) {
                 //   System.out.println(messageCodes[i].getName()+": "+i);
                //}

                Set<Class<?>> handlers = reflections.getTypesAnnotatedWith(DefaultHandler.class);
                for (Class<?> handler : handlers) {
                    if (MessageHandler.class.isAssignableFrom(handler)) {
                        DefaultHandler handlerAnno = handler.getAnnotation(DefaultHandler.class);
                        Class<? extends Message> messageType = handlerAnno.type();
                        for (int i = 0; i < messageCodes.length; i++) {
                            if(messageType==messageCodes[i]) {
                                Constructor<?>[] constructors = handler.getConstructors();
                                defaultHandlers.put(i, (MessageHandler)handler.getConstructors()[0].newInstance());
                                break;
                            }
                        }
                    } else {
                        throw new Exception("Tried to assign non handler \""+handler+"\" as handler");
                    }
                }
            }
        }
    }

    public void setDefaultHandler(Integer id, MessageHandler handler) throws Exception{
        if(id>=0) {
            throw new Exception("Default handler id should be less than 0");
        }else{
            if(messageHandlers.putIfAbsent(id, handler)!=null) {
                throw new Exception("Default handler ID collision");
            }
        }
    }

    // Generates ID and assigns handler to map
    Integer assignHandler(MessageHandler handler) {
        Integer id = messageHandlers.size();
        if(handler==null) return id;

        while(messageHandlers.putIfAbsent(id, handler)!=null) {
            id++;
            if(id<0) id = 0;
        }

        return id;
    }

    private SocketReader messageReader;
    private SocketWriter messageWriter;
    private Thread messageReaderThread;
    private Thread messageWriterThread;

    void initMessagingService() throws Exception{
        messages = new LinkedBlockingQueue<>();
        messageHandlers = new ConcurrentHashMap<>();
        initDefaultHandlers();

        messageReader = new SocketReader();
        messageWriter = new SocketWriter();

        messageReaderThread = new Thread(messageReader);
        messageWriterThread = new Thread(messageWriter);

        messageReaderThread.start();
        messageWriterThread.start();
    }

    public ReceivedMessage sendMessageAndWait(Message msg, Integer respondingTo) throws Exception {
        final ReceivedMessage[] response = new ReceivedMessage[1];
        Object lock = new Object();
        sendMessage(msg,
                (ReceivedMessage m) -> {
                    response[0] = m;
                    //log.log(Level.INFO, String.format("Message handler for type: %s", m.getMessage().getClass().getName()));
                    synchronized(lock) {
                        lock.notifyAll();
                    }
                }
                , respondingTo);
        synchronized(lock) {
            lock.wait();
            //log.log(Level.INFO, String.format("Unlocking for type: %s", response[0].getMessage().getClass().getName()));
        }
        return response[0];
    }

    public ReceivedMessage sendMessageAndWait(Message msg) throws Exception {
        Integer respondingTo = -1;
        return sendMessageAndWait(msg, respondingTo);
    }

    public void sendMessage(Message msg, MessageHandler handler, Integer respondingTo) throws Exception {
        MessagePair messagePair = new MessagePair(msg, handler, respondingTo);
        messages.put(messagePair);
    }

    public void sendFirstMessage(Message msg, MessageHandler handler) throws Exception {
        sendMessage(msg, handler, -1);
    }
}
