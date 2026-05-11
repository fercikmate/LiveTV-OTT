package com.kafica.ott.tis;

import com.kafica.ott.client.OttClient;
import com.kafica.ott.client.ProxyUrlBuilder;
import com.kafica.ott.model.CatalogResponse;
import com.kafica.ott.model.MetadataResponse;
import com.kafica.ott.model.ProgramInfo;
import com.kafica.ott.model.ServiceInfo;
import com.kafica.ott.parser.CatalogParser;
import com.kafica.ott.parser.MetadataParser;
import com.kafica.ott.repository.OttChannelRepository;
import android.content.Context;

// Maps to OTT Control block in diagram
// Orchestrates the full installation sequence from your diagram
// Must be called from a background thread
public class OttInstallController {

    public interface InstallCallback {
        void onProgress(int processed, int total, String serviceId);
        void onServiceSkipped(String serviceId);
        void onCompleted(int serviceCount);
        void onCatalogError(String error);
        void onTvProviderError(String error);
    }

    private final OttClient client;
    private final Context context;
    private final CatalogParser catalogParser;
    private final MetadataParser metadataParser;
    private final OttChannelRepository repository;
    private final InstallCallback callback;

    public OttInstallController(Context context,
                                OttChannelRepository repository,
                                InstallCallback callback) {
        this.context = context;
        this.client = new OttClient(context);
        this.catalogParser = new CatalogParser();
        this.metadataParser = new MetadataParser();
        this.repository = repository;
        this.callback = callback;
    }

    // startInstallation(request) — entry point from diagram
    public void startInstallation() {

        // requestServiceCatalog → HTTP GET /catalog → parseCatalog
        CatalogResponse catalog;
        try {
            String json = client.fetchCatalog();
            catalog = catalogParser.parse(json);
        } catch (Exception e) {
            // [catalog request failed] alt frame
            callback.onCatalogError(e.getMessage());
            return;
        }

        int total = catalog.services.size();

        // loop [for each discovered service]
        for (int i = 0; i < catalog.services.size(); i++) {
            ServiceInfo basic = catalog.services.get(i);

            // requestMetadata → HTTP GET /metadata/{serviceId}
            // → parseServiceInfo
            MetadataResponse metadata;
            try {
                String json = client.fetchMetadata(basic.serviceId);
                metadata = metadataParser.parse(json);
            } catch (Exception e) {
                // [metadata request failed] alt frame — skip, continue loop
                callback.onServiceSkipped(basic.serviceId);
                continue;
            }

            // map MetadataResponse → ServiceInfo
            ServiceInfo full = new ServiceInfo();
            full.serviceId = metadata.serviceId;
            full.name = metadata.name;
            full.type = metadata.type;
            full.streamUri = ProxyUrlBuilder.wrap(metadata.streamUri);
            full.metadataUri = metadata.metadataUri;
            full.logoUri = metadata.logoUri;
            full.description = metadata.description;
            full.rating = metadata.rating != null ? metadata.rating : "TV-G";
            full.broadcastOnid = metadata.broadcastOnid;
            full.broadcastTsid = metadata.broadcastTsid;
            full.broadcastSid = metadata.broadcastSid;

            try {
                // assignChannelNumber — index based
                String displayNumber = String.valueOf(i + 1);

                // insertOrUpdateChannel(...)
                long channelId = repository.insertChannel(full, displayNumber);

                // insertOrUpdateProgram(...)
                // channelId from above is mandatory
                ProgramInfo program = new ProgramInfo(
                        channelId,
                        full.name,
                        full.description != null ? full.description : "",
                        System.currentTimeMillis(),
                        System.currentTimeMillis() + 3600000L,
                        full.rating
                );
                repository.insertProgram(program);

            } catch (Exception e) {
                // [TV Provider insert failed] alt frame — fatal, abort
                callback.onTvProviderError(e.getMessage());
                return;
            }

            // installationProgress(processed, totalServices, serviceId)
            callback.onProgress(i + 1, total, basic.serviceId);
        }

        // installationCompleted(status=SUCCESS, serviceCount)
        callback.onCompleted(total);
    }
}