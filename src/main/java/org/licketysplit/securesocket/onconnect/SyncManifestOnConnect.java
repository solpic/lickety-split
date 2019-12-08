package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.UserInfo;

/**
 * After handshaking, this class will sync two peer's manifests.
 */
public class SyncManifestOnConnect implements NewConnectionHandler {
    /**
     * Syncs the manifests.
     * @param user the user we confirmed with
     * @param sock the socket
     * @param env  our Environment
     * @throws Exception
     */
    @Override
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception {
        env.getSyncManager().syncManifestWith(sock);
    }
}
