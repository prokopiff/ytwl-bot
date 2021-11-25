package com.vprokopiv.ytbot.yt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;
import com.vprokopiv.ytbot.state.StateManager;
import org.springframework.stereotype.Component;

@Component
public class MapDataStoreFactory implements DataStoreFactory {
    static final String PREFIX = "ds_";
    private final StateManager stateManager;

    public MapDataStoreFactory(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    @Override
    public DataStore<StoredCredential> getDataStore(String datastoreId) {
        var credential = stateManager.getState(PREFIX + datastoreId, new TypeReference<StoredCredential>() {});
        return new MapDataStore(datastoreId, this, credential.orElse(null));
    }

    void save(String id, StoredCredential credential) {
        stateManager.setState(PREFIX + id, credential);
    }
}
