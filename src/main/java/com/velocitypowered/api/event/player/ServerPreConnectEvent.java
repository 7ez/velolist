package com.velocitypowered.api.event.player;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Fired before a player is sent to a backend server.  Set a
 * {@link ServerResult#denied()} result to prevent the connection.</p>
 */
public final class ServerPreConnectEvent {

    private final Player player;
    private final RegisteredServer originalServer;
    private ServerResult result;

    public ServerPreConnectEvent(Player player, RegisteredServer originalServer) {
        this.player = player;
        this.originalServer = originalServer;
        this.result = ServerResult.allowed(originalServer);
    }

    public Player getPlayer() { return player; }

    public RegisteredServer getOriginalServer() { return originalServer; }

    public ServerResult getResult() { return result; }

    public void setResult(ServerResult result) { this.result = result; }

    /**
     * The result of a {@link ServerPreConnectEvent}.
     * Matches the actual Velocity API static factory names exactly.
     */
    public static final class ServerResult {

        private final RegisteredServer server; // null → denied

        private ServerResult(RegisteredServer server) {
            this.server = server;
        }

        public boolean isAllowed() { return server != null; }

        public java.util.Optional<RegisteredServer> getServer() {
            return java.util.Optional.ofNullable(server);
        }

        public static ServerResult denied() {
            return new ServerResult(null);
        }

        public static ServerResult allowed(RegisteredServer server) {
            return new ServerResult(server);
        }
    }
}
