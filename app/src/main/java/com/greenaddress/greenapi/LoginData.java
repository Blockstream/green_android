package com.greenaddress.greenapi;

import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    public final String exchange;
    public final String currency;
    public final Map<String, Object> appearance;
    public final String nlocktime_blocks;
    public final Map<String, String> limits;
    public final Map<String, String> privacy;
    public final String cache_password;
    public final Map<String, String> last_login;
    public final ArrayList expired_deposits;
    public final ArrayList subaccounts;
    public final String receiving_id;
    public final String country;
    public String gait_path;  // can change on first login (registration)

    public LoginData(final Map<?, ?> map) throws IOException {
        this.exchange = (String) map.get("exchange");
        this.currency = (String) map.get("currency");
        this.appearance = new MappingJsonFactory().getCodec().readValue(
                (String) map.get("appearance"), Map.class);
        this.nlocktime_blocks = map.get("nlocktime_blocks").toString();
        this.limits = (Map<String, String>) map.get("limits");
        this.privacy = (Map<String, String>) map.get("privacy");
        this.cache_password = (String) map.get("cache_password");
        this.last_login = (Map<String, String>) map.get("last_login");
        this.expired_deposits = (ArrayList) map.get("expired_deposits");
        this.subaccounts = (ArrayList) map.get("subaccounts");
        this.gait_path = (String) map.get("gait_path");
        this.receiving_id = (String) map.get("receiving_id");
        this.country = (String) map.get("country");
    }
}
