package com.velocitypowered.api.command;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Metadata for a registered command: its primary alias, extra aliases,
 * and the owning plugin.</p>
 */
public interface CommandMeta {

    /**
     * Builder for {@link CommandMeta} instances.
     */
    interface Builder {

        /**
         * Adds extra aliases for the command.
         *
         * @param aliases additional aliases
         * @return this builder
         */
        Builder aliases(String... aliases);

        /**
         * Associates the command with the given plugin instance.
         *
         * @param plugin the owning plugin
         * @return this builder
         */
        Builder plugin(Object plugin);

        /**
         * Builds the {@link CommandMeta}.
         *
         * @return the built metadata
         */
        CommandMeta build();
    }
}
