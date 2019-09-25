package org.licketysplit.securesocket.messages;

import org.licketysplit.securesocket.SecureSocket;

public class ReceivedMessage {
    Message msg;
    SecureSocket conn;
    int respondId;

    public void respond(Message m, MessageHandler handler) throws Exception {
        conn.sendMessage(m, handler, respondId);
    }

    public ReceivedMessage(Message msg, SecureSocket conn, int respondId) {
        this.msg = msg;
        this.conn = conn;
        this.respondId = respondId;
    }

    public Message getMessage() {
        return msg;
    }
}
