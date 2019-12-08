package org.licketysplit.filesharer;

import org.apache.commons.codec.digest.DigestUtils;
import org.licketysplit.env.Environment;
import org.licketysplit.env.Retrier;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkAvailabilityResponse;
import org.licketysplit.securesocket.*;
import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Callback function interface.
 */
interface UpdateDownloads{
    /**
     * Update.
     */
    void update();
}

/**
 * This class initializes and keeps track of downloads.
 * If you want to interact with a download in any way, you go through
 * this class.
 */
public class FileSharer {
    /**
     * This peer's environment.
     */
    private Environment env;
    /**
     * Map of downloads currently in progress.
     */
    private HashMap<String, DownloadManager> downloads;

    /**
     * Instantiates a new File sharer.
     */
    public FileSharer() {
        this.downloads = new HashMap<String, DownloadManager>();
    }

    /**
     * Sets env.
     *
     * @param env the env
     */
    public void setEnv(Environment env) {
        this.env = env;
    }

    /**
     * Getter for downloads in progress.
     *
     * @return the map of downloads in progress
     */
    public HashMap<String, DownloadManager> getDownloads(){
        return this.downloads;
    }


    /**
     * Response handler for ChunkAvailabilityRequests, updates the
     * download manager with the information about a peer having
     * a set of chunks.
     */
    public static class ChunkAvailabilityRequestHandler implements MessageHandler {
        /**
         * The download manager that sent out this request.
         */
        public DownloadManager dManager;
        /**
         * The peer it is being sent to.
         */
        public UserInfo userInfo;

        /**
         * Instantiates a new Chunk availability request handler.
         *
         * @param dManager the download manager
         * @param userInfo the peer
         */
        public ChunkAvailabilityRequestHandler(DownloadManager dManager, UserInfo userInfo) {
            this.dManager = dManager;
            this.userInfo = userInfo;
        }

        /**
         * Tells the download manager about the new information.
         * @param m the message
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkAvailabilityResponse decodedMessage = m.getMessage();
            PeerChunkInfo peerChunkInfo = decodedMessage.peerChunkInfo;
            dManager.addPeerAndRequestChunkIfPossible(peerChunkInfo, m.getConn(), this.userInfo);
        }
    }

    /**
     * Another map of downloads in progress.
     */
    private Map<String, DownloadManager> currentDownloads = new HashMap<>();

    /**
     * Check if a download is in progress.
     *
     * @param name the name of the file
     * @return if the download is in progress
     */
    public boolean isInProgress(String name) {
        synchronized (currentDownloads) {
            return currentDownloads.containsKey(name);
        }
    }

    /**
     * Get the download manager associated with an in progress download.
     *
     * @param name the name of the file
     * @return the download manager of this in progress download or null if not in progress
     */
    public DownloadManager currentProgress(String name) {
        synchronized (currentDownloads) {
            if(currentDownloads.containsKey(name))
            return currentDownloads.get(name);
            else return null;
        }
    }

    /**
     * Removes a download from the in-progress map.
     *
     * @param file the file being downloaded
     */
    public void cancelOrFinish(FileInfo file) {
        synchronized (currentDownloads) {
            if(currentDownloads.containsKey(file.name)) {
                env.log(String.format("Removing download reservation for '%s'", file.name));
                currentDownloads.remove(file.name);
            }else{
                env.log(String.format("Tried to remove reservation for nonexistent '%s'", file.name));
            }
        }
    }


    /**
     * Called when a download has failed, simply removes from in progress map.
     *
     * @param fileInfo the file being downloaded
     */
    public void failed(FileInfo fileInfo) {
        String name = fileInfo.name;
        cancelOrFinish(fileInfo);
    }

//    Retrier downloadRetrier = new Retrier(new int[]{5000, 10000, 15000, 20000, 30000}, 30000);

    /**
     * Starts a download for a given file in a new thread.
     *
     * @param fileInfo the file to download
     */
    public void download(FileInfo fileInfo){
        new Thread(() -> {
            try {
                Object doneLock = new Object();
                DownloadManager download = downloadWrapper(fileInfo, doneLock);
            } catch(Exception e) {
                env.getLogger().log(Level.INFO,
                        "Error in download watcher",
                        e);
            }
        }).start();
    }

    /**
     * Starts a download, assuming it is not already in progress.
     *
     * @param fileInfo the file to download
     * @param doneLock lock to notify when done
     * @return the download manager of this download
     * @throws Exception the exception
     */
    public DownloadManager downloadWrapper(FileInfo fileInfo, Object doneLock) throws Exception {
        DownloadManager dManager;
        // First check if the download is in progress, if it is, don't do anything and return
        synchronized (currentDownloads) {
            if(currentDownloads.containsKey(fileInfo.name)) {
                return null;
            }else{
                // If it's not in progress, instantiate a new download manager and save as in-progress
                env.log(String.format("Reserving download for '%s'", fileInfo.name));
                UpdateDownloads updateDownloads = () -> this.downloads.remove(fileInfo);
                dManager = new DownloadManager(fileInfo, this.env, updateDownloads, doneLock);
                currentDownloads.put(fileInfo.name, dManager);
            }
        }
        this.downloads.put(fileInfo.getName(), dManager);
        this.env.getLogger().log(Level.INFO, "DOWNLOADS SIZE: " + this.downloads.size());

        // Start the download manager thread
        Thread dThread = new Thread(dManager);
        dThread.start();

        // Send out chunk availability requests to all peers
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        for (Map.Entry<UserInfo, SecureSocket> peer : peers.entrySet()) {
            //log the chunks they have and send download request if applicable
            peer.getValue().sendFirstMessage(new ChunkAvailabilityRequest(fileInfo), new ChunkAvailabilityRequestHandler(dManager, peer.getKey()));
        }

        return dManager; // So UI has access to download manager (primarily for cancellation)
    }

    /**
     * Returns true if download is in progress for FileInfo.
     *
     * @param fileInfo the file info
     * @return if download is in progress
     */
    public boolean downloadInProgress(FileInfo fileInfo){
        synchronized(currentDownloads) {
            if(currentDownloads.containsKey(fileInfo.name) ) return true;
        }
        return false;
    }

    /**
     * Returns true if download is in progress for name.
     *
     * @param name the name of the file
     * @return if download is in progress
     */
    public boolean downloadInProgress(String name) {
        synchronized(currentDownloads) {
            if(currentDownloads.containsKey(name) ) return true;
        }
        return false;
    }

    /**
     * Gets the completed chunks for a given download, for use in chunk available request.
     *
     * @param fileInfo the file to check for
     * @return list of chunks that can be served for download
     */
    public ArrayList<Integer> getChunks(FileInfo fileInfo){
        return this.downloads.get(fileInfo.getName()).getCompletedChunks();
    }

    /**
     * No longer used.
     *
     * @param fileInfo the file info
     * @return the int
     */
    public int getChunksLength(FileInfo fileInfo){
        return this.downloads.containsKey(fileInfo.getName()) ? this.downloads.get(fileInfo.getName()).getCompletedChunks().size() : -1;
    }
}
