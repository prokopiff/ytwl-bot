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
import com.vprokopiv.ytbot.config.Config;
import com.vprokopiv.ytbot.yt.model.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

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
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Component
@Profile("!test")
public class YouTubeService {
    private static final Logger LOG = LoggerFactory.getLogger(YouTubeService.class);

    private final Resource clientSecrets;

    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final String APPLICATION_NAME = "Bot Client 1";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    public static final int GET_LENGTH_TRESHOLD = 25;
    public static final long PAGE_SIZE = 25L;
    public static final String CONTENT_DETAILS = "contentDetails";
    public static final String SNIPPET = "snippet";

    private final YouTube service;
    private final Config config;

    private YouTubeService(Consumer<String> sendMessageHandler,
                           Config config,
                           @Value("classpath:client_secrets.json") Resource clientSecrets)
            throws GeneralSecurityException, IOException {
        this.config = config;
        this.clientSecrets = clientSecrets;
        this.service = getService(sendMessageHandler);
    }

    private Credential authorize(Consumer<String> sendMessageHandler) throws IOException {
        LOG.debug("Authorising");
        // Load client secrets.
        InputStream in = config.getSecretsLocation().map(
                file -> {
                    try {
                        return (InputStream) new FileInputStream(file);
                    } catch (FileNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }).orElseGet(() -> {
                    try {
                        return clientSecrets.getInputStream();
                    } catch (IOException e) {
                        LOG.error(e.getMessage(), e);
                        throw new RuntimeException(e);
                    }
                });
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        var fileDataStoreFactory = new FileDataStoreFactory(
                new File(System.getProperty("user.home"), config.getLocalDir()));
        var oauth = new OAuthForDevice(
                clientSecrets.getDetails().getClientId(),
                clientSecrets.getDetails().getClientSecret(),
                HTTP_TRANSPORT,
                sendMessageHandler,
                fileDataStoreFactory);

        return oauth.getCredential();
    }

    private YouTube getService(Consumer<String> sendMessageHandler)
            throws GeneralSecurityException, IOException {
        LOG.debug("Initializing API");
        final var httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        var credential = authorize(sendMessageHandler);

        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Channel> getSubscriptions() throws IOException {
        var subsRequest = service.subscriptions()
                .list(List.of(SNIPPET));

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
            var activitiesRequest = service.activities()
                    .list(List.of(SNIPPET, CONTENT_DETAILS));

            ActivityListResponse response = activitiesRequest.setMaxResults(PAGE_SIZE)
                    .setChannelId(channelId)
                    .setPublishedAfter(after.toStringRfc3339())
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
        LOG.debug("Adding {} to WL", videoId);
        addToList(videoId, config.getWlPlaylistId());
    }

    public void addToLL(String videoId) throws IOException {
        LOG.debug("Adding {} to LL", videoId);
        addToList(videoId, config.getLlPlaylistId());
    }

    private void addToList(String videoId, String playlistId) throws IOException {
        Set<String> wl = getCurrentList(playlistId);
        if (wl.contains(videoId)) {
            LOG.debug("Already in list");
        } else {
            var snippet = new PlaylistItemSnippet()
                    .setPlaylistId(playlistId)
                    .setResourceId(new ResourceId()
                            .setKind("youtube#video")
                            .setVideoId(videoId)
                    );

            service.playlistItems()
                    .insert(List.of(SNIPPET), new PlaylistItem().setSnippet(snippet))
                    .execute();
            LOG.info("List entry added");
        }
    }

    private Set<String> getCurrentList(String playlistId) throws IOException {
        if (!config.isCheckWlDuplicates()) {
            LOG.debug("Not getting current list");
            return Set.of();
        }

        LOG.debug("Getting current list");
        var requqest = service.playlistItems()
                .list(List.of(SNIPPET, CONTENT_DETAILS))
                .setPlaylistId(playlistId);
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

    public Map<String, Duration> getDurations(List<String> ids) throws IOException {
        LOG.info("Getting durations for {} videos", ids.size());
        if (ids.isEmpty()) {
            return Map.of();
        } else if (ids.size() > GET_LENGTH_TRESHOLD) {
            LOG.warn("Too many video ids to get durations. Getting first 25");
            return getDurations(ids.stream().limit(25).toList());
        }

        var request = service.videos()
                .list(List.of(CONTENT_DETAILS))
                .setId(ids);

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
