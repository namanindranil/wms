/*
 *  Copyright 2012 Unicommerce eSolutions (P) Limited . All Rights Reserved.
 *  UNICOMMERCE ESOLUTIONS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *  
 *  @version     1.0, Jul 3, 2012
 *  @author singla
 */
package com.uc.wms.aspect.locking;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import com.uc.wms.annotation.Level;

public interface ILockingService {

    ReadWriteLock getReadWriteLock(Namespace namespace, String key);

    ReadWriteLock getReadWriteLock(Namespace namespace, String key, Level level);

    Lock getLock(Namespace namespace, String key);

    Lock getLock(Namespace namespace, String key, Level level);

    Lock getLock(Namespace namespace, String key, Level level, String lockSection);

    boolean isConnectionInterrupted();
}
