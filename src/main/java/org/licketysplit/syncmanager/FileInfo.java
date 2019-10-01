package org.licketysplit.syncmanager;

import java.io.File;

public class FileInfo {
    public String name;
    public long length;

    public FileInfo(String name,long length) {
        this.name = name;
        this.length = length;
    }

    public FileInfo(File file) {
        this.name = file.getName();
        this.length = file.length();
    }

    public String toString() {
        return "{\"name\": \"" + this.name+ "\", \"length\": \"" + this.length + "\"}";
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }


}
