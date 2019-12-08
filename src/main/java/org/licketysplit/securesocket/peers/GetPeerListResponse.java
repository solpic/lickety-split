package org.licketysplit.securesocket.peers;

import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.Message;

import java.util.List;

/**
 * This is the response to a GetPeerListRequest. Simply
 * gives the peer a copy of our peer list and our peer info directory.
 */
public class GetPeerListResponse extends JSONMessage {
    /**
     * A copy of our peer list
     */
    List<PeerManager.PeerAddress> peerList;
    /**
     * A copy of our info directory.
     */
    PeerInfoDirectory info;

    /**
     * Gets peer list.
     *
     * @return the peer list
     */
    public List<PeerManager.PeerAddress> getPeerList() {
        return peerList;
    }

    /**
     * Sets peer list.
     *
     * @param peerList the peer list
     */
    public void setPeerList(List<PeerManager.PeerAddress> peerList) {
        this.peerList = peerList;
    }

    /**
     * Gets peer info directory.
     *
     * @return the info
     */
    public PeerInfoDirectory getInfo() {
        return info;
    }

    /**
     * Sets peer info directory.
     *
     * @param info the info
     */
    public void setInfo(PeerInfoDirectory info) {
        this.info = info;
    }

    /**
     * Instantiates a new Get peer list response.
     *
     * @param peers the peer list
     * @param info  the peer info directory
     */
    public GetPeerListResponse(List<PeerManager.PeerAddress> peers, PeerInfoDirectory info) {
        peerList = peers;
        this.info = info;
    }

    /**
     * Instantiates a new Get peer list response.
     */
    public GetPeerListResponse() {}
}
