package com.aochi.velolist.database;

/**
 * Represents a single whitelist entry stored in the database.
 */
public class WhitelistEntry {

    private final int id;
    private final String username;
    private final String uuid; // nullable – filled in on first connect
    private final String serverScope; // "global" or a specific server name

    public WhitelistEntry(int id, String username, String uuid, String serverScope) {
        this.id = id;
        this.username = username;
        this.uuid = uuid;
        this.serverScope = serverScope;
    }

    public int getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getUuid() {
        return uuid;
    }

    public String getServerScope() {
        return serverScope;
    }

    @Override
    public String toString() {
        return username + (uuid != null ? " (" + uuid + ")" : "") + " [" + serverScope + "]";
    }
}
