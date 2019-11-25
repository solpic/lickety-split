package org.licketysplit.securesocket.encryption;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;

public class SymmetricCipher {
    Cipher encryptor;
    Cipher decryptor;
    public SymmetricCipher() {
        encryptor = null;
        decryptor = null;
    }


    public byte[] encrypt(byte[] plaintext) throws Exception {
        return encryptor.doFinal(plaintext);
    }

    public byte[] decrypt(byte[] ciphertext) throws Exception {
        return decryptor.doFinal(ciphertext);
    }
    public static class SymmetricKey{
        SecretKey key;
        byte[] iv;

        public SecretKey getKey() {
            return key;
        }

        public void setKey(SecretKey key) {
            this.key = key;
        }

        public byte[] getIv() {
            return iv;
        }

        public void setIv(byte[] iv) {
            this.iv = iv;
        }

        public SymmetricKey(SecretKey key, byte[] iv) {
            this.key = key;
            this.iv = iv;
        }
    }

    public byte[] keyBytesStored;
    public byte[] ivBytesStored;
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
