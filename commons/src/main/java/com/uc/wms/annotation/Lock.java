/*
 * Copyright 2015 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @version     1.0, 5/16/15 3:18 PM
 * @author amdalal
 */

package com.uc.wms.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.uc.wms.aspect.locking.Namespace;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface Lock {

    /**
     * Namespace on which the lock is to be taken.
     *
     * @return
     */
    Namespace ns();

    /**
     * Level of the lock - TENANT, FACILITY or GLOBAL.
     *
     * @return
     */
    Level level() default Level.TENANT;

    /**
     * Timeout in seconds. -1 would imply infinite.
     *
     * @return
     */
    long timeoutInSeconds() default Long.MIN_VALUE;

    /**
     * {@link com.unifier.core.expressions.Expression} of the lock key. "#{#args[n]}" would mean nth argument of the
     * method.
     *
     * @return
     */
    String key();

    /**
     * Do or do not log time taken in acquiring/releasing locks.
     * 
     * @return
     */
    boolean log() default false;
}
