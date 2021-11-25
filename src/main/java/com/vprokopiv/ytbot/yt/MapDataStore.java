package com.vprokopiv.ytbot.yt;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.DataStoreFactory;

import java.util.Collection;
import java.util.Set;

public class MapDataStore implements DataStore<StoredCredential> {
    private StoredCredential credential;
    private final String id;
    private final MapDataStoreFactory dataStoreFactory;

    public MapDataStore(String id, MapDataStoreFactory dataStoreFactory, StoredCredential credential) {
        this.id = id;
        this.credential = credential;
        this.dataStoreFactory = dataStoreFactory;
    }

    private void save() {
        this.dataStoreFactory.save(id, credential);
    }

    @Override
    public DataStoreFactory getDataStoreFactory() {
        return this.dataStoreFactory;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public int size() {
        return credential == null ? 0 : 1;
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(String s) {
        return id.equals(s);
    }

    @Override
    public boolean containsValue(StoredCredential credential1) {
        return credential.equals(credential1);
    }

    @Override
    public Set<String> keySet() {
        return Set.of(id);
    }

    @Override
    public Collection<StoredCredential> values() {
        return Set.of(credential);
    }

    @Override
    public StoredCredential get(String s) {
        return credential;
    }

    @Override
    public DataStore<StoredCredential> set(String s, StoredCredential value) {
        this.credential = value;
        save();
        return this;
    }

    @Override
    public DataStore<StoredCredential> clear() {
        credential = null;
        save();
        return this;
    }

    @Override
    public DataStore<StoredCredential> delete(String s) {
        credential = null;
        save();
        return this;
    }
}
