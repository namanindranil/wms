/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author namanindranil
 */

package com.uc.wms.lock;

import java.util.concurrent.locks.ReadWriteLock;

import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessReadWriteLock;

public class DistributedReadWriteLock implements ReadWriteLock {

    private InterProcessReadWriteLock distributedReadWriteLock;

    private DistributedLock           readMutex;

    private DistributedLock           writeMutex;

    public DistributedReadWriteLock(LockingClient client, String path) {
        this(client, path, null);
    }

    public DistributedReadWriteLock(LockingClient client, String path, String lockData) {
        this.distributedReadWriteLock = new InterProcessReadWriteLock(client.getLockingClient(),
                path, StringUtils.isEmpty(lockData)? null : lockData.getBytes());
        this.readMutex = new DistributedLock(client, this.distributedReadWriteLock.readLock(), path);
        this.writeMutex = new DistributedLock(client, this.distributedReadWriteLock.writeLock(), path);
    }

    @Override
    public DistributedLock readLock() {
        return this.readMutex;
    }

    @Override
    public DistributedLock writeLock() {
        return this.writeMutex;
    }
}
