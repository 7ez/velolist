package com.velocitypowered.api.proxy;

import net.kyori.adventure.text.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Only the methods actually called by VeloList are declared here.</p>
 */
public interface Player {

    String getUsername();

    UUID getUniqueId();

    Optional<ServerConnection> getCurrentServer();

    void disconnect(Component reason);
}
