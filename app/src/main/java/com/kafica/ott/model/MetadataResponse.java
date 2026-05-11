package com.kafica.ott.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// POJO for /metadata/{serviceId} HTTP response
public class MetadataResponse {

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

    @SerializedName("tracks")
    public List<TrackInfo> tracks;
}