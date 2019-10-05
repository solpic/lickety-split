package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class FileRequestMessage extends Message {
    public String data;

    @Override
    public byte[] toBytes() {
        return data.toString().getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {}

    public FileRequestMessage() {}

    public FileRequestMessage(String fileName){
        this.data = fileName;
    }


    @DefaultHandler(type = FileRequestMessage.class)
    public static class FileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            FileRequestMessage tstMsg = (FileRequestMessage) m.getMessage();
            System.out.println("Received: "+tstMsg.data);
            try {
                m.respond(new FileRequestMessage(), new TestHandler());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    public static class TestMessage extends Message {
        @Override
        public byte[] toBytes() {
            return data.toString().getBytes();
        }

        @Override
        public void fromBytes(byte[] data) {
            this.data = Integer.parseInt(new String(data));
        }
        public TestMessage() {}

        public Integer data;
        public TestMessage(int data) {
            this.data = data;
        }
    }

}
