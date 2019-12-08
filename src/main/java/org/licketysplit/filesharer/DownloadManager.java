package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.filesharer.messages.ChunkAvailabilityRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadRequest;
import org.licketysplit.filesharer.messages.ChunkDownloadResponse;
import org.licketysplit.securesocket.SecureSocket;

import org.licketysplit.securesocket.messages.MessageHandler;
import org.licketysplit.securesocket.messages.ReceivedMessage;
import org.licketysplit.securesocket.peers.UserInfo;
import org.licketysplit.syncmanager.FileInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

/**
 * Callback interface to finish download.
 */
interface IsFinished {
    /**
     * Sets finished.
     *
     * @param isFinished the is finished
     */
    void setFinished(boolean isFinished);
}

/**
 * This class downloads a file over the network. For every in-progress download,
 * there is one instance of this class.
 *
 * Concisely, the download manager sends out chunk availability requests to
 * find out which peers have what chunks, then sends out chunk download requests
 * to download each chunk. Then the FileAssembler object actually writes the chunks
 * to the file.
 *
 * Further implementation details will be provided next to each method.
 */
public class DownloadManager implements Runnable {

    /**
     * Class that actually writes each chunk to the target file.
     */
    private FileAssembler fileAssembler;
    /**
     * Stores which peers have which chunks.
     */
    private ConcurrentHashMap<UserInfo, PeerDownloadInfo> peers;
    /**
     * List of which chunks we need to complete our download that are also available (i.e. some peer has them).
     */
    private ArrayList<Integer> necessaryAndAvailableChunks;
    /**
     * This peer's env.
     */
    private Environment env;
    /**
     * FileAssembler thread.
     */
    private Thread assemblingThread;
    /**
     * No longer used.
     */
    private int updateAfterTwenty;

    /**
     * Keeps track of whether the download has been cancelled.
     */
    private AtomicBoolean isCanceled;
    /**
     * Keeps track of whether the download has finished.
     */
    private AtomicBoolean isFinished;

    /**
     * No longer used.
     */
    private ReentrantLock lock;

    /**
     * Callback function for status updates on the download.
     */
    private UpdateDownloads updateDownloads;
    /**
     * List of chunks that have been received and don't need to be downloaded.
     */
    private ArrayList<Integer> completedChunks;
    /**
     * Path of the file the chunks are being written to.
     */
    private File downloadToPath;

    /**
     * Instantiates a new Download manager.
     *
     * @param fileInfo        the file to be downloaded
     * @param env             this peer's env
     * @param updateDownloads the update callback
     * @param doneLock        the lock to notify when done
     * @throws Exception the exception
     */
    public DownloadManager(FileInfo fileInfo, Environment env, UpdateDownloads updateDownloads, Object doneLock) throws Exception {
        // Initialize instance variables
        this.doneLock = doneLock;
        IsFinished isFinished = (boolean finish) -> {
            this.isFinished.set(finish);
        };
        this.isFinished = new AtomicBoolean(false);
        this.isCanceled = new AtomicBoolean(false);
        this.isFinished.set(false);
        this.fileAssembler = new FileAssembler(fileInfo, env, this.getLengthInChunks(fileInfo), isFinished, this);
        this.assemblingThread = new Thread(fileAssembler);
        this.downloadToPath = fileAssembler.downloadToPath;

        // Start file assembler thread
        this.assemblingThread.start();
        this.necessaryAndAvailableChunks = new ArrayList<Integer>();
        this.peers = new ConcurrentHashMap<UserInfo, PeerDownloadInfo>();
        this.env = env;
        this.updateAfterTwenty = 0;
        this.isCanceled.set(false);
        this.updateDownloads = updateDownloads;
        this.completedChunks = new ArrayList<Integer>();
    }

    /**
     * Used by the GUI to get the current speed of the download.
     */
    float speed = 0.0f;
    /**
     * The Speed lock.
     */
    Object speedLock = new Object();

    /**
     * Gets speed of the download in bytes/second.
     *
     * @return the speed
     */
    public float getSpeed() {
        synchronized (speedLock) {
            return speed;
        }
    }

