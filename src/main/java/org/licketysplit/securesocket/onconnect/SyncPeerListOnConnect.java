package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.GetPeerListRequest;
import org.licketysplit.securesocket.peers.GetPeerListResponse;
import org.licketysplit.securesocket.peers.PeerManager;

import java.util.logging.Level;

public class SyncPeerListOnConnect implements NewConnectionHandler {
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        EnvLogger logger = env.getLogger();
        logger.log(Level.INFO, "Requesting peer list");

        sock.sendFirstMessage(new GetPeerListRequest(), (ReceivedMessage m) -> {
            GetPeerListResponse lst = m.getMessage();
            for (PeerManager.PeerAddress peer : lst.getPeerList()) {
                m.getEnv().getPm().addPeer(peer);
            }
        });
    }



    @DefaultHandler(type=GetPeerListRequest.class)
    public static class GetPeerListRequestHandler implements MessageHandler {

        @Override
        public void handle(ReceivedMessage m) {
            GetPeerListRequest r = m.getMessage();
            EnvLogger logger = m.getEnv().getLogger();
            try {
                m.respond(m.getEnv().getPm().getPeerListResponse(), null);
            }catch(Exception e) {
                logger.log(Level.SEVERE, "Error sending peer list", e);
            }
        }
    }
}
