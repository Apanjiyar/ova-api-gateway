package com.ms.gateway.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "redis.keys")
public record RedisPrefixProperties(
        String blacklistToken,
        String userInfo
) {}
