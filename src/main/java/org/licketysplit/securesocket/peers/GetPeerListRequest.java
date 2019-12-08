package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.Message;

/**
 * Asks a peer for a copy of their peer list, this message
 * contains no additional fields.
 */
public class GetPeerListRequest extends Message {
    /**
     * Serialize message to byte array
     * @return serialized message
     */
    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    /**
     * Deserializes
     * @param data serialized message as a byte array
     */
    @Override
    public void fromBytes(byte[] data) {

    }

    /**
     * Instantiates a new Get peer list request.
     */
    public GetPeerListRequest() {}
}
