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

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({ METHOD })
@Retention(RUNTIME)
public @interface Locks {

    Lock[] value() default {};
}
