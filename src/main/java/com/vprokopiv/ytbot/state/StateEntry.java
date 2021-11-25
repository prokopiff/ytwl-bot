package com.vprokopiv.ytbot.state;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "state")
public class StateEntry {

    @Id
    @Column(name = "entry_key")
    private String key;

    @Column(nullable = false, name = "entry_value")
    @Lob
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
