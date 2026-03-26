package com.aochi.velolist.command;

import com.aochi.velolist.VeloList;
import com.aochi.velolist.database.DatabaseManager;
import com.aochi.velolist.database.WhitelistEntry;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * The {@code /velolist} command (alias: {@code /vl}).
 *
 * <pre>
 *   /velolist add    &lt;player&gt; [server|global]  – add to whitelist
 *   /velolist remove &lt;player&gt; [server|global]  – remove from whitelist
 *   /velolist list   [server|global]            – list whitelisted players
 *   /velolist on     [server|global]            – enable whitelist
 *   /velolist off    [server|global]            – disable whitelist
 *   /velolist reload                            – reload config.yml
 * </pre>
 *
 * <p>All subcommands require the {@code velolist.admin} permission.</p>
 */
public class WhitelistCommand implements SimpleCommand {

    private static final String PERMISSION = "velolist.admin";
    private static final String SCOPE_GLOBAL = "global";

    private final VeloList plugin;
    private final DatabaseManager database;
    private final ProxyServer proxyServer;

    public WhitelistCommand(VeloList plugin, DatabaseManager database, ProxyServer proxyServer) {
        this.plugin = plugin;
        this.database = database;
        this.proxyServer = proxyServer;
    }

    // -------------------------------------------------------------------------
    // SimpleCommand implementation
    // -------------------------------------------------------------------------

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();

        if (!source.hasPermission(PERMISSION)) {
            source.sendMessage(err("You do not have permission to use this command."));
            return;
        }

