package org.licketysplit.syncmanager;



public class ConfigsInfo {
    private String username;

    public ConfigsInfo(String username){
        this.username = username;
    }

    public String toString() {
        return "{\"username\": \"" + this.username + "\"}";
    }

    public String getUsername() {
        return this.username;
    }

}
