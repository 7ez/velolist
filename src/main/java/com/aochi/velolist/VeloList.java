package com.aochi.velolist;

import com.aochi.velolist.command.WhitelistCommand;
import com.aochi.velolist.config.PluginConfig;
import com.aochi.velolist.database.DatabaseManager;
import com.aochi.velolist.listener.PreConnectListener;
import com.google.inject.Inject;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutionException;

/**
 * Main entry point for the VeloList plugin.
 *
 * <p>VeloList provides a cross-proxy whitelist that supports both online-mode and
 * offline-mode players.  Whitelist entries are stored in an embedded H2 database and
 * may be scoped to individual backend servers or applied globally.  All database
 * operations are performed asynchronously to keep the proxy event loop free.</p>
 */
@Plugin(
    id = "velolist",
    name = "VeloList",
    version = "1.0.0",
    description = "Cross-proxy whitelist plugin supporting online and offline mode players",
    authors = {"Aochi"}
)
public class VeloList {

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;

    private PluginConfig pluginConfig;
    private DatabaseManager databaseManager;

    @Inject
    public VeloList(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    @Subscribe
    public void onProxyInitialize(ProxyInitializeEvent event) {
        // Ensure the data directory exists before reading/writing any files.
        try {
            Files.createDirectories(dataDirectory);
        } catch (IOException e) {
            logger.error("Could not create plugin data directory", e);
        }

        // Copy and load the configuration.
        loadConfig();

        // Initialise the database (blocks briefly on start-up; acceptable here).
        databaseManager = new DatabaseManager(dataDirectory, logger);
        try {
            databaseManager.init().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Database initialisation was interrupted", e);
            return;
        } catch (ExecutionException e) {
            logger.error("Failed to initialise the database", e.getCause());
            return;
        }

        // Register the ServerPreConnectEvent listener.
        server.getEventManager().register(this, new PreConnectListener(server, pluginConfig, databaseManager));

        // Register /velolist (alias /vl).
        server.getCommandManager().register(
            server.getCommandManager().metaBuilder("velolist")
                .aliases("vl")
                .plugin(this)
                .build(),
            new WhitelistCommand(this, databaseManager, server)
        );

        Plugin annotation = getClass().getAnnotation(Plugin.class);
        logger.info("VeloList {} has been enabled.", annotation != null ? annotation.version() : "unknown");
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (databaseManager != null) {
            databaseManager.close();
        }
        logger.info("VeloList has been disabled.");
    }

    // -------------------------------------------------------------------------
    // Config helpers (public so WhitelistCommand can trigger a reload)
    // -------------------------------------------------------------------------

    /**
     * Loads (or reloads) {@code config.toml} from the data directory.
     * The default config is copied from the jar if the file does not yet exist.
     */
    public void loadConfig() {
        Path configFile = dataDirectory.resolve("config.toml");
        try {
            if (!Files.exists(configFile)) {
                try (InputStream in = getClass().getResourceAsStream("/config.toml")) {
                    if (in != null) {
                        Files.copy(in, configFile);
                    }
                }
            }
            pluginConfig = PluginConfig.load(configFile);
        } catch (IOException e) {
            logger.error("Failed to load config.toml – using defaults", e);
            pluginConfig = new PluginConfig();
        }
    }

    // -------------------------------------------------------------------------
    // Accessors
    // -------------------------------------------------------------------------

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public Path getDataDirectory() {
        return dataDirectory;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxyServer() {
        return server;
    }
}
