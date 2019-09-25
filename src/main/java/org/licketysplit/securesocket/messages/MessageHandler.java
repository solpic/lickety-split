package org.licketysplit.securesocket.messages;

import org.licketysplit.securesocket.SecureSocket;

public interface MessageHandler {
    void handle(ReceivedMessage m);
}