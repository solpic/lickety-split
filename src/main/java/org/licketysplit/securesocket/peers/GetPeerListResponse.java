package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.util.List;

public class GetPeerListResponse extends JSONMessage {
    List<PeerManager.PeerAddress> peerList;
    PeerInfoDirectory info;

    public List<PeerManager.PeerAddress> getPeerList() {
        return peerList;
    }

    public void setPeerList(List<PeerManager.PeerAddress> peerList) {
        this.peerList = peerList;
    }

    public PeerInfoDirectory getInfo() {
        return info;
    }

    public void setInfo(PeerInfoDirectory info) {
        this.info = info;
    }

    public GetPeerListResponse(List<PeerManager.PeerAddress> peers, PeerInfoDirectory info) {
        peerList = peers;
        this.info = info;
    }

    public GetPeerListResponse() {}
}
