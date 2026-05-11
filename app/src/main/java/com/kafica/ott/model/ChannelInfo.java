package com.kafica.ott.model;

// Internal model for a TV Provider channel row
// Used by repository → tis → player layers
public class ChannelInfo {

    public long channelId;
    public String channelUri;
    public String inputId;
    public String serviceId;
    public String displayName;
    public String displayNumber;
    public String streamUri;
    public String logoUri;
    public String contentRating;
    public int originalNetworkId;
    public int transportStreamId;
    public int dvbServiceId;
}