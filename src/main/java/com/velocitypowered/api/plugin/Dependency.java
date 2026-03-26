package com.velocitypowered.api.plugin;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Compile-time stub – replaced by the real Velocity API at runtime. */
@Retention(RetentionPolicy.RUNTIME)
@Target({})
public @interface Dependency {
    String id();
    boolean optional() default false;
}
