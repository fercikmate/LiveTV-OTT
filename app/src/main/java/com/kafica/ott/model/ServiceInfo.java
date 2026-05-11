package com.kafica.ott.model;

import com.google.gson.annotations.SerializedName;

// POJO representing one OTT service from the catalog
// broadcastOnid/Tsid/Sid stored for broadcast→OTT fallback matching if implemented
public class ServiceInfo {

    @SerializedName("serviceId")
    public String serviceId;

    @SerializedName("name")
    public String name;

    @SerializedName("type")
    public String type;

    @SerializedName("streamUri")
    public String streamUri;

    @SerializedName("metadataUri")
    public String metadataUri;

    @SerializedName("logoUri")
    public String logoUri;

    @SerializedName("description")
    public String description;

    @SerializedName("rating")
    public String rating;

    @SerializedName("broadcastOnid")
    public int broadcastOnid;

    @SerializedName("broadcastTsid")
    public int broadcastTsid;

    @SerializedName("broadcastSid")
    public int broadcastSid;
}