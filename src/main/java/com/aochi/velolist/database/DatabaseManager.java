package com.aochi.velolist.database;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Manages the H2 embedded database that backs the whitelist.
 *
 * <p>All public methods submit their work to a single-threaded executor, ensuring that
 * database access is serialised and never blocks the proxy event loop.</p>
 */
public class DatabaseManager {

    private static final String SCOPE_GLOBAL = "global";

    private final Path dataDirectory;
    private final Logger logger;
    private final ExecutorService executor;
    private Connection connection;

    public DatabaseManager(Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "velolist-database");
            t.setDaemon(true);
            return t;
        });
    }

    /**
     * Initialises the database: creates the data directory, opens the H2 connection,
     * and creates the whitelist table if it does not already exist.
     *
     * @return a future that completes when initialisation is done.
     */
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                throw new RuntimeException("Could not create data directory", e);
            }

            String dbPath = dataDirectory.resolve("whitelist").toAbsolutePath().toString();
            String url = "jdbc:h2:" + dbPath + ";DB_CLOSE_DELAY=-1";

            try {
                // Load the driver explicitly so it registers with DriverManager even after shading.
                Class.forName("org.h2.Driver");
                connection = DriverManager.getConnection(url, "sa", "");
                createTables();
                logger.info("VeloList database initialised successfully.");
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("H2 driver not found", e);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to open H2 database", e);
            }
        }, executor);
    }

    private void createTables() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS whitelist ("
                + "  id          INT AUTO_INCREMENT PRIMARY KEY,"
                + "  username    VARCHAR(16)  NOT NULL,"
                + "  uuid        VARCHAR(36),"
                + "  server_scope VARCHAR(64) NOT NULL DEFAULT 'global',"
                + "  CONSTRAINT uq_whitelist UNIQUE (username, server_scope)"
                + ")"
            );
        }
    }

    // -------------------------------------------------------------------------
    // Query helpers
    // -------------------------------------------------------------------------

    /**
     * Returns {@code true} if the player is on the whitelist for either the
     * {@code global} scope or the specified {@code serverName}.
     *
     * <p>Matching is case-insensitive for both the username and the server scope.
     * When a UUID is supplied it is also checked so that name-change scenarios are
     * handled correctly in online-mode deployments.</p>
     */
    public CompletableFuture<Boolean> isWhitelisted(String username, String uuid, String serverName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return queryIsWhitelisted(username, uuid, serverName);
            } catch (SQLException e) {
                logger.error("Error checking whitelist for {}", username, e);
                // Fail-open: if the DB is unavailable, let the player through.
                return true;
            }
        }, executor);
    }

    private boolean queryIsWhitelisted(String username, String uuid, String serverName) throws SQLException {
        if (uuid != null && !uuid.isEmpty()) {
            String sql =
                "SELECT COUNT(*) FROM whitelist "
                + "WHERE (LOWER(username) = LOWER(?) OR uuid = ?) "
                + "AND (server_scope = ? OR LOWER(server_scope) = LOWER(?))";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, uuid);
                ps.setString(3, SCOPE_GLOBAL);
                ps.setString(4, serverName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        } else {
            String sql =
                "SELECT COUNT(*) FROM whitelist "
                + "WHERE LOWER(username) = LOWER(?) "
                + "AND (server_scope = ? OR LOWER(server_scope) = LOWER(?))";
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setString(1, username);
                ps.setString(2, SCOPE_GLOBAL);
                ps.setString(3, serverName);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            }
        }
    }

    /**
     * Adds a player to the whitelist.  If an entry for {@code (username, serverScope)}
     * already exists the operation is a no-op.
     *
     * @param username    the player's username (case-insensitive on lookup)
     * @param uuid        the player's UUID string, or {@code null} if not yet known
     * @param serverScope {@code "global"} or a specific server name
     * @return a future that completes when the insert is done
     */
    public CompletableFuture<Void> addPlayer(String username, String uuid, String serverScope) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql =
                    "MERGE INTO whitelist (username, uuid, server_scope) "
                    + "KEY (username, server_scope) "
                    + "VALUES (?, ?, ?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, uuid);  // may be null
                    ps.setString(3, serverScope);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to add player to whitelist", e);
            }
        }, executor);
    }

    /**
     * Removes a player from the whitelist for the given scope.
     *
     * @return a future that resolves to {@code true} if a row was actually deleted.
     */
    public CompletableFuture<Boolean> removePlayer(String username, String serverScope) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String sql =
                    "DELETE FROM whitelist "
                    + "WHERE LOWER(username) = LOWER(?) AND LOWER(server_scope) = LOWER(?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, username);
                    ps.setString(2, serverScope);
                    return ps.executeUpdate() > 0;
                }
            } catch (SQLException e) {
                throw new RuntimeException("Failed to remove player from whitelist", e);
            }
        }, executor);
    }

    /**
     * Returns all whitelist entries for the given scope, or all entries if
     * {@code serverScope} is {@code null}.
     */
    public CompletableFuture<List<WhitelistEntry>> getEntries(String serverScope) {
        return CompletableFuture.supplyAsync(() -> {
            List<WhitelistEntry> entries = new ArrayList<>();
            try {
                String sql;
                PreparedStatement ps;
                if (serverScope == null) {
                    sql = "SELECT id, username, uuid, server_scope FROM whitelist ORDER BY username";
                    ps = connection.prepareStatement(sql);
                } else {
                    sql = "SELECT id, username, uuid, server_scope FROM whitelist "
                          + "WHERE LOWER(server_scope) = LOWER(?) ORDER BY username";
                    ps = connection.prepareStatement(sql);
                    ps.setString(1, serverScope);
                }
                try (ps; ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        entries.add(new WhitelistEntry(
                            rs.getInt("id"),
                            rs.getString("username"),
                            rs.getString("uuid"),
                            rs.getString("server_scope")
                        ));
                    }
                }
            } catch (SQLException e) {
                logger.error("Failed to query whitelist entries", e);
            }
            return entries;
        }, executor);
    }

    /**
     * Updates the UUID for all rows whose UUID is currently {@code null} and whose
     * username matches (case-insensitive).  This is called automatically when a player
     * who was added by name only first connects to the proxy.
     */
    public CompletableFuture<Void> updateUUIDIfMissing(String username, String uuid) {
        return CompletableFuture.runAsync(() -> {
            try {
                String sql =
                    "UPDATE whitelist SET uuid = ? "
                    + "WHERE uuid IS NULL AND LOWER(username) = LOWER(?)";
                try (PreparedStatement ps = connection.prepareStatement(sql)) {
                    ps.setString(1, uuid);
                    ps.setString(2, username);
                    ps.executeUpdate();
                }
            } catch (SQLException e) {
                logger.error("Failed to update UUID for {}", username, e);
            }
        }, executor);
    }

    /**
     * Gracefully shuts down the executor and closes the database connection.
     */
    public void close() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                logger.error("Error closing database connection", e);
            }
        }
    }
}
