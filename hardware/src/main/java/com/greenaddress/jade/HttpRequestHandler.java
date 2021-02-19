package com.greenaddress.jade;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.net.URL;
import java.util.List;

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
public interface HttpRequestHandler{
    JsonNode httpRequest(final JsonNode details) throws IOException;

    JsonNode httpRequest(final String method,
                                final List<URL> urls,
                                final String data,
                                final String accept,
                                final List<String> certs) throws IOException;
}