    /**
     * Sets speed of download in bytes per second.
     *
     * @param f download speed
     */
    void setSpeed(float f) {
        synchronized (speedLock) {
            speed = f;
        }
    }

    /**
     * Current download progress from 0-1.
     */
    float progress = 0.0f;
    /**
     * Lock for progress.
     */
    Object progressLock = new Object();

    /**
     * Used by the GUI to get the current progress of the download.
     *
     * @return the download progress from 0.0-1.0
     */
    public float getProgress() {
        synchronized (progressLock) {
            return progress;
        }
    }

    /**
     * Sets the current progress of the download.
     *
     * @param p the progress
     */
    public void setProgress(float p) {
        synchronized (progressLock) {
            progress = p;
        }
    }

    /**
     * No longer used.
     */
    public void changed() {

    }

    /**
     * Starts a thread that calculates and stores the progress and speed of the download,
     * for use by the GUI.
     */
    void startProgressWatcher() {
        int waitPeriod = 10000;
        String fname = fileAssembler.getFileInfo().getName();
        new Thread(() -> {
            int sleepTime = 1000;
            if(!env.getFM().triggerGUIChanges()) sleepTime = 2000;

            // We want to only be running this while it is not finished or cancelled
            while(!isFinished.get() && !isCanceled.get()) {
                try {
                    /*
                    Calculate the progress as chunksDownloaded/totalChunks.
                    Speed is calculated by getting the current chunks downloaded,
                    waiting 1 second, then getting the current chunks downloaded.
                    Then we calculate the speed as (currentChunksDownloaded-initialChunksDownloaded)*chunkSize
                     */
                    long chunksDownloadedInitial = fileAssembler.chunksDownloaded.get();
                    Thread.sleep(sleepTime);
                    long chunksDownloaded = fileAssembler.chunksDownloaded.get();
                    long totalChunks = fileAssembler.totalChunks.get();

                    float progress = (float)chunksDownloaded/(float)totalChunks;
                    float speed = (float)((chunksDownloaded-chunksDownloadedInitial)*fileAssembler.chunkSize()*1000.0f)/((float)sleepTime);

                    // Updates the speed and progress variables
                    setProgress(progress);
                    env.getLogger().trigger("progress", progress);
                    setSpeed(speed);
                    env.log(String.format(
                            "Download progress for '%s': %f%%, speed: %f KB/s",
                            fname, progress*100, speed/1024.0
                    ));

                    // Triggers the GUI to update the speed and progress display
                    env.getFM().triggerDownloadUpdate(fileAssembler.getFileInfo());
                } catch(Exception e) {
                    env.getLogger().log(Level.INFO,
                            "Exception in progress watcher for "+fname, e);
                }
            }
            env.getFM().triggerGUIChanges();
        }).start();
        /*
        Originally, this thread would monitor the download and if no progress has been
        made for some period of time (maybe 30 seconds) it would restart the download.
        This is no longer needed, and right now it no longer does anything.
         */
        int stopAfterTime = 30000;
        new Thread(() -> {
            long lastChunkWritten = System.currentTimeMillis();
            long chunksDownloaded = fileAssembler.chunksDownloaded.get();
            while(!isFinished.get() && !isCanceled.get()) {
                try {
                        Thread.sleep(20000);
                        long nChunks = fileAssembler.chunksDownloaded.get();
                        if(nChunks>chunksDownloaded) {
                            chunksDownloaded = nChunks;
                        }else{
                            //failBecauseStopped();
                            //return;
                        }
                } catch(Exception e) {
                    env.getLogger().log(Level.INFO,
                            "Exception in progress watcher for "+fname, e);
                }
            }
        }).start();
    }

    /**
     * This is not used anymore, but was used to cancel the download when progress had stopped
     * for some period of time.
     */
    void failBecauseStopped() {
        if(!isFinished.get() && !isCanceled.get()) {
            isFinished.set(true);
        }
    }

