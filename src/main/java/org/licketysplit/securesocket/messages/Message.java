package org.licketysplit.securesocket.messages;

// Messages must have a default empty constructor
public abstract class Message {
    // Parsing to and from
    public abstract byte[] toBytes();
    public abstract void fromBytes(byte[] data);
}
