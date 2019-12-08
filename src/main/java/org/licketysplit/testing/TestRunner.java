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

/**
 * This class is only used for testing and doesn't affect normal usage of the application
 * whatsoever. This class serves as the entry point for simulated peers under various conditions,
 * mainly to test adding, updating and deleting large numbers of files over time.
 */
public class TestRunner {
    /**
     * Instantiates a new Test runner.
     */
    public TestRunner() {

    }

    /**
     * The constant rootKeyFilename.
     */
    public static String rootKeyFilename = "rootkey";
    /**
     * The constant idKeyFilename.
     */
    public static String idKeyFilename = "idkey";
    /**
     * The constant infoFilename.
     */
    public static String infoFilename = "infodir";
    /**
     * The constant cmdFilename.
     */
    public static String cmdFilename = "run.sh";

    /**
     * Gets env.
     *
     * @param ip        the ip
     * @param port      the port
     * @param username  the username
     * @param isRoot    the is root
     * @param localPath the local path
     * @return the env
     * @throws Exception the exception
     */
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

    /**
     * User number int.
     *
     * @param username the username
     * @return the int
     * @throws Exception the exception
     */
    int userNumber(String username) throws Exception{
        int i = username.indexOf('-');
        int i2 = username.indexOf('-', i + 1);

        return Integer.parseInt(username.substring(i+1, i2));
    }

    /**
     * Compare byte files boolean.
     *
     * @param expected the expected
     * @param actual   the actual
     * @return the boolean
     */
    boolean compareByteFiles(byte[] expected, byte[] actual) {
        if(expected.length!=actual.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if(expected[i]!=actual[i]) return false;
        }
        return true;
    }

