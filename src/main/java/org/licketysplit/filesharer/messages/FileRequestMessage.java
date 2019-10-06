package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class FileRequestMessage extends Message {
    public String data;

    @Override
    public byte[] toBytes() {
        return data.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.data = new String(data);
    }


    public FileRequestMessage() {}

    public FileRequestMessage(String fileName){
        this.data = fileName;
    }

    public static class FileRequestResponseHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            FileRequestResponseMessage decodedMessage = (FileRequestResponseMessage) m.getMessage();

            try {
                System.out.println(decodedMessage.data);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @DefaultHandler(type = FileRequestMessage.class)
    public static class FileRequestHandler implements MessageHandler {
        @Override
        public void handle(ReceivedMessage m) {
            FileRequestMessage tstMsg = (FileRequestMessage) m.getMessage();
            System.out.println("s2" + tstMsg.data);

            try {
                m.respond(new FileRequestResponseMessage("jojo"), new FileRequestResponseHandler());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
