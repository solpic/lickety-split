package org.licketysplit.syncmanager;


/**
 * Stores configuration info.
 */
public class ConfigsInfo {
    /**
     * Our username.
     */
    private String username;

    /**
     * Instantiates a new Configs info.
     *
     * @param username our username
     */
    public ConfigsInfo(String username){
        this.username = username;
    }

    /**
     * Serializer.
     * @return to string
     */
    public String toString() {
        return "{\"username\": \"" + this.username + "\"}";
    }

    /**
     * Gets username.
     *
     * @return the username
     */
    public String getUsername() {
        return this.username;
    }

}
