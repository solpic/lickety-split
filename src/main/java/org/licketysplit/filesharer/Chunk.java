package org.licketysplit.filesharer;

import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.messages.ReceivedMessage;

public class Chunk {
    public byte[] bytes;
    public int chunk;
    public ReceivedMessage m;

    public Chunk(ReceivedMessage m, byte[] bytes, int chunk){
        this.bytes = bytes;
        this.chunk = chunk;
        this.m = m;
    }
}
