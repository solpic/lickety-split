package org.licketysplit.syncmanager;

import org.json.JSONObject;
import org.licketysplit.securesocket.messages.JSONMessage;

import java.io.File;
import java.util.Date;

public class FileInfo {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    public String name;
    public long length;
    public long timeStamp;
    public boolean deleted;

    public FileInfo(){}

    public FileInfo(File file, long timeStamp) { // For when you're updating or adding a new file
        this.name = file.getName();
        this.length = file.length();
        this.timeStamp = timeStamp;
        this.deleted = false;
    }

    public FileInfo(String fileName, boolean deleted){
        this.name = fileName;
        this.length = 0;
        this.timeStamp = 0;
        this.deleted = deleted;
    }
}
