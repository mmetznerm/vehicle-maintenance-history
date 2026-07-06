package com.mmetzner.vmh.auth.infrastructure.security;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.cors")
public record CorsProperties(
        List<String> allowedOrigins
) {

    public CorsProperties {
        allowedOrigins = allowedOrigins == null
                ? List.of()
                : List.copyOf(allowedOrigins);
    }
}