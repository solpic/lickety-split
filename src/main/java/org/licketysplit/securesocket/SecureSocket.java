package org.licketysplit.securesocket;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.encryption.AsymmetricCipher;
import org.licketysplit.securesocket.encryption.SymmetricCipher;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.GetPeerListResponse;
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
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;

/**
 * All network communication goes through SecureSocket instances.
 * There is one SecureSocket instance between each pair of connected peers.
 * The SecureSocket wraps a single TCP socket, and the client and server both
 * read and write to this TCP socket. Each SecureSocket has a thread for reading
 * from the TCP socket, and a thread for writing to the TCP socket. The main functionality
 * that the SecureSocket provides is an RPC system (which we call a messaging system) as well
 * as encryption.<br><br>
 * To send a message, the caller provides a Message object and a MessageHandler instance
 * (or a null for the MessageHandler if the caller doesn't care about the response).
 * The message and message handler are then placed into a BlockingQueue.
 * The writer thread sleeps while there are no elements in the blocking queue, and wakes up
 * as soon as there is a new message in the queue. The writer thread takes the message from the queue
 * and then generates a unique integer ID for the message, which is then used to put the response handler
 * into a map with the ID as the key. The writer thread serializes the message using the toBytes method,
 * and encrypts the message. Then, metadata consisting of the message's ID, the message's length, and
 * the classname of the message is also encrypted. The encrypted metadata is written to the socket
 * and then the encrypted message is written to the socket. This is all the writer thread does.
 * <br><br>
 * The reader thread blocks until new information is written to the socket. When new data is in the socket,
 * it reads the data, decrypts it, and divides it into the metadata and the message. Based on the classname
 * in the metadata, it deserializes the message into its correct Message subclass using the fromBytes method.
 * If this message is not a response to another message, the writer thread finds the corresponding DefaultHandler
 * for this message type, and calls that DefaultHandler with the message as an argument. If the message is a response,
 * it finds the response handler for the message using the ID given in the metadata, and then calls that response
 * handler with the message as an argument.
 */
public class SecureSocket {
    /**
     * The TCP socket.
     */
    private Socket socket;
    /**
     * Used to write raw bytes to the socket.
     */
    private final DataOutputStream out;
    /**
     * Used to read raw bytes from the socket.
     */
    private final DataInputStream in;
    /**
     * Environment for the user.
     */
    private Environment env;
    /**
     * Logger.
     */
    private EnvLogger log;
    /**
     * User info for the peer we are connected to.
     */
    private UserInfo userInfo;
    /**
     * Server info for the peer we are connected to.
     */
    private PeerManager.ServerInfo serverInfo;

    /**
     * This encrypts data if encryption has been activated,
     * using a one time AES key.
     */
    private SymmetricCipher cipher;
    /**
     * This is true if encryption has been activated.
     */
    private boolean useEncryption = false;
    /**
     * No longer used.
     */
    private Object encryptionLock = new Object();

    /**
     * Gets cipher.
     *
     * @return the cipher
     */
    public SymmetricCipher getCipher() {
        return cipher;
    }

    /**
     * Activate encryption.
     */
    public void activateEncryption() {
        useEncryption = true;
    }

    /**
     * Sets cipher.
     *
     * @param cipher the cipher
     */
    public void setCipher(SymmetricCipher cipher) {
        this.cipher = cipher;
    }

    /**
     * The username of the peer we are connected to.
     */
    public String peerUsername = "unknown";
    /**
     * For debugging.
     */
    int mySocketId;

    /**
     * Instantiates a new Secure socket.
     *
     * @param socket   the TCP socket
     * @param out      the output stream
     * @param in       the input stream
     * @param env      the env
     * @param userInfo the user info
     * @param server   the server info
     * @throws Exception the exception
     */
    public SecureSocket(Socket socket, DataOutputStream out, DataInputStream in, Environment env, UserInfo userInfo, PeerManager.ServerInfo server) throws Exception {
        this.socket = socket;
        this.out = out;
        this.in = in;
        this.env = env;
        this.log = env.getLogger();
        this.userInfo = userInfo;
        this.serverInfo = server;
        mySocketId = sockId.getAndIncrement();
        initMessagingService();
    }

