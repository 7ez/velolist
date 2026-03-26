package com.velocitypowered.api.command;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Manages command registrations on the Velocity proxy.</p>
 */
public interface CommandManager {

    /**
     * Returns a new builder for the given primary command alias.
     *
     * @param alias the primary alias (e.g. {@code "velolist"})
     * @return a fresh {@link CommandMeta.Builder}
     */
    CommandMeta.Builder metaBuilder(String alias);

    /**
     * Registers the given {@link Command} with the proxy under the aliases
     * declared in {@code meta}.
     *
     * <p>The second parameter uses the base {@link Command} type (not
     * {@code SimpleCommand}) to match the actual Velocity 3.x method
     * descriptor exactly, ensuring binary compatibility at runtime.</p>
     *
     * @param meta    the command metadata
     * @param command the command handler; must implement a registrable
     *                {@link Command} sub-interface such as {@link SimpleCommand}
     */
    void register(CommandMeta meta, Command command);
}
