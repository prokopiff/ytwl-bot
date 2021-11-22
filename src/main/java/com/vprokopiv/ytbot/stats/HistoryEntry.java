package com.vprokopiv.ytbot.stats;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Objects;

@Entity
@Table(name = "history")
public class HistoryEntry {

    @Id
    @Column
    private String id;

    @Column(nullable = false)
    private String title;

    private Integer durationSeconds;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private String channelId;

    @Column(nullable = false)
    private String channelName;

    private Long addedToWl;

    private Long addedToLl;

    public HistoryEntry() {
    }

    public HistoryEntry(String id,
                        String title,
                        int durationSeconds,
                        String description,
                        String channelId,
                        String channelName,
                        Long addedToWl,
                        Long addedToLl) {
        this.id = id;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.description = description;
        this.channelId = channelId;
        this.channelName = channelName;
        this.addedToWl = addedToWl;
        this.addedToLl = addedToLl;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getChannelId() {
        return channelId;
    }

    public void setChannelId(String channelId) {
        this.channelId = channelId;
    }

    public String getChannelName() {
        return channelName;
    }

    public void setChannelName(String channelName) {
        this.channelName = channelName;
    }

    public Long getAddedToWl() {
        return addedToWl;
    }

    public void setAddedToWl(Long addedToWl) {
        this.addedToWl = addedToWl;
    }

    public Long getAddedToLl() {
        return addedToLl;
    }

    public void setAddedToLl(Long addedToLl) {
        this.addedToLl = addedToLl;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        HistoryEntry that = (HistoryEntry) o;
        return id.equals(that.id) && title.equals(that.title) && Objects.equals(durationSeconds, that.durationSeconds) && description.equals(that.description) && channelId.equals(that.channelId) && channelName.equals(that.channelName) && Objects.equals(addedToWl, that.addedToWl) && Objects.equals(addedToLl, that.addedToLl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, title, durationSeconds, description, channelId, channelName, addedToWl, addedToLl);
    }

    @Override
    public String toString() {
        return "HistoryEntry{" +
                "id='" + id + '\'' +
                ", title='" + title + '\'' +
                '}';
    }
}
