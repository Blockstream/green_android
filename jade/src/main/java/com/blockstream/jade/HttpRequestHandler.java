package com.blockstream.jade;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URL;
import java.util.List;

import kotlinx.serialization.json.JsonElement;

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
public interface HttpRequestHandler{

    void prepareHttpRequest();
    JsonElement httpRequest(final JsonElement details) throws IOException;

    JsonElement httpRequest(final String method,
                                final List<URL> urls,
                                final String data,
                                final String accept,
                                final List<String> certs) throws IOException;
}
