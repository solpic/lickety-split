package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.util.List;

public class GetPeerListResponse extends JSONMessage {
    List<PeerManager.PeerAddress> peerList;

    public List<PeerManager.PeerAddress> getPeerList() {
        return peerList;
    }

    public void setPeerList(List<PeerManager.PeerAddress> peerList) {
        this.peerList = peerList;
    }

    public GetPeerListResponse(List<PeerManager.PeerAddress> peers) {
        peerList = peers;
    }

    public GetPeerListResponse() {}
}
