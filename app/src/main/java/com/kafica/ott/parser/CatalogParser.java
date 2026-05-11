package com.kafica.ott.parser;

import com.google.gson.Gson;
import com.kafica.ott.model.CatalogResponse;

// Maps to Metadata Processing block in diagram
// Parses raw /catalog JSON into CatalogResponse POJO
public class CatalogParser {

    private final Gson gson = new Gson();

    // parseCatalog(catalogJson) from diagram
    public CatalogResponse parse(String json) {
        return gson.fromJson(json, CatalogResponse.class);
    }
}