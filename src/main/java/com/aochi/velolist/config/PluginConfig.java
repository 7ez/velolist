package com.aochi.velolist.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.toml.TomlParser;
import com.electronwill.nightconfig.toml.TomlWriter;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the runtime configuration for VeloList.
 *
 * <p>Configuration is loaded from {@code config.toml} in the plugin data directory.
 * A default config is written on first run.  All changes made via commands are
 * persisted back to disk immediately.</p>
 */
public class PluginConfig {

    private String kickMessage = "&cYou are not whitelisted on this server.";
    private boolean whitelistEnabled = true;
    // server name -> enabled override; absent keys fall through to whitelistEnabled
    private Map<String, Boolean> servers = new HashMap<>();

    // -------------------------------------------------------------------------
    // Factory / IO
    // -------------------------------------------------------------------------

    /**
     * Loads a {@link PluginConfig} from the given TOML file.
     * Returns a default config if the file is missing or cannot be parsed.
     */
    public static PluginConfig load(Path file) throws IOException {
        PluginConfig config = new PluginConfig();
        if (!Files.exists(file)) {
            return config;
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            CommentedConfig raw = new TomlParser().parse(reader);

            String kickMsg = raw.get("kick-message");
            if (kickMsg != null) {
                config.kickMessage = kickMsg;
            }

            Boolean enabled = raw.get("whitelist-enabled");
            if (enabled != null) {
                config.whitelistEnabled = enabled;
            }

            Config serversSection = raw.get("servers");
            if (serversSection != null) {
                serversSection.valueMap().forEach((key, value) -> {
                    if (value instanceof Boolean) {
                        config.servers.put(key, (Boolean) value);
                    }
                });
            }
        }
        return config;
    }

    /**
     * Saves the current configuration back to {@code file} in TOML format.
     */
    public void save(Path file) throws IOException {
        CommentedConfig cfg = CommentedConfig.inMemory();
        cfg.set("kick-message", kickMessage);
        cfg.set("whitelist-enabled", whitelistEnabled);
        if (!servers.isEmpty()) {
            CommentedConfig serversSection = CommentedConfig.inMemory();
            servers.forEach(serversSection::set);
            cfg.set("servers", serversSection);
        }
        try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            new TomlWriter().write(cfg, writer);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} when the whitelist should be enforced for {@code serverName}.
     * A server-specific override takes priority over the global toggle.
     */
    public boolean isWhitelistEnabled(String serverName) {
        return servers.getOrDefault(serverName, whitelistEnabled);
    }

    public String getKickMessage() {
        return kickMessage;
    }

    public void setKickMessage(String kickMessage) {
        this.kickMessage = kickMessage;
    }

    public boolean isWhitelistEnabled() {
        return whitelistEnabled;
    }

    public void setWhitelistEnabled(boolean enabled) {
        this.whitelistEnabled = enabled;
    }

    /**
     * Sets an override for a specific server name.
     * Pass {@code null} to remove the override and fall back to the global toggle.
     */
    public void setServerEnabled(String serverName, Boolean enabled) {
        if (enabled == null) {
            servers.remove(serverName);
        } else {
            servers.put(serverName, enabled);
        }
    }

    public Map<String, Boolean> getServers() {
        return servers;
    }
}