    /**
     * Gets server info.
     *
     * @return the server info
     */
    public PeerManager.ServerInfo getServerInfo() {
        return serverInfo;
    }

    /**
     * Sets server info.
     *
     * @param serverInfo the server info
     */
    public void setServerInfo(PeerManager.ServerInfo serverInfo) {
        this.serverInfo = serverInfo;
    }

    /**
     * True if we should exit out of any threads.
     */
    boolean shouldClose = false;

    /**
     * Closes the socket by closing the input stream, output stream,
     * TCP socket, and telling the peer manager that this socket is closing
     * so that it can update the peer list accordingly.
     *
     * @param failure no longer used
     */
    public void close(boolean failure){
        try {
            in.close();
            out.close();
            socket.close();
            shouldClose = true;
            env.getPm().confirmClosed(this);
        }catch(Exception e) {
            env.getLogger().log(Level.INFO, "Error closing socket", e);
        }
    }

    /**
     * Callback interface for when a new connection happens.
     */
    public static interface NewConnectionCallback {
        /**
         * On connect.
         *
         * @param sock the socket
         * @throws Exception the exception
         */
        void onConnect(SecureSocket sock) throws Exception;
    }

    /**
     * Gets peer address for ourself.
     *
     * @return the peer address
     */
    public PeerManager.PeerAddress getPeerAddress() {
        return new PeerManager.PeerAddress(socket.getInetAddress().toString(), socket.getPort(), userInfo, getServerInfo());
    }

    /**
     * Sets user info.
     *
     * @param userInfo the user info
     */
    public void setUserInfo(UserInfo userInfo) {
        this.userInfo = userInfo;
    }

    /**
     * Listens for incoming TCP connections on the specified port.
     * This method blocks.
     *
     * @param port the port
     * @param fnc  callback function for new connections
     * @param env  the environment
     * @throws Exception the exception
     */
    public static void listen(int port, NewConnectionCallback fnc, Environment env) throws Exception {
        if(port<=0) {
            throw new Exception("Can't listen on negative port");
        }
        // Create listener
        ServerSocket serverSocket = new ServerSocket(port);
        while(true) {
            try {
                env.getLogger().log(Level.INFO,
                        "Listening for new connection on port: " + port);

                // Accept a new connection and create streams
                Socket clientSocket = serverSocket.accept();
                DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
                DataInputStream input = new DataInputStream(clientSocket.getInputStream());
                env.getLogger().log(Level.INFO, "Accepting new connection");

                // Call callback function
                fnc.onConnect(new SecureSocket(clientSocket, output, input, env, null, null));
            } catch(Exception e) {
                env.getLogger().log(Level.INFO,
                        String.format("Error while listening"),
                        e);
            }
        }
    }


    /**
     * No longer used.
     */
    public static class ConnectionError extends Exception {
        /**
         * Instantiates a new Connection error.
         *
         * @param e the e
         */
        public ConnectionError(String e) {
            super(e);
        }
    }

    /**
     * Attempts to connect over TCP to a peer at the given PeerAddress.
     *
     * @param peer the peer
     * @param env  our env
     * @return the secure socket
     * @throws Exception the exception
     */
    public static SecureSocket connect(PeerManager.PeerAddress peer, Environment env) throws Exception {
        try {
            // Extract IP address and port
            String ip = peer.getServerInfo().getIp();
            int port = peer.getServerInfo().getPort();
            if(port<0) {
                throw new Exception("Can't connect to negative port");
            }
            env.getLogger().log(Level.INFO,
                    "Attempting to connect to IP: " + ip + ", port: " + port+", user: "+peer.getUser().getUsername());

            // Attempt connection, then create streams
            Socket clientSocket = new Socket(ip, port);
            DataOutputStream output = new DataOutputStream(clientSocket.getOutputStream());
            DataInputStream input = new DataInputStream(clientSocket.getInputStream());
            env.getLogger().log(Level.INFO, "Connected to IP: " + ip + ", port: " + port);
            return new SecureSocket(clientSocket, output, input, env, peer.getUser(), null);
        } catch(Exception e) {
            env.getLogger().log(Level.INFO, String.format("%s: Error connecting to IP: %s, port: %d, user: %s, %s",
                    env.getUserInfo().getUsername(), peer.getServerInfo().getIp(), peer.getServerInfo().getPort(),
                    peer.getUser().getUsername(), e.getMessage()));
            return null;
        }
    }

