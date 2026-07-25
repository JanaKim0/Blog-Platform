package com.blogplatform.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Origins the browser app is served from, under {@code app.cors}. */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
