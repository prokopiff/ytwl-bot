package com.vprokopiv.ytbot;

import com.vprokopiv.ytbot.tg.Telegram;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.function.Consumer;

@SpringBootApplication
@EnableScheduling
public class YoutubeBotApplication {

    public static void main(String[] args) {
        SpringApplication.run(YoutubeBotApplication.class, args);
    }

    @Bean
    public Consumer<String> addToWlHandler(QueuesManager queuesManager) {
        return queuesManager::putAddToWlMessage;
    }

    @Bean
    public Consumer<String> sendMessageHandler(QueuesManager queuesManager, Telegram telegram) {
        return s -> queuesManager.putTgMessage(telegram.sendMessageOf(s));
    }
}
