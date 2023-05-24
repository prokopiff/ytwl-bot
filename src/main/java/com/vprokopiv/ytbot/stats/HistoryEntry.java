package com.vprokopiv.ytbot.stats;

import com.vprokopiv.ytbot.yt.model.Video;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "history")
public class HistoryEntry {

    @Id
    @Column
    private String id;

    @Column(nullable = false)
    private String title;

    private Long durationSeconds;

    @Column(nullable = false, length = 10_000)
    private String description;

    @Column(nullable = false)
    private String channelId;

    @Column(nullable = false)
    private String channelName;

    private Long addedToWl;

    private Long addedToLl;

    @Column(nullable = false)
    private Long created;

    public HistoryEntry() {
        created = System.currentTimeMillis();
    }

    public HistoryEntry(String id,
                        String title,
                        Long durationSeconds,
                        String description,
                        String channelId,
                        String channelName,
                        Long addedToWl,
                        Long addedToLl) {
        this();
        this.id = id;
        this.title = title;
        this.durationSeconds = durationSeconds;
        this.description = description;
        this.channelId = channelId;
        this.channelName = channelName;
        this.addedToWl = addedToWl;
        this.addedToLl = addedToLl;
    }

    public HistoryEntry(Video video) {
        this(
                video.id(),
                video.title(),
                video.description(),
                video.channel().id(),
                video.channel().title(),
                video.duration()
        );
    }

    private HistoryEntry(
            String id, String title, String description, String channelId, String channelName, Long duration) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.channelId = channelId;
        this.channelName = channelName;
        this.durationSeconds = duration;
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

    public Long getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Long durationSeconds) {
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

    public Long getCreated() {
        return created;
    }

    public void setCreated(Long created) {
        this.created = created;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        HistoryEntry that = (HistoryEntry) o;
        return id.equals(that.id) && title.equals(that.title) && Objects.equals(durationSeconds, that.durationSeconds)
                && description.equals(that.description) && channelId.equals(that.channelId)
                && channelName.equals(that.channelName) && Objects.equals(addedToWl, that.addedToWl)
                && Objects.equals(addedToLl, that.addedToLl);
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
