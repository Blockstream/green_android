package com.greenaddress.greenapi.data;

import android.content.SharedPreferences;
import android.util.Base64;

import com.blockstream.libwally.Wally;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class PinData extends JSONData {
    private String salt;
    private String encryptedData;
    private String pinIdentifier;

    @JsonIgnore
    public static PinData fromPreferenceValues(final SharedPreferences pin) {
        final PinData pinData = new PinData();
        pinData.setPinIdentifier(pin.getString("ident", null));
        final String[] split = pin.getString("encrypted", null).split(";");
        final byte[] encryptedData = Base64.decode(split[1], Base64.NO_WRAP);
        pinData.setSalt(split[0]);
        pinData.setEncryptedData(Wally.hex_from_bytes(encryptedData));
        return pinData;
    }

    @JsonIgnore
    public String getEncryptedGB() {
        return String.format("%s;%s", salt, Base64.encodeToString( Wally.hex_to_bytes(encryptedData), Base64.NO_WRAP));
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(final String salt) {
        this.salt = salt;
    }

    public String getEncryptedData() {
        return encryptedData;
    }

    public void setEncryptedData(final String encryptedData) {
        this.encryptedData = encryptedData;
    }

    public String getPinIdentifier() {
        return pinIdentifier;
    }

    public void setPinIdentifier(final String pinIdentifier) {
        this.pinIdentifier = pinIdentifier;
    }

}
