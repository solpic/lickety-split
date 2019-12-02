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

interface IsFinished {
    void setFinished(boolean isFinished);
}

public class DownloadManager implements Runnable {

    private FileAssembler fileAssembler;
    private ConcurrentHashMap<UserInfo, PeerDownloadInfo> peers;
    private ArrayList<Integer> necessaryAndAvailableChunks;
    private Environment env;
    private Thread assemblingThread;
    private int updateAfterTwenty;
    private AtomicBoolean isCanceled;
    private AtomicBoolean isFinished;
    private ReentrantLock lock;
    private UpdateDownloads updateDownloads;
    private ArrayList<Integer> completedChunks;
    private File downloadToPath;

    public DownloadManager(FileInfo fileInfo, Environment env, UpdateDownloads updateDownloads, Object doneLock) throws Exception {
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
        this.assemblingThread.start();
        this.necessaryAndAvailableChunks = new ArrayList<Integer>();
        this.peers = new ConcurrentHashMap<UserInfo, PeerDownloadInfo>();
        this.env = env;
        this.updateAfterTwenty = 0;
        this.isCanceled.set(false);
        this.updateDownloads = updateDownloads;
        this.completedChunks = new ArrayList<Integer>();
    }

    float speed = 0.0f;
    Object speedLock = new Object();
    public float getSpeed() {
        synchronized (speedLock) {
            return speed;
        }
    }

    void setSpeed(float f) {
        synchronized (speedLock) {
            speed = f;
        }
    }

    float progress = 0.0f;
    Object progressLock = new Object();
    public float getProgress() {
        synchronized (progressLock) {
            return progress;
        }
    }

    public void setProgress(float p) {
        synchronized (progressLock) {
            progress = p;
        }
    }


    public void changed() {

    }

