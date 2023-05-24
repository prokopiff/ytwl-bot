package com.vprokopiv.ytbot.state;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "state")
public class StateEntry {

    @Id
    @Column(name = "entry_key")
    private String key;

    @Column(nullable = false, name = "entry_value", length = 10_000)
    private String value;

    public StateEntry() {
    }

    public StateEntry(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }
}
