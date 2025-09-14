package fr.siovision.voyages.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.front")
public record FrontProperties(
    String url
) {}
