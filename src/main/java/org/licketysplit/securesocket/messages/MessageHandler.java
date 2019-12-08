package org.licketysplit.securesocket.messages;

import org.licketysplit.securesocket.SecureSocket;

/**
 * The interface Message handler.
 * This interface is implemented when specifying response handlers
 * for messages. So when you send a message, you also specify a response handler.
 * If/when a response is received, that handler is called.
 */
public interface MessageHandler {
    /**
     * This method is called when a response is received for your message.
     *
     * @param m the received message
     * @throws Exception the exception
     */
    void handle(ReceivedMessage m) throws Exception;
}