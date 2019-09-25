package org.licketysplit.securesocket;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.reflections.Reflections;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.List;
import java.net.*;
import java.io.*;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

public class SecureSocket {
    private Socket socket;
    private final DataOutputStream out;
    private final DataInputStream in;


    //<editor-fold desc="Constructor/deconstructor">
    public SecureSocket(Socket socket, DataOutputStream out, DataInputStream in) throws Exception {
        this.socket = socket;
        this.out = out;
        this.in = in;

        initMessagingService();
    }

    public void close() throws Exception{
        in.close();
        out.close();
        socket.close();
    }
    //</editor-fold>
    //<editor-fold desc="Factories">
    public static SecureSocket listen(int port) throws Exception {
        ServerSocket serverSocket = new ServerSocket(port);
        Socket clientSocket = serverSocket.accept();
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());

        return new SecureSocket(clientSocket, output, input);
    }

    public static SecureSocket connect(PeerInfo peer) throws Exception {
        Socket clientSocket = new Socket(peer.getIp(), peer.getPort());
        DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
        DataInputStream input = new DataInputStream(clientSocket.getInputStream());

        return new SecureSocket(clientSocket, output, input);
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
                    System.out.println(String.format("Received message of size: %d, MyID: %d, ResponseID: %d, classcode: %d", size, myId, responseId, classCode));

                    Message msg;// = Message.factory(classCode, payload);
                    msg = (Message)messageCodes[classCode].getConstructor().newInstance();
                    msg.fromBytes(payload);
                    MessageHandler messageHandler;
                    if(myId>=0) {
                         messageHandler = messageHandlers.get(myId);
                    }else{
                        messageHandler = defaultHandlers.get(classCode);
                    }
                    messageHandler.handle(new ReceivedMessage(msg, SecureSocket.this, responseId));
                } catch (Exception e) {
                    System.err.println("Error in reader thread");
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

                    System.out.println(String.format("Sending message with ID: %d, ResponseID: %d, Code: %d, Size: %d", id, nextMessage.respondingToMessage, classCode, payload.length));

                    out.writeInt(id);
                    out.writeInt(nextMessage.respondingToMessage);
                    out.writeInt(classCode);
                    out.writeInt(payload.length);
                    out.write(payload);
                } catch (Exception e) {
                    System.err.println("Exception during socket writer");
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
                System.out.println("Running default handler initialozro");
                defaultHandlers = new ConcurrentHashMap<>();
                Reflections reflections = new Reflections("org.licketysplit");
                Set<Class<? extends Message>> messageTypes = reflections.getSubTypesOf(Message.class);
                messageCodes =  messageTypes.toArray(new Class[messageTypes.size()]);

                Arrays.sort(messageCodes, (Class a, Class b) -> a.getName().compareTo(b.getName()));

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

    public List<Peer> getPeerList() throws Exception {
        throw new Exception("Not implemented");
    }
}
