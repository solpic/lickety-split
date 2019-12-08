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

/**
 * This class is instantiated by the DownloadManager
 * and writes chunks to file as they are received in ChunkDownloadRequests/Responses.
 */
public class FileAssembler implements Runnable{
    /**
     * Manifest info about the file.
     */
    private FileInfo fileInfo;
    /**
     * Length of the file in chunks.
     */
    private int lengthInChunks;
    /**
     * Number of chunks that have been written.
     */
    private int numOfChunks;
    /**
     * The peer's environment.
     */
    private Environment env;

    /**
     * File handle.
     */
    private RandomAccessFile file;
    /**
     * Queue of chunks to be written.
     */
    private BlockingQueue<Chunk> chunks;
    /**
     * Chunks that have been written.
     */
    private HashSet<Integer> completed;
    /**
     * Callback function for when the download is finished.
     */
    private IsFinished isFinished;
    /**
     * Path of file we are downloading to.
     */
    public File downloadToPath;
    /**
     * DownloadManager that is controlling this FileAssembler.
     */
    DownloadManager mgr;

    /**
     * Instantiates a new File assembler.
     *
     * @param fileInfo       the file to be downloaded
     * @param env            this peer's Environment
     * @param lengthInChunks the length of the file in chunks
     * @param isFinished     the isFinished callback
     * @param mgr            the download manager handling this download
     * @throws Exception the exception
     */
    public FileAssembler(FileInfo fileInfo, Environment env, int lengthInChunks, IsFinished isFinished, DownloadManager mgr) throws Exception {
        this.mgr = mgr;
        this.env = env;
        this.fileInfo = fileInfo;
        this.lengthInChunks = lengthInChunks;
        this.numOfChunks = 0;

        // Create file
        File temp = new File(this.env.getFM().getSharedDirectoryPath(fileInfo.name));
        if(temp.exists())temp.delete();
        temp.createNewFile();
        downloadToPath = temp;

        // Get file handle
        this.file = FileAccessor.getFile(temp.getAbsolutePath());
        this.file.setLength(fileInfo.getLength()); // Set file length to desired file's length
        this.chunks = new LinkedBlockingQueue<Chunk>();
        this.completed = new HashSet<Integer>();
        this.isFinished = isFinished;
    }

    /**
     * Gets file info.
     *
     * @return the file info
     */
    public FileInfo getFileInfo() {
        return fileInfo;
    }

    /**
     * Variable that keeps track of when the last chunk was written to the file, for monitoring progress.
     */
    public AtomicLong lastChunkWritten;

    /**
     * Gets last chunk written time.
     *
     * @return the last chunk written time
     */
    long getLastChunkWrittenTime() {
        if(lastChunkWritten!=null) {
            return lastChunkWritten.get();
        }else{
            return -1;
        }
    }

    /**
     * This class is purely static and does two things:
     * First it makes sure that any file that is being accessed only uses one file handle,
     * since many different threads might be accessing a given file.
     *
     * Secondly, the first time a file is accessed it will check the MD5 digest of a file
     * and compare it to the manifest, if they aren't equal it will delete the file.
     * This way we can be doubly sure that a corrupt file isn't being stored.
     */
    public static class FileAccessor {
        private static AtomicBoolean hasInit = new AtomicBoolean(false);

        /**
         * Initialize static variables, only called once.
         */
        public static void init() {
            if(hasInit.get()) return;
            synchronized (hasInit) {
                if(hasInit.get()) return;
                files = new ConcurrentHashMap<>();
                startCloserThread();
                hasInit.set(true);
            }
        }

        /**
         * No longer used.
         */
        static void startCloserThread() {
            new Thread(() -> {

            }).start();
        }

        /**
         * Helper class to store the file handle and other metadata.
         */
        public static class RAFObj {
            /**
             * The RandomAccessFile file handle.
             */
            public RandomAccessFile f;
            /**
             * The time of last access to the file.
             */
            public long lastAccess;
            /**
             * The last modified time of the file during the last integrity check, if
             * this changes we need to do another integrity check.
             */
            public long lastIntegrityCheck = 0;

            /**
             * Instantiates a new Raf obj.
             */
            public RAFObj() {}
        }

