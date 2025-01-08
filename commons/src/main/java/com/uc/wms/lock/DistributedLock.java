/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author namanindranil
 */

package com.uc.wms.lock;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import com.uc.wms.lock.exception.LockingException;
import com.uc.wms.lock.exception.ConnectionInterruptedException;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedLock implements Lock {

    private static final Logger LOG = LoggerFactory.getLogger(DistributedLock.class);

    private InterProcessMutex   distributedMutex;

    private final String        path;

    private final String        section;

    private long                lockTakenTime;

    private LockingClient       client;

    DistributedLock(LockingClient client, InterProcessMutex interProcessMutex, String path) {
        this(client, interProcessMutex, path, "");
    }

    public DistributedLock(LockingClient client, String path) {
        this(client, path, "");
    }

    public DistributedLock(LockingClient client, String path, String section) {
        this(client, path, section, null);
    }

    public DistributedLock(LockingClient client, String path, String section, String lockData) {
        this(client, buildInterProcessMutex(client, path, lockData), path, section);
    }

    private DistributedLock(LockingClient client, InterProcessMutex interProcessMutex, String path, String section) {
        this.client = client;
        this.distributedMutex = interProcessMutex;
        this.path = path;
        this.section = section;
    }

    @Override
    public void lock() {
        long start = System.currentTimeMillis();
        if (!client.isConnectionInterrupted()) {
            try {
                LOG.info("Trying to acquire lock via lock method on path: {}, section:{}", path, section);
                distributedMutex.acquire();
                lockTakenTime = System.currentTimeMillis();
                LOG.info("Acquired lock on path: {}, section:{}, in {} ms", path, section, (lockTakenTime - start));
            } catch (Exception e) {
                LOG.error("Error while acquiring lock on on path: " + path + ", section:" + section, e);
                throw new LockingException(e);
            }
            if (!onLockAcquired()) {
                throw new ConnectionInterruptedException("Released lock as zookeeper connection was recently suspended/lost on path: " + path + ", section: " + section);
            }
        } else {
            LOG.error("Not acquiring lock on path: {}, section:{} as zookeeper connection was recently suspended/lost", path, section);
            throw new ConnectionInterruptedException("Not acquiring lock on path: " + path + ", section: " + section +" as zookeeper connection was recently suspended/lost");
        }
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        throw new UnsupportedOperationException("not supported with DistributedLock");
    }

    @Override
    public boolean tryLock() {
        return tryLock(0, TimeUnit.NANOSECONDS);
    }

    @Override
    public boolean tryLock(long time, TimeUnit unit) {
        long start = System.currentTimeMillis();
        boolean acquired = false;
        if (!client.isConnectionInterrupted()) {
            LOG.info("Trying to acquire lock via tryLock method on path: {}, section:{}", path, section);
            try {
                acquired = distributedMutex.acquire(time, unit);
                if (acquired) {
                    lockTakenTime = System.currentTimeMillis();
                    LOG.info("Acquired lock on path: {}, section:{}, in {} ms", path, section, (lockTakenTime - start));
                    acquired = onLockAcquired();
                } else {
                    logDistributedMutexNodeDetails();
                    LOG.error("Unable to acquire lock on path: {}, section:{}", path, section);
                }
            } catch (Exception e) {
                logDistributedMutexNodeDetails();
                LOG.error("Error while acquiring lock on on path: " + path + ", section:" + section, e);
                throw new LockingException(e);
            }
        } else {
            LOG.error("Skipping to acquire lock on path: {}, section:{} as zookeeper connection was recently suspended/lost", path, section);
        }
        return acquired;
    }

    @Override
    public void unlock() {
        unlock(false);
    }

    @Override
    public Condition newCondition() {
        throw new UnsupportedOperationException("not supported with DistributedLock");
    }

    @Override
    public String toString() {
        return "LockPath: " + path;
    }

    /* checking if onLockAcquired executed successfully if not then releasing the lock */
    private boolean onLockAcquired() {
        boolean success = client.onLockAcquired(path);
        if(!success) {
            unlock(true);
        }
        return success;
    }

    private void unlock(boolean unlockForLockAcquiredOnInterruptedConnection) {
        try {
            long start = System.currentTimeMillis();
            LOG.info("Releasing lock {}: {}, section: {} holdTime:{}",
                    (unlockForLockAcquiredOnInterruptedConnection ? "because of zookeeper connection lost/suspended before acquisition lock on path" : "on path"), path, section,
                    (System.currentTimeMillis() - lockTakenTime));
            distributedMutex.release();
            LOG.info("Released lock on path: {}, section:{} in {} ms", path, section, System.currentTimeMillis() - start);
        } catch (Exception e) {
            throw new LockingException(e);
        } finally {
            // if onLockAcquired returns false it means it has nothing to do with onLockReleased
            if (!unlockForLockAcquiredOnInterruptedConnection) {
                client.onLockReleased(path);
            }
        }
    }

    private void logDistributedMutexNodeDetails() {
        try {
            distributedMutex.getParticipantNodes().stream().map(node -> {
                        try {
                            return client.getLockingClient().getData().forPath(node);

                        } catch (Exception e) {
                            return null;
                        }
                    }).map(nodeData -> {
                        if (nodeData != null) {
                            return new String(nodeData, StandardCharsets.UTF_8);
                        } else {
                            return new String();
                        }
                    })
                    .forEach(nodeDataString -> LOG.info("Lock on path {} acquired by {} ",path, !nodeDataString.isEmpty()? nodeDataString : "None"));
        }
        catch (Exception e)
        {
            LOG.error("Error while retrieving DistributedMutex Lock Node details : {}",e.getMessage());
        }
    }

    private static InterProcessMutex buildInterProcessMutex(LockingClient client, String path, String lockData) {
        return new InterProcessMutex(client.getLockingClient(), path) {
            @Override
            protected byte[] getLockNodeBytes() {
                return StringUtils.isBlank(lockData) ? super.getLockNodeBytes() : lockData.getBytes();
            }
        };
    }
}
