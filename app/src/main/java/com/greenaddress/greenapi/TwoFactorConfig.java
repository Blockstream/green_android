package com.greenaddress.greenapi;

import java.util.Map;

public class TwoFactorConfig {
    public final Boolean gauth;
    public final Boolean sms;
    public final String email_addr;
    public final Boolean any;
    public final Boolean phone;
    public final String gauth_url;
    public final Boolean email_confirmed;
    public final Boolean email;

    public TwoFactorConfig(final Map<?, ?> map) {
        this.gauth = (Boolean) map.get("gauth");
        this.sms = (Boolean) map.get("sms");
        this.email_addr = (String) map.get("email_addr");
        this.any = (Boolean) map.get("any");
        this.phone = (Boolean) map.get("phone");
        this.email = (Boolean) map.get("email");
        this.email_confirmed = (Boolean) map.get("email_confirmed");
        this.gauth_url = (String) map.get("gauth_url");
    }
}