package com.aochi.velolist.listener;

import com.aochi.velolist.config.PluginConfig;
import com.aochi.velolist.database.DatabaseManager;
import com.velocitypowered.api.event.EventTask;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.ServerPreConnectEvent;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.concurrent.CompletableFuture;

/**
 * Listens for {@link ServerPreConnectEvent} and denies access to backend servers
 * for players who are not on the whitelist.
 *
 * <p>Database lookups are performed asynchronously via {@link EventTask} so that the
 * proxy event loop is never blocked.</p>
 */
public class PreConnectListener {

    private final ProxyServer proxyServer;
    private final PluginConfig config;
    private final DatabaseManager database;

    public PreConnectListener(ProxyServer proxyServer, PluginConfig config, DatabaseManager database) {
        this.proxyServer = proxyServer;
        this.config = config;
        this.database = database;
    }

    @Subscribe
    public EventTask onServerPreConnect(ServerPreConnectEvent event) {
        Player player = event.getPlayer();
        String serverName = event.getOriginalServer().getServerInfo().getName();

        // Skip the async DB call entirely when the whitelist is off for this server.
        if (!config.isWhitelistEnabled(serverName)) {
            return null;
        }

        String username = player.getUsername();
        String uuid = player.getUniqueId().toString();

        CompletableFuture<Void> future = database.isWhitelisted(username, uuid, serverName)
            .thenCompose(whitelisted -> {
                if (!whitelisted) {
                    // Deny the connection to the backend server.
                    event.setResult(ServerPreConnectEvent.ServerResult.denied());

                    // If the player has no current server this is the initial connection;
                    // kick them from the proxy with the configured message.
                    if (!player.getCurrentServer().isPresent()) {
                        player.disconnect(parseMessage(config.getKickMessage()));
                    }
                    return CompletableFuture.completedFuture(null);
                }

                // Player is whitelisted – opportunistically fill in any missing UUID rows
                // so future lookups can also match by UUID (handles name-change scenarios).
                return database.updateUUIDIfMissing(username, uuid);
            });

        return EventTask.resumeWhenComplete(future);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static Component parseMessage(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