    /**
     * Used to hold a Message instance, a MessageHandler response handler, and if applicable,
     * the ID of the message that this message is responding to.
     */
    class MessagePair {
        /**
         * The message to be sent.
         */
        Message msg;
        /**
         * The response handler for this message.
         */
        MessageHandler handler;
        /**
         * The ID of the message we are responding to, if we are responding to a message.
         */
        Integer respondingToMessage;

        /**
         * Instantiates a new Message pair.
         *
         * @param msg                 the message
         * @param handler             the respond handler
         * @param respondingToMessage the response ID
         */
        public MessagePair(Message msg, MessageHandler handler, Integer respondingToMessage) {
            this.msg = msg;
            this.handler = handler;
            this.respondingToMessage = respondingToMessage;
        }
    }

    /**
     * Custom exception for message errors.
     */
    public static class MessagingError extends Exception {
        /**
         * Instantiates a new Messaging error.
         *
         * @param error the error
         */
        public MessagingError(String error) {
            super(error);
        }
    }

    /**
     * Gets env.
     *
     * @return the env
     */
    public Environment getEnv() {
        return env;
    }

    /**
     * Blocking queue that contains all messages that have yet to be sent.
     */
    private BlockingQueue<MessagePair> messages;
    /**
     * HashMap that maps integer message IDs to MessageHandler response handlers
     */
    private ConcurrentHashMap<Integer, MessageHandler> messageHandlers;
    /**
     * HashMap that contains the default handlers for every message type.
     */
    private static ConcurrentHashMap<Integer, MessageHandler> defaultHandlers;
    /**
     * Array of all message Class types. This allows us to map the class of a message
     * to an integer code, and this is sent as header information for a message
     * to make sure we call the correct deserialization function and default
     * response handler.
     */
    private static Class[] messageCodes;
    /**
     * Locks the default handlers map.
     */
    public static Object defaultHandlersLock = new Object();

    /**
     * Size of headers, in bytes.
     */
    final int headersSize = 4*4;

    /**
     * Keeps track of how many errors this socket has experienced.
     */
    Integer socketErrorCount = 0;

    /**
     * Called when there is an error in the reader or writer thread.
     * After four errors, we close the socket.
     */
    protected void socketError(){
        synchronized (socketErrorCount) {
            socketErrorCount++;
            if(socketErrorCount>4) {
                try {
                    close(true);
                }
                catch(Exception e) {
                    env.getLogger().log(Level.SEVERE, "Couldn't close socket");
                }
            }
        }
    }

    /**
     * For debugging.
     */
    boolean showDetailedDebug = false;
    /**
     * For debugging.
     */
    boolean showSocketMessages = false;

    /**
     * For debugging.
     */
    public static AtomicInteger sockId = new AtomicInteger(0);

    /**
     * Implements the reader thread.
     */
    class SocketReader implements Runnable {

