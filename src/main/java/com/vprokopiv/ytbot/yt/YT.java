package com.vprokopiv.ytbot.yt;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.Activity;
import com.google.api.services.youtube.model.ActivityListResponse;
import com.google.api.services.youtube.model.PlaylistItem;
import com.google.api.services.youtube.model.PlaylistItemContentDetails;
import com.google.api.services.youtube.model.PlaylistItemSnippet;
import com.google.api.services.youtube.model.ResourceId;
import com.google.api.services.youtube.model.Subscription;
import com.google.api.services.youtube.model.SubscriptionListResponse;
import com.google.api.services.youtube.model.Video;
import com.vprokopiv.ytbot.Config;
import com.vprokopiv.ytbot.yt.model.Channel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class YT {
    private static final Logger LOG = LogManager.getLogger(YT.class);

    private static final String CLIENT_SECRETS = "client_secrets.json";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final String APPLICATION_NAME = "Bot Client 1";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_DIRECTORY = Config.getRequiredProperty("local-dir");
    private static final String BOT_WL_PLAYLIST_ID = Config.getRequiredProperty("yt.playlist-id");
    private static final boolean CHECK_WL_DUPLICATES = Config.getProperty("yt.check-wl-duplicates")
            .map(v -> Set.of("true", "1", "yes").contains(v.toLowerCase()))
            .orElse(true);

    private static YT instance;
    private final YouTube service;

    private YT(Consumer<String> sendMessageHandler) throws GeneralSecurityException, IOException {
        this.service = getService(sendMessageHandler);
    }

    public static synchronized YT getInstance(Consumer<String> sendMessageHandler)
            throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new YT(sendMessageHandler);
        }
        return instance;
    }

    private static Credential authorize(Consumer<String> sendMessageHandler) throws IOException {
        LOG.debug("Authorising");
        // Load client secrets.
        var secrets = Config.getProperty("secrets");
        InputStream in = secrets.map(
                file -> {
                    try {
                        return (InputStream) new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).orElseGet(() -> ClassLoader.getSystemResourceAsStream(CLIENT_SECRETS));
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(
                new File(System.getProperty("user.home"), CREDENTIALS_DIRECTORY));
        var oauth = new OAuthForDevice(
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                HTTP_TRANSPORT,
                sendMessageHandler,
                fileDataStoreFactory);

        return oauth.getCredential();
    }

    private static YouTube getService(Consumer<String> sendMessageHandler)
            throws GeneralSecurityException, IOException {
        LOG.debug("Initializing API");
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(sendMessageHandler);

        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<com.vprokopiv.ytbot.yt.model.Channel> getSubscriptions() throws IOException {
        YouTube.Subscriptions.List subsRequest = service.subscriptions()
                .list("snippet");

        SubscriptionListResponse response = subsRequest.setMaxResults(500L)
                .setMine(true)
                .setOrder("alphabetical")
                .execute();

        List<Subscription> result = new ArrayList<>(response.getItems());

        while (response.getNextPageToken() != null) {
            response = subsRequest
                    .setPageToken(response.getNextPageToken())
                    .execute();
            result.addAll(response.getItems());
        }

        return result.stream()
                .map(sub -> new Channel(
                        sub.getSnippet().getResourceId().getChannelId(),
                        sub.getSnippet().getTitle()
                ))
                .toList();
    }

    public List<Activity> getActivities(String channelId, DateTime after) {
        try {
            YouTube.Activities.List activitiesRequest = service.activities()
                    .list("snippet,contentDetails");

            ActivityListResponse response = activitiesRequest.setMaxResults(25L)
                    .setChannelId(channelId)
                    .setPublishedAfter(after)
                    .execute();

            List<Activity> result = new ArrayList<>(response.getItems());

            while (response.getNextPageToken() != null) {
                response = activitiesRequest
                        .setPageToken(response.getNextPageToken())
                        .execute();
                result.addAll(response.getItems());
            }

            return result;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public void addToWL(String videoId) throws IOException {
        Set<String> wl = getWl();
        if (wl.contains(videoId)) {
            LOG.debug("Already in WL");
        } else {
            LOG.debug("Adding {} to WL", videoId);
            var snippet = new PlaylistItemSnippet()
                    .setPlaylistId(BOT_WL_PLAYLIST_ID)
                    .setResourceId(new ResourceId()
                            .setKind("youtube#video")
                            .setVideoId(videoId)
                    );

            service.playlistItems()
                    .insert("snippet", new PlaylistItem().setSnippet(snippet))
                    .execute();
            LOG.info("WL entry added");
        }
    }

    private Set<String> getWl() throws IOException {
        if (!CHECK_WL_DUPLICATES) {
            LOG.debug("Not getting current WL");
            return Set.of();
        }

        LOG.debug("Getting current WL");
        var requqest = service.playlistItems()
                .list("snippet,contentDetails")
                .setPlaylistId(BOT_WL_PLAYLIST_ID);
        var response = requqest.execute();
        List<PlaylistItem> result = new ArrayList<>(response.getItems());
        while (response.getNextPageToken() != null) {
            requqest.setPageToken(response.getNextPageToken());
            response = requqest.execute();
            result.addAll(response.getItems());
        }
        return result
                .stream()
                .map(PlaylistItem::getContentDetails)
                .map(PlaylistItemContentDetails::getVideoId)
                .collect(Collectors.toSet());
    }

    public Map<String, Duration> getDurations(Set<String> ids) throws IOException {
        LOG.info("Getting durations for {} videos", ids.size());
        if (ids.isEmpty()) {
            return Map.of();
        } else if (ids.size() > 25) {
            LOG.warn("Too many video ids to get durations. Getting first 25");
            return getDurations(ids.stream().limit(25).collect(Collectors.toSet()));
        }

        var request = service.videos()
                .list("contentDetails")
                .setId(String.join(",", ids));

        var response = request.execute();
        List<Video> result = response.getItems();
        while (response.getNextPageToken() != null) {
            request.setPageToken(response.getNextPageToken());
            response = request.execute();
            result.addAll(response.getItems());
        }
        return result.stream().collect(Collectors.toMap(
                Video::getId,
                v -> Duration.parse(v.getContentDetails().getDuration())));
    }
}
