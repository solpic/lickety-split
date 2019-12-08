package org.licketysplit.securesocket.messages;

/**
 * This is a core part of the messaging system. All
 * network communication is done using messages, and
 * all messages must be a subclass (not necessarily
 * an immediate subclass) of the Message class. The two
 * main methods you must override when you subclass Message
 * are toBytes and fromBytes. toBytes serializes the message
 * and fromBytes deserializes it. So when a message is written
 * to the TCP socket, toBytes serializes it, and when the message is
 * read on the other end, it is deserialized with the fromBytes method.
 * Additionally, at startup, the reflections API is used to get all subclasses
 * of Message, and unique integer codes are assigned to each message class,
 * so that when sending a message the type can be specified as an integer
 * and when receiving a message the type is known by said integer.
 * Message subclasses must also have a default empty constructor.
 */
public abstract class Message {
    /**
     * Serialize message to byte array.
     *
     * @return the serialized message
     * @throws Exception the exception
     */
    public abstract byte[] toBytes() throws Exception;

    /**
     * Deserialize message from a byte array.
     *
     * @param data serialized message as a byte array
     * @throws Exception the exception
     */
    public abstract void fromBytes(byte[] data) throws Exception;

    /**
     * Specifies that after this message is sent, all future messages
     * will be encrypted.
     */
    public void activateEncryption() {
        shouldActivateEncryption = true;
    }

    /**
     * If this is true, after sending this message the SecureSocket will
     * encrypt all future messages.
     *
     * @return the boolean
     */
    public boolean doesActivateEncryption() {
        return shouldActivateEncryption;
    }

    /**
     * Stores whether encryption will be activated.
     */
    private boolean shouldActivateEncryption = false;
}
