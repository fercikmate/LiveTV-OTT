package com.kafica.ott.model;

import com.google.gson.annotations.SerializedName;

// POJO for one A/V track inside MetadataResponse
public class TrackInfo {

    @SerializedName("type")
    public String type;

    @SerializedName("id")
    public String id;

    @SerializedName("language")
    public String language;

    @SerializedName("width")
    public int width;

    @SerializedName("height")
    public int height;

    @SerializedName("codec")
    public String codec;
}