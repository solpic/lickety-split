package org.licketysplit.syncmanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.File;

public class FileInfo {
    public String name;
    public long length;

    @JsonCreator
    public FileInfo(@JsonProperty("name") String name, @JsonProperty("length") long length) {
        this.name = name;
        this.length = length;
    }

    public FileInfo(File file) {
        this.name = file.getName();
        this.length = file.length();
    }

    public String getName() {
        return name;
    }

    public long getLength() {
        return length;
    }


}
