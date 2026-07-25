package com.blogplatform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Where uploaded images are written, under {@code app.storage}. */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(String location) {
}
