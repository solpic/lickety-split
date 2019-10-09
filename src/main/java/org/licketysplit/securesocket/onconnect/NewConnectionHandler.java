package org.licketysplit.securesocket.onconnect;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.UserInfo;

public interface NewConnectionHandler {
    public void connectionConfirmed(UserInfo user, SecureSocket sock, Environment env) throws Exception;
}
