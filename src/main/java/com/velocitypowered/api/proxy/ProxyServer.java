package com.velocitypowered.api.proxy;

import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.EventManager;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import java.util.Collection;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Only the methods actually called by VeloList are declared here.</p>
 */
public interface ProxyServer {

    EventManager getEventManager();

    CommandManager getCommandManager();

    Collection<Player> getAllPlayers();

    Collection<RegisteredServer> getAllServers();
}
