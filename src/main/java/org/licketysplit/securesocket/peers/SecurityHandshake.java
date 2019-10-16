package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

public class SecurityHandshake {
    public static class SendPublicKeyMessage extends JSONMessage {
        byte[] key;

        public SendPublicKeyMessage() {
        }

        public SendPublicKeyMessage(byte[] key) {
            this.key = key;
        }

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }
    }

    public static class ReceivedSymmetricKeyMessage extends Message {

        @Override
        public byte[] toBytes() throws Exception {
            return new byte[0];
        }

        @Override
        public void fromBytes(byte[] data) throws Exception {

        }
    }
    public static class SendSymmetricKeyMessage extends JSONMessage {
        byte[] encryptedKey;
        byte[] encryptedIv;

        public byte[] getEncryptedKey() {
            return encryptedKey;
        }

        public void setEncryptedKey(byte[] encryptedKey) {
            this.encryptedKey = encryptedKey;
        }

        public SendSymmetricKeyMessage() {
        }

        public byte[] getEncryptedIv() {
            return encryptedIv;
        }

        public void setEncryptedIv(byte[] encryptedIv) {
            this.encryptedIv = encryptedIv;
        }

        public SendSymmetricKeyMessage(byte[] encryptedKey, byte[] iv) {
            this.encryptedKey = encryptedKey;
            this.encryptedIv = iv;
        }
    }
}
