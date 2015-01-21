package com.greenaddress.greenapi;

public class PinData {
    public final String ident, encrypted;

    public PinData(final String ident, final String encrypted) {
        this.ident = ident;
        this.encrypted = encrypted;
    }
}
