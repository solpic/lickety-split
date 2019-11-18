package org.licketysplit.securesocket.peers;

import java.util.Base64;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Paths;

public class KeyStore {
    byte[] key;

    public KeyStore() {}
    String file;
    public KeyStore(String file) {
        this.file = file;
    }
    public void load() throws Exception {
        key = Base64.getDecoder().decode(new String(Files.readAllBytes( Paths.get(file))));
    }
    public void save() throws Exception {

        BufferedWriter writer = new BufferedWriter(new FileWriter(file));
        writer.write(Base64.getEncoder().encodeToString(key));
        writer.close();
    }

    public byte[] getKey() {
        return key;
    }

    public void setKey(byte[] key) {
        this.key = key;
    }
}
