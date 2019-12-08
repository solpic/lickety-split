package org.licketysplit.securesocket.messages;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A Message subclass that makes serialization and deserialization much easier.
 * To serialize/deserialize subclasses of JSONMessage, simply make the fields
 * you want serialized public, or provide getters and setters for said fields.
 * Then, said fields will be automatically serialized, with no real performance
 * penalty.
 */
public class JSONMessage extends Message{
    /**
     * Serialization function that uses the Jackson JSON library.
     * @return byte array of serialized message
     * @throws Exception
     */
    @Override
    public byte[] toBytes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this).getBytes();
    }

    /**
     * Deserialization function that uses the Jackson JSON library.
     * @param data byte array of serialized message
     * @throws Exception
     */
    @Override
    public void fromBytes(byte[] data) throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this).readValue(new String(data));
    }
}
