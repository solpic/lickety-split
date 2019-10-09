package org.licketysplit.securesocket.messages;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONMessage extends Message{
    @Override
    public byte[] toBytes() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(this).getBytes();
    }

    @Override
    public void fromBytes(byte[] data) throws Exception{
        ObjectMapper mapper = new ObjectMapper();
        mapper.readerForUpdating(this).readValue(new String(data));
    }
}
