package com.infernokun.infernoGames.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "inferno-games")
public class InfernoGamesConfig {
    private String applicationName;
    private String defaultAdminUsername;
    private String defaultAdminPassword;
    private String encryptionKey;

    // IGDB Integration (uses Twitch API)
    private String igdbClientId;
    private String igdbClientSecret;

    // AI Description Generation (optional)
    private String groqAPIKey;
    private String groqModel = "llama-3.1-8b-instant";
    private boolean descriptionGeneration = false;
}
