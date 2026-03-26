package com.aochi.velolist.config;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Holds the runtime configuration for VeloList.
 *
 * <p>Configuration is loaded from {@code config.yml} in the plugin data directory.
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
     * Loads a {@link PluginConfig} from the given YAML file.
     * Returns a default config if the file is missing or cannot be parsed.
     */
    @SuppressWarnings("unchecked")
    public static PluginConfig load(Path file) throws IOException {
        PluginConfig config = new PluginConfig();
        if (!Files.exists(file)) {
            return config;
        }
        Yaml yaml = new Yaml();
        try (InputStream in = Files.newInputStream(file)) {
            Object raw = yaml.load(in);
            if (!(raw instanceof Map)) {
                return config;
            }
            Map<String, Object> data = (Map<String, Object>) raw;

            if (data.containsKey("kick-message")) {
                config.kickMessage = String.valueOf(data.get("kick-message"));
            }
            if (data.containsKey("whitelist-enabled")) {
                Object val = data.get("whitelist-enabled");
                if (val instanceof Boolean) {
                    config.whitelistEnabled = (Boolean) val;
                }
            }
            Object serversRaw = data.get("servers");
            if (serversRaw instanceof Map) {
                ((Map<?, ?>) serversRaw).forEach((k, v) -> {
                    if (k instanceof String && v instanceof Boolean) {
                        config.servers.put((String) k, (Boolean) v);
                    }
                });
            }
        }
        return config;
    }

    /**
     * Saves the current configuration back to {@code file}.
     */
    public void save(Path file) throws IOException {
        DumperOptions opts = new DumperOptions();
        opts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        opts.setPrettyFlow(true);
        Yaml yaml = new Yaml(opts);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("kick-message", kickMessage);
        data.put("whitelist-enabled", whitelistEnabled);
        data.put("servers", servers.isEmpty() ? new LinkedHashMap<>() : new LinkedHashMap<>(servers));

        try (Writer writer = new OutputStreamWriter(Files.newOutputStream(file), StandardCharsets.UTF_8)) {
            yaml.dump(data, writer);
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
