package org.licketysplit.syncmanager;

import org.json.JSONObject;
import org.licketysplit.securesocket.messages.JSONMessage;

import java.io.File;
import java.util.Date;

/**
 * Stores metadata about a file, based on the manifest.
 */
public class FileInfo {

    /**
     * Gets name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets name.
     *
     * @param name the name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets length.
     *
     * @return the length
     */
    public long getLength() {
        return length;
    }

    /**
     * Sets length.
     *
     * @param length the length
     */
    public void setLength(long length) {
        this.length = length;
    }

    /**
     * Gets time stamp.
     *
     * @return the time stamp
     */
    public long getTimeStamp() {
        return timeStamp;
    }

    /**
     * Sets time stamp.
     *
     * @param timeStamp the time stamp
     */
    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    /**
     * True if the file is deleted.
     *
     * @return the boolean
     */
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets deleted.
     *
     * @param deleted the deleted
     */
    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * The name of the file.
     */
    public String name;
    /**
     * The length of the file.
     */
    public long length;
    /**
     * The timestamp when this entry was last modified, so that the most recent version can be used.
     */
    public long timeStamp;
    /**
     * True if the file has been deleted.
     */
    public boolean deleted;
    /**
     * The MD5 digest of the file.
     */
    public String md5;

    /**
     * Gets MD5
     *
     * @return the MD5
     */
    public String getMd5() {
        return md5;
    }

    /**
     * Sets MD5.
     *
     * @param md5 the MD5
     */
    public void setMd5(String md5) {
        this.md5 = md5;
    }

    /**
     * Instantiates a new File info.
     */
    public FileInfo(){}

    /**
     * Instantiates a new File info.
     *
     * @param file      the file
     * @param timeStamp the timestamp
     */
    public FileInfo(File file, long timeStamp) { // For when you're updating or adding a new file
        this.name = file.getName();
        this.length = file.length();
        this.timeStamp = timeStamp;
        this.deleted = false;
    }

    /**
     * Instantiates a new File info.
     *
     * @param fileName the file name
     * @param deleted  true if deleted
     */
    public FileInfo(String fileName, boolean deleted){
        this.name = fileName;
        this.length = 0;
        this.timeStamp = new Date().getTime();
        this.deleted = deleted;
    }

    /**
     * Instantiates a new File info.
     *
     * @param fileName the file name
     * @param deleted  true if deleted
     * @param length   the length
     */
    public FileInfo(String fileName, boolean deleted, int length){
        //For testing
        this.name = fileName;
        this.length = length;
        this.timeStamp = new Date().getTime();
        this.deleted = deleted;
    }

    /**
     * Instantiates a new File info from a JSONObject.
     *
     * @param info the info
     */
    public FileInfo(JSONObject info){
        this.name = info.getString("name");
        this.length = info.getLong("length");
        this.timeStamp = info.getLong("timeStamp");
        this.deleted = info.getBoolean("deleted");
        if(!this.deleted && info.has("md5"))
            this.md5 = info.getString("md5");
        else this.md5 = null;
    }
}