        /**
         * Does an integrity check on a file using an MD5 digest.
         *
         * @param env  this peer's Environment
         * @param name the name of the file
         * @return whether the integrity check succeeded
         * @throws Exception the exception
         */
        static boolean integrityCheck(Environment env, String name) throws Exception {
            return env.getSyncManager().checkMD5AgainstManifest(name);
        }

        /**
         * Dictionary of open files.
         */
        private static ConcurrentHashMap<String, RAFObj> files;

        /**
         * Gets a file handle for a file, but also, if the file has changed since the last time there was
         * an MD5 check, check the MD5. If the MD5 indicates a corrupt file, delete the file.
         *
         * @param env  this peer's Environment
         * @param name the name of the file
         * @param path the path of the file
         * @return the file handle if the check succeeded
         * @throws Exception the exception
         */
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

        /**
         * Gets a file handle for a given file.
         *
         * @param path the path of the file
         * @return the file handle
         * @throws Exception the exception
         */
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

    /**
     * Keeps track of the total chunks for a file as well as how many chunks have been downloaded
     * We make them atomics so we don't have to lock them.
     */
    public AtomicLong totalChunks = new AtomicLong(0);
    /**
     * The number of chunks written so far.
     */
    public AtomicLong chunksDownloaded = new AtomicLong(0);

    /**
     * Gets size of a chunk.
     *
     * @return the size of chunks
     */
    public int chunkSize() {
        return DownloadManager.chunkLengthRaw;
    }

    /**
     * Mark chunk as having been written to the file, now it can be served in chunk download requests
     * and chunk availability requests.
     *
     * @param chunk the completed chunk
     */
    void addToCompleted(int chunk) {
        synchronized (completed) {
            completed.add(chunk);
        }
    }

    /**
     * Gets a list of the chunks that can be served in download or availability requests.
     *
     * @return list of completed chunks
     */
    ArrayList<Integer> getCompleted() {
        synchronized (completed) {
            return new ArrayList(completed.stream().collect(Collectors.toList()));
        }
    }

    /**
     * Starts the thread.
     */
    @Override
    public void run() {
        try {
            // Initialize variables
            lastChunkWritten = new AtomicLong(-1);
            totalChunks.set(lengthInChunks);
            chunksDownloaded.set(0);
            Chunk chunk;
            //consuming messages until exit message is received
            while (true) {
                // Block until there is a chunk in the BlockingQueue, then take it (get and remove)
                chunk = this.chunks.take();
                if (chunk != null) {
                    // A chunk with index -1 is a "poison pill" which cancels the thread
                    if(chunk.chunk == -1) return; //Download canceled

                    // Check first that chunk hasn't yet been written
                    if(!this.completed.contains(chunk.chunk)) {
                        // Check that the bytes aren't null, which would be a possible error condition
                        if(chunk.bytes!=null) {
                            // Lock file object and write chunk
                            synchronized(this.file) {
                                // Seek to address of chunk
                                this.file.seek(chunk.chunk * DownloadManager.chunkLengthRaw);
                                this.file.write(chunk.bytes);
                            }
                            // Mark chunk as completed and increment numOfChunks downloaded
                            addToCompleted(chunk.chunk);
                            this.numOfChunks++;
                            chunksDownloaded.set(this.numOfChunks);
                            // Update the lastChunkWritten time
                            lastChunkWritten.set(System.currentTimeMillis());
                            this.mgr.changed();
                        }else{
                            env.log("Chunk has null bytes");
                        }
                    }
                }
                // When there are no chunks remaining in the queue and the number
                // of written chunks is equal to the number of downloaded chunks
                // this means the download is complete
                if(this.chunks.isEmpty() && this.numOfChunks == this.lengthInChunks){
                    ArrayList<Integer> completedList = new ArrayList<Integer>(this.completed);
                    Collections.sort(completedList);
                    // Check that we have actually read all chunks,
                    // this should always be true
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

    /**
     * Adds chunk to the blocking queue.
     *
     * @param data  the raw bytes of this chunk
     * @param chunk the chunk index
     */
    public void saveChunk(byte[] data, int chunk){
        this.chunks.add(new Chunk(data, chunk));
    }

    /**
     * Add "poison pill" to blocking queue which will stop the thread.
     */
    public void cancel(){
        this.chunks.add(new Chunk(new byte[0], -1));
    }
}
