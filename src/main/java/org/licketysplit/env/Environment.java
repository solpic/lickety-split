package org.licketysplit.env;

import org.licketysplit.securesocket.peers.KeyStore;
import org.licketysplit.securesocket.peers.PeerInfoDirectory;
import org.licketysplit.filesharer.FileSharer;
import org.licketysplit.securesocket.peers.PeerManager;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileManager;
import org.licketysplit.syncmanager.SyncManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class is made to enable a threaded application to simulate using singletons.
 * For various classes, such as PeerManager, FileSharer, FileManager,
 * there is only ever one instance at a time for any peer. However,
 * many tests are done simulating each peer in different threads, rather than
 * in a different process. If singletons were used for the aforementioned classes,
 * these different simulated peers would be accessing the same instance of these classes
 * when instead each peer should have its own instance.<br><br>
 *
 * The Environment class stores each singleton instance, and in nearly all places of the application
 * code there is access to an instance of the Environment. This allows any thread
 * to use "singletons" from the environment, and it will be singletons for that thread,
 * not for all threads.<br><br>
 *
 * It is worth noting that any time you see an instance of Environment, it is representing
 * the Environment of the peer running the code. At no point would one peer have access to
 * multiple Environments, or to another peer's Environment.
 */
public class Environment {
    /**
     * Gets peer manager.
     *
     * @return the peer manager
     */
    public PeerManager getPm() {
        return pm;
    }

    /**
     * Log a message.
     *
     * @param msg the msg
     */
    public void log(String msg)  {
        logger.log(msg);
    }

    /**
     * Gets user info.
     *
     * @return the user info
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * The User info.
     */
    UserInfo userInfo;
    /**
     * The Logger.
     */
    EnvLogger logger;
    /**
     * The file sharer.
     */
    FileSharer fS;
    /**
     * The file manager.
     */
    FileManager fM;
    /**
     * The shared files directory.
     */
    String directory;
    /**
     * The config directory.
     */
    String configs;

    /**
     * This function returns the full path for a file named 'filename' in the
     * shared directory folder of the peer.
     *
     * @param fileName the file name
     * @return the full path
     */
    public String getDirectory(String fileName) {
        Path path = Paths.get(this.directory, fileName);
        return path.toString();
    }

    /**
     * No longer used.
     *
     * @param directoryName the directory name
     * @param fileName      the file name
     * @return the string
     */
    public String getTempDirectory(String directoryName, String fileName){
        Path path = Paths.get(this.directory, directoryName, fileName);
        return path.toString();
    }

    /**
     * Sets shared directory path.
     *
     * @param directory the directory
     */
    public void setDirectory(String directory) {
        this.directory = directory;
    }

    /**
     * Sets config folder path.
     *
     * @param configs the configs
     */
    public void setConfigs(String configs){
        this.configs = configs;
    }

    /**
     * Gets the full path to a file in the config directory.
     *
     * @param fileName the file name
     * @return the full path
     */
    public String getConfigs(String fileName){
        Path path = Paths.get(this.configs, fileName);
        return path.toString();
    }

    /**
     * Gets path to configs directory.
     *
     * @return the path
     */
    public String getConfigs(){
        return Paths.get(this.configs).toString();
    }

    /**
     * The rootKey is the public key of the RSA keypair used to sign messages
     * from the root user, i.e. verify a message came from root
     * This is used to verify new users and bans. If this object exists
     * it is the private key for the root key, however everyone has a copy
     * of the public key.
     */
    KeyStore rootKey;

    /**
     * The private key for this peers RSA identity key.
     */
    KeyStore identityKey;

    /**
     * Stores info about peer usernames, IP addresses/ports, and the public key of their identity keys.
     */
    PeerInfoDirectory info;

    /**
     * A debugging helper class.
     */
    Debugger debug;

    /**
     * Gets root key.
     *
     * @return the root key
     */
    public KeyStore getRootKey() {
        return rootKey;
    }

    /**
     * Sets root key.
     *
     * @param rootKey the root key
     */
    public void setRootKey(KeyStore rootKey) {
        this.rootKey = rootKey;
    }