    /**
     * Check final state.
     *
     * @param env the env
     * @throws Exception the exception
     */
    void checkFinalState(Environment env) throws Exception {
        boolean good = true;
        StringBuilder msg = new StringBuilder();
        synchronized (finalContents) {
            int successDownload = 0;
            float successDownloadMB = 0;
            int successDelete = 0;
            for (Map.Entry<String, String> fileEntry : finalContents.entrySet()) {
                String filename = fileEntry.getKey();
                File file = new File(env.getFM().getSharedDirectoryPath(filename));
                if (!file.exists()) {
                    if(fileEntry.getValue()==null) {
                        successDelete++;
                    }else {
                        good = false;
                        msg.append(String.format("%s does not exist BUT SHOULD\n", file.getPath()));
                    }
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
                        successDownload++;
                        successDownloadMB += ((float)length)/(1024.0f*1024.0f);
                    } else {
                        good = false;
                        msg.append(String.format("%s does not match original ->\n\tHASH-ORIG: %s\n\tHASH-DOWN: %s",
                                file.getAbsolutePath(),
                                md5orig,
                                md5downloaded));
                    }
                }

            }

            msg.append(String.format("Successful downloads: %d, size: %f MB, successful deletes: %d", successDownload, successDownloadMB, successDelete));
        }
        env.getLogger().trigger("assert", good, msg.toString());
        //env.log(String.format("FINAL GOOD: %b, MSG:\n%s", good, msg.toString()));
    }


    /**
     * The Random file maker.
     */
    Random randomFileMaker = new Random(100);

    /**
     * Random big file byte [ ].
     *
     * @param length the length
     * @return the byte [ ]
     */
    byte[] randomBigFile(int length) {
        byte[] bytes = new byte[length];
        randomFileMaker.nextBytes(bytes);
        return bytes;
    }

    /**
     * The type File change.
     */
    public static class FileChange {
        /**
         * The Path.
         */
        public String path;
        /**
         * The Tmp path.
         */
        public String tmpPath;
        /**
         * The Update name.
         */
        public String updateName;
        /**
         * The New version name.
         */
        public String newVersionName;
        /**
         * The Delete name.
         */
        public String deleteName;
        /**
         * The Create.
         */
        public boolean create = false;
        /**
         * The Update.
         */
        public boolean update = false;
        /**
         * The Delete.
         */
        public boolean delete = false;
        /**
         * The User number.
         */
        public int userNumber;
        /**
         * The Delay.
         */
        public int delay;

        /**
         * Can delete or update boolean.
         *
         * @param finalContents the final contents
         * @return the boolean
         */
        public static boolean canDeleteOrUpdate(Map<String, String> finalContents) {
            synchronized (finalContents) {
                return finalContents.entrySet().stream().filter(e -> e.getValue() != null)
                        .count() > 0;
            }
        }

        /**
         * Random create file change.
         *
         * @param env      the env
         * @param r        the r
         * @param delay    the delay
         * @param sizeMin  the size min
         * @param sizeMax  the size max
         * @param maxUsers the max users
         * @return the file change
         */
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

        /**
         * Random delete or update file change.
         *
         * @param env           the env
         * @param r             the r
         * @param delay         the delay
         * @param sizeMin       the size min
         * @param sizeMax       the size max
         * @param maxUsers      the max users
         * @param finalContents the final contents
         * @return the file change
         * @throws Exception the exception
         */
        public static FileChange randomDeleteOrUpdate(Environment env, Random r, int delay, int sizeMin, int sizeMax, int maxUsers, Map<String, String> finalContents) throws Exception {
            float deleteChance = 0.3f;
            FileChange change = new FileChange();
            change.userNumber = r.nextInt(maxUsers);
            change.delay = delay;
            List<String> existing = finalContents.entrySet().stream().filter(e -> e.getValue() != null)
                    .map(e-> e.getKey())
                    .collect(Collectors.toList());
            String name = existing.get(r.nextInt(existing.size()));
            if(r.nextFloat()<=deleteChance) {
                change.delete = true;
                change.deleteName = name;
            }else{
                change.update = true;
                change.updateName = name;
                SyncManager.FileVersion v = env.getSyncManager().getVersionFromName(name);
                v.increase();
                change.newVersionName = v.get();
                int size = r.nextInt(sizeMax - sizeMin) + sizeMin;
                File temp = Files.createTempFile("temp", null).toFile();
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
            }
            return change;
        }

        /**
         * Change message string.
         *
         * @return the string
         */
        public String changeMessage() {
            return null;
        }
    }

    /**
     * The Final contents.
     */
    Map<String, String> finalContents = new HashMap<>();

    /**
     * Apply change.
     *
     * @param change the change
     */
    void applyChange(FileChange change) {
        synchronized (finalContents) {
            if(change.create) {
                finalContents.put("v1 "+change.path, change.tmpPath);
            }else if(change.delete) {
                finalContents.put(change.deleteName, null);
            }else if(change.update) {
                finalContents.put(change.updateName, null);
                finalContents.put(change.newVersionName, change.tmpPath);
            }
        }
    }

    /**
     * Bytes to repr string.
     *
     * @param data the data
     * @return the string
     */
    String bytesToRepr(byte[] data) {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < data.length; i++) {

            str.append(String.format("%d", (int)data[i]));
            if(i<data.length-1) str.append("\n");
        }
        return str.toString();
    }

    /**
     * Nonzeroes int.
     *
     * @param data the data
     * @return the int
     */
    int nonzeroes(byte[] data) {
        int count = 0;
        for (byte datum : data) {
            if(datum==0) count++;
        }
        return count;
    }

    /**
     * The constant fileMB.
     */
    public static int fileMB = 20;
    /**
     * The constant fileMBRange.
     */
    public static int fileMBRange = 5;

    /**
     * The Changes.
     */
    List<FileChange> changes = new ArrayList<>();

    /**
     * Run main.
     *
     * @param env        the env
     * @param usernumber the usernumber
     * @param localPath  the local path
     * @throws Exception the exception
     */
    void runMain(Environment env, int usernumber, String localPath) throws Exception{
        env.getLogger().log(Level.INFO, String.format("I am user number %d", usernumber));
        env.getLogger().trigger("print", String.format("User number %d", usernumber));
        Random r = new Random(100);
        int users = env.getInfo().getPeers().size();
        long startTime = System.currentTimeMillis();
        long lastChange = startTime;
        long waitAfterChangesMin = 10000;
        long waitPerChange = 5000;
        long waitAfterCheck = 10000;
        int changeCycle = 0;
        Thread.sleep(10000);
        lastChange += 10000;

        while(true) {
            int changeCount = 3;
            for (int i = 0; i < changeCount; i++) {
                float mb = fileMB;
                FileChange change;
                float chance = r.nextFloat();
                if(FileChange.canDeleteOrUpdate(finalContents)&&chance>0.5f) {
                    change = FileChange.randomDeleteOrUpdate(env, r, 5000,
                            (int) (1024 * 1024 * (mb - fileMBRange)),
                            (int) (1024 * 1024 * (mb + fileMBRange)), users, finalContents);
                }else {
                    change = FileChange.randomCreate(
                            env,
                            r,
                            5000,
                            (int) (1024 * 1024 * (mb - fileMBRange)),
                            (int) (1024 * 1024 * (mb + fileMBRange)),
                            users
                    );
                }
                applyChange(change);
                lastChange += change.delay;
                long startChangeAt = lastChange;


                if(usernumber==change.userNumber) {
                    new Thread(() -> {
                        try {
                            long diff = startChangeAt - System.currentTimeMillis();
                            if(diff>0)
                                Thread.sleep(diff);

                            if(change.create) {
                                SyncManager sm = env.getSyncManager();
                                sm.addFile(change.tmpPath);
                                env.getLogger().trigger("print",
                                        String.format("Adding file %s", change.path));
                            }else if(change.delete) {
                                env.getSyncManager().deleteFile(change.deleteName);
                                env.getLogger().trigger("print",
                                        String.format("Deleting file %s", change.deleteName));
                            }else if(change.update) {
                                env.getSyncManager().updateFile(change.updateName, change.tmpPath);
                                env.getLogger().trigger("print",
                                        String.format("Updating file %s", change.updateName));
                                //
                            }
                        } catch(Exception e) {
                            env.getLogger().log(Level.INFO, "Error doing change", e);
                        }
                    }).start();
                }
            }
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
                env.log(String.format("Waiting %d seconds until checking", diff/1000));
                Thread.sleep(diff);
            }
            if(env.getFM().autoDownloadNewFiles&&env.getFM().autoDownloadUpdates) {
                env.log("Checking state");
                checkFinalState(env);
            }else{
                env.log("Not checking state");
            }
            lastChange += waitAfterCheck;
            diff = lastChange - System.currentTimeMillis();
            env.log(String.format("Waiting %d seconds for next change cycle", diff/1000));
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

    /**
     * Start peer.
     *
     * @param args the args
     * @throws Exception the exception
     */
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
            if(cmd.equals("peerstart-with-local")) {

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
                    while (true) {
                        Thread.sleep(1000000);
                    }
                }

            }else if(cmd.equals("peerstart")) {
                env.getPm().start();
                runMain(env, userNumber(username), localPath);
            }else if(cmd.equals("headless")) {
                env.getPm().start();
                while(true) {
                    Thread.sleep(1000000);
                }
            }
        } catch (Exception e) {
            env.log("Exception in runner!");
            env.getLogger().log(Level.SEVERE, "RunMain exception", e);
        }
        env.getLogger().log(Level.INFO, "Exiting");
        if(!isLocalThread)  System.exit(0);
    }

    /**
     * Run.
     *
     * @param args the args
     * @throws Exception the exception
     */
    public void run(String[] args) throws Exception {
        if(args==null||args.length==0) {
            throw new Exception("Expected arguments");
        }
        startPeer(args);
    }
}