    /**
     * Helper class to represent the next chunk to be downloaded.
     */
    class PeerChunk {
        /**
         * Stores which chunks a peer has.
         */
        PeerDownloadInfo info;
        /**
         * Index of the chunk to be downloaded.
         */
        int chunk;
        /**
         * Metadata about this peer.
         */
        UserInfo user;
    }

    /**
     * Random number generator.
     */
    Random rand = new Random();
    /**
     * List of peers, cached so we don't have to constantly ask the peer manager.
     */
    ArrayList<UserInfo> peerList = new ArrayList<>();
    /**
     * shuffleCount keeps track of how many times getNextPeerChunk has been called
     * and every shuffleMod (100) times it has been called, it randomly shuffles
     * the peerList variable, so that we aren't favoring certain peers when asking
     * for chunks.
     */
    long shuffleCount = 0;
    /**
     * The Shuffle mod.
     */
    long shuffleMod = 100;

    /**
     * This function selects the next chunk to be requested from a peer in a ChunkDownloadRequest.
     * It attempts to apply a fair amount of randomization to this because that will prevent
     * specific peers from getting too many download requests, and will also help seed the network
     * with ALL the chunks from the file, so that everyone can help with the upload..
     *
     * @return the next chunk to download
     * @throws Exception the exception
     */
    PeerChunk getNextPeerChunk() throws Exception {
        synchronized (this.peers) {
            ArrayList<Integer> necessaryAndAvailableChunks = this.getNecessaryAndAvailableChunks();
            // We can only select a chunk if there is a chunk to be selected
            if(necessaryAndAvailableChunks.size()>0) {
                // Pick a random chunk that we need and that some peer has
                int next = necessaryAndAvailableChunks.get(rand.nextInt(necessaryAndAvailableChunks.size()));

                // The index should be >=0
                if (next >= 0) {
                    // Shuffle the peer list periodically so that we don't favor certain peers
                    if(shuffleCount%shuffleMod==0) {
                        Collections.shuffle(peerList);
                    }
                    // Iterate through the peer list and find a peer who has this chunk
                    // then return a PeerChunk object
                    for (UserInfo user : peerList) {
                        if(peers.containsKey(user)) {
                            PeerDownloadInfo i = peers.get(user);
                            if (i.hasChunk(next)) {
                                PeerChunk peerChunk = new PeerChunk();
                                peerChunk.info = i;
                                peerChunk.chunk = next;
                                peerChunk.user = user;

                                peerList.remove(user);
                                peerList.add(user);
                                return peerChunk;
                            }
                        }
                    }

                    // If we've gotten here than no peers have the chunk, this shouldn't be reached
                    env.getLogger().log("Couldn't find chunk because no peers have it");
                }else{
                    // For some reason the chunk index is less than 0
                    env.getLogger().log("Couldn't find chunk because next returned -1");
                }
            }
        }
        // null means we shouldn't request any chunk right now
        return null;
    }

