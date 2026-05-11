package com.kafica.ott.model;

import com.google.gson.annotations.SerializedName;
import java.util.List;

// POJO for /catalog HTTP response
public class CatalogResponse {

    @SerializedName("services")
    public List<ServiceInfo> services;
}