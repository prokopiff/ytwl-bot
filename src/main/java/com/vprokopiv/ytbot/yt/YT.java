package com.vprokopiv.ytbot.yt;

import com.google.api.client.auth.oauth2.AuthorizationCodeRequestUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;

import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.*;
import com.vprokopiv.ytbot.Config;
import com.vprokopiv.ytbot.Main;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

public class YT {
    private static final Logger LOG = LogManager.getLogger(YT.class);

    private static final String CLIENT_SECRETS = "client_secrets.json";
    private static final Collection<String> SCOPES =
            singletonList("https://www.googleapis.com/auth/youtube");
    private static final String DATASTORE = "yt_bot";
    private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();

    private static final String APPLICATION_NAME = "Bot Client 1";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_DIRECTORY = ".oauth-credentials";
    private static final String BOT_WL_PLAYLIST_ID = Config.getRequiredProperty("yt.playlist-id");
    public static final String HOST = Config.getProperty("yt.oauth.host").orElse("localhost");
    public static final int PORT = Config.getProperty("yt.oauth.port")
            .map(Integer::parseInt)
            .orElse(8888);

    private static YT instance;
    private final YouTube service;

    private YT(Consumer<String> sendMessageHandler) throws GeneralSecurityException, IOException {
        this.service = getService(sendMessageHandler);
    }

    public static synchronized YT getInstance(Consumer<String> sendMessageHandler) throws GeneralSecurityException, IOException {
        if (instance == null) {
            instance = new YT(sendMessageHandler);
        }
        return instance;
    }

    private static Credential authorize(Consumer<String> sendMessageHandler) throws IOException {
        LOG.debug("Authorising");
        // Load client secrets.
        InputStream in = ClassLoader.getSystemResourceAsStream(CLIENT_SECRETS);
        GoogleClientSecrets clientSecrets =
                GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        // Build flow and trigger user authorization request.
        FileDataStoreFactory fileDataStoreFactory = new FileDataStoreFactory(new File(System.getProperty("user.home") + "/" + CREDENTIALS_DIRECTORY));
        DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore(DATASTORE);

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setCredentialDataStore(datastore)
                .build();

        // Build the local server and bind it to port 8080
        LocalServerReceiver localReceiver = new LocalServerReceiver.Builder()
                .setHost(HOST)
                .setPort(PORT)
                .build();

        // Authorize.
        return new AuthorizationCodeInstalledApp(flow, localReceiver) {
            @Override
            protected void onAuthorization(AuthorizationCodeRequestUrl authorizationUrl) throws IOException {
                LOG.info("Adding URL to message queue");
                sendMessageHandler.accept(authorizationUrl.build());
                super.onAuthorization(authorizationUrl);
            }
        }
        .authorize("user");
    }

    private static YouTube getService(Consumer<String> sendMessageHandler) throws GeneralSecurityException, IOException {
        LOG.debug("Initializing API");
        final NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = authorize(sendMessageHandler);

        return new YouTube.Builder(httpTransport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public List<Channel> getSubscriptions() throws IOException {
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
        LOG.debug("Adding {} to WL", videoId);
        var snippet = new PlaylistItemSnippet()
                .setPlaylistId(BOT_WL_PLAYLIST_ID)
                .setResourceId(new ResourceId()
                        .setKind("youtube#video")
                        .setVideoId(videoId)
                );

        var response = service.playlistItems()
                .insert("snippet", new PlaylistItem().setSnippet(snippet))
                .execute();

        LOG.debug("Response: {}", response);
    }
}