package org.licketysplit.testing;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.DownloadManager;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.gui.NetworkView;
import org.licketysplit.securesocket.SecureSocket;
import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.SyncManager;

import java.awt.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TestRunner {
    public TestRunner() {

    }
    public static String rootKeyFilename = "rootkey";
    public static String idKeyFilename = "idkey";
    public static String infoFilename = "infodir";
    public static String cmdFilename = "run.sh";
    public Environment getEnv(String ip, int port, String username, boolean isRoot, String localPath) throws Exception {
        PeerInfoDirectory info = new PeerInfoDirectory(Paths.get(localPath, infoFilename).toString());
        info.load();

        KeyStore rootKeyStore = null;
        if(isRoot) {
            rootKeyStore = new KeyStore(Paths.get(localPath, rootKeyFilename).toString());
            rootKeyStore.load();
        }

        KeyStore idKeyStore = new KeyStore(Paths.get(localPath, idKeyFilename).toString());
        idKeyStore.load();

        PeerManager.ServerInfo serverInfo = new PeerManager.ServerInfo(port, ip);
        org.licketysplit.securesocket.peers.UserInfo user = new UserInfo(username, serverInfo);
        PeerManager pm = new PeerManager();
        Environment env = new Environment(user, pm, true);
        File log = Paths.get(localPath, "log").toFile();
        env.getLogger().setLogFile(log);

        pm.setEnv(env);
        env.setInfo(info);
        env.setIdentityKey(idKeyStore);
        if(isRoot) env.setRootKey(rootKeyStore);
        info.env(env);

        FileManager fm = new FileManager();
        FileSharer fs = new FileSharer();

        String directory = "sharedFilesffff";
        File sharedDir = new File(Paths.get(localPath, directory).toString());
        if(sharedDir.exists()) {
            FileUtils.cleanDirectory(sharedDir);
        }else {
            sharedDir.mkdir();
        }
        String configs = Paths.get(localPath, "configs1").toString();
        SyncManager sm = new SyncManager();

        env.setFM(fm);
        env.setFS(fs);
        env.setDirectory(sharedDir.getAbsolutePath());
        env.setConfigs(configs);
        fs.setEnv(env);
        fm.setEnv(env);
        pm.setEnv(env);
        sm.setEnv(env);
        env.setSyncManager(sm);

        fm.initializeFiles("default");

        return env;
    }

    int userNumber(String username) throws Exception{
        int i = username.indexOf('-');
        int i2 = username.indexOf('-', i + 1);

        return Integer.parseInt(username.substring(i+1, i2));
    }

    boolean compareByteFiles(byte[] expected, byte[] actual) {
        if(expected.length!=actual.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if(expected[i]!=actual[i]) return false;
        }
        return true;
    }

    void checkFinalState(Environment env) throws Exception {
        boolean good = true;
        StringBuilder msg = new StringBuilder();
        synchronized (finalContents) {
            for (Map.Entry<String, String> fileEntry : finalContents.entrySet()) {
                String filename = fileEntry.getKey();
                File file = new File(env.getFM().getSharedDirectoryPath(filename));
                if (!file.exists()) {
                    good = false;
                    msg.append(String.format("%s does not exist BUT SHOULD\n", file.getPath()));
                } else if (env.getFS().isInProgress(file.getName())) {
                    good = false;
                    msg.append(String.format("%s is not done downloading\n", file.getPath()));
                } else {
                    String md5downloaded = env.getSyncManager().getMD5(file);
                    String md5orig = env.getSyncManager().getMD5(new File(fileEntry.getValue()));
                    long length = file.length();
                    String size = String.format("%f MB", ((float)length)/(1024.0f*1024.0f));
                    byte[] downloaded = FileUtils.readFileToByteArray(file);
                    if (md5orig.equals(md5downloaded)) {
                        msg.append(String.format("%s matches original, size %s\n", file.getPath(), size));
                    } else {
                        good = false;
                        msg.append(String.format("%s does not match original ->\n\tHASH-ORIG: %s\n\tHASH-DOWN: %s",
                                file.getAbsolutePath(),
                                md5orig,
                                md5downloaded));
                    }
                }

            }
        }
        env.getLogger().trigger("assert", good, msg.toString());
        //env.log(String.format("FINAL GOOD: %b, MSG:\n%s", good, msg.toString()));
    }



    Random randomFileMaker = new Random(100);
    byte[] randomBigFile(int length) {
        byte[] bytes = new byte[length];
        randomFileMaker.nextBytes(bytes);
        return bytes;
    }

    public static class FileChange {
        public String path;
        public String tmpPath;
        public boolean create = false;
        public boolean update = false;
        public boolean delete = false;
        public int userNumber;
        public int delay;


        public static FileChange randomCreate(Environment env, Random r, int delay, int sizeMin, int sizeMax, int maxUsers) {
            try {
                FileChange change = new FileChange();
                change.userNumber = r.nextInt(maxUsers);
                change.create = true;
                change.path = String.format("random-file-%d", r.nextInt(100000));
                int size = r.nextInt(sizeMax - sizeMin) + sizeMin;
                File temp = new File(change.path);
                temp.createNewFile();
                temp.deleteOnExit();
                RandomAccessFile rw = new RandomAccessFile(temp.getPath(), "rw");
                rw.setLength(size);
                change.tmpPath = temp.getPath();

                int bytes = r.nextInt(1000)+1000;
                for(int i = 0; i<bytes; i++) {
                    rw.seek(r.nextInt(size-50));
                    rw.write(r.nextInt());
                }

                change.delay = delay;
                return change;
            }catch(Exception e) {
                env.log("Error making change", e);
                return null;
            }
        }

        public String changeMessage() {
            return null;
        }
    }

    Map<String, String> finalContents = new HashMap<>();
    void applyChange(FileChange change) {
        synchronized (finalContents) {
            if(change.create) {
                finalContents.put(change.path, change.tmpPath);
            }
        }
    }

    String bytesToRepr(byte[] data) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < data.length; i++) {

            str.append(String.format("%d", (int)data[i]));
            if(i<data.length-1) str.append(", ");
        }
        return str.toString();
    }

    int nonzeroes(byte[] data) {
        int count = 0;
        for (byte datum : data) {
            if(datum==0) count++;
        }
        return count;
    }

    List<FileChange> changes = new ArrayList<>();
    void runMain(Environment env, int usernumber, String localPath) throws Exception{
        env.getLogger().log(Level.INFO, String.format("I am user number %d", usernumber));
        env.getLogger().trigger("print", String.format("User number %d", usernumber));
        Random r = new Random(100);
        int users = env.getInfo().getPeers().size();
        long startTime = System.currentTimeMillis();
        long lastChange = startTime;
        long waitAfterChangesMin = 10000;
        long waitPerChange = 10000;
        long waitAfterCheck = 10000;
        int changeCycle = 0;
        Thread.sleep(10000);

        String[] path = new String[]{""};
        env.getDebug().setTrigger("chunk", (Object ...args) -> {
            new Thread(() -> {
                try {
                    byte[] data = (byte[]) args[0];
                    int chunk = (int) args[1];
                    SecureSocket conn = (SecureSocket) args[2];


                    RandomAccessFile rw = new RandomAccessFile(path[0], "r");
                    byte[] orig = new byte[data.length];
                    rw.seek(chunk* DownloadManager.chunkLengthRaw);
                    rw.readFully(orig);
                    rw.close();

                    if(compareByteFiles(orig, data)) {
                        env.getLogger().trigger("print",
                                String.format(
                                        "Chunk #%d not equal, sent from %s to %s:\nORIG: %d\nREC: %d",
                                        chunk, conn.peerUsername, env.getUserInfo().getUsername(),
                                        bytesToRepr(orig),
                                        bytesToRepr(data)
                                ));
                    }
                }catch(Exception e) {
                    env.log("Comp", e);
                }
            }).start();
        });
        while(true) {
            int changeCount = 1 + changeCycle*5;
            for (int i = 0; i < 1; i++) {
                FileChange change = FileChange.randomCreate(
                        env,
                        r,
                        5000,
                        1024*1024*400,
                        1024*1024*800,
                        users
                );
                path[0] = change.tmpPath;
                applyChange(change);
                lastChange += change.delay;
                long startChangeAt = lastChange;


                if(usernumber==change.userNumber) {
                    new Thread(() -> {
                        try {
                            long diff = startChangeAt - System.currentTimeMillis();
                            if(diff>0)
                                Thread.sleep(diff);

                            env.getLogger().trigger("print", String.format("User number %d initiating change", usernumber));
                            if(change.create) {
                                SyncManager sm = env.getSyncManager();
                                sm.addFile(change.tmpPath);
                            }
                        } catch(Exception e) {
                            env.getLogger().log(Level.INFO, "Error doing change", e);
                        }
                    }).start();
                }
            }
            Thread.sleep(100000000);
            lastChange += waitAfterChangesMin + waitPerChange*changeCount;
            long diff = lastChange - System.currentTimeMillis();
            if(usernumber==0) {
                env.getLogger().trigger("print",
                        String.format("Waiting %d seconds for changes to propagate", (int)(diff/1000)));
                long finalLastChange = lastChange;
                new Thread(() -> {
                    try {
                        Thread.sleep(10000);
                        long remaining = finalLastChange - System.currentTimeMillis();
                        while(remaining>10000){
                            env.getLogger().trigger("print",
                                    String.format("Remaining seconds %d", remaining / 1000));
                            Thread.sleep(10000);
                            remaining = finalLastChange - System.currentTimeMillis();
                        }
                    }catch(Exception e) {

                    }
                }).start();
            }
            if(diff>0) {
                Thread.sleep(diff);
            }
            if(env.getFM().autoDownloadNewFiles&&env.getFM().autoDownloadUpdates) {
                checkFinalState(env);
            }
            lastChange += waitAfterCheck;
            diff = lastChange - System.currentTimeMillis();
            if(usernumber==0) {
                env.getLogger().trigger("print",
                        String.format("Waiting %d seconds for next change cycle", (int)(diff/1000)));
            }
            if(diff>0) {
                Thread.sleep(diff);
            }
            changeCycle++;
        }

    }

    public void startPeer(String[] args) throws Exception {
        String cmd = args[0];
        String username = args[1];
        String ip = args[2];
        int port = Integer.parseInt(args[3]);

        boolean isRoot = false;
        if(args.length>4)isRoot = "isroot=yes".equals(args[4]);
        String localPath = ".";
        boolean isLocalThread = false;
        boolean isLocalUser = false;
        if(args.length>5) {
            localPath = args[5];
            isLocalThread = true;
            isLocalUser = true;
        }

        Environment env = getEnv(ip, port, username, isRoot, localPath);

        env.getLogger().log(Level.INFO, String.format("Starting peer at IP: %s, ARGS: %s", ip, Arrays.stream(args).collect(Collectors.joining(", "))));

        env.getFM().autoDownloadNewFiles = true;
        env.getFM().autoDownloadUpdates = true;

        try {
            if(cmd.equals("peerstart")) {
                env.getPm().start();
                runMain(env, userNumber(username), localPath);
            }else if(cmd.equals("headless")) {
                if(isLocalUser && userNumber(username) == 0) {
                    File bootstrap = env.getInfo().generateBootstrapFile(
                            env.getUserInfo().getUsername(),
                            env.getUserInfo().getServer().getIp(),
                            Integer.toString(env.getUserInfo().getServer().getPort()),
                            env.getIdentityKey().getKey(),
                            true,
                            env.getRootKey().getKey()
                    );
                    System.out.println("Bootstrap file at "+bootstrap.getAbsolutePath());

                    Desktop.getDesktop().open(bootstrap.getParentFile());
                }else {
                    env.getPm().start();
                    while(true) {
                        Thread.sleep(1000000);
                    }
                }
            }
        } catch (Exception e) {
            env.log("Exception in runner!");
            env.getLogger().log(Level.SEVERE, "RunMain exception", e);
        }
        env.getLogger().log(Level.INFO, "Exiting");
        if(!isLocalThread)  System.exit(0);
    }

    public void run(String[] args) throws Exception {
        if(args==null||args.length==0) {
            throw new Exception("Expected arguments");
        }
        startPeer(args);
    }
}
