package com.vprokopiv.ytbot.tg;


import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.ParseMode;
import com.pengrad.telegrambot.request.EditMessageReplyMarkup;
import com.pengrad.telegrambot.request.SendMessage;
import com.vprokopiv.ytbot.config.Config;
import com.vprokopiv.ytbot.yt.model.Video;
import com.vprokopiv.ytbot.yt.model.WlUpdate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Component
@Profile("!test")
public class Telegram {
    private static final Logger LOG = LoggerFactory.getLogger(Telegram.class);

    private final TelegramBot bot;
    private final Config config;

    public Telegram(Function<String, WlUpdate> wlUpdateHandler, Config config) {
        LOG.info("Initializing Telegram component");
        this.config = config;
        this.bot = new TelegramBot(config.getToken());

        bot.setUpdatesListener(updates -> {
            updates.forEach(update -> {
                if (update.callbackQuery() != null) {
                    var wlUpdate = wlUpdateHandler.apply(update.callbackQuery().data());
                    InlineKeyboardMarkup markup = null;
                    switch (wlUpdate.updateType()) {
                        case WL_ADD:
                            var removeFromWL = new InlineKeyboardButton("Remove from WL");
                            removeFromWL.callbackData(WlUpdate.of(WlUpdate.UpdateType.WL_REMOVE, wlUpdate.videoId()).toString());
                            markup = new InlineKeyboardMarkup(removeFromWL);
                            break;
                        case LL_ADD:
                            var removeFromLL = new InlineKeyboardButton("Remove from LL");
                            removeFromLL.callbackData(WlUpdate.of(WlUpdate.UpdateType.LL_REMOVE, wlUpdate.videoId()).toString());
                            markup = new InlineKeyboardMarkup(removeFromLL);
                            break;
                        case WL_REMOVE:
                        case LL_REMOVE:
                            var addToWL = new InlineKeyboardButton("Add to WL");
                            addToWL.callbackData(WlUpdate.of(WlUpdate.UpdateType.WL_ADD, wlUpdate.videoId()).toString());
                            var addToLL = new InlineKeyboardButton("Add to LL");
                            addToLL.callbackData(WlUpdate.of(WlUpdate.UpdateType.LL_ADD, wlUpdate.videoId()).toString());
                            markup = new InlineKeyboardMarkup(addToWL, addToLL);
                            break;
                    }

                    bot.execute(
                        new EditMessageReplyMarkup(config.getChatId(), update.callbackQuery().message().messageId())
                            .replyMarkup(markup)
                    );
                }
            });
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    public void sendVideos(List<Video> videos) {
        LOG.info("Sending videos");
        videos.forEach(video -> {
            var toWlButton = new InlineKeyboardButton("Add to WL")
                    .callbackData(WlUpdate.of(WlUpdate.UpdateType.WL_ADD, video.id()).toString());
            var toLlButton = new InlineKeyboardButton("Add to LL")
                    .callbackData(WlUpdate.of(WlUpdate.UpdateType.LL_ADD, video.id()).toString());
            var message = new SendMessage(config.getChatId(), video.toMessageString())
                    .parseMode(ParseMode.Markdown)
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
