package com.velocitypowered.api.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Compile-time stub – replaced by the real Velocity API at runtime. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Subscribe {

    /**
     * @deprecated Use {@link #priority()} instead.
     */
    @Deprecated
    PostOrder order() default PostOrder.NORMAL;

    short priority() default 0;

    boolean async() default true;
}
