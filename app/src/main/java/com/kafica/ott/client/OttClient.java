package com.kafica.ott.client;

import android.content.Context;
import com.kafica.ott.R;
import com.kafica.ott.util.JsonResourceHelper;
import java.io.IOException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// Maps to OTT Client / HTTP TCP Network Stack blocks in diagram
// Responsible for all raw HTTP GET calls
// Returns raw JSON strings — parsing is done in parser package
public class OttClient {

    // Toggle this to true for local testing
    private boolean isLocalMode = false;
    // replace with your actual server IP
    private static final String BASE_URL ="http://172.31.10.223:3000"; // "http://10.0.2.2:3000";
    private final Context context;

    private final OkHttpClient httpClient;


    public OttClient(Context context) {
        this.context = context;
        this.httpClient = new OkHttpClient();
    }

    // HTTP GET /catalog
    public String fetchCatalog() throws IOException {
        if (isLocalMode) {
            // Reads from res/raw/catalog.json
            return JsonResourceHelper.getJsonFromRaw(context, R.raw.catalog);
        }
        return get(BASE_URL + "/catalog");
    }

    // HTTP GET /metadata/{serviceId}
    public String fetchMetadata(String serviceId) throws IOException {
        if (isLocalMode) {
            // For local testing, you can use one generic metadata file
            // or use a switch statement to return different files per serviceId
            return JsonResourceHelper.getJsonFromRaw(context, R.raw.metadata_sample);
        }
        return get(BASE_URL + "/metadata/" + serviceId);
    }

    // shared GET — throws IOException on any non-2xx
    private String get(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException(
                        "HTTP error " + response.code() + " for " + url);
            }
            return response.body().string();
        }
    }
}