package com.vprokopiv.ytbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration
@ConfigurationProperties(prefix = "bot")
public class Config {
    private String token;
    private String chatId;
    private String wlPlaylistId;
    private String llPlaylistId;
    private boolean checkWlDuplicates = true;
    private boolean disableShorts;

    private String secretsLocation;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getChatId() {
        return chatId;
    }

    public void setChatId(String chatId) {
        this.chatId = chatId;
    }

    public String getWlPlaylistId() {
        return wlPlaylistId;
    }

    public void setWlPlaylistId(String wlPlaylistId) {
        this.wlPlaylistId = wlPlaylistId;
    }

    public String getLlPlaylistId() {
        return llPlaylistId;
    }

    public void setLlPlaylistId(String llPlaylistId) {
        this.llPlaylistId = llPlaylistId;
    }

    public boolean isCheckWlDuplicates() {
        return checkWlDuplicates;
    }

    public void setCheckWlDuplicates(boolean checkWlDuplicates) {
        this.checkWlDuplicates = checkWlDuplicates;
    }

    public boolean isDisableShorts() {
        return disableShorts;
    }

    public void setDisableShorts(boolean disableShorts) {
        this.disableShorts = disableShorts;
    }

    public Optional<String> getSecretsLocation() {
        return Optional.ofNullable(secretsLocation);
    }

    public void setSecretsLocation(String secretsLocation) {
        this.secretsLocation = secretsLocation;
    }
}
