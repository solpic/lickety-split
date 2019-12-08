package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.util.Random;

/**
 * This class contains some helper classes
 * and methods for use during handshaking.
 */
public class SecurityHandshake {
    /**
     * This message sends an RSA public key as well as the sender's username.
     */
    public static class SendPublicKeyMessage extends JSONMessage {
        /**
         * The RSA public key
         */
        byte[] key;
        /**
         * The username of the peer sending the message.
         */
        String username;

        /**
         * Gets username.
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets username.
         *
         * @param username the username
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Instantiates a new Send public key message.
         */
        public SendPublicKeyMessage() {
        }

        /**
         * Instantiates a new Send public key message.
         *
         * @param key      the public key
         * @param username the sender
         */
        public SendPublicKeyMessage(byte[] key, String username) {
            this.key = key;
            this.username = username;
        }

        /**
         * Gets public key.
         *
         * @return the byte [ ]
         */
        public byte[] getKey() {
            return key;
        }

        /**
         * Sets public key.
         *
         * @param key the key
         */
        public void setKey(byte[] key) {
            this.key = key;
        }
    }

    /**
     * This message sends an AES key (which consists of a key byte array and
     * an IV spec byte array) that has been encrypted using an RSA public key.
     */
    public static class SendSymmetricKeyMessage extends JSONMessage {
        /**
         * The Encrypted key.
         */
        byte[] encryptedKey;
        /**
         * The Encrypted iv spec.
         */
        byte[] encryptedIv;
        /**
         * The senders username.
         */
        String username;

        /**
         * Gets username.
         *
         * @return the username
         */
        public String getUsername() {
            return username;
        }

        /**
         * Sets username.
         *
         * @param username the username
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Get encrypted key byte [ ].
         *
         * @return the byte [ ]
         */
        public byte[] getEncryptedKey() {
            return encryptedKey;
        }

        /**
         * Sets encrypted key.
         *
         * @param encryptedKey the encrypted key
         */
        public void setEncryptedKey(byte[] encryptedKey) {
            this.encryptedKey = encryptedKey;
        }

        /**
         * Instantiates a new Send symmetric key message.
         */
        public SendSymmetricKeyMessage() {
        }

        /**
         * Get encrypted iv spec byte [ ].
         *
         * @return the byte [ ]
         */
        public byte[] getEncryptedIv() {
            return encryptedIv;
        }

        /**
         * Sets encrypted iv spec.
         *
         * @param encryptedIv the encrypted iv
         */
        public void setEncryptedIv(byte[] encryptedIv) {
            this.encryptedIv = encryptedIv;
        }

        /**
         * Instantiates a new Send symmetric key message.
         *
         * @param encryptedKey the encrypted key
         * @param iv           the iv
         * @param username     the username
         */
        public SendSymmetricKeyMessage(byte[] encryptedKey, byte[] iv, String username) {
            this.encryptedKey = encryptedKey;
            this.encryptedIv = iv;
            this.username = username;
        }
    }
}
