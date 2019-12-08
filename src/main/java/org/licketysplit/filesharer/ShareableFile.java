package org.licketysplit.filesharer;
import org.licketysplit.env.Environment;

import java.io.*;

/**
 * Wrapper class to access files for serving chunks.
 */
public class ShareableFile extends File{
    /**
     * Size of a chunk.
      */
    private int chunkSize;
    /**
     * Filename.
     */
    private String name;

    /**
     * Instantiates a new Shareable file.
     *
     * @param name      the name of the file
     * @param pathname  the path of the file
     * @param chunkSize the chunk size
     */
    public ShareableFile(String name, String pathname, int chunkSize) {
        super(pathname);
        this.name = name;
        this.chunkSize = chunkSize;
    }

    /**
     * Reads a chunk from a file as a byte array.
     *
     * @param chunk the chunk
     * @param env   the env
     * @return the byte [ ]
     * @throws Exception the exception
     */
    public synchronized byte[] getChunk(int chunk, Environment env) throws Exception {
        int delay = 5000;
        /*
        Originally there was an issue where chunks would be stored in memory
        before being served in download requests, however it was changed to
        only read the chunk immediately before writing to the TCP socket,
        so catching OutOfMemoryErrors is no longer needed.
         */
        while(true) {
            try {
                // Calculate position of chunk
                int offset = 0;
                if (chunk > 0) offset = this.getOffset(chunk);
                // Size of chunk
                int spaceNeeded = this.getSpaceNeeded(chunk, offset);
                // Byte array to store chunk
                byte[] bytes = new byte[spaceNeeded];
                RandomAccessFile raf;
                /*
                If the download is in progress we simply get the file handle, however
                if a download is not in progress we want to make sure that an integrity
                check was done on the file at some point. The integrity check is only done once
                so this doesn't incur a performance hit, and seeding with corrupt files is a very
                bad thing.
                 */
                if(env.getFS().downloadInProgress(name))
                    raf = FileAssembler.FileAccessor.getFile(this.getAbsolutePath());
                else
                    raf = FileAssembler.FileAccessor.getFileAndCheckOK(env, name, this.getAbsolutePath());

                // Read the chunk to the byte array
                synchronized (raf) {
                    raf.seek(chunk * DownloadManager.chunkLengthRaw);
                    raf.readFully(bytes);
                }
                return bytes;
            } catch (OutOfMemoryError e) {
                // If there is an out of memory error wait and then retry
                env.log(
                        String.format("Out of memory while downloading chunk, waiting %d seconds", delay/1000)
                );
                Thread.sleep(delay);
                delay *= 2;
            }
        }
    }

    /**
     * Calculates the position of a chunk at index chunk
     * @param chunk index to calculate offset for
     * @return
     */
    private int getOffset(int chunk){
        long length = this.length();
        int offset = chunk * this.chunkSize;
        if(offset > length){
            return 0;
        }

        return offset;
    }

    /**
     * Calculates the size of this chunk if it is at the end of the file.
     * @param chunk index of chunk
     * @param offset position of chunk
     * @return
     */
    private int getSpaceNeeded(int chunk, long offset){
        if(offset + this.chunkSize > this.length()){
            return (int)(this.length() - offset);
        }

        return (int)this.chunkSize;
    }

}
