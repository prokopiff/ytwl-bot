package com.vprokopiv.ytbot.tg;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.Config;
import com.vprokopiv.ytbot.yt.model.Vid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.function.Consumer;

public class TG {
    private static final Logger LOG = LogManager.getLogger(TG.class);

    private static final String TOKEN = Config.getRequiredProperty("bot.token");
    private static final long CHAT_ID = Long.parseLong(Config.getRequiredProperty("bot.chat-id"));

    private static TG instance;

    private final TelegramBot bot;

    private TG(Consumer<String> addToWlHandler) {
        this.bot = new TelegramBot(TOKEN);

        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.callbackQuery() != null) {
                    addToWlHandler.accept(update.callbackQuery().data());
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public static synchronized TG getInstance(Consumer<String> addToWlHandler) {
        if (instance == null) {
            instance = new TG(addToWlHandler);
        }
        return instance;
    }

    public void sendVideos(List<Vid> vids) {
        LOG.info("Sending videos");
        vids.forEach(vid -> {
            var toWlButton = new InlineKeyboardButton("Add to WL")
                    .callbackData(vid.id());
            var message = new SendMessage(CHAT_ID, vid.getUrl())
                    .replyMarkup(new InlineKeyboardMarkup(toWlButton));
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

    public static SendMessage sendMessageOf(String msg) {
        return new SendMessage(TG.CHAT_ID, msg);
    }
}
