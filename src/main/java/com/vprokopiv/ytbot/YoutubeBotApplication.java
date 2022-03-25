package com.vprokopiv.ytbot;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vprokopiv.ytbot.tg.Telegram;
import com.vprokopiv.ytbot.yt.model.WlUpdate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.function.Consumer;
import java.util.function.Function;

@SpringBootApplication
@EnableScheduling
public class YoutubeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeBotApplication.class, args);
    }

    @Bean
    @Profile("!test")
    public Function<String, WlUpdate> wlUpdateHandler(QueuesManager queuesManager) {
        return queuesManager::putWlUpdateMessage;
    }

    @Bean
    @Profile("!test")
    public Consumer<String> sendMessageHandler(QueuesManager queuesManager, Telegram telegram) {
        return s -> queuesManager.putTgMessage(telegram.sendMessageOf(s));
    }

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