        /**
         * The reader thread entry point. Blocks until there is data
         * incoming in the socket, then reads headers, then message payload.
         * Decrypts headers and message payload, deserializes the message,
         * and calls the corresponding
         * response handler or default handler as applicable.
         */
        @Override
        public void run() {
            while(true) {
                Integer responseId = null;
                Integer myId = null;
                Integer classCode = null;
                Integer size = null;
                Integer headersSize = null;
                byte[] payload = null;
                try {
                    if(showDetailedDebug) log.log(Level.INFO, "READER: Awaiting next message");

                    String username = env.getUserInfo().getUsername();

                    if(showDetailedDebug) log.log(Level.INFO, "READER: Reading headers size");

                    // Read size of headers from socket
                    headersSize = in.readInt();

                    // Read headers from socket
                    byte[] headers = new byte[headersSize];
                    if(showDetailedDebug) log.log(Level.INFO, "READER: Reading headers");
                    in.readFully(headers, 0, headersSize);

                    // For debugging, make copy of headers
                    byte[] oldHeaders = new byte[headersSize];
                    for (int i = 0; i < headers.length; i++) {
                        oldHeaders[i] = headers[i];
                    }

                    // If encryption has been enabled, decrypt the headers
                    byte[] headersFinal = null;
                    if(useEncryption) {
                        headersFinal = cipher.decrypt(headers);
                    }else{
                        headersFinal = headers;
                    }

                    // Extract the responseId, myId, classCode, and size variables from the headers
                    ByteBuffer headersBuffer = ByteBuffer.wrap(headersFinal);
                    responseId = headersBuffer.getInt();
                    myId = headersBuffer.getInt();
                    classCode = headersBuffer.getInt();
                    size = headersBuffer.getInt();

                    // Logging
                    if(responseId<0||myId<-1||classCode<0||size<0) {
                        env.getLogger().log(Level.SEVERE, "Negative value");
                        throw new MessagingError(String.format("For '%s', negative value from '%s'", env.getUserInfo().getUsername(),
                                userInfo!=null?userInfo.getUsername():"unknown"));
                    }

                    // Read the payload
                    payload = new byte[size];
                    if(showDetailedDebug)log.log(Level.INFO, String.format("Reading payload of size: %d", size));

                    if(showDetailedDebug) log.log(Level.INFO, "READER: Reading payload of size "+size.toString());
                    in.readFully(payload, 0, size);
                    if(showDetailedDebug) log.log("READER: got payload");

                    // If encryption is enabled, decrypt payload
                    byte[] payloadFinal = null;
                    if(useEncryption) {
                        if(showDetailedDebug) log.log("READER: decrypting payload");
                        payloadFinal = cipher.decrypt(payload);
                    }else{
                        if(showDetailedDebug) log.log("READER: no decryption");
                        payloadFinal = payload;
                    }

                    if(showDetailedDebug) log.log("READER: ok");
                    if(showDetailedDebug) log.log(String.format("Classcode: %d", classCode));
                    if(showSocketMessages) log.log(String.format("READER: Received %s from '%s'", messageCodes[classCode].getName(), peerUsername));
                    if(showDetailedDebug) log.log("READER: ok ok");

                    // Deserialize message
                    Message msg;// = Message.factory(classCode, payload);
                    msg = (Message)messageCodes[classCode].getConstructor().newInstance();
                    msg.fromBytes(payloadFinal);

                    // Get handler for this message
                    MessageHandler messageHandler;
                    if(messageHandlers.containsKey(myId)) {
                         messageHandler = messageHandlers.get(myId);
                         messageHandlers.remove(myId);
                    }else{
                        messageHandler = defaultHandlers.get(classCode);
                    }
                    if(showDetailedDebug) log.log(Level.OFF, "Calling response handler");

                    // Call response handler
                    messageHandler.handle(new ReceivedMessage(msg, SecureSocket.this, responseId, env));
                    if(showDetailedDebug) log.log(Level.OFF, "Done calling response handler");
                } catch(EOFException e) {
                    close(false);
                }
                catch (Exception e) {
                    socketError();
                    if(shouldClose) return;
                }
            }
        }
    }

    /**
     * Implements the writer thread.
     */
    class SocketWriter implements Runnable {

