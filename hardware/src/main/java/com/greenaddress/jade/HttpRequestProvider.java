package com.greenaddress.jade;

// Provides an HttpRequestHandler every time is required
// A provider pattern is usefull when support for multi-session are enabled.
public interface HttpRequestProvider{
    HttpRequestHandler getHttpRequest();
}
