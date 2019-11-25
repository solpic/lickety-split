package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.UserInfo;

public class SyncManifestOnConnect implements NewConnectionHandler {
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        env.getSyncManager().syncManifestWith(sock);
    }
}
