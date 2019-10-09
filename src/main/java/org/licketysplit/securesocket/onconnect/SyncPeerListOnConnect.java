package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.EnvLogger;
import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.UserInfo;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.GetPeerListRequest;
import org.licketysplit.securesocket.peers.GetPeerListResponse;

import java.util.logging.Level;

public class SyncPeerListOnConnect implements NewConnectionHandler {
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        EnvLogger logger = env.getLogger();
        logger.log(Level.INFO, "Requesting peer list");

        sock.sendFirstMessage(new GetPeerListRequest(), (ReceivedMessage m) -> {
            GetPeerListResponse r = m.getMessage();
            logger.log(Level.INFO, "Received peer list");
        });
    }
}