    /**
     * Main thread of the DownloadManager.
     * Sends out ChunkAvailabilityRequests and ChunkDownloadRequests as needed.
     */
    @Override
    public void run() {
        try {
            // Start the thread that will monitor progress for the GUI
            startProgressWatcher();
            PeerDownloadInfo peer;
            UserInfo user;
            int chunk;
            env.getLogger().log(Level.INFO, "Starting download manager thread");
            long updateTime = System.currentTimeMillis()+15000;

            // These two variables will keep track of which chunks have been requested and how many
            pendingChunks = new ConcurrentHashMap<>();
            pendingCount = new AtomicLong(0);
            int counter = 0;

            // Sends out chunk availability requests
            updatePeerList();

            // Keep going as long as the download hasn't finished or been cancelled
            while(!isFinished.get() && !isCanceled.get()) {
                // Get next chunk to be requested
                PeerChunk next = getNextPeerChunk();
                if(next!=null) {
                    // Remove from future potential chunk requests
                    this.remove(next.chunk);
                    // Add to completed and pending chunks
                    this.addToCompleted(next.chunk);
                    // Send the chunk download request
                    this.sendDownloadRequest(next.chunk, next.user, next.info);
                }else{
                    // For whatever reason, no chunk needs to be downloaded
                    // So we will sleep for a little bit so we don't spin
                    counter++;
                    Thread.sleep(500);
                }
                int maxPending = 1000;
                int chunkCancelMilliseconds = 10000;

                // Here we check if there is more than 1000 pending chunk download requests
                // or if 15 seconds have gone by without sending out a new request
                if(pendingCount.get()>maxPending||counter>30) {
                    // If so, we will remove chunks that have been pending for more than 10 seconds
                    // from the completed list and add it back to the necessary chunks list
                    // So then a new request will be sent out for the chunk
                    counter = 0;
                    Thread.sleep(5000);
                    boolean changed = false;
                    Set<Map.Entry<Integer, Long>> entries = pendingChunks.entrySet();
                    long curTime = System.currentTimeMillis();
                    for (Map.Entry<Integer, Long> entry : entries) {
                        if (entry.getValue() + chunkCancelMilliseconds < curTime) {
                            changed = true;
                            this.getNecessaryAndAvailableChunks().remove(entry.getKey());
                            this.pendingChunks.remove(entry.getKey());
                            this.pendingCount.set(pendingChunks.size());
                        }
                    }
                }

                // If 15 seconds have gone by in this thread then send out chunk availability requests
                // because it is very likely some peers have more chunks available
                if (System.currentTimeMillis()>updateTime) {
                    this.updatePeerList();
                    updateTime = System.currentTimeMillis()+15000;
                }
            }
            env.log(String.format("Download manager done, finished: %b, Cancelled: %b", isFinished.get(), isCanceled.get()));
            this.finish();
        } catch(Exception e){
            env.log("Error during download manager", e);
        }
    }

    /**
     * Gets file assembler.
     *
     * @return the file assembler
     */
    public FileAssembler getFileAssembler() {
        return fileAssembler;
    }

    /**
     * Size of chunks, right now it is 500kb.
     */
    public static final int chunkLengthRaw = 500*1024;//1024*500;

    /**
     * Calculate the number of chunks in a file
     * @param fileInfo the file
     * @return
     */
    private int getLengthInChunks(FileInfo fileInfo){
        double chunkLength = chunkLengthRaw;
        double preciseChunks = fileInfo.getLength() / chunkLength;
        return (int) Math.ceil(preciseChunks);
    }

    /**
     * Getter for chunks needed and available.
     *
     * @return the list of chunks
     */
    public ArrayList<Integer> getNecessaryAndAvailableChunks(){
        return this.necessaryAndAvailableChunks;
    }

    /**
     * Adds peer to the cached peer list and adds this peers chunks to the necessaryAndAvailableChunks list.
     *
     * @param peerInfo stores which chunks a peer has
     * @param socket   the socket for this peer
     * @param userInfo metadata about the peer
     * @throws Exception the exception
     */
    public void addPeerAndRequestChunkIfPossible(PeerChunkInfo peerInfo, SecureSocket socket, UserInfo userInfo) throws Exception {
        if(peerInfo.getChunks().size() == 0) {
            //env.getLogger().log(String.format("Peer %s has NO chunks", userInfo.getUsername()));
            return;
        }
        synchronized (peers) {
            this.peers.put(userInfo, new PeerDownloadInfo(peerInfo, socket)); //TODO(will) check if possible
        }
        this.updateAvailableChunks(peerInfo);
        //this.makeUserAvailable(userInfo);
    }

    /**
     * Get peers map.
     *
     * @return the peers map
     */
    public ConcurrentHashMap<UserInfo, PeerDownloadInfo> getPeers(){
        return this.peers;
    }

    /**
     * Remove chunk from list.
     *
     * @param chunk the chunk
     */
    public void remove(int chunk){
        this.necessaryAndAvailableChunks.remove(Integer.valueOf(chunk));
    }

    /**
     * Keeps track of pending chunks, i.e. chunks that have been requested for download but
     * that request hasn't been filled yet.
     */
    ConcurrentHashMap<Integer, Long> pendingChunks;
    /**
     * The number of chunks pending.
     */
    AtomicLong pendingCount;

