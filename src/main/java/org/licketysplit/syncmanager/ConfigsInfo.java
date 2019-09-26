package org.licketysplit.syncmanager;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;


public class ConfigsInfo {
    private String username;
    private String sharedDirectory;

    @JsonCreator
    public ConfigsInfo(@JsonProperty("username") String username, @JsonProperty("sharedDirectory") String sharedDirectory){
        this.username = username;
        this.sharedDirectory = sharedDirectory;
    }

    public String getUsername() {
        return this.username;
    }

    public String getSharedDirectory() {
        return this.sharedDirectory;
    }
}
