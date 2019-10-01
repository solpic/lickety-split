package org.licketysplit.syncmanager;



public class ConfigsInfo {
    private String username;
    private String sharedDirectory;

    public ConfigsInfo(String username, String sharedDirectory){
        this.username = username;
        this.sharedDirectory = sharedDirectory;
    }

    public String toString() {
        return "{\"username\": \"" + this.username + "\", \"sharedDirectory\": \"" + this.sharedDirectory + "\"}";
    }

    public String getUsername() {
        return this.username;
    }

    public String getSharedDirectory() {
        return this.sharedDirectory;
    }
}
