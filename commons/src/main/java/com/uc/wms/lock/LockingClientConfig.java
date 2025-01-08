package com.uc.wms.lock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LockingClientConfig {

    @Value("${zookeeper.url}")
    private String zookeeperUrl;

    @Value("${zookeeper.sessionTimeout}")
    private int zookeeperSessionTimeout;

    @Value("${zookeeper.connectionTimeout}")
    private int zookeeperConnectionTimeout;

    @Bean
    public LockingClient lockingClient() {
        return new LockingClient(zookeeperUrl, zookeeperSessionTimeout, zookeeperConnectionTimeout);
    }
}
