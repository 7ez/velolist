# VeloList

A cross-proxy whitelist plugin for [Velocity](https://papermc.io/software/velocity) that supports per-server and global whitelisting for both online-mode and offline-mode players, backed by an embedded H2 database.

## Requirements

- Velocity 3.x
- Java 11+

## Installation

1. Download the latest release jar and place it in your Velocity `plugins/` folder.
2. Restart the proxy.
3. Edit `plugins/velolist/config.toml` to configure the whitelist.

## Configuration

`plugins/velolist/config.toml`:

```toml
# Message shown to players who are not on the whitelist.
# Supports legacy colour codes using '&'.
kick-message = "&cYou are not whitelisted on this server."

# Global whitelist toggle.
# When true, the whitelist is enforced on every backend server unless a
# server-specific override is set to false.
whitelist-enabled = true

# Per-server overrides.
# Keys are the server names as defined in velocity.toml.
# A value of 'true' enables the whitelist for that server regardless of the
# global toggle. A value of 'false' disables it for that server.
[servers]
# lobby = false
# survival = true
```

## Commands

All commands require the `velolist.admin` permission. The main command is `/velolist` (alias `/vl`).

| Command | Description |
|---|---|
| `/vl add <player> [server]` | Add a player to the whitelist. Omit `[server]` to whitelist globally. |
| `/vl remove <player> [server]` | Remove a player from the whitelist. |
| `/vl list [server]` | List whitelisted players for a server (or globally). |
| `/vl on [server]` | Enable the whitelist for a server (or globally). |
| `/vl off [server]` | Disable the whitelist for a server (or globally). |
| `/vl reload` | Reload `config.toml` without restarting the proxy. |

## Scope model

- An entry with no server specified (global) grants access to **all** servers.
- Per-server entries are checked first; if no per-server entry is found the global entry is checked as a fallback.

## Permissions

| Permission | Description |
|---|---|
| `velolist.admin` | Access to all `/velolist` subcommands. |

## Building from source

```bash
mvn package
```

The shaded jar is produced at `target/velolist-<version>.jar`.
