/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author namanindranil
 */

package com.uc.wms.lock;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DistributedLockingHelper {
    private static final Logger              LOG                = LoggerFactory.getLogger(DistributedLockingHelper.class);

    private final Map<Thread, LocksMetadata> locksMetadataCache = new ConcurrentHashMap<>();

    private final ReadWriteLock              readWriteLock      = new ReentrantReadWriteLock();

    public boolean addCurrentThreadMetadata(String path) {
        readWriteLock.readLock().lock();
        try {
            Thread key = Thread.currentThread();
            LocksMetadata locksMetadata = locksMetadataCache.get(key);
            if (locksMetadata == null) {
                locksMetadata = new LocksMetadata();
                locksMetadataCache.put(key, locksMetadata);
            } else if (locksMetadata.isInterrupted()) {
                LOG.error("Unable to add lock meta data because of interruption. Data: {}", locksMetadata);
                return false;
            }
            locksMetadata.add(path);
        } finally {
            readWriteLock.readLock().unlock();
        }
        return true;
    }

    public void removeCurrentThreadMetadata(String path) {
        readWriteLock.readLock().lock();
        try {
            Thread thread = Thread.currentThread();
            LocksMetadata details = locksMetadataCache.get(thread);
            if (details != null) {
                details.remove(path);
                if (details.isEmpty()) {
                    locksMetadataCache.remove(thread);
                }
            }
        } finally {
            readWriteLock.readLock().unlock();
        }
    }

    public void doInterruptLocks() {
        readWriteLock.writeLock().lock();
        try {
            for (Map.Entry<Thread, LocksMetadata> entry : locksMetadataCache.entrySet()) {
                LocksMetadata locksMetadata = entry.getValue();
                if(!locksMetadata.isInterrupted()) {
                    LOG.info("Marking zookeeper lock's metadata as interrupted for thread({}) because zookeeper connection lost/interrupted after last readWriteLock acquired!!", entry.getKey().getName());
                    locksMetadata.interrupt();
                }
            }
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    public boolean isConnectionInterrupted() {
        readWriteLock.readLock().lock();
        LocksMetadata locksMetadata = locksMetadataCache.get(Thread.currentThread());
        boolean interrupted = locksMetadata != null && locksMetadata.isInterrupted();
        readWriteLock.readLock().unlock();
        return interrupted;
    }

    public int getThreadCountHoldingLock() {
        return this.locksMetadataCache.keySet().size();
    }

    private static class LocksMetadata {
        private Map<String, Integer> lockPaths = new HashMap<>();
        private volatile boolean     interrupted;

        void add(String path) {
            Integer value = lockPaths.get(path);
            if (value == null) {
                lockPaths.put(path, 1);
            } else {
                lockPaths.put(path, value + 1);
            }
        }

        void remove(String path) {
            Integer counter = lockPaths.get(path);
            if (counter == null || counter == 0) {
                LOG.error("LocksMetadata.remove -- Invalid path received: {}, interrupted: {}, acquired paths: {}", path, interrupted, lockPaths);
            } else if (counter == 1) {
                lockPaths.remove(path);
            } else {
                lockPaths.put(path, --counter);
            }
        }

        boolean isEmpty() {
            return lockPaths.isEmpty();
        }

        boolean isInterrupted() {
            return interrupted;
        }

        void interrupt() {
            this.interrupted = true;
        }

        @Override
        public String toString() {
            return "LocksMetadata{" +
                    "lockPaths=" + lockPaths +
                    ", interrupted=" + interrupted +
                    '}';
        }
    }
}
