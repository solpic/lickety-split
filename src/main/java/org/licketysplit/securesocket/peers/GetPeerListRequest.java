package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.Message;

public class GetPeerListRequest extends Message {
    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    @Override
    public void fromBytes(byte[] data) {

    }
    public GetPeerListRequest() {}
}
