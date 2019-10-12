package org.licketysplit.syncmanager;

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

    public FileInfo(File file) {
        Date date = new Date();
        this.name = file.getName();
        this.length = file.length();
        this.timeStamp = date.getTime();
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
