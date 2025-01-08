/*
 * Copyright 2025 Unicommerce Technologies (P) Limited . All Rights Reserved.
 * UNICOMMERCE TECHONOLOGIES PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 * @author namanindranil
 */

package com.uc.wms.lock;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LockingClient {

    private static final Logger LOG                 = LoggerFactory.getLogger(LockingClient.class);

    private CuratorFramework    lockingClient;

    private static final int    DEFAULT_MAX_RETRIES = 5;

    private static final int    DEFAULT_SLEEP_MS    = 10;

    private DistributedLockingHelper lockingHelper;

    public LockingClient(String destination) {
        this(destination, DEFAULT_SLEEP_MS, DEFAULT_MAX_RETRIES);
    }

    public LockingClient(String destination, int sessionTimeoutMs, int connectionTimeoutMs) {
        this(destination, sessionTimeoutMs, connectionTimeoutMs, DEFAULT_SLEEP_MS, DEFAULT_MAX_RETRIES);
    }

    public LockingClient(String destination, int sessionTimeoutMs, int connectionTimeoutMs, int sleepMs, int maxRetry) {
        LOG.info("Instantiating new LockingClient with destination: {}, sessionTimeout: {}, connectionTimeout: {}, maxRetry: {}, and sleep: {}",
                destination,
                sessionTimeoutMs,
                connectionTimeoutMs,
                sleepMs,
                maxRetry);
        LOG.info("Creating Locking Helper..");
        this.lockingHelper = new DistributedLockingHelper();

        this.lockingClient = CuratorFrameworkFactory.newClient(destination, sessionTimeoutMs, connectionTimeoutMs, new ExponentialBackoffRetry(sleepMs, maxRetry));
        this.lockingClient.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            @Override
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                switch (newState) {
                    case LOST:
                        LOG.error("Zookeeper connection lost");
                        lockingHelper.doInterruptLocks();
                        break;
                    case SUSPENDED:
                        LOG.warn("Zookeeper connection suspended");
                        lockingHelper.doInterruptLocks();
                        break;
                    case RECONNECTED:
                        LOG.info("Zookeeper connection re-established");
                        break;
                }
            }
        });

        LOG.info("Starting Locking client..");
        long start = System.currentTimeMillis();
        lockingClient.start();
        LOG.info("Done starting LockingClient in {} ms", (System.currentTimeMillis() - start));
    }

    CuratorFramework getLockingClient() {
        return this.lockingClient;
    }

    public boolean isZookeeperConnected(){
        return this.lockingClient.getZookeeperClient().isConnected();
    }


    public boolean onLockAcquired(String path) {
        return this.lockingHelper.addCurrentThreadMetadata(path);
    }

    public void onLockReleased(String path) {
        this.lockingHelper.removeCurrentThreadMetadata(path);

    }

    /* return true if zookeeper connection was suspended/lost atleast once for the current thread */
    public boolean isConnectionInterrupted() {
        return this.lockingHelper.isConnectionInterrupted();
    }

    public int getThreadCountHoldingLocks() {
        return this.lockingHelper.getThreadCountHoldingLock();
    }
}