    /**
     * Mark chunk as having been sent out for download request.
     *
     * @param chunk the chunk
     */
    public void addToCompleted(int chunk){
        this.completedChunks.add(chunk);
        this.pendingChunks.put(chunk, System.currentTimeMillis());
        pendingCount.set(pendingChunks.size());
    }

    /**
     * Gets a list of chunks that have been written to the file
     * From the file assembler. These are chunks that we can serve up in a chunkdownloadrequest
     * or chunkavailabilityrequest.
     *
     * @return the list of chunks
     */
    public ArrayList<Integer> getCompletedChunks(){
        return this.fileAssembler.getCompleted();
    }

    /**
     * No longer used.
     *
     * @param user the user
     */
    public void setUserToAvailable(UserInfo user){
        this.getPeers().get(user).setInUse(false);
    }

    /**
     * No longer used.
     *
     * @param userInfo the user info
     * @throws Exception the exception
     */
    public void makeUserAvailable(UserInfo userInfo) throws Exception {
        this.setUserToAvailable(userInfo);
    }

    /**
     * Sends out chunk availability requests to every peer in the peer list.
     *
     * @throws Exception the exception
     */
    public void updatePeerList() throws Exception {
        env.log("Updating peer list for download");
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        Set<UserInfo> newPeers = new HashSet<UserInfo>(peers.keySet());
        Set<UserInfo> currPeers = new HashSet(this.getPeers().keySet());
        newPeers.removeAll(currPeers);
        ArrayList<UserInfo> newPeersLs = new ArrayList<UserInfo>(newPeers);
        for (UserInfo peer: peers.keySet()) {
            if(!peerList.contains(peer)) peerList.add(peer);
            peers.get(peer).sendFirstMessage(new ChunkAvailabilityRequest(this.fileAssembler.getFileInfo()), new FileSharer.ChunkAvailabilityRequestHandler(this, peer));
        }
    }

    /**
     * Sends a download request for an indicated chunk to an indicated peer.
     *
     * @param chunk    the chunk to download
     * @param userInfo metadata about peer
     * @param peer     peer chunk availability info
     * @throws Exception the exception
     */
    public void sendDownloadRequest(int chunk, UserInfo userInfo, PeerDownloadInfo peer) throws Exception {
        //env.log(String.format("Requesting chunk %d from %s", chunk, peer.getSocket().getPeerAddress().getUser().getUsername()));
        peer.getSocket().sendFirstMessage(new ChunkDownloadRequest(this.fileAssembler.getFileInfo(), chunk), new ChunkDownloadRequestHandler(chunk,  this, userInfo)); //need to close request and remove chunk
    }

    /**
     * Called when a chunk download request has been filled.
     *
     * @param chunk    the chunk
     * @param userInfo metadata about peer
     * @throws Exception the exception
     */
    public void onChunkCompleted(int chunk, UserInfo userInfo) throws Exception {
        this.makeUserAvailable(userInfo);
        this.pendingChunks.remove(chunk);
        this.pendingCount.set(pendingChunks.size());
    }

    /**
     * Updates the necessaryAndAvailableChunks list with new info about available chunks.
     *
     * @param peerChunkInfo the peer chunk info
     */
    public synchronized void updateAvailableChunks(PeerChunkInfo peerChunkInfo){
        ArrayList<Integer> newChunks = peerChunkInfo.getChunks();
        Set<Integer> chunks = new HashSet<Integer>(this.necessaryAndAvailableChunks);
        for(int i = 0; i < newChunks.size(); i++){
            chunks.add(newChunks.get(i));
        }
        this.necessaryAndAvailableChunks = new ArrayList<Integer>(chunks);
    }

    /**
     * Cancel download.
     */
    public void cancelDownload(){
        this.fileAssembler.cancel();
        this.isCanceled.set(true);
    }

    /**
     * Checks if the download is finished.
     * @return if finished
     */
    private boolean isFinished() {
        return this.isFinished.get();
    }

