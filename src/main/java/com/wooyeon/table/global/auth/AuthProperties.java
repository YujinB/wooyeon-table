package com.wooyeon.table.global.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public record AuthProperties(String tokenSecret, long tokenTtlHours, String demoPassword) {
}
