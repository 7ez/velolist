package com.velocitypowered.api.command;

import net.kyori.adventure.text.Component;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Represents anything that can execute commands and receive messages
 * (players and the console).</p>
 */
public interface CommandSource {
    boolean hasPermission(String permission);
    void sendMessage(Component message);
}
