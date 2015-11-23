package com.greenaddress.greenapi;

class SetPinData {
    public final byte[] json;
    public final String ident;

    public SetPinData(final byte[] json, final String ident) {
        this.json = json;
        this.ident = ident;
    }
}