package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.UserInfo;

/**
 * The interface New connection handler. This callback will be called after a handshake has been completed
 */
public interface NewConnectionHandler {
    /**
     * The method to call after the connection has been confirmed
     *
     * @param user the user we confirmed with
     * @param sock the socket
     * @param env  our Environment
     * @throws Exception the exception
     */
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception;
}
