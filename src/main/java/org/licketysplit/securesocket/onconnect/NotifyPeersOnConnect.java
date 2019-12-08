package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.JSONMessage;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;

import java.util.logging.Level;

/**
 * This class is called after a connection has been handshaked/confirmed.
 * It sends out a message to all of our connectd peers telling them about the
 * new connection, and they then attempt to connect to the peer.
 * However, it is no longer used in favor of alternate connection methods.
 */
public class NotifyPeersOnConnect implements NewConnectionHandler {
    /**
     * Called when the connection is confirmed
     * @param user the user we confirmed with
     * @param sock the socket
     * @param env  our Environment
     * @throws Exception
     */
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        //env.getLogger().log(Level.INFO, "Sending new peer connection notification");
        env.getPm().messageAllPeers(new NewPeerConnectedNotification(sock.getPeerAddress()), null);
    }

    /**
     * The default handler for NewPeerConnectedNotifications
     */
    @DefaultHandler(type=NewPeerConnectedNotification.class)
    public static class NewPeerConnectedNotificationHandler implements MessageHandler {

        /**
         * Called upon receipt of the message.
         * @param m the received message
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            EnvLogger log = m.getEnv().getLogger();
            //log.log(Level.INFO, "Received new peer connection notification");
            NewPeerConnectedNotification msg = m.getMessage();
//            m.getEnv().getPm().addPeer(msg.getPeer());
        }
    }

    /**
     * The message alerting peers that a new peer has connected.
     */
    public static class NewPeerConnectedNotification extends JSONMessage {
        /**
         * The peer that connected.
         */
        PeerManager.PeerAddress peer;

        /**
         * Gets peer.
         *
         * @return the peer
         */
        public PeerManager.PeerAddress getPeer() {
            return peer;
        }

        /**
         * Sets peer.
         *
         * @param peer the peer
         */
        public void setPeer(PeerManager.PeerAddress peer) {
            this.peer = peer;
        }

        /**
         * Instantiates a new New peer connected notification.
         *
         * @param peer the peer
         */
        public NewPeerConnectedNotification(PeerManager.PeerAddress peer) {
            this.peer = peer;
        }

        /**
         * Instantiates a new New peer connected notification.
         */
        public NewPeerConnectedNotification() {
        }
    }
}
