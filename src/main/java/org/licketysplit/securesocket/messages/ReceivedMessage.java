package org.licketysplit.securesocket.messages;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;

public class ReceivedMessage {
    Message msg;
    SecureSocket conn;
    int respondId;
    Environment env;

    public void respond(Message m, MessageHandler handler) throws Exception {
        conn.sendMessage(m, handler, respondId);
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
