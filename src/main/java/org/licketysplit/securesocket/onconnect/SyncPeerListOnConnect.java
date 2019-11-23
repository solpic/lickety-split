package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.*;
import org.licketysplit.securesocket.messages.DefaultHandler;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;

import java.util.logging.Level;

public class SyncPeerListOnConnect implements NewConnectionHandler {
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        EnvLogger logger = env.getLogger();
        //logger.log(Level.INFO, "Requesting peer list");

        sock.sendFirstMessage(new GetPeerListRequest(), (ReceivedMessage m) -> {
            GetPeerListResponse lst = m.getMessage();
            PeerInfoDirectory info = lst.getInfo();
            env.getInfo().syncInfo(info);
            for (PeerManager.PeerAddress peer : lst.getPeerList()) {

                try {
                    m.getEnv().getPm().addPeer(peer);
                } catch(Exception e) {
                    env.getLogger().log(Level.SEVERE,
                            String.format("Couldn't connect to peer %s at ip %s, port %d",
                                    peer.getUser().getUsername(),
                                    peer.getServerInfo().getIp(),
                                    peer.getServerInfo().getPort()));
                }
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
