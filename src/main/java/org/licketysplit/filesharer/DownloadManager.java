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

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;

interface IsFinished {
    void setFinished(boolean isFinished);
}

public class DownloadManager implements Runnable {

    private FileAssembler fileAssembler;
    private HashMap<UserInfo, PeerDownloadInfo> peers;
    private ArrayList<Integer> necessaryAndAvailableChunks;
    private Environment env;
    private Thread assemblingThread;
    private int updateAfterTwenty;
    private boolean isCanceled;
    private boolean isFinished;
    private ReentrantLock lock;
    private UpdateDownloads updateDownloads;
    private ArrayList<Integer> completedChunks;

    public DownloadManager(FileInfo fileInfo, Environment env, UpdateDownloads updateDownloads) throws IOException {
        IsFinished isFinished = (boolean finish) -> {
            this.isFinished = finish;
        };
        this.fileAssembler = new FileAssembler(fileInfo, env, this.getLengthInChunks(fileInfo), isFinished);
        this.assemblingThread = new Thread(fileAssembler);
        this.assemblingThread.start();
        this.necessaryAndAvailableChunks = new ArrayList<Integer>();
        this.peers = new HashMap<UserInfo, PeerDownloadInfo>();
        this.env = env;
        this.updateAfterTwenty = 0;
        this.isCanceled = false;
        this.updateDownloads = updateDownloads;
        this.completedChunks = new ArrayList<Integer>();
    }

    @Override
    public void run() {
        try {
            PeerDownloadInfo peer;
            UserInfo user;
            int chunk;
            env.getLogger().log(Level.INFO, "Starting download manager thread");
            while(!isFinished || !isCanceled) {
                if ((user = this.getFreeUser()) != null) {
                    peer = this.getPeers().get(user);
                    chunk = peer.getRandomDesirableChunk(this.getNecessaryAndAvailableChunks());
                    if (chunk == -1) continue;
                    this.remove(chunk);
                    this.addToCompleted(chunk);
                    this.sendDownloadRequest(chunk, user, peer);
//                    if (this.updateAfterTwenty == 20) {
//                        this.updateAfterTwenty = 0;
//                        this.updatePeerList();
//                    } else {
//                        this.updateAfterTwenty++;
//                    }
                }
            }
            this.finish();
        } catch(Exception e){
            e.printStackTrace();
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

    private int getLengthInChunks(FileInfo fileInfo){
        double chunkLength = 1024.0;
        double preciseChunks = fileInfo.getLength() / chunkLength;
        return (int) Math.ceil(preciseChunks);
    }

    public ArrayList<Integer> getNecessaryAndAvailableChunks(){
        return this.necessaryAndAvailableChunks;
    }

    public void addPeerAndRequestChunkIfPossible(PeerChunkInfo peerInfo, SecureSocket socket, UserInfo userInfo) throws Exception {
        env.getLogger().log(String.format("Peer %s has chunk", userInfo.getUsername()));
        if(peerInfo.getChunks().size() == 0) return;
        this.peers.put(userInfo, new PeerDownloadInfo(peerInfo, socket)); //TODO(will) check if possible
        this.updateAvailableChunks(peerInfo);
        this.makeUserAvailable(userInfo);
    }

    public HashMap<UserInfo, PeerDownloadInfo> getPeers(){
        return this.peers;
    }

    public void remove(int chunk){
        this.necessaryAndAvailableChunks.remove(Integer.valueOf(chunk));
    }

    public void addToCompleted(int chunk){
        this.completedChunks.add(chunk);
    }

    public ArrayList<Integer> getCompletedChunks(){
        return this.completedChunks;
    }

    public void setUserToAvailable(UserInfo user){
        this.getPeers().get(user).setInUse(false);
    }

    public void makeUserAvailable(UserInfo userInfo) throws Exception {
        this.setUserToAvailable(userInfo);
    }

    public void updatePeerList() throws Exception {
        ConcurrentHashMap<UserInfo, SecureSocket> peers = this.env.getPm().getPeers();
        Set<UserInfo> newPeers = new HashSet<UserInfo>(peers.keySet());
        Set<UserInfo> currPeers = new HashSet(this.getPeers().keySet());
        newPeers.removeAll(currPeers);
        ArrayList<UserInfo> newPeersLs = new ArrayList<UserInfo>(newPeers);
        for (UserInfo peer: newPeersLs) {
            peers.get(peer).sendFirstMessage(new ChunkAvailabilityRequest(this.fileAssembler.getFileInfo()), new FileSharer.ChunkAvailabilityRequestHandler(this, peer));
        }
    }

    public void sendDownloadRequest(int chunk, UserInfo userInfo, PeerDownloadInfo peer) throws Exception {
        peer.getSocket().sendFirstMessage(new ChunkDownloadRequest(this.fileAssembler.getFileInfo(), chunk), new ChunkDownloadRequestHandler(chunk,  this, userInfo)); //need to close request and remove chunk
    }

    public void onChunkCompleted(int chunk, UserInfo userInfo) throws Exception {
        this.makeUserAvailable(userInfo);
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
        this.isCanceled = true;
    }

    private boolean isFinished() {
        return this.isFinished;
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
            //If error add chunk back to availableChunks.
            ChunkDownloadResponse decodedMessage = m.getMessage();
            if(this.dManager.isFinished()) return;
            this.dManager.getFileAssembler().saveChunk(decodedMessage.data, this.chunk);
            this.dManager.onChunkCompleted(this.chunk, this.userInfo);
        }
    }

    public void finish(){
        this.env.getLogger().log(Level.INFO, "FINISHED FILE " + this.fileAssembler.getFileInfo().getName());
        this.updateDownloads.update();
    }

}

