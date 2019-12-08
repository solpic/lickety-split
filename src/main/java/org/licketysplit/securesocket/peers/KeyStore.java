package org.licketysplit.securesocket.peers;

import java.util.Base64;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class stores and loads keys (represented by byte arrays)
 * to and from files.
 */
public class KeyStore {
    /**
     * Byte array representing the key.
     */
    byte[] key;

    /**
     * Instantiates a new Key store.
     */
    public KeyStore() {}

    /**
     * The file this key is stored in.
     */
    String file;

    /**
     * Instantiates a new Key store.
     *
     * @param file the file
     */
    public KeyStore(String file) {
        this.file = file;
    }

    /**
     * Reads the key from the file.
     *
     * @throws Exception the exception
     */
    public void load() throws Exception {
        key = Base64.getDecoder().decode(new String(Files.readAllBytes( Paths.get(file))));
    }

    /**
     * Writes the key to the file.
     *
     * @throws Exception the exception
     */
    public void save() throws Exception {

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(Base64.getEncoder().encodeToString(key));
        writer.close();
    }

    /**
     * Gets the key byte array.
     *
     * @return the key
     */
    public byte[] getKey() {
        return key;
    }

    /**
     * Sets the key byte array.
     *
     * @param key the key
     */
    public void setKey(byte[] key) {
        this.key = key;
    }
}