    /**
     * Response handler for chunk download requests. Tells the file assembler
     * to save the chunk and tells the download manager that the chunk
     * has been received.
     */
    public static class ChunkDownloadRequestHandler implements MessageHandler {
        /**
         * The index of the chunk.
         */
        public int chunk;
        /**
         * The download manager who made the request.
         */
        public DownloadManager dManager;
        /**
         * Metadata about the peer.
         */
        public UserInfo userInfo;

        /**
         * Instantiates a new Chunk download request handler.
         *
         * @param chunk    the chunk
         * @param dManager the download manager
         * @param userInfo the user info
         */
        public ChunkDownloadRequestHandler(int chunk, DownloadManager dManager, UserInfo userInfo){
            this.chunk = chunk;
            this.dManager = dManager;
            this.userInfo = userInfo;
        }

        /**
         * Called when response is received
         * @param m the received message
         * @throws Exception
         */
        @Override
        public void handle(ReceivedMessage m) throws Exception {
            ChunkDownloadResponse decodedMessage = m.getMessage();
            // If download is done do nothing
            if(this.dManager.isFinished()) return;

            // Otherwise tell the file assembler to write the chunk and the download manager
            // that the chunk has been received
            this.dManager.getFileAssembler().saveChunk(decodedMessage.data, this.chunk);
            this.dManager.onChunkCompleted(this.chunk, this.userInfo);
        }
    }

    /**
     * True if download failed.
     */
    public boolean hasFailed = false;
    /**
     * Lock that is notified when download is complete.
     */
    public Object doneLock;

    /**
     * Called when the download is cancelled/finished.
     *
     * @throws Exception the exception
     */
    public void finish() throws Exception{
        // Get what the MD5 hash should be from the manifest
        String md5 = env.getSyncManager().getMD5(downloadToPath);
        // Calculate the MD5 hash of what we have downloaded
        String compMD5 = this.fileAssembler.getFileInfo().md5;
        // Tell the file assembler thread to stop
        this.fileAssembler.cancel();
        // Check if the md5's are equal
        boolean md5Equals = md5.equals(compMD5);
        this.env.log(String.format("Finished: MD5 correct? -> %b", md5Equals));

        if(md5Equals&&!isCanceled.get()) {
            // If MD5s are equal and the download wasn't cancelled, success!
            this.env.getLogger().log(Level.INFO, "FINISHED FILE " + this.fileAssembler.getFileInfo().getName());
            this.env.getLogger().trigger("download-complete", this.fileAssembler.getFileInfo().getName(), this.fileAssembler.totalChunks.get());
            this.updateDownloads.update();
            env.getFS().cancelOrFinish(this.fileAssembler.getFileInfo());
        }else if(!isCanceled.get()){
            // If the MD5s aren't equal, this means the download was corrupted somehow
            this.hasFailed = true;
            downloadToPath.delete();
            env.getFS().failed(this.fileAssembler.getFileInfo());
            this.env.getLogger().trigger("download-failed", this.fileAssembler.getFileInfo().getName(), this.fileAssembler.totalChunks.get());
            this.env.getLogger().log(String.format("" +
                    "MD5's don't match, theirs: %s, ours %s, file download failed for %s",
                    compMD5, md5, this.fileAssembler.getFileInfo().getName()));
            this.env.changes.runHandler("download-failed", this.fileAssembler.getFileInfo().getName());
        }else{
            // If the download is cancelled
            env.getLogger().log("File download cancelled");
            this.env.getLogger().trigger("download-cancelled", this.fileAssembler.getFileInfo().getName(), this.fileAssembler.totalChunks.get());
            File downloadToPath = this.fileAssembler.downloadToPath;
            downloadToPath.delete();
            env.getFS().cancelOrFinish(this.fileAssembler.getFileInfo());
        }

        // Trigger changes in the GUI and notify any thread that has been waiting on the download to finish
        env.getFM().triggerDownloadUpdate(fileAssembler.getFileInfo());
        synchronized (doneLock) {
            doneLock.notifyAll();
        }
    }

}

