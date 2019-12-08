package org.licketysplit.filesharer;

/**
 * Class representing a chunk.
 */
public class Chunk {
    /**
     * Raw bytes of chunk.
     */
    public byte[] bytes;
    /**
     * Index of chunk.
     */
    public int chunk;

    /**
     * Instantiates a new Chunk.
     *
     * @param bytes the bytes
     * @param chunk the chunk
     */
    public Chunk(byte[] bytes, int chunk){
        this.bytes = bytes;
        this.chunk = chunk;
    }
}