    /**
     * Gets identity key.
     *
     * @return the identity key
     */
    public KeyStore getIdentityKey() {
        return identityKey;
    }

    /**
     * Gets debugger.
     *
     * @return the debugger
     */
    public Debugger getDebug() {
        return debug;
    }

    /**
     * Sets debugger.
     *
     * @param debug the debugger
     */
    public void setDebug(Debugger debug) {
        this.debug = debug;
    }

    /**
     * Sets identity key.
     *
     * @param identityKey the identity key
     */
    public void setIdentityKey(KeyStore identityKey) {
        this.identityKey = identityKey;
    }

    /**
     * Gets peer info directory.
     *
     * @return the info directory
     */
    public PeerInfoDirectory getInfo() {
        return info;
    }

    /**
     * Sets peer info directory.
     *
     * @param info the directory
     */
    public void setInfo(PeerInfoDirectory info) {
        this.info = info;
    }

    /**
     * The Sync manager.
     */
    SyncManager syncManager;

    /**
     * Gets sync manager.
     *
     * @return the sync manager
     */
    public SyncManager getSyncManager() {
        return syncManager;
    }

    /**
     * Sets sync manager.
     *
     * @param syncManager the sync manager
     */
    public void setSyncManager(SyncManager syncManager) {
        this.syncManager = syncManager;
    }

    /**
     * A class to allow any thread to trigger callbacks elsewhere, mostly for use in the GUI.
     */
    public ChangeHandler changes;

    /**
     * Instantiates a new Environment.
     * In the constructor, the environment creates a logger, debugger, and changehandler.
     *
     * @param userInfo     the user info
     * @param pm           the peer manager
     * @param debugEnabled should enable debugging?
     */
    public Environment(UserInfo userInfo, PeerManager pm, boolean debugEnabled) {
        this.userInfo = userInfo;
        this.pm = pm;
        logger = new EnvLogger(userInfo.getUsername());
        this.debug = new Debugger(debugEnabled);
        this.changes = new ChangeHandler();

    }

    /**
     * Maps string keys onto callback functions, used to trigger updates in the GUI.
     */
    public static class ChangeHandler {
        /**
         * The interface Change.
         */
        public interface Change {
            /**
             * Update.
             *
             * @param arg the arg
             */
            void update(Object arg);
        }

        /**
         * The map of keys to callback functions.
         */
        ConcurrentHashMap<String, Change> changes;

        /**
         * Instantiates a new Change handler.
         */
        ChangeHandler() {
            changes = new ConcurrentHashMap<>();
        }

        /**
         * Run callback function.
         *
         * @param name the name of the callback
         * @param arg  the argument to the callback
         */
        public void runHandler(String name, Object arg) {
            Change change = changes.get(name);
            if(change!=null) {
                change.update(arg);
            }
        }

        /**
         * Sets handler.
         *
         * @param name   the name
         * @param change the change
         */
        public void setHandler(String name, Change change) {
            synchronized (changes) {
                changes.put(name, change);
            }
        }
    }

    /**
     * Set file sharer.
     *
     * @param fS the filesharer
     */
    public void setFS(FileSharer fS){
        this.fS = fS;
    }

    /**
     * Get file sharer.
     *
     * @return the file sharer
     */
    public FileSharer getFS(){
        return this.fS;
    }

    /**
     * Set filemanager.
     *
     * @param fM the filemanager
     */
    public void setFM(FileManager fM){
        this.fM = fM;
    }

    /**
     * Get file manager.
     *
     * @return the file manager
     */
    public FileManager getFM(){
        return this.fM;
    }

    /**
     * Log a message and exception.
     *
     * @param s the message
     * @param e the exception
     */
    public void log(String s, Exception e) {
        getLogger().log(s, e);
    }

    /**
     * Gets logger.
     *
     * @return the logger
     */
    public EnvLogger getLogger() {
        return logger;
    }

    /**
     * The peer manager.
     */
    PeerManager pm;

}
