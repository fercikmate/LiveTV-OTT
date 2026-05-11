package com.kafica.ott.model;

// Internal model for a TV Provider program row
// channelId is required — must come from channel insert first
public class ProgramInfo {

    public long programId;
    public long channelId;
    public String title;
    public String description;
    public long startTimeUtcMillis;
    public long endTimeUtcMillis;
    public String contentRating;

    public ProgramInfo(long channelId, String title, String description,
                       long startTimeUtcMillis, long endTimeUtcMillis,
                       String contentRating) {
        this.channelId = channelId;
        this.title = title;
        this.description = description;
        this.startTimeUtcMillis = startTimeUtcMillis;
        this.endTimeUtcMillis = endTimeUtcMillis;
        this.contentRating = contentRating;
    }
}