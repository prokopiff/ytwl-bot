package com.vprokopiv.ytbot.tg;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.AnswerCallbackQuery;
import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.config.Config;
import com.vprokopiv.ytbot.yt.model.Vid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Consumer;

@Component
public class Telegram {
    private static final Logger LOG = LoggerFactory.getLogger(Telegram.class);

    private final TelegramBot bot;
    private final Config config;

    public Telegram(Consumer<String> addToWlHandler, Config config) {
        LOG.info("Initializing Telegram component");
        this.config = config;
        this.bot = new TelegramBot(config.getToken());

        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.callbackQuery() != null) {
                    addToWlHandler.accept(update.callbackQuery().data());
                    bot.execute(new AnswerCallbackQuery(update.callbackQuery().id())
                            .text("\u2713") // Checkmark
                    );
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public void sendVideos(List<Vid> vids) {
        LOG.info("Sending videos");
        vids.forEach(vid -> {
            var toWlButton = new InlineKeyboardButton("Add to WL")
                    .callbackData("WL" + vid.id());
            var toLlButton = new InlineKeyboardButton("Add to LL")
                    .callbackData("LL" + vid.id());
            var message = new SendMessage(config.getChatId(),
                    "%s\n%s\n\n%s".formatted(vid.channel(), vid.duration(), vid.getUrl()))
                    .replyMarkup(new InlineKeyboardMarkup(toWlButton, toLlButton));
            bot.execute(message);
        });
    }

    public void sendMessage(SendMessage msg) {
        bot.execute(msg);
    }
    public void sendMessage(String message) {
        LOG.info("Sending message");
        var msg = sendMessageOf(message);
        sendMessage(msg);
    }

    public SendMessage sendMessageOf(String msg) {
        return new SendMessage(config.getChatId(), msg);
    }
}
