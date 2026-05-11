package com.kafica.ott.parser;

import com.google.gson.Gson;
import com.kafica.ott.model.MetadataResponse;

// Maps to Metadata Processing block in diagram
// Parses raw /metadata/{serviceId} JSON into MetadataResponse POJO
public class MetadataParser {

    private final Gson gson = new Gson();

    // parseServiceInfo(serviceId, metadataJson) from diagram
    public MetadataResponse parse(String json) {
        return gson.fromJson(json, MetadataResponse.class);
    }
}