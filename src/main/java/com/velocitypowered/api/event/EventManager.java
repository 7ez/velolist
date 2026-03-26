package com.velocitypowered.api.event;

/** Compile-time stub – replaced by the real Velocity API at runtime. */
public interface EventManager {
    void register(Object plugin, Object listener);
    void unregisterListeners(Object plugin);
    void unregisterListener(Object plugin, Object listener);
}
