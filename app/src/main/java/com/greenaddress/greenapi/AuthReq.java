package com.greenaddress.greenapi;


public class AuthReq {
    public final String token;
    public final String challenge;

    public AuthReq(final String token, final String challenge) {
        this.token = token;
        this.challenge = challenge;
    }
}
