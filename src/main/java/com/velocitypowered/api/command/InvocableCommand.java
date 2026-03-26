package com.velocitypowered.api.command;

import java.util.Collections;
import java.util.List;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * @param <I> the invocation type
 */
public interface InvocableCommand<I extends CommandInvocation<?>> extends Command {

    void execute(I invocation);

    default List<String> suggest(I invocation) {
        return Collections.emptyList();
    }

    default boolean hasPermission(I invocation) {
        return true;
    }
}
