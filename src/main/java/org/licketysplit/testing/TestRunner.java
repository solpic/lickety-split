package org.licketysplit.testing;

import org.licketysplit.env.Environment;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TestRunner {
    public TestRunner() {

    }
    public static String rootKeyFilename = "rootkey";
    public static String idKeyFilename = "idkey";
    public static String infoFilename = "infodir";
    public static String cmdFilename = "run.sh";
    public Environment getEnv(String ip, int port, String username, boolean isRoot) throws Exception {
        PeerInfoDirectory info = new PeerInfoDirectory(infoFilename);
        info.load();

        KeyStore rootKeyStore = null;
        if(isRoot) {
            rootKeyStore = new KeyStore(rootKeyFilename);
            rootKeyStore.load();
        }

        KeyStore idKeyStore = new KeyStore(idKeyFilename);
        idKeyStore.load();

        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, ip);
        org.licketysplit.securesocket.peers.UserInfo user = new UserInfo(username, serverInfo);
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm, true);
        File log = Paths.get("log").toFile();
        env.getLogger().setLogFile(log);

        pm.setEnv(env);
        env.setInfo(info);
        env.setIdentityKey(idKeyStore);
        if(isRoot) env.setRootKey(rootKeyStore);
        info.env(env);

        return env;
    }

    public void startPeer(String[] args) throws Exception {
        String username = args[1];
        String ip = args[2];
        int port = Integer.parseInt(args[3]);

        boolean isRoot = false;
        if(args.length>4)isRoot = "isroot=yes".equals(args[4]);

        Environment env = getEnv(ip, port, username, isRoot);
        env.getLogger().log(Level.INFO, "Starting peer");
        env.getPm().start();
        env.getLogger().log(Level.INFO, "Waiting 30 seconds");
        Thread.sleep(30000);
        env.getLogger().log(Level.INFO, "Exiting");
        System.exit(0);
    }

    public void run(String[] args) throws Exception {
        if(args==null||args.length==0) {
            throw new Exception("Expected arguments");
        }
        startPeer(args);
    }
}
