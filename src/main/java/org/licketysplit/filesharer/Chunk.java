package org.licketysplit.filesharer;

public class Chunk {
    public byte[] bytes;
    public int chunk;

    public Chunk(byte[] bytes, int chunk){
        this.bytes = bytes;
        this.chunk = chunk;
    }
}
