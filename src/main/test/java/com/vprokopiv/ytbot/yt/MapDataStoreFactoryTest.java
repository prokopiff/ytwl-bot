package com.vprokopiv.ytbot.yt;

import com.google.api.client.auth.oauth2.StoredCredential;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class MapDataStoreFactoryTest {

    @Autowired
    private MapDataStoreFactory dataStoreFactory;

    @Test
    void test() throws IOException {
        var ds = dataStoreFactory.getDataStore("dsid");
        var credential = new StoredCredential()
                .setAccessToken("acc")
                .setRefreshToken("ref")
                .setExpirationTimeMilliseconds(1L);
        ds.set("dsid", credential);

        var savedDs = dataStoreFactory.getDataStore("dsid");

        assertEquals(credential, savedDs.get("key1"));
    }

}
