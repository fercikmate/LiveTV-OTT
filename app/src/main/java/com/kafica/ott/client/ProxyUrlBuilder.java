package com.kafica.ott.client;

// Wraps any stream URI through the local HLS proxy
// Proxy rewrites all nested playlist and segment URLs automatically
public class ProxyUrlBuilder {

    private static final String PROXY_BASE = "http://172.31.10.223:8080/proxy?url=";

    // wraps a raw stream URI through the proxy
    public static String wrap(String streamUri) {
        if (streamUri == null || streamUri.isEmpty()) return streamUri;
        // avoid double-wrapping if already proxied
        if (streamUri.startsWith(PROXY_BASE)) return streamUri;
        return PROXY_BASE + streamUri;
    }
}