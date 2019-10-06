package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.Message;


public class FileRequestResponseMessage extends Message {
    public String data;

    @Override
    public byte[] toBytes() {
        return data.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.data = new String(data);
    }

    public FileRequestResponseMessage() {}

    public FileRequestResponseMessage(String fileName){
        this.data = fileName;
    }


}
