package com.vprokopiv.ytbot.config;

import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "secrets")
public class GoogleSecretsConfig {
    private String clientId;
    private String clientSecret;

    public GoogleClientSecrets getSecrets() {
        var config = new GoogleClientSecrets();
        var installed = new GoogleClientSecrets.Details();
        installed.setClientId(clientId);
        installed.setClientSecret(clientSecret);
        config.setInstalled(installed);

        return config;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
}
