package org.licketysplit.securesocket.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

/**
 * This class handles all symmetric encryption for the application,
 * done using the AES algorithm. This class handles encryption/decryption,
 * generation of 128 byte keys, and serialization/deserialization of said keys.
 */
public class SymmetricCipher {
    /**
     * Cipher object that handles encryption.
     */
    Cipher encryptor;
    /**
     * Cipher object that handles decryption
     */
    Cipher decryptor;

    /**
     * Instantiates a new Symmetric cipher.
     */
    public SymmetricCipher() {
        encryptor = null;
        decryptor = null;
    }


    /**
     * Encrypts a plaintext byte array into an encrypted byte array.
     *
     * @param plaintext the plaintext as a byte array
     * @return ciphertext as a byte array
     * @throws Exception the exception
     */
    public byte[] encrypt(byte[] plaintext) throws Exception {
        return encryptor.doFinal(plaintext);
    }

    /**
     * Decrypts the byte array ciphertext and returns a byte array
     * of the plaintext
     *
     * @param ciphertext the ciphertext as a byte array
     * @return the plaintext as a byte array
     * @throws Exception the exception
     */
    public byte[] decrypt(byte[] ciphertext) throws Exception {
        return decryptor.doFinal(ciphertext);
    }

    /**
     * Class representing a SymmetricKey, used to help
     * with serialization/deserialization. A symmetric key has
     * two parts, the actual key, and the IV.
     */
    public static class SymmetricKey{
        /**
         * The Key.
         */
        SecretKey key;
        /**
         * The Iv.
         */
        byte[] iv;

        /**
         * Key getter
         *
         * @return the key
         */
        public SecretKey getKey() {
            return key;
        }

        /**
         * Sets key.
         *
         * @param key the key
         */
        public void setKey(SecretKey key) {
            this.key = key;
        }

        /**
         * Get iv byte [ ].
         *
         * @return the byte [ ]
         */
        public byte[] getIv() {
            return iv;
        }

        /**
         * Sets iv.
         *
         * @param iv the iv
         */
        public void setIv(byte[] iv) {
            this.iv = iv;
        }

        /**
         * Instantiates a new Symmetric key.
         *
         * @param key the key
         * @param iv  the iv
         */
        public SymmetricKey(SecretKey key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }
    }

    /**
     * The Key bytes stored.
     * This was used at one point for debugging but is no longer used.
     */
    public byte[] keyBytesStored;
    /**
     * The Iv bytes stored.
     * This was used at one point for debugging but is no longer used
     */
    public byte[] ivBytesStored;

    /**
     * Sets key from byte arrays representing the key and the IV spec.
     * Initializes the encryptor and decryptor objects using these
     * values.
     *
     * @param keyBytes the key bytes
     * @param iv       the iv spec
     * @throws Exception the exception
     */
    public void setKey(byte[] keyBytes, byte[] iv) throws Exception {
        SecretKeySpec key = new SecretKeySpec(keyBytes, "AES");
        IvParameterSpec ivspec = new IvParameterSpec(iv);

        encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptor.init(Cipher.ENCRYPT_MODE, key, ivspec);
        decryptor.init(Cipher.DECRYPT_MODE, key, ivspec);

        keyBytesStored = keyBytes;
        ivBytesStored = iv;
    }

    /**
     * Generates a random 128 byte symmetric key and returns it.
     * Also initializes the encryptor and decryptor objects with said key.
     *
     * @return the new symmetric key
     * @throws Exception the exception
     */
    public SymmetricKey generateKey() throws Exception {
        SecureRandom sr = new SecureRandom();
        byte[] iv = new byte[128/8];
        sr.nextBytes(iv);
        IvParameterSpec ivspec = new IvParameterSpec(iv);
        KeyGenerator keyGen = KeyGenerator.getInstance("AES");
        keyGen.init(128); // for example
        SecretKey key = keyGen.generateKey();

        encryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        decryptor = Cipher.getInstance("AES/CBC/PKCS5Padding");
        encryptor.init(Cipher.ENCRYPT_MODE, key, ivspec);
        decryptor.init(Cipher.DECRYPT_MODE, key, ivspec);

        keyBytesStored = key.getEncoded();
        ivBytesStored = iv;
        return new SymmetricKey(key, iv);
    }
}
