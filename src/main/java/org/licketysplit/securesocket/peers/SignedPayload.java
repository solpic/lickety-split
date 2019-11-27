package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.encryption.AsymmetricCipher;

import java.security.MessageDigest;
import java.security.Signature;
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
        byte[] key;
        public Verifier(byte[] key) throws Exception {
            this.key = new byte[key.length];
            for (int i = 0; i < key.length; i++) {
                this.key[i] = key[i];
            }
        }

        public void verify(SignedPayload signedPayload) throws Exception {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(AsymmetricCipher.bytesToPublicKey(key));
            byte[] payload = signedPayload.getPayload();
            byte[] salt = signedPayload.getSalt();
            byte[] totalPayload = new byte[payload.length+salt.length];
            for (int i = 0; i < payload.length; i++) {
                totalPayload[i] = payload[i];
            }
            for (int i = 0; i < salt.length; i++) {
                totalPayload[i+payload.length] = salt[i];
            }
            sign.update(totalPayload);
            if(!sign.verify(signedPayload.getSignature())) {
                throw new Exception("Couldn't verify signed payload");
            }
//
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(totalPayload);
//            byte[] calculatedDigest = md.digest();
//            byte[] signedDigest = cipher.decrypt(signedPayload.getSignature());
//            for (int i = 0; i < calculatedDigest.length || i<signedDigest.length; i++) {
//                if(calculatedDigest[i]!=signedDigest[i]) throw new Exception();
//            }
        }
    }

    public static class Signer {
        byte[] key;
        public Signer(byte[] key) throws Exception {
            this.key = new byte[key.length];
            for (int i = 0; i < key.length; i++) {
                this.key[i] = key[i];
            }
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

            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(AsymmetricCipher.bytesToPrivateKey(key));
            sign.update(totalPayload);
            byte[] signature = sign.sign();
            buffer.setSignature(signature);
            return buffer;
//            MessageDigest md = MessageDigest.getInstance("MD5");
//            md.update(totalPayload);
//            buffer.setSignature(cipher.encrypt(md.digest()));
//
//            return buffer;
        }
    }
}
