package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.util.Random;

public class SecurityHandshake {
    public static class SendPublicKeyMessage extends JSONMessage {
        byte[] key;
        String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public SendPublicKeyMessage() {
        }

        public SendPublicKeyMessage(byte[] key, String username) {
            this.key = key;
            this.username = username;
        }

        public byte[] getKey() {
            return key;
        }

        public void setKey(byte[] key) {
            this.key = key;
        }
    }

    public static class SendSymmetricKeyMessage extends JSONMessage {
        byte[] encryptedKey;
        byte[] encryptedIv;
        String username;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

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

        public SendSymmetricKeyMessage(byte[] encryptedKey, byte[] iv, String username) {
            this.encryptedKey = encryptedKey;
            this.encryptedIv = iv;
            this.username = username;
        }
    }
}
