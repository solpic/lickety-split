package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.encryption.AsymmetricCipher;

import java.security.MessageDigest;
import java.util.Random;

public class SignedPayload {
    byte[] payload;
    byte[] salt;
    byte[] signature;

    public byte[] getPayload() {
        return payload;
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    public byte[] getSalt() {
        return salt;
    }

    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    public byte[] getSignature() {
        return signature;
    }

    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    public SignedPayload() {
    }

    public static class Verifier {
        AsymmetricCipher cipher;
        public Verifier(byte[] privateKey) throws Exception {
            cipher = new AsymmetricCipher();
            cipher.setPrivateKey(privateKey);
        }

        public void verify(SignedPayload signedPayload) throws Exception {
            byte[] payload = signedPayload.getPayload();
            byte[] salt = signedPayload.getSalt();
            byte[] totalPayload = new byte[payload.length+salt.length];
            for (int i = 0; i < payload.length; i++) {
                totalPayload[i] = payload[i];
            }
            for (int i = 0; i < salt.length; i++) {
                totalPayload[i+payload.length] = salt[i];
            }

            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(totalPayload);
            byte[] calculatedDigest = md.digest();
            byte[] signedDigest = cipher.decrypt(signedPayload.getSignature());
            for (int i = 0; i < calculatedDigest.length || i<signedDigest.length; i++) {
                if(calculatedDigest[i]!=signedDigest[i]) throw new Exception();
            }
        }
    }

    public static class Signer {
        AsymmetricCipher cipher;
        public Signer(byte[] publicKey) throws Exception {
            cipher = new AsymmetricCipher();
            cipher.setPublicKey(publicKey);
        }

        public SignedPayload sign(byte[] payload) throws Exception {
            SignedPayload buffer = new SignedPayload();
            buffer.setPayload(payload);

            Random random = new Random(System.currentTimeMillis());
            byte[] salt = new byte[50+random.nextInt(50)];
            random.nextBytes(salt);
            buffer.setSalt(salt);

            byte[] totalPayload = new byte[payload.length+salt.length];
            for (int i = 0; i < payload.length; i++) {
                totalPayload[i] = payload[i];
            }
            for (int i = 0; i < salt.length; i++) {
                totalPayload[i+payload.length] = salt[i];
            }
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(totalPayload);
            buffer.setSignature(cipher.encrypt(md.digest()));

            return buffer;
        }
    }
}
