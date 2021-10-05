package com.vprokopiv.ytbot.yt;

import com.google.api.client.auth.oauth2.BearerToken;
import com.google.api.client.auth.oauth2.ClientParametersAuthentication;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.DataStoreCredentialRefreshListener;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.UrlEncodedContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Clock;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Consumer;

public class OAuthForDevice {
    private static final Logger LOG = LogManager.getLogger(OAuthForDevice.class);

    private static final String TOKEN_STORE_USER_ID = "user";
    private static final String SCOPE = "https://www.googleapis.com/auth/youtube";
    private static final JsonFactory JSON_FACTORY = new JacksonFactory();

    private final String clientId;
    private final String clientSecret;
    private final Consumer<String> messageHandler;
    private final FileDataStoreFactory fileDataStoreFactory;

    private final HttpTransport httpTransport;


    public OAuthForDevice(String clientId,
                          String clientSecret,
                          HttpTransport httpTransport,
                          Consumer<String> messageHandler, FileDataStoreFactory fileDataStoreFactory) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.httpTransport = httpTransport;
        this.messageHandler = messageHandler;
        this.fileDataStoreFactory = fileDataStoreFactory;
    }

    public Credential getCredential() {
        LOG.info("Getting credential");
        Credential credential = null;
        try {
            DataStore<StoredCredential> datastore = fileDataStoreFactory.getDataStore("youtube_token");

            credential = loadCredential(TOKEN_STORE_USER_ID, datastore);
            if (credential == null) {
                LOG.info("Couldn't get credential saved locally");

                var deviceUrl = new GenericUrl("https://oauth2.googleapis.com/device/code");

                Map<String, String> mapData = new HashMap<>();
                mapData.put("client_id", clientId);
                mapData.put("scope", SCOPE);
                var content = new UrlEncodedContent(mapData);
                var requestFactory = httpTransport.createRequestFactory(
                        r -> r.setParser(new JsonObjectParser(JSON_FACTORY)));
                var postRequest = requestFactory.buildPostRequest(deviceUrl, content);

                var device = postRequest.execute().parseAs(Properties.class);

                var userCode = device.getProperty("user_code");
                var deviceCode = device.getProperty("device_code");
                var expiresIn = bdObj(device.get("expires_in"));
                var interval = bdObj(device.get("interval"));
                var verificationUrl = device.getProperty("verification_url");

                LOG.info("Got device response");
                LOG.info("user code: {}", userCode);
                LOG.info("device code: {}", device);
                LOG.info("expires in: {}", expiresIn);
                LOG.info("interval: {}", interval);
                LOG.info("verification_url: {}", verificationUrl);

                messageHandler.accept("Go to %s and enter %s".formatted(verificationUrl, userCode));

                mapData = new HashMap<>();
                mapData.put("client_id", clientId);
                mapData.put("client_secret", clientSecret);
                mapData.put("device_code", deviceCode);
                mapData.put("grant_type", "urn:ietf:params:oauth:grant-type:device_code");

                content = new UrlEncodedContent(mapData);
                var tokenUrl = new GenericUrl("https://oauth2.googleapis.com/token");
                postRequest = requestFactory.buildPostRequest(tokenUrl, content);

                Properties deviceToken = null;
                long expiresAt = System.currentTimeMillis()
                        + expiresIn * 1000L;
                LOG.info("Waiting for user to enter code");
                while (System.currentTimeMillis() < expiresAt) {
                    try {
                        deviceToken = postRequest.execute().parseAs(Properties.class);
                    } catch (HttpResponseException e) {
                        if (!e.getContent().contains("authorization_pending")) {
                            throw new RuntimeException(e);
                        }
                        deviceToken = new Properties();
                    }

                    var accessToken = deviceToken.getProperty("access_token");

                    if (accessToken != null) {
                        LOG.info("Got access token");
                        LOG.info("device access token: {}", accessToken);
                        LOG.info("device token_type: {}", deviceToken.getProperty("token_type"));
                        LOG.info("device refresh_token: {}", deviceToken.getProperty("refresh_token"));
                        LOG.info("device expires_in: {}", deviceToken.get("expires_in"));
                        break;
                    }
                    LOG.info("waiting for {} seconds", interval);
                    Thread.sleep(interval * 1000L);
                }

                LOG.info("Finished waiting");

                var dataCredential = new StoredCredential();
                dataCredential.setAccessToken(deviceToken.getProperty("access_token"));
                dataCredential.setRefreshToken(deviceToken.getProperty("refresh_token"));
                dataCredential.setExpirationTimeMilliseconds(bdObj(deviceToken.get("expires_in")) * 1000);

                datastore.set(TOKEN_STORE_USER_ID, dataCredential);

                credential = loadCredential(TOKEN_STORE_USER_ID, datastore);
            } else {
                LOG.info("Got credential saved locally");
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return credential;
    }

    private Credential loadCredential(String userId, DataStore<StoredCredential> credentialDataStore)
            throws IOException {
        var credential = newCredential(userId, credentialDataStore);
        if (credentialDataStore != null) {
            StoredCredential stored = credentialDataStore.get(userId);
            if (stored == null) {
                return null;
            }
            credential.setAccessToken(stored.getAccessToken());
            credential.setRefreshToken(stored.getRefreshToken());
            credential.setExpirationTimeMilliseconds(stored.getExpirationTimeMilliseconds());
        }
        return credential;
    }

    private Credential newCredential(String userId, DataStore<StoredCredential> credentialDataStore) {

        var builder = new Credential.Builder(BearerToken
                .authorizationHeaderAccessMethod()).setTransport(httpTransport)
                .setJsonFactory(JSON_FACTORY)
                .setTokenServerEncodedUrl("https://oauth2.googleapis.com/token")
                .setClientAuthentication(new ClientParametersAuthentication(clientId, clientSecret))
                .setRequestInitializer(null)
                .setClock(Clock.SYSTEM);

        builder.addRefreshListener(
                new DataStoreCredentialRefreshListener(userId, credentialDataStore));

        return builder.build();
    }

    private Long bdObj(Object o) {
        return ((BigDecimal) o).longValue();
    }
}
