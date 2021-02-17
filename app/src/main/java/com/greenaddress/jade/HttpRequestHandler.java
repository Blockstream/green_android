package com.greenaddress.jade;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

// HttpRequestHandler is used for network calls during pinserver handshake
// useful on TOR enabled sessions
public interface HttpRequestHandler{
    JsonNode httpRequest(final JsonNode details) throws IOException;
}
