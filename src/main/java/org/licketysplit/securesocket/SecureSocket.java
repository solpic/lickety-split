package org.licketysplit.securesocket;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.net.*;
import java.io.*;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
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

    public void close() throws Exception{
        in.close();
        out.close();
        socket.close();
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

    //</editor-fold>
    //<editor-fold desc="Factories">
    public static void listen(int port, NewConnectionCallback fnc, Environment env) throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        while(true) {
            env.getLogger().log(Level.INFO,
                    "Listening for new connection on port: "+port);
            Socket clientSocket = serverSocket.accept();
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            env.getLogger().log(Level.INFO,"Accepting new connection");
            fnc.onConnect(new SecureSocket(clientSocket, output, input, env, null, null));
        }
    }

    public static SecureSocket connect(PeerManager.PeerAddress peer, Environment env) throws Exception {
        String ip = peer.getServerInfo().getIp();
        int port = peer.getServerInfo().getPort();
        env.getLogger().log(Level.INFO,"Attempting to connect to IP: "+ip+", port: "+port);
        Socket clientSocket = new Socket(ip, port);
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());
        env.getLogger().log(Level.INFO,"Connecting");
        return new SecureSocket(clientSocket, output, input, env, peer.getUser(), null);
    }
    //</editor-fold>

    /*
    Messaging Interface:

    Header:
    4 byte id (int), 8 byte size (long)
     */
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
    private BlockingQueue<MessagePair> messages;
    private ConcurrentHashMap<Integer, MessageHandler> messageHandlers;
    private static ConcurrentHashMap<Integer, MessageHandler> defaultHandlers;
    private static Class[] messageCodes;
    public static Object defaultHandlersLock = new Object();

    class SocketReader implements Runnable {

        @Override
        public void run() {
            while(true) {
                try {
                    // Read message

                    //ID to send back in response
                    int responseId = in.readInt();

                    //ID to map to handler on this end
                    int myId = in.readInt();
                    int classCode = in.readInt();
                    int size = in.readInt();


                    byte[] payload = new byte[size];
                    in.read(payload, 0, size);
                    log.log(Level.INFO,
                            String.format("Received message of size: %d, MyID: %d, ResponseID: %d, classcode: %d, name: %s",
                            size, myId, responseId, classCode, messageCodes[classCode].getName()));


                    Message msg;// = Message.factory(classCode, payload);
                    msg = (Message)messageCodes[classCode].getConstructor().newInstance();
                    msg.fromBytes(payload);
                    MessageHandler messageHandler;
                    if(messageHandlers.containsKey(myId)) {
                         messageHandler = messageHandlers.get(myId);
                    }else{
                        messageHandler = defaultHandlers.get(classCode);
                    }
                    messageHandler.handle(new ReceivedMessage(msg, SecureSocket.this, responseId, env));
                } catch (Exception e) {
                    log.log(Level.SEVERE,"Error in reader thread");
                    e.printStackTrace();
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

                    log.log(Level.INFO, String.format("Sending message with ID: %d, ResponseID: %d, Code: %d, Class: %s, Size: %d",
                            id, nextMessage.respondingToMessage, classCode, messageCodes[classCode].getName(), payload.length));

                    out.writeInt(id);
                    out.writeInt(nextMessage.respondingToMessage);
                    out.writeInt(classCode);
                    out.writeInt(payload.length);
                    out.write(payload);

//                    if(nextMessage.msg instanceof ChunkDownloadResponse) {
//                        log.log(Level.INFO, String.format("CHUNK: ID: %d, rID: %d, payload: %s",
//                                id, nextMessage.respondingToMessage,
//                                Base64.getEncoder().encodeToString(((ChunkDownloadResponse)nextMessage.msg).data)));
//                    }
                } catch (Exception e) {
                    log.log(Level.SEVERE, "Exception during socket writer");
                    e.printStackTrace();
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
                for (int i = 0; i < messageCodes.length; i++) {
                    System.out.println(messageCodes[i].getName()+": "+i);
                }

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

    public void sendMessage(Message msg, MessageHandler handler, Integer respondingTo) throws Exception {
        MessagePair messagePair = new MessagePair(msg, handler, respondingTo);
        messages.put(messagePair);
    }

    public void sendFirstMessage(Message msg, MessageHandler handler) throws Exception {
        sendMessage(msg, handler, -1);
    }
}