        /**
         * The writer thread blocks until there is a message to be written in the blocking queue.
         * Then, it generates a unique ID for the message, and puts the response handler into the
         * response handler map. Next, it serializes the message, and encrypts the message.
         * Then it generates headers for the message and encrypts the headers. Next, it writes
         * the encrypted headers to the socket followed by the encrypted message payload.
         */
        @Override
        public void run() {
            while(true) {
                try {
                    // Get next message from queue, or block until there is a message in the queue
                    MessagePair nextMessage = messages.take();

                    // Generate ID and save handler
                    Integer id = assignHandler(nextMessage.handler);

                    // Get class code for this message type
                    Integer classCode = getOpCode(nextMessage.msg);

                    // Serialize message
                    byte[] payload = nextMessage.msg.toBytes();
                    if(id<0||nextMessage.respondingToMessage<-1||classCode<0) {
                        env.getLogger().log(Level.SEVERE, "Negative value");
                        throw new MessagingError("Negative value");
                    }

                    // Create headers
                    ByteBuffer headers = ByteBuffer.allocate(headersSize);
                    headers.putInt(id);
                    headers.putInt(nextMessage.respondingToMessage);
                    headers.putInt(classCode);
                    byte[] payloadBytes = null;

                    if(payload==null) payload = new byte[0];

                    // Encrypt payload
                    if (useEncryption) {
                        payloadBytes = cipher.encrypt(payload);
                    }else{
                        payloadBytes = payload;
                    }

                    // Set length in headers after encrypting payload
                    headers.putInt(payloadBytes.length);
                    byte[] headersBytes = null;

                    // Encrypt headers
                    if(useEncryption) {
                        headersBytes = cipher.encrypt(headers.array());
                    }else{
                        headersBytes = headers.array();
                    }

                    // For debugging, store copy of payload
                    int ss = 20>payloadBytes.length?payloadBytes.length:20;
                    byte[] rawPayloadBytesHead = new byte[ss];
                    for (int i = 0; i < ss; i++) {
                        rawPayloadBytesHead[i] = payloadBytes[i];
                    }
                    String rawPayloadHead = Base64.getEncoder().encodeToString(rawPayloadBytesHead);
                    if(showSocketMessages)
                        log.log(String.format("Sending %s to '%s'", messageCodes[classCode].getName(), peerUsername));

                    // Write length of headers to socket, followed by headers, followed by message payload
                    out.writeInt(headersBytes.length);
                    out.write(headersBytes);
                    out.write(payloadBytes);
                    out.flush();

                    if(nextMessage.msg.doesActivateEncryption()) {
                        activateEncryption();
                    }
                    if(showSocketMessages)
                        log.log(String.format("SENT %s to '%s'", messageCodes[classCode].getName(), peerUsername));

                }catch(Exception e) {
                    log.log(Level.SEVERE, String.format("Exception during socket writer with '%s'", peerUsername), e);
                    socketError();
                    if(shouldClose) return;
                }
            }
        }
    }

    /**
     * Gets the OP code for a message subclass. For every subclass of Message
     * there is one and only one integer op code.
     *
     * @param m the message
     * @return the op code
     * @throws Exception the exception
     */
    public static int getOpCode(Message m) throws Exception{
        Class<? extends Message> clazz = m.getClass();
        for (int i = 0; i < messageCodes.length; i++) {
            if(messageCodes[i].equals(clazz)) return i;
        }

        throw new Exception("Unregistered message class");
    }

