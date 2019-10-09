package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.Message;

import java.util.List;

public class GetPeerListResponse extends Message {
    List<PeerManager.PeerAddress> peerList;
    @Override
    public byte[] toBytes() {
        return new byte[0];
    }

    @Override
    public void fromBytes(byte[] data) {

    }

    public GetPeerListResponse(List<PeerManager.PeerAddress> peers) {
        peerList = peers;
    }

    public GetPeerListResponse() {}
}
