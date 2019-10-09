package org.licketysplit.filesharer.messages;

import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.Message;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class FileRequestMessage extends Message {
    public String fileName;

    @Override
    public byte[] toBytes() {
        return fileName.getBytes();
    }

    @Override
    public void fromBytes(byte[] data) {
        this.fileName = new String(data);
    }


    public FileRequestMessage() {}

    public FileRequestMessage(String fileName){
        this.fileName = fileName;
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
            String requestedFileName = tstMsg.fileName;
            try {
                m.respond(new FileRequestResponseMessage(requestedFileName), new FileRequestResponseHandler());
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
