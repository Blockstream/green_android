package com.greenaddress.greenapi;

import org.codehaus.jackson.map.MappingJsonFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

public class LoginData {
    public final String exchange;
    public final String currency;
    public final Map<String, Object> appearance;
    public final ArrayList subaccounts;
    public final String receiving_id;
    public String gait_path;  // can change on first login (registration)

    public LoginData(final Map<?, ?> map) throws IOException {
        this.exchange = (String) map.get("exchange");
        this.currency = (String) map.get("currency");
        this.appearance = new MappingJsonFactory().getCodec().readValue(
                (String) map.get("appearance"), Map.class);
        this.subaccounts = (ArrayList) map.get("subaccounts");
        this.gait_path = (String) map.get("gait_path");
        this.receiving_id = (String) map.get("receiving_id");
    }
}
