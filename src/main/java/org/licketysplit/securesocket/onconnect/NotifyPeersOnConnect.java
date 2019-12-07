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

public class NotifyPeersOnConnect implements NewConnectionHandler {
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        //env.getLogger().log(Level.INFO, "Sending new peer connection notification");
        env.getPm().messageAllPeers(new NewPeerConnectedNotification(sock.getPeerAddress()), null);
    }

    @DefaultHandler(type=NewPeerConnectedNotification.class)
    public static class NewPeerConnectedNotificationHandler implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) throws Exception {
            EnvLogger log = m.getEnv().getLogger();
            //log.log(Level.INFO, "Received new peer connection notification");
            NewPeerConnectedNotification msg = m.getMessage();
//            m.getEnv().getPm().addPeer(msg.getPeer());
        }
    }

    public static class NewPeerConnectedNotification extends JSONMessage {
        PeerManager.PeerAddress peer;

        public PeerManager.PeerAddress getPeer() {
            return peer;
        }

        public void setPeer(PeerManager.PeerAddress peer) {
            this.peer = peer;
        }

        public NewPeerConnectedNotification(PeerManager.PeerAddress peer) {
            this.peer = peer;
        }

        public NewPeerConnectedNotification() {
        }
    }
}
