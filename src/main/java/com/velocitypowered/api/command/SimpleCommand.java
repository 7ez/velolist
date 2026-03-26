package com.velocitypowered.api.command;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>A command whose handler receives a flat {@code String[]} of arguments,
 * following the convention popularised by Bukkit and BungeeCord.</p>
 */
public interface SimpleCommand extends InvocableCommand<SimpleCommand.Invocation> {

    /**
     * Context passed to every invocation of a {@link SimpleCommand}.
     */
    interface Invocation extends CommandInvocation<String[]> {
        /** Returns the alias used to invoke this command. */
        String alias();
    }
}
