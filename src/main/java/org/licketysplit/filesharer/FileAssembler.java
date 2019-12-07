package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;
import org.licketysplit.syncmanager.FileInfo;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class FileAssembler implements Runnable{

    private FileInfo fileInfo;
    private int lengthInChunks;
    private int numOfChunks;
    private Environment env;
    private RandomAccessFile file;
    private BlockingQueue<Chunk> chunks;
    private HashSet<Integer> completed;
    private IsFinished isFinished;

    public File downloadToPath;
    DownloadManager mgr;
    public FileAssembler(FileInfo fileInfo, Environment env, int lengthInChunks, IsFinished isFinished, DownloadManager mgr) throws Exception {
        this.mgr = mgr;
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.numOfChunks = 0;
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        if(temp.exists())temp.delete();
        temp.createNewFile();
        downloadToPath = temp;
        this.file = FileAccessor.getFile(temp.getAbsolutePath());
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.chunks = new LinkedBlockingQueue<Chunk>();
        this.completed = new HashSet<Integer>();
        this.isFinished = isFinished;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public AtomicLong lastChunkWritten;
    long getLastChunkWrittenTime() {
        if(lastChunkWritten!=null) {
            return lastChunkWritten.get();
        }else{
            return -1;
        }
    }

    public static class FileAccessor {
        private static AtomicBoolean hasInit = new AtomicBoolean(false);
        public static void init() {
            if(hasInit.get()) return;
            synchronized (hasInit) {
                if(hasInit.get()) return;
                files = new ConcurrentHashMap<>();
                startCloserThread();
                hasInit.set(true);
            }
        }
        static void startCloserThread() {
            new Thread(() -> {

            }).start();
        }
        public static class RAFObj {
            public RandomAccessFile f;
            public long lastAccess;
            public long lastIntegrityCheck = 0;
            public RAFObj() {}
        }

        static boolean integrityCheck(Environment env, String name) throws Exception {
            return env.getSyncManager().checkMD5AgainstManifest(name);
        }

        private static ConcurrentHashMap<String, RAFObj> files;
        public static RandomAccessFile getFileAndCheckOK(Environment env, String name, String path) throws Exception {
            init();
            if(!files.containsKey(path)) {
                files.putIfAbsent(path, new RAFObj());
            }
            RAFObj raf = files.get(path);
            synchronized (raf) {
                File f = new File(path);
                long l = f.lastModified();
                if(l>raf.lastIntegrityCheck) {
                    if(!integrityCheck(env, name)) {
                        f.delete();
                        env.log(String.format("MD5 isn't good for file '%s', deleting", name));
                        throw new Exception("MD5 isn't good for file, deleting");
                    }else{
                        env.log(String.format("Confirmed MD5 for file '%s'", name));
                    }
                    raf.lastIntegrityCheck = l;
                }
                raf.lastAccess = System.currentTimeMillis();
                if(raf.f==null) {
                    raf.f = new RandomAccessFile(path, "rw");
                }
                return raf.f;
            }
        }

        public static RandomAccessFile getFile(String path) throws Exception {
            init();

            if(!files.containsKey(path)) {
                files.putIfAbsent(path, new RAFObj());
            }
            RAFObj raf = files.get(path);
            synchronized (raf) {
                raf.lastAccess = System.currentTimeMillis();
                if(raf.f==null) {
                    raf.f = new RandomAccessFile(path, "rw");
                }
                return raf.f;
            }
        }
    }

    public AtomicLong totalChunks = new AtomicLong(0);
    public AtomicLong chunksDownloaded = new AtomicLong(0);
    public int chunkSize() {
        return DownloadManager.chunkLengthRaw;
    }
    void addToCompleted(int chunk) {
        synchronized (completed) {
            completed.add(chunk);
        }
    }

    ArrayList<Integer> getCompleted() {
        synchronized (completed) {
            return new ArrayList(completed.stream().collect(Collectors.toList()));
        }
    }
    @Override
    public void run() {
        try {
            lastChunkWritten = new AtomicLong(-1);
            totalChunks.set(lengthInChunks);
            chunksDownloaded.set(0);
            Chunk chunk;
            //consuming messages until exit message is received
            while (true) {
                chunk = this.chunks.take();
                if (chunk != null) {
                    if(chunk.chunk == -1) return; //Download canceled
                    if(!this.completed.contains(chunk.chunk)) {
                        if(chunk.bytes!=null) {
                            synchronized(this.file) {
                                this.file.seek(chunk.chunk * DownloadManager.chunkLengthRaw);
                                this.file.write(chunk.bytes);
                            }
                            addToCompleted(chunk.chunk);
                            this.numOfChunks++;
                            chunksDownloaded.set(this.numOfChunks);
                            lastChunkWritten.set(System.currentTimeMillis());
                            this.mgr.changed();
                        }else{
                            env.log("Chunk has null bytes");
                        }
                    }
                }
                if(this.chunks.isEmpty() && this.numOfChunks == this.lengthInChunks){
                    ArrayList<Integer> completedList = new ArrayList<Integer>(this.completed);
                    Collections.sort(completedList);
                    for(int i = 0; i < completedList.size(); i++){
                        if(completedList.get(i) != i){
                            env.log("wrong " + i);
                            return;
                        }
                    }
                    this.isFinished.setFinished(true);
                    env.log("FINISHED AND PERFECT to "+downloadToPath);
                    return;
                }
            }
        } catch (InterruptedException | IOException e) {
            env.log("Error during assembler", e);
        }
    }

    public void saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
    }

    public void cancel(){
        this.chunks.add(new Chunk(new byte[0], -1)); //Add "poison pill" to blocking queue
    }
}