    /**
     * This method does two important things. First, it finds all subclasses
     * of Message and stores them in an array, which is later used to generate
     * the Message OP codes. Then, it finds all classes annotated with the
     * DefaultHandler annotation, and puts those into the defaultHandlers map,
     * allowing DefaultHandlers to be registered simply with the annotation.
     *
     * @throws Exception the exception
     */
    public static void initDefaultHandlers() throws Exception {
        synchronized (defaultHandlersLock) {
            if (defaultHandlers == null && messageCodes == null) {
                defaultHandlers = new ConcurrentHashMap<>();
                Reflections reflections = new Reflections("org.licketysplit");

                // Get all subclasses of Message
                Set<Class<? extends Message>> messageTypes = reflections.getSubTypesOf(Message.class);
                messageCodes =  messageTypes.toArray(new Class[messageTypes.size()]);

                // Sort alphabetically
                Arrays.sort(messageCodes, (Class a, Class b) -> a.getName().compareTo(b.getName()));

                // Get all classes annotated with DefaultHandler and put into defaultHandlers map
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

    /**
     * No longer used
     *
     * @param id      the id
     * @param handler the handler
     * @throws Exception the exception
     */
    public void setDefaultHandler(Integer id, MessageHandler handler) throws Exception{
        if(id>=0) {
            throw new Exception("Default handler id should be less than 0");
        }else{
            if(messageHandlers.putIfAbsent(id, handler)!=null) {
                throw new Exception("Default handler ID collision");
            }
        }
    }

    /**
     * Generates a unique integer ID for a message being sent, and
     * stores the response handler in the handlers map with said ID
     * as a key.
     *
     * @param handler the handler
     * @return the integer
     */
    Integer assignHandler(MessageHandler handler) {
        Integer id = messageHandlers.size();
        if(handler==null) return id;

        while(messageHandlers.putIfAbsent(id, handler)!=null) {
            id++;
            if(id<0) id = 0;
        }

        return id;
    }

    /**
     * The SocketReader instance.
     */
    private SocketReader messageReader;
    /**
     * The SocketWriter instance.
     */
    private SocketWriter messageWriter;
    /**
     * The reader thread.
     */
    private Thread messageReaderThread;
    /**
     * The writer thread.
     */
    private Thread messageWriterThread;

    /**
     * Called from the constructor, initializes
     * necessary data structures as well as calling
     * the static method initDefaultHandlers
     * which will only be called once across all
     * SecureSocket instances. Also starts the reader and
     * writer threads.
     *
     * @throws Exception the exception
     */
    void initMessagingService() throws Exception{
        // Init data structures
        messages = new LinkedBlockingQueue<>();
        messageHandlers = new ConcurrentHashMap<>();

        // Init default handlers and message op codes
        initDefaultHandlers();

        // Initialize and start reader and writer threads
        messageReader = new SocketReader();
        messageWriter = new SocketWriter();

        messageReaderThread = new Thread(messageReader);
        messageWriterThread = new Thread(messageWriter);

        messageReaderThread.start();
        messageWriterThread.start();
    }

    /**
     * Sends a message and returns the response synchronously.
     * After timeoutTime milliseconds this will throw an exception
     * using the callback function timeout.
     *
     * @param msg          the message to send
     * @param respondingTo the message we are responding to
     * @param timeout      the timeout exception callback
     * @param timeoutTime  the timeout time
     * @return the received message
     * @throws Exception the exception
     */
    public ReceivedMessage sendMessageAndWait(Message msg, Integer respondingTo, TimeoutException timeout, int timeoutTime) throws Exception {
        final ReceivedMessage[] response = new ReceivedMessage[1];
        response[0] = null;
        Object lock = new Object();
        sendMessage(msg,
                (ReceivedMessage m) -> {
                    response[0] = m;
                    // When the message has been received notify the lock
                    synchronized(lock) {
                        lock.notifyAll();
                    }
                }
                , respondingTo);
        synchronized(lock) {
            if(timeoutTime>0) {
                // If there is a timeout, wait on the lock until timeoutTime milliseconds have elapsed
                // if the message still hasn't been received, throw an exception
                lock.wait(timeoutTime);
                if(response[0]==null) {
                    timeout.timeout();
                    return null;
                }
            }else {
                // Otherwise, wait until received, which could be forever
                lock.wait();
            }
        }
        return response[0];
    }

    /**
     * Callback function when a message times out, should throw an exception.
     */
    public interface TimeoutException {
        /**
         * Called when the message times out.
         *
         * @throws Exception the exception
         */
        public void timeout() throws Exception;
    }

    /**
     * Factory method to create TimeoutException objects.
     *
     * @param msg the exception message
     * @return the timeout exception
     */
    public static TimeoutException timeoutFactory(String msg) {
        return () -> {
            throw new Exception(msg);
        };
    }

    /**
     * Wrapper function to sendMessageAndWait for a message that
     * is not a response.
     *
     * @param msg         the message
     * @param timeout     the timeout callback
     * @param timeoutTime the timeout time
     * @return the received message
     * @throws Exception the exception
     */
    public ReceivedMessage sendMessageAndWait(Message msg, TimeoutException timeout, int timeoutTime) throws Exception {
        Integer respondingTo = -1;
        return sendMessageAndWait(msg, respondingTo, timeout, timeoutTime);
    }

    /**
     * Sends a message by putting it into the blocking queue, the message will then be picked up
     * by the writer thread and written to the socket.
     *
     * @param msg          the message
     * @param handler      the response handler
     * @param respondingTo the ID of the message we are responding to
     * @throws Exception the exception
     */
    public void sendMessage(Message msg, MessageHandler handler, Integer respondingTo) throws Exception {
        MessagePair messagePair = new MessagePair(msg, handler, respondingTo);
        messages.put(messagePair);
    }

    /**
     * Wrapper function to send a message that is not a response
     *
     * @param msg     the message
     * @param handler the response handler
     * @throws Exception the exception
     */
    public void sendFirstMessage(Message msg, MessageHandler handler) throws Exception {
        sendMessage(msg, handler, -1);
    }
}