    void startProgressWatcher() {
        int waitPeriod = 10000;
        String fname = fileAssembler.getFileInfo().getName();
        new Thread(() -> {
            int sleepTime = 1000;
            if(!env.getFM().triggerGUIChanges()) sleepTime = 2000;
            while(!isFinished.get() && !isCanceled.get()) {
                try {
                    long chunksDownloadedInitial = fileAssembler.chunksDownloaded.get();
                    Thread.sleep(sleepTime);
                    long chunksDownloaded = fileAssembler.chunksDownloaded.get();
                    long totalChunks = fileAssembler.totalChunks.get();

                    float progress = (float)chunksDownloaded/(float)totalChunks;
                    float speed = (float)((chunksDownloaded-chunksDownloadedInitial)*fileAssembler.chunkSize()*1000.0f)/((float)sleepTime);
                    setProgress(progress);
                    env.getLogger().trigger("progress", progress);
                    setSpeed(speed);
                    env.log(String.format(
                            "Download progress for '%s': %f%%, speed: %f KB/s",
                            fname, progress*100, speed/1024.0
                    ));
                    env.getFM().triggerDownloadUpdate(fileAssembler.getFileInfo());
                } catch(Exception e) {
                    env.getLogger().log(Level.INFO,
                            "Exception in progress watcher for "+fname, e);
                }
            }
            env.getFM().triggerGUIChanges();
        }).start();
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
                            failBecauseStopped();
                            return;
                        }
                } catch(Exception e) {
                    env.getLogger().log(Level.INFO,
                            "Exception in progress watcher for "+fname, e);
                }
            }
        }).start();
    }
    void failBecauseStopped() {
        if(!isFinished.get() && !isCanceled.get()) {
            isFinished.set(true);
        }
    }

    @Override
    public void run() {
        try {
            startProgressWatcher();
            PeerDownloadInfo peer;
            UserInfo user;
            int chunk;
            env.getLogger().log(Level.INFO, "Starting download manager thread");
            long updateTime = System.currentTimeMillis()+15000;
            pendingChunks = new ConcurrentHashMap<>();
            pendingCount = new AtomicLong(0);
            int counter = 0;
            while(!isFinished.get() && !isCanceled.get()) {
                if ((user = this.getFreeUser()) != null) {
                    peer = this.getPeers().get(user);
                    chunk = peer.getRandomDesirableChunk(this.getNecessaryAndAvailableChunks());
                    if (chunk >=0) {
                        this.remove(chunk);
                        this.addToCompleted(chunk);
                        this.sendDownloadRequest(chunk, user, peer);
                    }else {
                        counter++;
                    }
                }else{
                    counter++;
                }
                int maxPending = 1000;
                int chunkCancelMilliseconds = 10000;
                if(pendingCount.get()>maxPending||counter>200) {
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

    public FileAssembler getFileAssembler() {
        return fileAssembler;
    }

    private UserInfo getFreeUser(){
        List<UserInfo> shuffledList = new ArrayList<UserInfo>( this.getPeers().keySet() );
        Collections.shuffle( shuffledList );
        for (UserInfo user: shuffledList) {
           if(!this.getPeers().get(user).getInUse()){
               return user;
           }
        }
        return null;
    }

    private UserInfo getAnyUser() {
        List<UserInfo> shuffledList = new ArrayList<UserInfo>( this.getPeers().keySet() );
        return shuffledList.get(new Random(System.currentTimeMillis()).nextInt(shuffledList.size()));
    }

    public static final int chunkLengthRaw = 500*1024;//1024*500;

    private int getLengthInChunks(FileInfo fileInfo){
        double chunkLength = chunkLengthRaw;
        double preciseChunks = fileInfo.getLength() / chunkLength;
        return (int) Math.ceil(preciseChunks);
    }

    public ArrayList<Integer> getNecessaryAndAvailableChunks(){
        return this.necessaryAndAvailableChunks;
    }

    public void addPeerAndRequestChunkIfPossible(PeerChunkInfo peerInfo, SecureSocket socket, UserInfo userInfo) throws Exception {
        if(peerInfo.getChunks().size() == 0) {
            //env.getLogger().log(String.format("Peer %s has NO chunks", userInfo.getUsername()));
            return;
        }
        this.peers.put(userInfo, new PeerDownloadInfo(peerInfo, socket)); //TODO(will) check if possible
        this.updateAvailableChunks(peerInfo);
        this.makeUserAvailable(userInfo);
    }

    public ConcurrentHashMap<UserInfo, PeerDownloadInfo> getPeers(){
        return this.peers;
    }

    public void remove(int chunk){
        this.necessaryAndAvailableChunks.remove(Integer.valueOf(chunk));
    }

    ConcurrentHashMap<Integer, Long> pendingChunks;
    AtomicLong pendingCount;

    public void addToCompleted(int chunk){
        this.completedChunks.add(chunk);
        this.pendingChunks.put(chunk, System.currentTimeMillis());
        pendingCount.set(pendingChunks.size());
    }

    public ArrayList<Integer> getCompletedChunks(){
        return this.fileAssembler.getCompleted();
    }

    public void setUserToAvailable(UserInfo user){
        this.getPeers().get(user).setInUse(false);
    }

    public void makeUserAvailable(UserInfo userInfo) throws Exception {
        this.setUserToAvailable(userInfo);
    }

    public void updatePeerList() throws Exception {
        env.log("Updating peer list for download");
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        Set<UserInfo> newPeers = new HashSet<UserInfo>(peers.keySet());
        Set<UserInfo> currPeers = new HashSet(this.getPeers().keySet());
        newPeers.removeAll(currPeers);
        ArrayList<UserInfo> newPeersLs = new ArrayList<UserInfo>(newPeers);
        for (UserInfo peer: peers.keySet()) {
            peers.get(peer).sendFirstMessage(new ChunkAvailabilityRequest(this.fileAssembler.getFileInfo()), new FileSharer.ChunkAvailabilityRequestHandler(this, peer));
        }
    }

    public void sendDownloadRequest(int chunk, UserInfo userInfo, PeerDownloadInfo peer) throws Exception {
        //env.log(String.format("Requesting chunk %d from %s", chunk, peer.getSocket().getPeerAddress().getUser().getUsername()));
        peer.getSocket().sendFirstMessage(new ChunkDownloadRequest(this.fileAssembler.getFileInfo(), chunk), new ChunkDownloadRequestHandler(chunk,  this, userInfo)); //need to close request and remove chunk
    }

    public void onChunkCompleted(int chunk, UserInfo userInfo) throws Exception {
        this.makeUserAvailable(userInfo);
        this.pendingChunks.remove(chunk);
        this.pendingCount.set(pendingChunks.size());
    }

    public void updateAvailableChunks(PeerChunkInfo peerChunkInfo){
        ArrayList<Integer> newChunks = peerChunkInfo.getChunks();
        Set<Integer> chunks = new HashSet<Integer>(this.necessaryAndAvailableChunks);
        for(int i = 0; i < newChunks.size(); i++){
            chunks.add(newChunks.get(i));
        }
        this.necessaryAndAvailableChunks = new ArrayList<Integer>(chunks);
    }

    public void cancelDownload(){
        this.fileAssembler.cancel();
        this.isCanceled.set(true);
    }

    private boolean isFinished() {
        return this.isFinished.get();
    }

    public static class ChunkDownloadRequestHandler implements MessageHandler {
        public int chunk;
        public DownloadManager dManager;
        public UserInfo userInfo;

        public ChunkDownloadRequestHandler(int chunk, DownloadManager dManager, UserInfo userInfo){
            this.chunk = chunk;
            this.dManager = dManager;
            this.userInfo = userInfo;
        }

        @Override
        public void handle(ReceivedMessage m) throws Exception {
//            m.getEnv().log("Received chunk response");
            //If error add chunk back to availableChunks.
            ChunkDownloadResponse decodedMessage = m.getMessage();
            if(this.dManager.isFinished()) return;
            this.dManager.getFileAssembler().saveChunk(decodedMessage.data, this.chunk);
            this.dManager.onChunkCompleted(this.chunk, this.userInfo);
        }
    }

    public boolean hasFailed = false;
    public Object doneLock;
    public void finish() throws Exception{
        String md5 = env.getSyncManager().getMD5(downloadToPath);
        String compMD5 = this.fileAssembler.getFileInfo().md5;
        this.fileAssembler.cancel();
        boolean md5Equals = md5.equals(compMD5);
        if(true&&!isCanceled.get()) {
            this.env.getLogger().log(Level.INFO, "FINISHED FILE " + this.fileAssembler.getFileInfo().getName());
            this.env.getLogger().trigger("download-complete", this.fileAssembler.getFileInfo().getName());
            this.updateDownloads.update();
            env.getFS().cancelOrFinish(this.fileAssembler.getFileInfo());
        }else if(!isCanceled.get()){
            this.hasFailed = true;
            env.getFS().failed(this.fileAssembler.getFileInfo());
            this.env.getLogger().log(String.format("" +
                    "MD5's don't match, theirs: %s, ours %s, file download failed for %s",
                    compMD5, md5, this.fileAssembler.getFileInfo().getName()));
        }else{
            env.getLogger().log("File download cancelled");
        }

        env.getFM().triggerDownloadUpdate(fileAssembler.getFileInfo());
        synchronized (doneLock) {
            doneLock.notifyAll();
        }
    }

}

