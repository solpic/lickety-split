package org.licketysplit.syncmanager;

import org.json.JSONObject;

import java.io.File;
import java.util.Date;

public class FileInfo {
    public String name;
    public long length;
    public long timeStamp;

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

    public String toString() {
        return "{\"name\": \"" + this.name+ "\", \"length\": \"" + this.length + "\"," + "\"" + this.timeStamp + "\"}";
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }

    public long getTimeStamp() { return timeStamp; }


}
