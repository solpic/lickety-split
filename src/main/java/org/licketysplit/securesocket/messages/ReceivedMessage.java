package org.licketysplit.securesocket.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;

/**
 * Helper class to make sending and receiving messages easier and less verbose.
 */
public class ReceivedMessage {
    /**
     * The actual message that has been received. This can be cast to the correct type later on.
     */
    Message msg;
    /**
     * The SecureSocket the message was sent and received over.
     */
    SecureSocket conn;
    /**
     * The ID that should be responded to if you want to respond to this message.
     */
    int respondId;
    /**
     * The Environment that this message was received in. This is important because
     * many message handlers are called in a static or anonymous context and won't
     * otherwise know information about their environment.
     */
    Environment env;

    /**
     * Logging convenience function.
     *
     * @param msg the msg
     * @throws Exception the exception
     */
    public void log(String msg) throws Exception {
        env.getLogger().log(msg);
    }

    /**
     * Respond to this message.
     *
     * @param m       the message to send back
     * @param handler the response handler if there is a response
     * @throws Exception the exception
     */
    public void respond(Message m, MessageHandler handler) throws Exception {
        conn.sendMessage(m, handler, respondId);
    }

    /**
     * Respond to a message, block until a response is received, and return the response.
     * This allows us to send and receive messages without using anonymous functions.
     * If we block longer than the timout an exception is thrown.
     *
     * @param m           the response
     * @param timeout     callback to throw an exception
     * @param timeoutTime how long to wait
     * @return the response to our message
     * @throws Exception the exception
     */
    public ReceivedMessage respondAndWait(Message m, SecureSocket.TimeoutException timeout, int timeoutTime) throws Exception {
        return conn.sendMessageAndWait(m, respondId,  timeout, timeoutTime);
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
     * Instantiates a new Received message.
     *
     * @param msg       the msg
     * @param conn      the conn
     * @param respondId the respond id
     * @param env       the env
     */
    public ReceivedMessage(Message msg, SecureSocket conn, int respondId, Environment env) {
        this.msg = msg;
        this.conn = conn;
        this.respondId = respondId;
        this.env = env;
    }

    /**
     * Gets SecureSocket
     *
     * @return the socket
     */
    public SecureSocket getConn() {
        return conn;
    }

    /**
     * Gets message and casts to specified type.
     * This helper function makes our code slightly less verbose.
     * And we don't need to check our cast because
     * we always know what the type of response messages will be.
     *
     * @param <M> the message type to cast it to
     * @return the casted message
     */
    public <M> M getMessage() {
        return (M)msg;
    }
}
