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

    public String name;
    public long length;
    public long timeStamp;

    public FileInfo(){}

    public FileInfo(String name,long length, long timeStamp) {
        this.name = name;
        this.length = length;
        this.timeStamp = timeStamp;
    }

    public FileInfo(File file, long timeStamp) { // For when you're updating or adding a new file
        this.name = file.getName();
        this.length = file.length();
        this.timeStamp = timeStamp;
    }

    public FileInfo(JSONObject fileInfo) {
        this.name = fileInfo.getString("name");
        this.length = fileInfo.getLong("length");
        this.timeStamp = fileInfo.getLong("timestamp");
    }
}