        String[] args = invocation.arguments();
        if (args.length == 0) {
            sendHelp(source);
            return;
        }

        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "add":
                handleAdd(source, args);
                break;
            case "remove":
            case "rem":
                handleRemove(source, args);
                break;
            case "list":
            case "ls":
                handleList(source, args);
                break;
            case "on":
            case "enable":
                handleToggle(source, args, true);
                break;
            case "off":
            case "disable":
                handleToggle(source, args, false);
                break;
            case "reload":
                handleReload(source);
                break;
            default:
                sendHelp(source);
        }
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        String[] args = invocation.arguments();

        if (!invocation.source().hasPermission(PERMISSION)) {
            return Collections.emptyList();
        }

        if (args.length <= 1) {
            List<String> subs = Arrays.asList("add", "remove", "list", "on", "off", "reload");
            String prefix = args.length == 1 ? args[0].toLowerCase(Locale.ROOT) : "";
            return subs.stream()
                .filter(s -> s.startsWith(prefix))
                .collect(Collectors.toList());
        }

        // Second argument: player name or server name depending on subcommand
        if (args.length == 2) {
            String sub = args[0].toLowerCase(Locale.ROOT);
            if (sub.equals("on") || sub.equals("off") || sub.equals("enable") || sub.equals("disable")) {
                return serverSuggestions(args[1]);
            }
            // For add/remove/list suggest online player names
            return proxyServer.getAllPlayers().stream()
                .map(Player::getUsername)
                .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(args[1].toLowerCase(Locale.ROOT)))
                .collect(Collectors.toList());
        }

        // Third argument: server scope (for add/remove)
        if (args.length == 3) {
            return serverSuggestions(args[2]);
        }

        return Collections.emptyList();
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission(PERMISSION);
    }

    // -------------------------------------------------------------------------
    // Sub-command handlers
    // -------------------------------------------------------------------------

    private void handleAdd(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(err("Usage: /velolist add <player> [server|global]"));
            return;
        }
        String username = args[1];
        String scope = args.length >= 3 ? args[2] : SCOPE_GLOBAL;

        database.addPlayer(username, null, scope)
            .thenRun(() -> source.sendMessage(ok(
                "Added &e" + username + "&a to the whitelist for &e" + scope + "&a.")))
            .exceptionally(ex -> {
                source.sendMessage(err("Failed to add player: " + ex.getCause().getMessage()));
                return null;
            });
    }

    private void handleRemove(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(err("Usage: /velolist remove <player> [server|global]"));
            return;
        }
        String username = args[1];
        String scope = args.length >= 3 ? args[2] : SCOPE_GLOBAL;

        database.removePlayer(username, scope)
            .thenAccept(removed -> {
                if (removed) {
                    source.sendMessage(ok(
                        "Removed &e" + username + "&a from the whitelist for &e" + scope + "&a."));
                } else {
                    source.sendMessage(err(
                        username + " was not found in the whitelist for " + scope + "."));
                }
            })
            .exceptionally(ex -> {
                source.sendMessage(err("Failed to remove player: " + ex.getCause().getMessage()));
                return null;
            });
    }

    private void handleList(CommandSource source, String[] args) {
        String scope = args.length >= 2 ? args[1] : null;
        String displayScope = scope == null ? "all scopes" : scope;

        database.getEntries(scope)
            .thenAccept(entries -> {
                if (entries.isEmpty()) {
                    source.sendMessage(info("No players whitelisted for &e" + displayScope + "&7."));
                    return;
                }
                source.sendMessage(info("Whitelisted players (&e" + displayScope + "&7):"));
                for (WhitelistEntry entry : entries) {
                    String line = "&7 - &f" + entry.getUsername();
                    if (entry.getUuid() != null) {
                        line += " &8(" + entry.getUuid() + ")";
                    }
                    if (scope == null) {
                        line += " &8[" + entry.getServerScope() + "]";
                    }
                    source.sendMessage(parse(line));
                }
            })
            .exceptionally(ex -> {
                source.sendMessage(err("Failed to list players: " + ex.getCause().getMessage()));
                return null;
            });
    }

    private void handleToggle(CommandSource source, String[] args, boolean enable) {
        String scope = args.length >= 2 ? args[1] : SCOPE_GLOBAL;
        String action = enable ? "enabled" : "disabled";

        if (SCOPE_GLOBAL.equalsIgnoreCase(scope)) {
            plugin.getPluginConfig().setWhitelistEnabled(enable);
        } else {
            plugin.getPluginConfig().setServerEnabled(scope, enable);
        }

        try {
            plugin.getPluginConfig().save(plugin.getDataDirectory().resolve("config.yml"));
            source.sendMessage(ok("Whitelist " + action + " for &e" + scope + "&a."));
        } catch (IOException e) {
            source.sendMessage(err("Config saved in memory but could not be written to disk: " + e.getMessage()));
            plugin.getLogger().error("Failed to save config", e);
        }
    }

    private void handleReload(CommandSource source) {
        plugin.loadConfig();
        source.sendMessage(ok("Configuration reloaded."));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private void sendHelp(CommandSource source) {
        source.sendMessage(parse("&6VeloList Commands:"));
        source.sendMessage(parse("&7/velolist add &f<player> [server|global]"));
        source.sendMessage(parse("&7/velolist remove &f<player> [server|global]"));
        source.sendMessage(parse("&7/velolist list &f[server|global]"));
        source.sendMessage(parse("&7/velolist on &f[server|global]"));
        source.sendMessage(parse("&7/velolist off &f[server|global]"));
        source.sendMessage(parse("&7/velolist reload"));
    }

    private List<String> serverSuggestions(String prefix) {
        List<String> suggestions = proxyServer.getAllServers().stream()
            .map(rs -> rs.getServerInfo().getName())
            .filter(name -> name.toLowerCase(Locale.ROOT).startsWith(prefix.toLowerCase(Locale.ROOT)))
            .collect(Collectors.toList());
        if (SCOPE_GLOBAL.startsWith(prefix.toLowerCase(Locale.ROOT))) {
            suggestions.add(0, SCOPE_GLOBAL);
        }
        return suggestions;
    }

    private static Component ok(String message) {
        return parse("&a" + message);
    }

    private static Component err(String message) {
        return Component.text(message).color(NamedTextColor.RED);
    }

    private static Component info(String message) {
        return parse("&7" + message);
    }

    private static Component parse(String message) {
        return LegacyComponentSerializer.legacyAmpersand().deserialize(message);
    }
}
