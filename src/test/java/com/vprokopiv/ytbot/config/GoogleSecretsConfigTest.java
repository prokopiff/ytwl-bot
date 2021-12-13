package com.vprokopiv.ytbot.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class GoogleSecretsConfigTest {

    @Autowired
    private GoogleSecretsConfig secretsConfig;

    @Test
    void getSecrets() {
        var secrets = secretsConfig.getSecrets();
        assertAll(
                () -> assertEquals("client-id", secrets.getInstalled().getClientId()),
                () -> assertEquals("client-secret", secrets.getInstalled().getClientSecret())
        );
    }
}
