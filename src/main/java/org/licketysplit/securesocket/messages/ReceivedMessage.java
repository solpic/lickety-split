package org.licketysplit.securesocket.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;

public class ReceivedMessage {
    Message msg;
    SecureSocket conn;
    int respondId;
    Environment env;

    public void log(String msg) throws Exception {
        env.getLogger().log(msg);
    }

    public void respond(Message m, MessageHandler handler) throws Exception {
        conn.sendMessage(m, handler, respondId);
    }

    public ReceivedMessage respondAndWait(Message m, SecureSocket.TimeoutException timeout, int timeoutTime) throws Exception {
        return conn.sendMessageAndWait(m, respondId,  timeout, timeoutTime);
    }

    public Environment getEnv() {
        return env;
    }

    public ReceivedMessage(Message msg, SecureSocket conn, int respondId, Environment env) {
        this.msg = msg;
        this.conn = conn;
        this.respondId = respondId;
        this.env = env;
    }

    public SecureSocket getConn() {
        return conn;
    }

    public <M> M getMessage() {
        return (M)msg;
    }
}
