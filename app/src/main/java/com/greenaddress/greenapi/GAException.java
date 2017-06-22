package com.greenaddress.greenapi;

public class GAException extends Exception {
    public static final String AUTH = "#auth";
    public static final String INTERNAL = "#internal";

    public final String mUri;
    public final String mMessage;

    public GAException(final String uri, final String message) {
        super(message);
        mUri = uri.replaceAll(".*#", "#");
        mMessage = message;
    }

    public GAException(final String message) {
        super(message);
        mUri = INTERNAL;
        mMessage = message;
    }
}
