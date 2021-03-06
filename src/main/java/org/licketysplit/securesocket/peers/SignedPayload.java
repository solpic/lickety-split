package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.encryption.AsymmetricCipher;

import java.security.MessageDigest;
import java.security.Signature;
import java.util.Random;

/**
 * This class allows payloads to be cryptographically signed.
 * This means that given an RSA keypair, everyone can have the public key,
 * and only the signer has the private key. Given a byte array payload,
 * the signer generates a signature as a byte array. For verifiers,
 * given the payload, signature, and RSA public key, they can verify that
 * the signature was generated by the person with the private key.
 * This allows us to verify some message came from a particular user,
 * in this case, we want to confirm that new users were added only by root
 * and that banned users were banned by the root.
 */
public class SignedPayload {
    /**
     * The message itself.
     */
    byte[] payload;
    /**
     * A random salt added to the payload which helps with security.
     */
    byte[] salt;
    /**
     * The signature, used to verify the payload/salt combination.
     */
    byte[] signature;

    /**
     * Get payload byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Sets payload.
     *
     * @param payload the payload
     */
    public void setPayload(byte[] payload) {
        this.payload = payload;
    }

    /**
     * Get salt byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getSalt() {
        return salt;
    }

    /**
     * Sets salt.
     *
     * @param salt the salt
     */
    public void setSalt(byte[] salt) {
        this.salt = salt;
    }

    /**
     * Get signature byte [ ].
     *
     * @return the byte [ ]
     */
    public byte[] getSignature() {
        return signature;
    }

    /**
     * Sets signature.
     *
     * @param signature the signature
     */
    public void setSignature(byte[] signature) {
        this.signature = signature;
    }

    /**
     * Instantiates a new Signed payload.
     */
    public SignedPayload() {
    }

    /**
     * This helper class actually performs the verification on a SignedPayload object using the public key.
     */
    public static class Verifier {
        /**
         * The public key.
         */
        byte[] key;

        /**
         * Instantiates a new Verifier.
         *
         * @param key the public key
         * @throws Exception the exception
         */
        public Verifier(byte[] key) throws Exception {
            this.key = new byte[key.length];
            for (int i = 0; i < key.length; i++) {
                this.key[i] = key[i];
            }
        }

        /**
         * Verifies a payload. First we generate a byte array
         * consisting of the payload with the salt concatenated to the
         * end. Then, we use the RSA algorithm to verify that the signature
         * was generated from that payload + salt combination.
         *
         * @param signedPayload the signed payload
         * @throws Exception the exception
         */
        public void verify(SignedPayload signedPayload) throws Exception {
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initVerify(AsymmetricCipher.bytesToPublicKey(key));
            byte[] payload = signedPayload.getPayload();
            byte[] salt = signedPayload.getSalt();

            // Generate payload+salt concatenated byte array
            byte[] totalPayload = new byte[payload.length+salt.length];
            for (int i = 0; i < payload.length; i++) {
                totalPayload[i] = payload[i];
            }
            for (int i = 0; i < salt.length; i++) {
                totalPayload[i+payload.length] = salt[i];
            }
            sign.update(totalPayload);

            // Perform verification
            if(!sign.verify(signedPayload.getSignature())) {
                throw new Exception("Couldn't verify signed payload");
            }
        }
    }

    /**
     * This class will sign payloads by generating a salt and signature.
     */
    public static class Signer {
        /**
         * The private key.
         */
        byte[] key;

        /**
         * Instantiates a new Signer.
         *
         * @param key the private key
         * @throws Exception the exception
         */
        public Signer(byte[] key) throws Exception {
            this.key = new byte[key.length];
            for (int i = 0; i < key.length; i++) {
                this.key[i] = key[i];
            }
        }

        /**
         * Signs a payload, returning a SignedPayload instance.
         * To sign a payload, we generate a random salt byte array.
         * Then we create a new byte array consisting of the original
         * payload with the salt byte array concatenated to the end.
         * Finally, we use the RSA algorithm to generate a signature
         * from that concatenated payload.
         *
         * @param payload the payload
         * @return the signed payload
         * @throws Exception the exception
         */
        public SignedPayload sign(byte[] payload) throws Exception {
            SignedPayload buffer = new SignedPayload();
            buffer.setPayload(payload);

            // Generate random salt
            Random random = new Random(System.currentTimeMillis());
            byte[] salt = new byte[50+random.nextInt(50)];
            random.nextBytes(salt);
            buffer.setSalt(salt);

            // Concatenate salt to payload
            byte[] totalPayload = new byte[payload.length+salt.length];
            for (int i = 0; i < payload.length; i++) {
                totalPayload[i] = payload[i];
            }
            for (int i = 0; i < salt.length; i++) {
                totalPayload[i+payload.length] = salt[i];
            }

            // Initialize RSA cipher and generate signature
            Signature sign = Signature.getInstance("SHA256withRSA");
            sign.initSign(AsymmetricCipher.bytesToPrivateKey(key));
            sign.update(totalPayload);
            byte[] signature = sign.sign();
            buffer.setSignature(signature);
            return buffer;
        }
    }
}
