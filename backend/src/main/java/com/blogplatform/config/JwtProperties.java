package com.blogplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings under {@code app.jwt}. The secret must be at least 32 bytes long for
 * HMAC-SHA256; override it with {@code APP_JWT_SECRET} outside development.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, long expirationMs) {
}
