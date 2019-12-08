package org.licketysplit.securesocket.encryption;

import javax.crypto.Cipher;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

/**
 * AsymmetricCipher class does asymmetric encryption using the RSA algorithm
 * with a 128 byte key size. This helper class does a couple of things,
 * first it makes encrypting and decrypting as simple as passing in a byte array
 * and returning a byte array. Second, it handles generating new random keypairs.
 * Third, it handles serializing and deserializing keypairs so that they are
 * easier to send over a TCP socket or save to a file.
 * <p>
 * All RSA encryption is done through this class.
 */
public class AsymmetricCipher {
    /**
     * The cipher instance that will handle encryption or decryption (not both)
     */
    private Cipher cipher;

    /**
     * Instantiates a new Asymmetric cipher.
     */
    public AsymmetricCipher() {
        cipher = null;
    }

    /**
     * Sets public key from a byte array representing the key. It then
     * initializes the cipher object with the public key so this
     * AsymmetriCipher instance can now be used for encryption
     *
     * @param keyBytes the public key
     * @throws Exception the exception
     */
    public void setPublicKey(byte[] keyBytes) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey key = keyFactory.generatePublic(spec);
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.ENCRYPT_MODE, key);
    }

    /**
     * Converts a byte array representing a public key
     * to a PublicKey type which is used by the Cipher
     * object.
     *
     * @param key byte array representing public key
     * @return PublicKey object initialized from byte array
     * @throws Exception the exception
     */
    public static PublicKey bytesToPublicKey(byte[] key) throws Exception {
        X509EncodedKeySpec spec = new X509EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    /**
     * Converts a byte array representing a private key
     * to a PrivateKey type which is used by the Cipher
     * object.
     *
     * @param key byte array representing private key
     * @return PrivateKey object initialized from byte array
     * @throws Exception the exception
     */
    public static PrivateKey bytesToPrivateKey(byte[] key) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(key);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(spec);
    }

    /**
     * Sets private key from a byte array representing the key. It then
     * initializes the cipher object with the private key so this
     * AsymmetriCipher instance can now be used for decryption
     *
     * @param keyBytes the private key
     * @throws Exception the exception
     */
    public void setPrivateKey(byte[] keyBytes) throws Exception {
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PrivateKey key = keyFactory.generatePrivate(spec);
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
    }

    /**
     * Sets private key directly from a PrivateKey object,
     * initializing the cipher for decryption.
     *
     * @param key private key
     * @throws Exception the exception
     */
    public void setPrivateKey(PrivateKey key) throws Exception {
        cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
        cipher.init(Cipher.DECRYPT_MODE, key);
    }

    /**
     * Encrypts a byte array using the cipher initialized
     * with a public key.
     *
     * @param plaintext the plaintext to be encrypted
     * @return the encrypted ciphertext as a byte array
     * @throws Exception the exception
     */
    public byte[] encrypt(byte[] plaintext) throws Exception {
        return cipher.doFinal(plaintext);
    }

    /**
     * Decrypts a byte array using the cipher initialized
     * with a private key.
     *
     * @param ciphertext the ciphertext to be decrypted
     * @return the decrypted plaintext as a byte array
     * @throws Exception the exception
     */
    public byte[] decrypt(byte[] ciphertext) throws Exception {
        return cipher.doFinal(ciphertext);
    }

    /**
     * Maximum size of data that can be encrypted. For RSA encryption
     * the maximum size is equivalent to your keysize, to be safe
     * we return half of that.
     *
     * @return the maximum allowed bytes that can be encrypted at once
     */
    public static int idBlockSize() {
        return 64;
    }

    /**
     * Generates a random RSA keypair with keysize 128 bytes.
     *
     * @return the randomly generated keypair
     * @throws Exception the exception
     */
    public KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator rsa = KeyPairGenerator.getInstance("RSA");
        rsa.initialize(1024);
        KeyPair pair = rsa.generateKeyPair();
        return pair;
    }
}
