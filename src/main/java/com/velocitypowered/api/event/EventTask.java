package com.velocitypowered.api.event;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Compile-time stub – replaced by the real Velocity API at runtime.
 *
 * <p>Represents an asynchronous event task that can be returned from a
 * {@link Subscribe @Subscribe} handler method.</p>
 */
public interface EventTask {

    boolean requiresAsync();

    void execute(Continuation continuation);

    static EventTask async(final Runnable task) {
        return new EventTask() {
            @Override public boolean requiresAsync() { return true; }
            @Override public void execute(Continuation c) {
                try { task.run(); c.resume(); }
                catch (Throwable t) { c.resumeWithException(t); }
            }
        };
    }

    static EventTask withContinuation(final Consumer<Continuation> task) {
        return new EventTask() {
            @Override public boolean requiresAsync() { return false; }
            @Override public void execute(Continuation c) { task.accept(c); }
        };
    }

    /**
     * Creates an {@link EventTask} that completes when the given
     * {@link CompletableFuture} completes.
     *
     * <p>Note: the parameter type is {@link CompletableFuture}, not
     * {@code CompletionStage}, to match the Velocity 3.x API exactly.</p>
     */
    static EventTask resumeWhenComplete(final CompletableFuture<?> future) {
        return withContinuation(c -> future.whenComplete((r, ex) -> {
            if (ex != null) c.resumeWithException(ex);
            else c.resume();
        }));
    }
}
